package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.clickHouse.annotations.ClickHouseField;

import static ru.yandex.autotests.market.stat.attribute.Fields.*;

/**
 * Created by kateleb on 01.07.16
 */
@Data
@DictTable(name = "shop_crm")
public class ShopsCrm implements DictionaryRecord {
    @DictionaryIdField
    @ClickHouseField(CRM_SHOP_ID)
    private String shopId;

    @ClickHouseField(CH_CAMPAIGN_ID)
    private String campaignId;

    @ClickHouseField(CH_CLIENT_ID)
    private String clientId;

    @ClickHouseField(DOMAIN)
    private String domain;

    @ClickHouseField(REGION)
    private String region;

    @ClickHouseField(CH_REG_OBL)
    private String regObl;

    @ClickHouseField(COUNTRY)
    private String country;

    @ClickHouseField(CH_SHOP_NAME)
    private String shopName;

    @ClickHouseField(CH_IS_ONLINE)
    private String isOnline;

    @ClickHouseField(CH_IS_ALIVE)
    private String isAlive;

    @ClickHouseField(CH_YA_MANAGER)
    private String yaManager;

    @ClickHouseField(CH_CPA_IS_PARTNER_INTERFACE)
    private String cpaIsPartnerInterface;

    @ClickHouseField(CH_CPA_PAYMENT_CHECK_STATUS)
    private String cpaPaymentCheckStatus;

    @ClickHouseField(CH_AGENCY_ID)
    private String agencyId;

    @ClickHouseField(CH_CPC_BUDGET)
    private String cpcBudget;

    @ClickHouseField(CH_CPA_BUDGET)
    private String cpaBudget;

    @ClickHouseField(CH_SUM_OPERATION)
    private String sumOperation;

    @ClickHouseField(CH_CPC_BUDGET_DAY)
    private String cpcBudgetDay;

    @ClickHouseField(CH_CPA_BUDGET_DAY)
    private String cpaBudgetDay;

    @ClickHouseField(CH_SUM_OPERATION_DAY)
    private String sumOperationDay;

    @ClickHouseField(CH_CPA_ORDERS_CREATED)
    private String cpaOrdersCreated;

    @ClickHouseField(CH_CPA_ORDERS_FINISHED)
    private String cpaOrdersFinished;

    @ClickHouseField(CH_CPA_ORDERS_APPROVED)
    private String cpaOrdersApproved;

    @ClickHouseField(CH_CPA_ORDERS_CANCELED)
    private String cpaOrdersCanceled;

    @ClickHouseField(CH_CPA_ORDERS_CREATED_DAY)
    private String cpaOrdersCreatedDay;

    @ClickHouseField(CH_CPA_ORDERS_FINISHED_DAY)
    private String cpaOrdersFinishedDay;

    @ClickHouseField(CH_CPA_ORDERS_APPROVED_DAY)
    private String cpaOrdersApprovedDay;

    @ClickHouseField(CH_CPA_ORDERS_CANCELED_DAY)
    private String cpaOrdersCanceledDay;

    @ClickHouseField(CH_CPC_ENABLED)
    private String cpcEnabled;

    @ClickHouseField(CH_CPA_ENABLED)
    private String cpaEnabled;

    @ClickHouseField(CH_CPC_DISABLED_TIME)
    private String cpcDisabledTime;

    @ClickHouseField(CH_CPC_DISABLED_TIME_DAY)
    private String cpcDisabledTimeDay;

    @ClickHouseField(PHONE)
    private String phone;

    @ClickHouseField(NOFFERS)
    private String noffers;

    @ClickHouseField(BALANCE)
    private String balance;

    @ClickHouseField(CH_CLICK_COUNT)
    private String clickCount;

    @ClickHouseField(CH_AVG_CLICK_PRICE)
    private String avgClickPrice;

    @ClickHouseField(CH_CLICK_COUNT_DAY)
    private String clickCountDay;

    @ClickHouseField(CH_AVG_CLICK_PRICE_DAY)
    private String avgClickPriceDay;

    @ClickHouseField(CH_CPA_OFFERS)
    private String cpaOffers;

    @ClickHouseField(CH_MATCHED_OFFERS)
    private String matchedOffers;

    @ClickHouseField(CH_OUTLET_NUM)
    private String outletNum;

    @ClickHouseField(CH_CPA_REGIONS_ENABLED)
    private String cpaRegionsEnabled;

    @ClickHouseField(RATING)
    private String rating;

    @ClickHouseField(CH_LIFE_STATUS)
    private String lifeStatus;

    @ClickHouseField(CH_USES_OFFER_ID)
    private String usesOfferId;

    @ClickHouseField(CH_USES_PARTNER_API)
    private String usesPartnerApi;

    @ClickHouseField(CH_CPC_PLACEMENT_TIME)
    private String cpcPlacementTime;

    @ClickHouseField(CH_CPA_TESTING_PASSED)
    private String cpaTestingPassed;

    @ClickHouseField(RATING_DATE)
    private LocalDateTime day;


}
