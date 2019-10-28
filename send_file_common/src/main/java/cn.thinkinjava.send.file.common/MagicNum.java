package cn.thinkinjava.send.file.common;

/**
 * sendFile 的 rpc 协议魔数.
 */
public enum MagicNum {

    INSTANCE(422);

    int num;

    MagicNum(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }
}
