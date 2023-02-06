package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

/**
 * Created by astepanel on 26.04.17.
 */
@Data
@DictTable(name = "cpa_orders")
public class CpaOrder implements DictionaryRecord {
    @DictionaryIdField
    private String order_id; /*Actualy Long */

    private String shop_id; /*Actualy Long */

    private String campaign_id; /*Actualy Long */

    private String creation_date; /*Actualy Date */

    private String items_total; /*Actualy Long */

    private String delivery; /*Actualy Long */

    private String fee_sum; /*Actualy Long */

    @RequiredField
    private String fee_correct; /*Actualy Long */

    private String status; /*Actualy Integer */

    private String substatus; /*Actualy Integer */

    private String billing_status; /*Actualy Integer */

    private String trantime; /*Actualy Date */

    private String free; /*Actualy Boolean */

    private String order_num; /*Actualy String */

    private String shop_currency; /*Actualy String */

    private String auto_processed; /*Actualy Boolean */

    private String payment_type; /*Actualy Integer */

    private String payment_method; /*Actualy Integer */

    private String real_items_total; /*Actualy Long */

    private String cpa20; /*Actualy Boolean */


}
