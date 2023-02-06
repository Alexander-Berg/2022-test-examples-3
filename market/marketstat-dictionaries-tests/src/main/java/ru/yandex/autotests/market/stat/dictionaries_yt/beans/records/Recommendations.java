package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

/**
 * Created by kateleb on 08.06.17.
 */
@Data
@DictTable(name = "recommendation_rules")
public class Recommendations implements DictionaryRecord {
    @DictionaryIdField
    private String id;

    @RequiredField
    private String mainCategoryId;

    private String linkedCategoryId;

    @RequiredField
    private String name;

    private String linkType;

    private String direction;

    private String weight;

    @DictionaryIdField
    private String ruleId;

    private String ruleType;

    private String mainRecipe;

    private String linkedRecipe;

    private String valueType;

}
