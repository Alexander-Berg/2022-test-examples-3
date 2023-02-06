package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.clickHouse.annotations.ClickHouseField;

import static ru.yandex.autotests.market.stat.attribute.Fields.DOMAIN;
import static ru.yandex.autotests.market.stat.attribute.Fields.SHOP_ID;

/**
 * Created by kateleb on 22.09.16
 */
@Data
@DictTable(name = "domains")
public class Domains implements DictionaryRecord {
    @DictionaryIdField
    @ClickHouseField(SHOP_ID)
    private String shop_id; // in hive is bigint

    @ClickHouseField(DOMAIN)
    @RequiredField
    private String domain; // in hive is string

}
