package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.beans.WithPeriod;
import ru.yandex.autotests.market.stat.date.PeriodUtils;

/**
 * Created by kateleb on 15.12.16
 */
@Data
@DictTable(name = "visits_aggr")
public class VisitsAggr implements DictionaryRecord, WithPeriod {
    @DictionaryIdField
    private String counterId; // in hive is bigint
    @DictionaryIdField
    private String domain; // in hive is string
    private String searchEngine; // in hive is string
    @RequiredField
    private String users; // in hive is bigint
    @RequiredField
    private String visits; // in hive is bigint
    @DictionaryIdField
    private String category; // in hive is string
    private LocalDateTime day; // in hive is date


    @Override
    public LocalDateTime extractDayAndHour() {
        return PeriodUtils.truncateToDay(day);
    }
}
