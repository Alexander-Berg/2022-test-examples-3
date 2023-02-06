package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "premod_item_type")
public class AboPremodItemType implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Integer */
    private String name; /* String */
    private String order_in_recommendation; /* BigDecimal */
    private String recommendation_header; /* String */
    private String check_always; /* Boolean */
    private String group_id; /* BigDecimal */
    private String critical; /* BigDecimal */
    private String hidden; /* Boolean */
    private String for_offline; /* Boolean */
    private String description; /* String */
    private String relates_to_placement; /* String */
    private String position; /* BigDecimal */
}
