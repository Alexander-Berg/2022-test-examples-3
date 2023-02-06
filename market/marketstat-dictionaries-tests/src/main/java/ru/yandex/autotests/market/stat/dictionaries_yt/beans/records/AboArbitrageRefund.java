package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "arbitrage_refund")
public class AboArbitrageRefund implements DictionaryRecord {
    @DictionaryIdField
    private String id;
    private String order_id;
    private String amount;
    private String refund_comment;
    private String arbiter_uid;
    private String superarbiter_uid;
    private LocalDateTime created_time;
    private LocalDateTime updated_time;
    private String status;
}
