package ru.yandex.autotests.direct.cmd.data.stat.filter;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

public class SaveStatFilterRequest extends BasicDirectRequest {

    @SerializeKey("json_filters_set")
    @SerializeBy(ValueToJsonSerializer.class)
    private JsonFiltersSet jsonFiltersSet;

    public JsonFiltersSet getJsonFiltersSet() {
        return jsonFiltersSet;
    }

    public void setJsonFiltersSet(JsonFiltersSet jsonFiltersSet) {
        this.jsonFiltersSet = jsonFiltersSet;
    }

    public SaveStatFilterRequest withJsonFiltersSet(JsonFiltersSet jsonFiltersSet) {
        this.jsonFiltersSet = jsonFiltersSet;
        return this;
    }
}
