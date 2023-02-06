package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

/**
 * @author aostrikov
 */
@Data
@DictTable(name = "open_cutoff")
public class ShopOpenCutoff implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Actually long */
    @RequiredField
    private String datasource_id; /* Actually long */
    @RequiredField
    private String type; /* Actually long */
    private LocalDateTime from_time; /* Actually timestamp */
    private String comment; /* Actually string */
}
