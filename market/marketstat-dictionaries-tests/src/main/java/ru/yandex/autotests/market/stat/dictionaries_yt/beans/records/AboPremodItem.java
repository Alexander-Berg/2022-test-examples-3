package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "premod_item")
public class AboPremodItem implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Long */
    private String ticket_id; /* Long */
    private String type_id; /* Integer */
    private LocalDateTime creation_time; /* Timestamp */
    private LocalDateTime modification_time; /* Timestamp */
    private String status_id; /* BigDecimal */
    private String ya_uid; /* BigDecimal */
    private String user_comment; /* String */
}
