package ru.yandex.autotests.direct.cmd.data.counters;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxCheckUserCountersRequest extends BasicDirectRequest {

    @SerializeKey("counter")
    private Long counter;

    public Long getCounter() {
        return counter;
    }

    public void setCounter(Long counter) {
        this.counter = counter;
    }

    public AjaxCheckUserCountersRequest withCounter(Long counter) {
        this.counter = counter;
        return this;
    }
}
