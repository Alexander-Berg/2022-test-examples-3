package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "recheck_problem_class")
public class AboRecheckProblemClass implements DictionaryRecord {
    @DictionaryIdField
    private String id; /*BigDecimal*/
    private String title; /*String*/
}
