package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

/**
 * @author aostrikov
 */
@Data
@DictTable(name = "goods_ad_budget")
public class GoodsAdBudget implements DictionaryRecord {
    @DictionaryIdField
    private String geo_group_id; /* Actually long */
    @DictionaryIdField
    private String object_type_id; /* Actually long */
    @DictionaryIdField
    private String object_id; /* Actually long */
    @RequiredField
    private String value; /* Actually timestamp */

}
