package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "clch_cluster")
public class AboClchCluster implements DictionaryRecord {
    @DictionaryIdField
    private String id;
    private String shop_set_id;
    private LocalDateTime creation_time;
    private LocalDateTime deletion_time;
    private String user_id;
    private String status;
}
