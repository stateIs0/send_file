package cn.thinkinjava.send.file.common;

import cn.thinkinjava.send.file.common.util.JackSonUtil;

/**
 * RPC 数据包.
 */
public class RpcPacket {

    /**
     * 保留字段
     */
    private byte version = 1;
    /**
     * Request ID.
     */
    private long id;
    /**
     * 数据内容. JSON + UTF-8 String 编码.
     */
    private byte[] content;

    public RpcPacket(byte version, long id, byte[] content) {
        this.version = version;
        this.id = id;
        this.content = content;
    }

    public RpcPacket(long id, byte[] content) {
        this.id = id;
        this.content = content;
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "RpcPacket{" +
                "version=" + version +
                ", id=" + id +
                ", content=" + JackSonUtil.string2Obj(new String(content), SendResult.class) +
                '}';
    }
}
