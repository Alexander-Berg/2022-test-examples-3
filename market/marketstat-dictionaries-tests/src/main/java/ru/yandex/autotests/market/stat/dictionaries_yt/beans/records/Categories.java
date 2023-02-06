package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import org.beanio.annotation.Field;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.clickHouse.annotations.ClickHouseField;
import ru.yandex.autotests.market.stat.handlers.Handlers;

import static ru.yandex.autotests.market.stat.attribute.Fields.*;

/**
 * Created by kateleb on 22.09.16
 */
@Data
@DictTable(name = "categories")
public class Categories implements DictionaryRecord {

    @DictionaryIdField
    @ClickHouseField(ID)
    private String id; // in hive is bigint

    @ClickHouseField(PARENT_ID)
    private String parentId; // in hive is bigint

    @DictionaryIdField
    @ClickHouseField(HYPER_ID)
    private String hyperId; // in hive is bigint

    @ClickHouseField(NAME)
    @RequiredField
    private String name; // in hive is string

    @ClickHouseField(NOT_USED)
    private boolean notUsed; // in hive is boolean

    @ClickHouseField(NO_SEARCH)
    private boolean noSearch; // in hive is boolean

    @ClickHouseField(CHILDREN)
    @Field(handlerName = Handlers.UNQUOTED_LIST_HANDLER_YT)
    private String children; // in hive is array<bigint>

    @ClickHouseField(PARENTS)
    @Field(handlerName = Handlers.UNQUOTED_LIST_HANDLER_YT)
    private String parents; // in hive is array<bigint>

    @ClickHouseField(PARENT_HYPER_IDS)
    @Field(handlerName = Handlers.UNQUOTED_LIST_HANDLER_YT)
    private String parentHyperIds; // in hive is array<bigint>

    @ClickHouseField(PARENTS_NAMES)
    @Field(handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String parentsNames; // in hive is array<string>

    private String cpaType; // in hive is string

    @Field(handlerName = Handlers.UNQUOTED_LIST_HANDLER_YT)
    private String cpaRegions; // in hive is array<int>

    private String cpaFee; // in hive is int

    @Field(handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String hierarchyCpaTypes; // in hive is array<string>

    @Field(handlerName = Handlers.UNQUOTED_LIST_HANDLER_YT)
    private String hierarchyCpaRegions; // in hive is array<array<int>>

    @Field(handlerName = Handlers.UNQUOTED_LIST_HANDLER_YT)
    private String hierarchyHyperIds; // in hive is array<bigint>

    @Field(handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String hierarchyNames; // in hive is array<string>

    @ClickHouseField(EN_NAME)
    private String enName;

    @ClickHouseField(PARENTS_EN_NAMES)
    @Field(handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String parentsEnNames;

}
