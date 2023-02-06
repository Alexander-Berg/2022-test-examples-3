package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 29.06.17.
 */
@Data
@DictTable(name = "shop_vat")
public class ShopVat implements DictionaryRecord {
    @DictionaryIdField
    private String shopId; /*Actualy bigint */
    private String taxSystem; /*Actualy int */
    private String vat; /*Actualy int */
    private String vatSource; /*Actualy int */
    private String deliveryVat; /*Actualy int */

}
