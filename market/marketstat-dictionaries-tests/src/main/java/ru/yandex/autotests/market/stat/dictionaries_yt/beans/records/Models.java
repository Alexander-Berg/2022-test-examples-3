package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.clickHouse.annotations.ClickHouseField;

import static ru.yandex.autotests.market.stat.attribute.Fields.CATEGORY_ID;
import static ru.yandex.autotests.market.stat.attribute.Fields.CREATED_DATE;
import static ru.yandex.autotests.market.stat.attribute.Fields.ID;
import static ru.yandex.autotests.market.stat.attribute.Fields.ISO_CODE;
import static ru.yandex.autotests.market.stat.attribute.Fields.IS_GURU;
import static ru.yandex.autotests.market.stat.attribute.Fields.NAME;
import static ru.yandex.autotests.market.stat.attribute.Fields.VENDOR_ID;

/**
 * Created by kateleb on 22.09.16
 */
@Data
@DictTable(name = "models")
public class Models implements DictionaryRecord {
    @DictionaryIdField
    @ClickHouseField(ID)
    private String id; // in hive is bigint

    @ClickHouseField(NAME)
    @RequiredField
    private String name; // in hive is string

    @ClickHouseField(CATEGORY_ID)
    @RequiredField
    private String category_id; // in hive is bigint

    @ClickHouseField(ISO_CODE)
    private String iso_code; // in hive is string

    @ClickHouseField(VENDOR_ID)
    private String vendor_id; // in hive is bigint

    @ClickHouseField(CREATED_DATE)
    private String created_date; // in hive is date

    @ClickHouseField(IS_GURU)
    private String is_guru; // in hive is tinyint

}