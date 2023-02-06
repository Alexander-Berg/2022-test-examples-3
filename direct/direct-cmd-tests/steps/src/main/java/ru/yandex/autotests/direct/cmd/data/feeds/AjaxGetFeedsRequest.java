package ru.yandex.autotests.direct.cmd.data.feeds;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

/**
 * Created by aleran on 02.09.2015.
 */
public class AjaxGetFeedsRequest extends BasicDirectRequest {

    @SerializeKey("page")
    private Integer page;

    @SerializeKey("count")
    private Integer count;

    @SerializeKey("reverse")
    private Integer reverse;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getReverse() {
        return reverse;
    }

    public void setReverse(Integer reverse) {
        this.reverse = reverse;
    }
}
