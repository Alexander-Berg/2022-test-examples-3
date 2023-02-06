package ru.yandex.autotests.direct.cmd.data.feeds;

import org.apache.commons.lang3.StringUtils;
import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

import java.util.List;

/**
 * Created by aleran on 26.08.2015.
 */
public class AjaxDeleteFeedsRequest extends BasicDirectRequest {

    @SerializeKey("feeds_ids")
    private String feedsIds;

    public String getFeedsIds() {
        return feedsIds;
    }

    public void setFeedsIds(String feedsIds) {
        this.feedsIds = feedsIds;
    }

    public void setFeedsIds(String... feedsIds) {
        this.feedsIds = StringUtils.join(feedsIds, ',');
    }

    public void setFeedsIdsByString(List<String> feedsIds) {
        this.feedsIds = StringUtils.join(feedsIds, ',');
    }

    public void setFeedsIdsByLong(List<Long> feedsIds) {
        this.feedsIds = StringUtils.join(feedsIds, ',');
    }
}
