package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "internal_complaint_status")
public class AboInternalComplaintStatus implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Integer */
    private String description; /* String */
}
