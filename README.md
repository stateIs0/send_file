
# send_file

一个使用 NIO + send file 技术的 server + client ，专门用于服务器之间搬运文件。

# 项目结构

* send_file_client client 模块, 
* send_file_common 通用模块
* send_file_server server 端模块.
* example 使用例子. 

# IO 模型


# 线程模型

# RPC 协议

#### Server RPC 回复包协议

| 字段名称  | 字段长度(byte) | 字段作用                     |
| --------- | -------------- | ---------------------------- |
| magic_num | 4              | 魔数校验，fast fail          |
| version   | 1              | rpc 协议版本                 |
| id        | 8              | Request id， TCP 多路复用 id |
| length    | 8              | rpc 实际消息内容的长度       |
| arr       | length         | rpc 实际消息内容             |



#### Client RPC 发送包协议

| 字段名称    | 字段长度(byte) | 字段作用                                          |
| ----------- | -------------- | ------------------------------------------------- |
| magic_num   | 4              | 魔数校验，fast fail                               |
| id          | 8              | Request id， TCP 多路复用 id, 默认 -1，表示不回复 |
| nameContent | 2              | Request id， TCP 多路复用 id                      |
| bodyLength  | 8              | rpc 实际消息内容的长度                            |
| nameContent | bodyLength     | 文件名 UTF-8 数组                                 |


