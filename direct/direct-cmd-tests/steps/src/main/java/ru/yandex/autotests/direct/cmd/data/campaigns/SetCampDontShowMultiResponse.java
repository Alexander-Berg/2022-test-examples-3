package ru.yandex.autotests.direct.cmd.data.campaigns;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SetCampDontShowMultiResponse {

    @SerializedName("response")
    private List<String> response;

    public List<String> getResponse() {
        return response;
    }
}
