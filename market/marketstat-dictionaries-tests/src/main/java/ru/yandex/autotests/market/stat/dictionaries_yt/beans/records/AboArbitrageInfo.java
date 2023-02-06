package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "arbitrage_info")
public class AboArbitrageInfo implements DictionaryRecord {
    @DictionaryIdField
    private String conversation_id;
    private String shop_id;
    private String user_id;
    private String status_id;
    private LocalDateTime creation_time;
    private String unread;
    private String overdue;
}
