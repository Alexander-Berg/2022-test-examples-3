package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 18.09.17.
 */
@Data
@DictTable(name = "metrika_counter")
public class MetrikaCounter implements DictionaryRecord {
    @DictionaryIdField
    private String shopId;
    private String type;
    private String counterId;
    private String goalId;

}
