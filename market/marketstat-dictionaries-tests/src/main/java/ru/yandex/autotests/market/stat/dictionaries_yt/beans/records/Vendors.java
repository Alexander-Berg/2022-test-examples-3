package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.clickHouse.annotations.ClickHouseField;

import static ru.yandex.autotests.market.stat.attribute.Fields.ID;
import static ru.yandex.autotests.market.stat.attribute.Fields.NAME;

/**
 * @author Alexander Gavrikov <agavrikov@yandex-team.ru>
 */
@Data
@DictTable(name = "vendors")
public class Vendors implements DictionaryRecord {
    @DictionaryIdField
    @ClickHouseField(ID)
    private String id; // in hive is bigint

    @RequiredField
    @ClickHouseField(NAME)
    private String name; // in hive is string

}
