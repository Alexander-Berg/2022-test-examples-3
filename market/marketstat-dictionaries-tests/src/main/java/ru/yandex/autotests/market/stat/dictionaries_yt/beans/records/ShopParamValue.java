package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

/**
 * Created by kateleb on 24.08.16.
 */
@Data
@DictTable(name = "shop_param_value")
public class ShopParamValue implements DictionaryRecord {
    @DictionaryIdField
    private String paramValueId;

    @RequiredField
    private String paramTypeId;

    @RequiredField
    private String entityId;

    private String num;

    private String numValue;

    private String strValue;


    private LocalDateTime dateValue;


}
