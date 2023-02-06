package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import org.beanio.annotation.Field;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.handlers.Handlers;

/**
 * Created by kateleb on 08.06.17.
 */
@Data
@DictTable(name = "cataloger")
public class Cataloger implements DictionaryRecord {
    @DictionaryIdField
    private String id;

    @RequiredField
    private String parent;

    @RequiredField
    private String name;

    private String uniqName;

    private String description;

    private String outputType;

    private String marketView;

    private Boolean visual;

    private Boolean visualShowWithChilds;

    private String outputCategoryId;

    private String modelListId;

    private String position;

    private String special;

    @Field(handlerName = Handlers.UNQUOTED_LIST_HANDLER_YT)
    private String relatedCategories;

}
