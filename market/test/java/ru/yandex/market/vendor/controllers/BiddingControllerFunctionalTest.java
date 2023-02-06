package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static org.mockito.Mockito.when;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class BiddingControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    private Clock clock;
    @Autowired
    private NamedParameterJdbcTemplate marketVendorsClickHouseNamedJdbcTemplate;
    @Autowired
    private WireMockServer reportMock;


    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/testPutVendorModelBidsShouldSaveForecastSnapshot/before.csv",
            after = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/testPutVendorModelBidsShouldSaveForecastSnapshot/after.csv"
    )
    void testPutVendorModelBidsShouldSaveForecastSnapshot() {
        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 1, 10, 0, 0, 0)));

        reportMock.stubFor(get(anyUrl())
                .willReturn(okForContentType("application/json", "{}")));

        String body = getStringResource("/testPutVendorModelBidsShouldSaveForecastSnapshot/request.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/101/modelbids/bids?uid=100500", body);
    }

    @Test
    @DisplayName("111: полная модель, 222: полная модель, 333: нет рекомендаций по позициям-ставка," +
            "444: нет категории (больная модель. Не пишется в аудит, но ппроставляется ставка)," +
            "555: нет кликов, 666: нет показов, 777: полная, 888: в пришедшей рекомендации нет подходящей позиции")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/testPutCommonBidsForGroup/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/testPutCommonBidsForGroup/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/testPutCommonBidsForGroup/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testPutCommonBidsForGroup() {
        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 1, 10, 0, 0, 0)));

        reportMock.stubFor(get(anyUrl())
                .willReturn(okForContentType("application/json", "{}")));

        String body = getStringResource("/testPutCommonBidsForGroup/request.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/101/modelbids/bids/groups/1?uid=100500", body);
    }

    @Test
    @DisplayName("5 - group_id, в которой содержится модель, у которой нет категории в BRAND_MODELS")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/testPutCommonBidsForGroupCheckNPE/before.csv",
            after = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/testPutCommonBidsForGroupCheckNPE/after.csv"
    )
    void testPutCommonBidsForGroupCheckNPE() {
        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 1, 10, 0, 0, 0)));

        reportMock.stubFor(get(anyUrl())
                .willReturn(okForContentType("application/json", "{}")));

        String body = getStringResource("/testPutCommonBidsForGroupCheckNPE/request.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/101/modelbids/bids/groups/5?uid=100500", body);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/testResetBids/before.csv",
            after = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/testResetBids/after.csv"
    )
    void testResetBids() {

        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 1, 10, 0, 0, 0)));

        reportMock.stubFor(get(anyUrl())
                .willReturn(okForContentType("application/json", "{}")));

        String body = getStringResource("/testResetBids/request.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/102/modelbids/bids?uid=100500", body);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/testAuditSetBidInGroupAndNot/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/testAuditSetBidInGroupAndNot/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/testAuditSetBidInGroupAndNot/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    public void testAuditSetBidInGroupAndNot() {

        marketVendorsClickHouseNamedJdbcTemplate.update(
                "UPDATE MARKET_VENDORS.MODELBIDS_STATS SET DATE = today()-1 " +
                        "WHERE MODEL_ID = 1721921261 AND SHOWS_SUM_POSITION_TOTAL = 35",
                Collections.emptyMap());

        marketVendorsClickHouseNamedJdbcTemplate.update(
                "UPDATE MARKET_VENDORS.MODELBIDS_STATS SET DATE = today()-3 " +
                        "WHERE MODEL_ID = 1721921261 AND SHOWS_SUM_POSITION_TOTAL = 38",
                Collections.emptyMap());
        marketVendorsClickHouseNamedJdbcTemplate.update(
                "UPDATE MARKET_VENDORS.MODELBIDS_STATS SET DATE = today()-29 " +
                        "WHERE MODEL_ID = 1721921261 AND SHOWS_SUM_POSITION_TOTAL = 44",
                Collections.emptyMap());

        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 1, 10, 0, 0, 0)));

        reportMock.stubFor(get(anyUrl())
                .willReturn(okForContentType("application/json", "{}")));

        String body = getStringResource("/testAuditSetBidInGroupAndNot/request.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/101/modelbids/bids?uid=100500", body);
    }


    @Test
    @DisplayName("Сохраняет несуществующую ставку на модель, в 2 таблицы со ставками")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/saveSingleBid/before.csv",
            after = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/saveSingleBid/after.csv"
    )
    void saveSingleBid() {
        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 1, 10, 0, 0, 0)));

        reportMock.stubFor(get(anyUrl())
                .willReturn(okForContentType("application/json", "{}")));

        String body = getStringResource("/saveSingleBid/request.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/102/modelbids/bids?uid=100500", body);
    }

    @Test
    @DisplayName("Изменение ставок на модели")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/replaceSingleBid/before.csv",
            after = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/replaceSingleBid/after.csv"
    )
    void replaceSingleBid() {
        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 1, 11, 0, 0, 0)));

        reportMock.stubFor(get(anyUrl())
                .willReturn(okForContentType("application/json", "{}")));

        String requestInitial = getStringResource("/replaceSingleBid/request_initial.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/102/modelbids/bids?uid=100500", requestInitial);

        String body = getStringResource("/replaceSingleBid/request.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/102/modelbids/bids?uid=100500", body);
    }

    @Test
    @DisplayName("Изменение ставок на модели, продвигаемые на Покупках (автостратегия существует)")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/replaceSingleBidWithExistentAutostrategy/before.csv",
            after = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/replaceSingleBidWithExistentAutostrategy/after.csv"
    )
    void replaceSingleBidWithExistentAutostrategy() {
        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 1, 11, 0, 0, 0)));

        reportMock.stubFor(get(anyUrl())
                .willReturn(okForContentType("application/json", "{}")));

        String body = getStringResource("/replaceSingleBidWithExistentAutostrategy/request.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/102/modelbids/bids?uid=100500", body);
    }

    @Test
    @DisplayName("Сохранение ставок на модели при отключённой услуге")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/saveSingleBidWithCutoffs/before.cs_billing.csv",
            after = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/saveSingleBidWithCutoffs/after.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/saveSingleBidWithCutoffs/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/BiddingControllerFunctionalTest/saveSingleBidWithCutoffs/after.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void saveSingleBidWithCutoffs() {
        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 1, 11, 0, 0, 0)));

        reportMock.stubFor(get(anyUrl())
                .willReturn(okForContentType("application/json", "{}")));

        String body = getStringResource("/saveSingleBidWithCutoffs/request.json");
        FunctionalTestHelper.put(baseUrl + "/vendors/102/modelbids/bids?uid=100500", body);
    }
}
