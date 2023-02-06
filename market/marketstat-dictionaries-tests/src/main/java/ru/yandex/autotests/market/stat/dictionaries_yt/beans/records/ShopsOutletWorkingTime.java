package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 29.06.17.
 */
@Data
@DictTable(name = "shops_outlet_working_time")
public class ShopsOutletWorkingTime implements DictionaryRecord {
    private String workingDaysTill; /*Actualy int */
    @DictionaryIdField
    private String workingDaysFrom; /*Actualy int */
    private String workingHoursTill; /*Actualy string */
    @DictionaryIdField
    private String workingHoursFrom; /*Actualy string */
    @DictionaryIdField
    private String pointId; /*Actualy bigint */
    private LocalDateTime day; /*Actualy date */

}
