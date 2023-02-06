package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "recheck_problem")
public class AboRecheckProblem implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* BigDecimal */
    private String type_id; /* BigDecimal */
    private String ticket_id; /* BigDecimal */
    private String user_id; /* BigDecimal */
    private LocalDateTime created; /* Timestamp */
    private String text; /* String */
}
