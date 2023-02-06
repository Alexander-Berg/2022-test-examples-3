package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "clch_shop_favourite")
public class AboClchShopFavourite implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Actualy Long */
    private String shop_set_id; /* Actualy Long */
    private String shop_id; /* Actualy BigDecimal */
}
