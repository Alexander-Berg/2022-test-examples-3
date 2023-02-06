package ru.yandex.autotests.direct.cmd.data.stat.filter;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class DeleteStatFilterRequest extends BasicDirectRequest {

    @SerializeKey("filter_name")
    private String filterName;

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public DeleteStatFilterRequest withFilterName(String filterName) {
        this.filterName = filterName;
        return this;
    }
}
