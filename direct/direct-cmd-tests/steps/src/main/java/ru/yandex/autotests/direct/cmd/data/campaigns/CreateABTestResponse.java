package ru.yandex.autotests.direct.cmd.data.campaigns;

import com.google.gson.annotations.SerializedName;

public class CreateABTestResponse {
    @SerializedName("data")
    private DataAB data;

    @SerializedName("error")
    private String error;

    @SerializedName("success")
    private Integer success;

    public String getError() {
        return error;
    }

    public CreateABTestResponse withError(String error) {
        this.error = error;
        return this;
    }

    public Integer getSuccess() {
        return success;
    }

    public CreateABTestResponse withSuccess(Integer success) {
        this.success = success;
        return this;
    }

    public DataAB getData() {
        return data;
    }

    public CreateABTestResponse withData(DataAB data) {
        this.data = data;
        return this;
    }
}
