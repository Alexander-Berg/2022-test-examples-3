package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "premod_problem")
public class AboPremodProblem implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Long */
    private String item_id; /* Long */
    private String problem_type_id; /* Integer */
    private LocalDateTime creation_time; /* Timestamp */
    private String ya_uid; /* BigDecimal */
    private String user_comment; /* String */
}
