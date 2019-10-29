
# send_file Introduce

一个使用 NIO + selector + send file 技术的 server + client ，专门用于服务器之间搬运文件。

# quick start

1. 打开 example module src 目录.
2. 运行 example.ServerDemo
3. 运行 example.ClientDemo

注意: 需要注意 demo 里的目录路径,应该和你的不一致,改成自己的即可,只要这个目录下有文件,就可以.但是文件别太多,否则会写爆磁盘.

# 项目结构

* send_file_client 客户端模块, 
* send_file_common 通用模块
* send_file_server 服务端模块.
* example 使用例子. 

# 线程 模型



![image-20191029093524267](https://tva1.sinaimg.cn/large/006y8mN6ly1g8etjmkuejj30vv0u0wg2.jpg)



如上图所示。

#### 在 client 端：

 每个 Client 实例，维护一个 TCP 连接。该 Client 的写入方法是线程安全的。

当用户并发写入时，可并发写，并发回复，因为写和回复是异步的。

#### 在 server 端：

server 端维护着一个 ServerSocketChannel 实例，该实例的作用就是接收 accep 事件，且由一个线程维护这个 accept selector 。



当有新的 client 连接事件时，accept selector 就将这个连接交给 read 线程（默认 server 有 4 个 read 线程）。



#### 什么是“交给”？



注意：每个 read 线程都维护着一个单独的 selector。 4 个 read 线程，就维护了 4 个 selector。



当 accept 得到新的客户端连接时，先从 4 个read 线程组里 get 一个线程，然后将这个 客户端连接 作为 key 注册到这个线程所对应的 read selector 上。从而将这个 Socket “交给” read 线程。



而这个 read 线程则使用这个 selector 轮询事件，如果 socket 可读，那么就进行读，读完之后，利用 DMA 写进磁盘。



# IO 模型

![](https://tva1.sinaimg.cn/large/006y8mN6ly1g8esdrp8fhj30zr0u00u2.jpg)



如上图，Server 端支持海量客户端连接。



server 端含有 多个处理器，其中包括 accept 处理器，read 处理器组， write 处理器组。



accept 处理器将 serverSocketChannel 作为 key 注册到一个单独的 selector 上。专门用于监听 accept 事件。类似 netty 的 boss 线程。



当 accept 处理器成功连接了一个 socket 时，会随机将其交给一个 readProcessor 处理器，readProcessor 又会将其注册到 readSelector 上，当发生 read 事件时，readProcessor 将接受数据。



可以看到，readProcessor 可以认为是一个多路复用的线程，利用 selector 的能力，他管理着多个 socket。



readProcessor 在读到数据后，会将其写入到磁盘中（DMA 的方式，节省 CPU）。



然后，如果 client 在 RPC 协议中声明“需要回复（id 不为 -1）” 时，那就将结果发送到 Reply Queue 中，反之不必。



当结果发送到  Reply Queue 后，writer 组中的 写线程，则会从 Queue 中拉取回复包，然后将结果按照 RPC 协议，写回到 client socket 中。



client socket 也会监听着 read 事件，注意：client 是不需要 select 的，因为没必要，selector 只是性能优化的一种方式——即一个线程管理海量连接，如果没有 select， 应用层无法用较低的成本处理海量连接。



回过来，当 client socket  得到 server 的数据包，会进行解码反序列化，并唤醒阻塞在客户端的线程。从而完成一次调用。



# RPC 协议

#### Server RPC 回复包协议

| 字段名称  | 字段长度(byte) | 字段作用                          |
| --------- | -------------- | --------------------------------- |
| magic_num | 4              | 魔数校验，fast fail               |
| version   | 1              | rpc 协议版本                      |
| id        | 8              | Request id， TCP 多路复用 id      |
| length    | 8              | rpc 实际消息内容的长度            |
| Content   | length         | rpc 实际消息内容(JSON 序列化协议) |



#### Client RPC 发送包协议

| 字段名称    | 字段长度(byte) | 字段作用                                          |
| ----------- | -------------- | ------------------------------------------------- |
| magic_num   | 4              | 魔数校验，fast fail                               |
| id          | 8              | Request id， TCP 多路复用 id, 默认 -1，表示不回复 |
| nameContent | 2              | Request id， TCP 多路复用 id                      |
| bodyLength  | 8              | rpc 实际消息内容的长度                            |
| nameContent | bodyLength     | 文件名 UTF-8 数组                                 |



