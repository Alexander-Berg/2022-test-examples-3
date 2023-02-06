package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "no_placement_record")
public class AboNoPlacementRecord implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Long */
    private String shop_id; /* Long */
    private String reason_id; /* Integer */
    private String user_id; /* Long */
    private String abo_cutoff_type; /* String */
    private LocalDateTime creation_time; /* Timestamp */
    private String premod_ticket_id; /* Long */
    private String closed; /* Boolean */
}
