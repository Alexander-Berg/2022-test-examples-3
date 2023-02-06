package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import org.beanio.annotation.Field;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.handlers.Handlers;

/**
 * @author aostrikov
 */
@Data
@DictTable(name = "distribution_region_groups")
public class DistributionRegionGroups implements DictionaryRecord {
    @DictionaryIdField
    private String id;
    @RequiredField
    private String name;
    @Field(handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String incRegionIds;
    @Field(handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String excRegionIds;

}
