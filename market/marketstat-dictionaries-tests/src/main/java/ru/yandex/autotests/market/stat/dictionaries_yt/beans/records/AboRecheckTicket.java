package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "recheck_ticket")
public class AboRecheckTicket implements DictionaryRecord {
    @DictionaryIdField
    private String id; /*BigDecimal*/
    private String type_id; /*BigDecimal*/
    private String shop_id; /*BigDecimal*/
    private String check_item_id; /*BigDecimal*/
    private String status_id; /*BigDecimal*/
    private String ya_uid; /*BigDecimal*/
    private String synopsis; /*String*/
    private String result_comment; /*String*/
    private LocalDateTime creation_time; /*Timestamp*/
    private LocalDateTime modification_time; /*Timestamp*/
    private String check_method_id; /*BigDecimal*/
}
