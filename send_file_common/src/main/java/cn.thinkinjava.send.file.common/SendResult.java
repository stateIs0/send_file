package cn.thinkinjava.send.file.common;

/**
 * 发送结果.
 */
public class SendResult {

    private static SendResult sendResult = new SendResult();

    private boolean success = false;
    /**
     * 写出的文件字节数.不包括文件名称.
     */
    private long outBytesLength = -1;

    /**
     * 服务器的具体文件地址.注意: one way 时此值为空.
     */
    private String fileAddr = null;

    public SendResult() {
    }

    public static SendResult failInstance() {
        return sendResult;
    }

    public SendResult(boolean success, long outBytesLength) {
        this.success = success;
        this.outBytesLength = outBytesLength;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getOutBytesLength() {
        return outBytesLength;
    }

    public void setOutBytesLength(long outBytesLength) {
        this.outBytesLength = outBytesLength;
    }

    public String getFileAddr() {
        return fileAddr;
    }

    public void setFileAddr(String fileAddr) {
        this.fileAddr = fileAddr;
    }

    @Override
    public String toString() {
        return "SendResult{" +
                "success=" + success +
                ", outBytesLength=" + outBytesLength +
                ", fileAddr='" + fileAddr + '\'' +
                '}';
    }
}
