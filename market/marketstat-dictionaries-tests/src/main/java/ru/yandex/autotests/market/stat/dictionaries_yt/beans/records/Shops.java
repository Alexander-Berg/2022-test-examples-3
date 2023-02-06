package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import org.beanio.annotation.Field;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.handlers.Handlers;

/**
 * Created by entarrion on 05.10.16
 */
@Data
@DictTable(name = "shops")
public class Shops implements DictionaryRecord {
    @DictionaryIdField
    private String shopId; // in hive is int

    private String clientId; // in hive is int

    private String cpa; // in hive is string

    private String datasourceName; // in hive is string

    private String deliveryServices; // in hive is string

    private String deliverySrc; // in hive is string

    private String free; // in hive is boolean

    private String fromMarket; // in hive is boolean

    private String homeRegion; // in hive is int

    private String isBooknow; // in hive is boolean

    private String isCpaPartner; // in hive is boolean

    private String isCpaPrior; // in hive is boolean

    private String isDiscountsEnabled; // in hive is boolean

    private String isEnabled; // in hive is boolean

    private String isGlobal; // in hive is boolean

    private String isMock; // in hive is boolean

    private String localDeliveryCost; // in hive is decimal(38,18)

    private String phone; // in hive is string

    private String phoneDisplayOptions; // in hive is string

    private String prepayEnabled; // in hive is boolean

    private String priorityRegionOriginal; // in hive is int

    private String priorityRegions; // in hive is int

    private String shopClusterId; // in hive is decimal(38,18)

    private String shopCurrency; // in hive is string

    @DictionaryIdField
    private String shopname; // in hive is string

    private String showPremium; // in hive is boolean

    private String tariff; // in hive is string

    private String isOnline; // in hive is boolean

    private String yclidDisabled; // in hive is boolean

    @Field(handlerName = Handlers.UNQUOTED_LIST_HANDLER_YT)
    private String regions;

    private String url;

    @DictionaryIdField
    private String datafeedId;

    private String returnDeliveryAddress;

    @RequiredField
    private String is_placed;

    private String cpc; // in hive is string

    @RequiredField
    private Boolean is_cpa20;

    private Boolean supplier_type;
}
