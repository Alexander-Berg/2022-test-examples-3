package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import org.beanio.annotation.Field;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.handlers.Handlers;

/**
 * Created by kateleb on 24.08.16.
 */
@Data
@DictTable(name = "regions")
public class Regions implements DictionaryRecord {
    @DictionaryIdField
    private String id;

    private String ruName;

    @RequiredField
    private String type;

    private String parentId;

    private String parentRuName;

    private String parentType;

    private String countryId;

    private String countryRuName;

    @Field(handlerName = Handlers.UNQUOTED_LIST_HANDLER_YT)
    private String parents;

    @Field(handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String pathRuName;

    @Field(handlerName = Handlers.UNQUOTED_LIST_HANDLER_YT)
    private String children;

    private String enName;

    private String parentEnName;

    private String countryEnName;

    @Field(handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String pathEnName;

    private String distributionRegionGroupId;

}
