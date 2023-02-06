package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 06.02.18
 */
@Data
@DictTable(name = "sku")
public class SkuDictionaryRecord implements DictionaryRecord {
    @DictionaryIdField
    private String id;              /* Actually Long */
    private String model_id;        /* Actually Long */
    private String name;            /* Actually String */
    private String category_id;     /* Actually Long */
    private String iso_code;        /* Actually String */
    private String vendor_id;       /* Actually Long */
    private String created_date;    /* Actually Date */
    private String is_guru;         /* Actually Boolean */
    private String params;          /* Actually String */

}
