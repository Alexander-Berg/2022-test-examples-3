package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {

    @SerializedName("error")
    String error;

    @SerializedName("cmd")
    private String cmd;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public ErrorResponse withError(String error) {
        this.error = error;
        return this;
    }

    public String getCmd() {
        return cmd;
    }

    public ErrorResponse withCmd(String cmd) {
        this.cmd = cmd;
        return this;
    }
}
