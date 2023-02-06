package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "premod_ticket")
public class AboPremodTicket implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Long */
    private LocalDateTime creation_time; /* Timestamp */
    private LocalDateTime modification_time; /* Timestamp */
    private String status_id; /* Integer */
    private String substatus_id; /* Integer */
    private String check_type; /* BigDecimal */
    private String shop_id; /* BigDecimal */
    private String recommendation; /* String */
    private String cpa_recommendation; /* String */
    private String user_comment; /* String */
    private String try_number; /* BigDecimal */
    private String prev_id; /* Long */
    private String rec_sender_id; /* BigDecimal */
    private String check_method_id; /* Integer */
}
