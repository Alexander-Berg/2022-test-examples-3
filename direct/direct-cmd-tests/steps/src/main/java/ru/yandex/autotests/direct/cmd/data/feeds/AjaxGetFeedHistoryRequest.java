package ru.yandex.autotests.direct.cmd.data.feeds;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxGetFeedHistoryRequest extends BasicDirectRequest {

    @SerializeKey("feed_id")
    private String feedId;

    public String getFeedId() {
        return feedId;
    }

    public AjaxGetFeedHistoryRequest withFeedId(String feedId) {
        this.feedId = feedId;
        return this;
    }
}
