package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "core_problem_class")
public class AboCoreProblemClass implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Actualy Integer */
    private String name; /* Actualy String */
    private String shop_problem; /* Actualy Boolean */
    private String hidden; /* Actualy Boolean */
}
