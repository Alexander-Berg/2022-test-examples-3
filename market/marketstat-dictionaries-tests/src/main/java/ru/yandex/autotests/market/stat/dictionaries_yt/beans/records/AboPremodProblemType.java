package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "premod_problem_type")
public class AboPremodProblemType implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Integer */
    private String item_type_id; /* Integer */
    private String name; /* String */
    private String recommendation; /* String */
    private String hidden; /* Boolean */
    private String for_offline; /* Boolean */
    private String relates_to_placement; /* String */
    private String type_index; /* Integer */
}
