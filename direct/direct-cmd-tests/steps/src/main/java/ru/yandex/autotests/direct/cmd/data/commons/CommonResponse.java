package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;

public class CommonResponse {

    public static final String RESULT_OK = "ok";
    public static final String RESULT_ERROR = "error";

    @SerializedName("result")
    private String result;

    @SerializedName("success")
    private String success;

    @SerializedName("status")
    private String status;

    @SerializedName("failed")
    private String failed;

    @SerializedName("ok")
    private String ok;

    public String getOk() {
        return ok;
    }

    public CommonResponse withOk(String ok) {
        this.ok = ok;
        return this;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String getStatus() {
        return status;
    }

    public String getFailed() {
        return failed;
    }

    public void setFailed(String failed) {
        this.failed = failed;
    }
}
