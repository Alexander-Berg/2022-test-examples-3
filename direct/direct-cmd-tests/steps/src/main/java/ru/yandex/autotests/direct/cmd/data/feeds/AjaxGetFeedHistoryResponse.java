package ru.yandex.autotests.direct.cmd.data.feeds;

import com.google.gson.annotations.SerializedName;

public class AjaxGetFeedHistoryResponse {

    @SerializedName("result")
    private AjaxGetFeedHistoryResult result;

    public AjaxGetFeedHistoryResult getResult() {
        return result;
    }

    public AjaxGetFeedHistoryResponse withResult(AjaxGetFeedHistoryResult result) {
        this.result = result;
        return this;
    }
}
