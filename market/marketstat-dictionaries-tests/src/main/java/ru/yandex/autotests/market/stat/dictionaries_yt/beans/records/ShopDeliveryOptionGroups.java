package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "shop_delivery_option_groups")
public class ShopDeliveryOptionGroups implements DictionaryRecord {

    @DictionaryIdField
    private String delivery_option_group_id;
    private String region_group_id;
    private String category_order_num;
    private String offer_price_order_num;
    private String offer_weight_order_num;
    private String has_delivery;
}
