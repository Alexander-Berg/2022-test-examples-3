package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

/**
 * Created by kateleb on 24.08.16.
 */
@Data
@DictTable(name = "shop_param_type")
public class ShopParamType implements DictionaryRecord {
    @DictionaryIdField
    private String paramTypeId;

    @RequiredField
    private String valueType;

    @RequiredField
    private String paramName;

    private String entityName;

}
