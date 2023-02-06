package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import org.beanio.annotation.Field;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.handlers.Handlers;

/**
 * Created by kateleb on 28.06.17
 */
@Data
@DictTable(name = "group_region_stats")
public class GroupRegionStats implements DictionaryRecord {
    @DictionaryIdField
    private String modelId;

    @DictionaryIdField
    private String numOffers;

    private String maxDiscount;

    private String medianPriceRur;

    private String maxPriceRur;

    private String minPriceRur;

    private String minOldpriceRur;
    @DictionaryIdField
    @Field(handlerName = Handlers.UNQUOTED_LIST_HANDLER_YT)
    private String regions;

}
