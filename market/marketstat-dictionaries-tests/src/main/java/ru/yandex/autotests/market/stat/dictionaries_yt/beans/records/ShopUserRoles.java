package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by kateleb on 11.10.16
 */
@Data
@DictTable(name = "shop_user_roles")
public class ShopUserRoles implements DictionaryRecord {
    @DictionaryIdField
    private String id; // in hive is bigint

    private String login; // in hive is string

    private String role; // in hive is string

    private String fullname; // in hive is string

    private String id_1c; // in hive is string

    private String lastname_1c; // in hive is string

    private String email; // in hive is string

    private String replacedBy; // in hive is bigint

    private String groop; // in hive is string

    private String passportEmail; // in hive is string

}
