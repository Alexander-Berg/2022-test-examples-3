package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

/**
 * @author aostrikov
 */
@Data
@DictTable(name = "cutoff_types")
public class CutoffTypes implements DictionaryRecord {
    @DictionaryIdField
    private String id;  /* Actually long */
    @RequiredField
    private String name;  /* Actually string */
    @RequiredField
    private String shop_program;  /* Actually string */

}
