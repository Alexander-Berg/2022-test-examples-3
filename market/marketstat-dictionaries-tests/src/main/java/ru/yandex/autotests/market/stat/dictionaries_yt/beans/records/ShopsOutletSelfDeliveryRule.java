package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 29.06.17.
 */
@Data
@DictTable(name = "shops_outlet_self_delivery_rule")
public class ShopsOutletSelfDeliveryRule implements DictionaryRecord {
    private String priceFrom; /*Actualy bigint */
    private String priceTo; /*Actualy bigint */
    private String cost; /*Actualy bigint */
    private String minDeliveryDays; /*Actualy int */
    private String maxDeliveryDays; /*Actualy int */
    private String unspecifiedDelveryInterval; /*Actualy boolean */
    private String workInHoliday; /*Actualy boolean */
    private String dateSwitchHour; /*Actualy int */
    private String shipperId; /*Actualy bigint */
    private String shipperName; /*Actualy string */
    private String shipperHumanReadableId; /*Actualy string */
    private String shipperCountryIds; /*Actualy array */
    @DictionaryIdField
    private String pointId; /*Actualy bigint */

    private LocalDateTime day; /*Actualy date */

}
