package cn.thinkinjava.send.file.common;

import java.nio.ByteBuffer;

/**
 * 包编解码器.
 */
public class PacketCodec {

    public static ByteBuffer encode(RpcPacket packet) {
        long length = packet.getContent().length;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int) (4 + 1 + 8 + 8 + length));
        // 魔数 4
        // 版本 1
        // request id. 8
        // json 长度 8
        // json 内容 un know
        byteBuffer.putInt(MagicNum.INSTANCE.getNum());
        byteBuffer.put(packet.getVersion());
        byteBuffer.putLong(packet.getId());
        byteBuffer.putLong(length);
        byteBuffer.put(packet.getContent());
        return byteBuffer;
    }

    public static RpcPacket decode(ByteBuffer byteBuffer) {
        if (byteBuffer.hasRemaining()) {
            int magic_num = byteBuffer.getInt();
            if (magic_num != MagicNum.INSTANCE.getNum()) {
                throw new IllegalArgumentException("magic num not match");
            }
            // 保留字段
            byte version = byteBuffer.get();
            long id = byteBuffer.getLong();
            long length = byteBuffer.getLong();
            if (id == -1) {
                return null;
            }
            byte[] arr = new byte[(int) length];
            byteBuffer.get(arr);

            RpcPacket packet = new RpcPacket(version, id, arr);
            return packet;
        }
        return null;
    }

}
