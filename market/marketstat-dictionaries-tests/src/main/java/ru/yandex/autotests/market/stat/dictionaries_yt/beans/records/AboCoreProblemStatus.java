package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "core_problem_status")
public class AboCoreProblemStatus implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Actualy Integer */
    private String name; /* Actualy String */
}
