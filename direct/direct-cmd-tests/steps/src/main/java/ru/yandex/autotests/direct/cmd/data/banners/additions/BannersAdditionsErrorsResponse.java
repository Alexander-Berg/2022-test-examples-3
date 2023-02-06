package ru.yandex.autotests.direct.cmd.data.banners.additions;

import com.google.gson.annotations.SerializedName;

/*
* todo javadoc
*/
public class BannersAdditionsErrorsResponse {

    @SerializedName("error")
    private String error;

    @SerializedName("success")
    private String success;

    @SerializedName("callouts")
    private CalloutsErrors callouts;

    public CalloutsErrors getCallouts() {
        return callouts;
    }

    public BannersAdditionsErrorsResponse withCallouts(CalloutsErrors callouts) {
        this.callouts = callouts;
        return this;
    }

    public String getError() {
        return error;
    }

    public String getSuccess() {
        return success;
    }
}
