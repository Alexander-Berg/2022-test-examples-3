package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "shop_delivery_options")
public class ShopDeliveryOptions implements DictionaryRecord {

    @DictionaryIdField
    private String delivery_option_group_id;
    private String delivery_cost;
    private String days_from;
    private String days_to;
    private String order_before_hour;
    private String order_num;

}
