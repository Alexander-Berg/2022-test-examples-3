package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ErrorsResponse extends ErrorResponse {
    @SerializedName("errors")
    private List<String> commonErrors;

    public List<String>  getCommonErrors() {
        return commonErrors;
    }

    public ErrorResponse withCommonErrors(List<String>  errors) {
        this.commonErrors = errors;
        return this;
    }
}
