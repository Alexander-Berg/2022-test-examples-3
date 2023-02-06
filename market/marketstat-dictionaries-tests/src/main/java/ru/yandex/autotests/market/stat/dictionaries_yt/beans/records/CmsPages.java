package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import org.beanio.annotation.Field;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.handlers.Handlers;

import static ru.yandex.autotests.market.stat.attribute.Fields.CATEGORY_IDS;
import static ru.yandex.autotests.market.stat.attribute.Fields.ENTRY_POINTS;
import static ru.yandex.autotests.market.stat.attribute.Fields.MODEL_IDS;
import static ru.yandex.autotests.market.stat.attribute.Fields.NCATEGORY_IDS;
import static ru.yandex.autotests.market.stat.attribute.Fields.TAGS;

/**
 * Created by kateleb on 23.06.17.
 */
@Data
@DictTable(name = "cms_pages")
public class CmsPages implements DictionaryRecord {
    @DictionaryIdField
    private String type;

    @DictionaryIdField
    private String pageId;

    private String semanticId;

    @Field(name = CATEGORY_IDS, handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String categoryIds;

    @Field(name = NCATEGORY_IDS, handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String ncategoryIds;

    @DictionaryIdField
    @Field(name = MODEL_IDS, handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String modelIds;

    private String title;
    @DictionaryIdField
    @Field(name = TAGS, handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String tags;
    @Field(name = ENTRY_POINTS, handlerName = Handlers.QUOTED_LIST_HANDLER_YT)
    private String entryPoints;
    private Boolean archived;

}
