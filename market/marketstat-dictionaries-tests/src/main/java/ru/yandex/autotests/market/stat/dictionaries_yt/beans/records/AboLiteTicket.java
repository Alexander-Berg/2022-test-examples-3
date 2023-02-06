package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "lite_ticket")
public class AboLiteTicket implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* BigDecimal */
    private LocalDateTime creation_time; /* Timestamp */
    private LocalDateTime modification_time; /* Timestamp */
    private String status_id; /* BigDecimal */
    private String shop_id; /* BigDecimal */
    private String mbi_message; /* String */
    private String user_comment; /* String */
    private String finished; /* Boolean */
    private String check_method_id; /* BigDecimal */
    private String phone_num; /* String */
    private String employee_name; /* String */
}
