package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "recheck_problem_type")
public class AboRecheckProblemType implements DictionaryRecord {
    @DictionaryIdField
    private String id; /*BigDecimal*/
    private String class_id; /*BigDecimal*/
    private String format; /*String*/
    private String title; /*String*/
}
