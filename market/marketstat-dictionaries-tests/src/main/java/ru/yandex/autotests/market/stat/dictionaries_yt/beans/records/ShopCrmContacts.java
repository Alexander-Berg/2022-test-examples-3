package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by kateleb on 11.10.16
 */
@Data
@DictTable(name = "shop_crm_contacts")
public class ShopCrmContacts implements DictionaryRecord {
    @DictionaryIdField
    private String shopId; // in hive is bigint

    @DictionaryIdField
    private String name; // in hive is string

    private String phone; // in hive is string

    @DictionaryIdField
    private String email; // in hive is string

    @DictionaryIdField
    private String role; // in hive is string

}

