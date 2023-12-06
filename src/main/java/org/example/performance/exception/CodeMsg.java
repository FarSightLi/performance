package org.example.performance.exception;

public enum CodeMsg {
    //成功
    SUCCESS(0, "操作成功"),
    TOKEN_ERROR(5000, "Token错误"),
    SYSTEM_ERROR(10001, "系统错误"),
    PARAMETER_ERROR(10008, "参数错误，请参考API文档");

    private int code;
    private String codeRemark;

    private CodeMsg() {
    }

    private CodeMsg(int code, String codeRemark) {
        this.code = code;
        this.codeRemark = codeRemark;
    }

    @Override
    public String toString() {
        return "CodeMsg [code=" + code + ", codeRemark=" + codeRemark + "]";
    }

    public int getCode() {
        return code;
    }

    public String getCodeRemark() {
        return codeRemark;
    }
}
