package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 29.06.17.
 */
@Data
@DictTable(name = "delivery_region_groups")
public class DeliveryRegionGroups implements DictionaryRecord {
    @DictionaryIdField
    private String regionGroupId; /*Actualy bigint */
    @DictionaryIdField
    private String shopId; /*Actualy bigint */

    private String groupName; /*Actualy string */

    private String currency; /*Actualy string */

    private String isSelfRegion; /*Actualy int */

    private String groupCheckStatus; /*Actualy int */

    private String checkStatusModifiedAt; /*Actualy timestamp */

    private String deliveryTariffType; /*Actualy int */

    private String modifiedAt; /*Actualy timestamp */

    private String modifiedBy; /*Actualy bigint */

    private String notes; /*Actualy string */


    private LocalDateTime day; /*Actualy date */

}
