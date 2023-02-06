package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 29.08.17.
 */
@Data
@DictTable(name = "shop_price_labs")
public class ShopPriceLabs implements DictionaryRecord {
    @DictionaryIdField
    private String plShopId;

    @RequiredField
    private String campaignId;

    @RequiredField
    private String domain;

    @RequiredField
    private LocalDateTime createdAt;

    private String hasDefaultStrategy;

    private String minPriceCard;

    private String hasFilterBasedStrategy;

    private String doCardBids;

    private String doSearchBids;

    private String doFeeBids;

    private String usePriceMonitoring;

    private String useAnalyticSystem;

    private String hasReserveStrategies;

    private LocalDateTime day;

}
