package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 29.06.17.
 */
@Data
@DictTable(name = "shop_schedule")
public class ShopSchedule implements DictionaryRecord {
    @DictionaryIdField
    private String shopId; /*Actualy bigint */
    @DictionaryIdField
    private String startDay; /*Actualy bigint */
    private String days; /*Actualy bigint */
    @DictionaryIdField
    private String startMinute; /*Actualy bigint */
    private String minutes; /*Actualy bigint */

    private LocalDateTime day; /*Actualy date */

}
