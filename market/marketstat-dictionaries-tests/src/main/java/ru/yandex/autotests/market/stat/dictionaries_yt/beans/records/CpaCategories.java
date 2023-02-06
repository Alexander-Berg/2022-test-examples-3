package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

/**
 * Created by kateleb on 13.11.16
 */
@Data
@DictTable(name = "cpa_categories")
public class CpaCategories implements DictionaryRecord {
    @DictionaryIdField
    private String hyperId; // in hive is bigint

    @RequiredField
    private String fee; // in hive is int

    private String cpaType; // in hive is string

    private String regions; // in hive is array<int>

}