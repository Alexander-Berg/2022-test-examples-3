package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "internal_complaint")
public class AboInternalComplaint implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Long */
    private LocalDateTime create_time; /* Timestamp */
    private String author_id; /* Long */
    private String shop_id; /* Long */
    private String cmid; /* String */
    private String type; /* Integer */
    private String text; /* String */
    private String external; /* Boolean */
    private String ware_md5; /* String */
    private String status; /* Integer */
    private LocalDateTime mail_sent; /* Timestamp */
    private LocalDateTime offer_removed; /* Timestamp */
    private LocalDateTime modification_time; /* Timestamp */
    private String region_id; /* Long */
    private String session_id; /* String */
}
