package ru.yandex.autotests.direct.cmd.data.banners.additions;

import com.google.gson.annotations.SerializedName;

/*
* todo javadoc
*/
public class DeleteBannersAdditionsResponse {
    @SerializedName("success")
    private String success;

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }
}
