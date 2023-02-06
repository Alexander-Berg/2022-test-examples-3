package ru.yandex.autotests.direct.cmd.data.conditions;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxGetFilterSchemaRequest extends BasicDirectRequest {

    @SerializeKey("filter_type")
    private String filterType;

    public String getFilterType() {
        return filterType;
    }

    public AjaxGetFilterSchemaRequest withFilterType(String filterType) {
        this.filterType = filterType;
        return this;
    }
}
