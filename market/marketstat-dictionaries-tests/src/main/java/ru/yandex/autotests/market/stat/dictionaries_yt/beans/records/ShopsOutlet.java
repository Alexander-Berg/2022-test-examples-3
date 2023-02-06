package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 29.06.17.
 */
@Data
@DictTable(name = "shops_outlet")
public class ShopsOutlet implements DictionaryRecord {
    @DictionaryIdField
    private String pointId; /*Actualy bigint */

    private String shopPointId; /*Actualy string */

    private String pointName; /*Actualy string */

    private String pointType; /*Actualy string */

    private String isMain; /*Actualy boolean */

    private String localityName; /*Actualy string */

    private String thoroughfareName; /*Actualy string */

    private String premiseNumber; /*Actualy string */

    private String estate; /*Actualy string */

    private String block; /*Actualy string */

    private String addressAdd; /*Actualy string */

    private String regionId; /*Actualy bigint */

    @DictionaryIdField
    private String deliveryId; /*Actualy bigint */

    @DictionaryIdField
    private String shopId; /*Actualy bigint */

    private LocalDateTime day; /*Actualy date */

}
