package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "core_ticket_status")
public class AboCoreTicketStatus implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Integer */
    private String name; /* String */
}
