package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import org.beanio.annotation.Field;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

import static ru.yandex.autotests.market.stat.attribute.Fields.PARAM_XSLNAME;

/**
 * Created by kateleb on 28.06.17
 */
@Data
@DictTable(name = "parameters")
public class Parameters implements DictionaryRecord {
    @DictionaryIdField
    private String categoryId;
    @DictionaryIdField
    private String paramId;
    private String paramType;
    private String paramName;
    @Field(name = PARAM_XSLNAME)
    @RequiredField
    private String paramXslname;
    private String paramOptions;
    private String importance;
    private String description;

}
