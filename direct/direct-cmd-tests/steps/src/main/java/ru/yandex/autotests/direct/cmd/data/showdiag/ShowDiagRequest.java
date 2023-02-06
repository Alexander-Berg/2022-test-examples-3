package ru.yandex.autotests.direct.cmd.data.showdiag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ListToCommaSeparatedStringSerializer;

public class ShowDiagRequest {

    @SerializeKey("bid")
    private String bid;

    @SerializeKey("format")
    private String format;

    @SerializeKey("additions_item_id")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<Long> additionsItemsId;

    public List<Long> getAdditionsItemsId() {
        return additionsItemsId;
    }

    public ShowDiagRequest withAdditionsItemsId(Long... ids) {
        additionsItemsId = new ArrayList<>();
        Collections.addAll(additionsItemsId, ids);
        return this;
    }

    public String getFormat() {
        return format;
    }

    public ShowDiagRequest withFormat(String format) {
        this.format = format;
        return this;
    }

    public String getBid() {
        return bid;
    }

    public ShowDiagRequest withBid(String bid) {
        this.bid = bid;
        return this;
    }
}
