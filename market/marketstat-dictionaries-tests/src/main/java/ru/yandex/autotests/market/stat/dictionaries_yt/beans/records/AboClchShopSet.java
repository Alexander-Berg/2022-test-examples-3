package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;


@Data
@DictTable(name = "clch_shop_set")
public class AboClchShopSet implements DictionaryRecord {
    @DictionaryIdField
    private String set_id;
    @DictionaryIdField
    private String shop_id;
    private String was_notified;
}
