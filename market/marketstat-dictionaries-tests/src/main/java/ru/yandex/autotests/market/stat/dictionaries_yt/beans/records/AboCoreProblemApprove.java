package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;


@Data
@DictTable(name = "core_problem_approve")
public class AboCoreProblemApprove implements DictionaryRecord {
    @DictionaryIdField
    private String problem_id; /* Actualy Long */
    private LocalDateTime aprv_time;
    private LocalDateTime feed_time;
    private String aprv_price; /* Actualy Double*/
    private String feed_session; /* Actualy String*/
    private String feed_id; /* Actualy BigDecimal*/
    private String on_stock; /* Actualy Boolean*/
    private String raw_feed; /* Actualy Boolean*/
}
