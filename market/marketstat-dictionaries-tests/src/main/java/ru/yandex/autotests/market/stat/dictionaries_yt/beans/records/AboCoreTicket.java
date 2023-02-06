package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "core_ticket")
public class AboCoreTicket implements DictionaryRecord {
    @DictionaryIdField
    private String hyp_id; /* Actualy Long */
    private String offer_id; /* Actualy Long */
    private LocalDateTime creation_time; /* Actualy Timestamp */
    private LocalDateTime modification_time; /* Actualy Timestamp */
    private String region; /* Actualy BigDecimal */
    private String check_method_id; /* Actualy Integer */
    private String status_id; /* Actualy Integer */
    private String user_comment; /* Actualy String */
    private String creation_tag_id; /* Actualy Long */
    private String modification_tag_id; /* Actualy Long */
    private String delivery; /* Actualy BigDecimal */
}
