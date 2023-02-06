package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.clickHouse.annotations.ClickHouseField;

import static ru.yandex.autotests.market.stat.attribute.Fields.COMMENTS;
import static ru.yandex.autotests.market.stat.attribute.Fields.ID;
import static ru.yandex.autotests.market.stat.attribute.Fields.MAIN_CREATED_AT;
import static ru.yandex.autotests.market.stat.attribute.Fields.MAIN_MANAGER_ID;
import static ru.yandex.autotests.market.stat.attribute.Fields.NAME;

/**
 * Created by kateleb on 29.06.16.
 */
@Data
@DictTable(name = "shop_datasource")
public class ShopsMain implements DictionaryRecord {
    @DictionaryIdField
    @ClickHouseField(ID)
    private String id;

    @ClickHouseField(NAME)
    @RequiredField
    private String name;

    @ClickHouseField(MAIN_CREATED_AT)

    @RequiredField
    private LocalDateTime createdAt;

    @ClickHouseField(COMMENTS)
    private String comments;

    @ClickHouseField(MAIN_MANAGER_ID)
    private String managerId;

}
