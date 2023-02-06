package ru.yandex.market.vendor.controllers.marketing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static net.javacrumbs.jsonunit.JsonAssert.when;
import static ru.yandex.vendor.security.Role.admin_user;
import static ru.yandex.vendor.security.Role.manager_user;
import static ru.yandex.vendor.security.Role.marketing_banners_user;
import static ru.yandex.vendor.security.Role.marketing_landings_user;
import static ru.yandex.vendor.security.Role.marketing_shop_in_shop_user;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
public class MarketingServicesCampaignControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    @Autowired
    private WireMockServer csBillingApiMock;

    @Autowired
    private Clock clock;

    @BeforeEach
    public void beforeEach() {
        Mockito.when(clock.instant()).thenReturn(
                TimeUtil.toInstant(LocalDateTime.of(2020, Month.MAY, 20, 0, 0))
        );
    }

    @Test
    @DisplayName("Получение кампании по ID")
    void testGetMarketingCampaignById() {
        setVendorUserRoles(Collections.singleton(manager_user), 100500);
        String actual = FunctionalTestHelper.get(baseUrl + "/vendors/321/marketing/campaigns/100?uid=100500");
        String expected = getStringResource("/testGetMarketingCampaignById/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка всех кампаний менеджером")
    void testGetAllMarketingCampaignsByManager() {
        setVendorUserRoles(Collections.singleton(manager_user), 100500);
        String actual = FunctionalTestHelper.get(baseUrl + "/vendors/321/marketing/campaigns?uid=100500");
        String expected = getStringResource("/testGetAllMarketingCampaignsByManager/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка всех кампаний вендором")
    void testGetAllMarketingCampaignsByVendor() {
        setVendorUserRoles(Collections.singleton(admin_user), 100501, 321L);
        String actual = FunctionalTestHelper.get(baseUrl + "/vendors/321/marketing/campaigns?uid=100501");
        String expected = getStringResource("/testGetAllMarketingCampaignsByVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка всех кампаний вендором с неполным набором ролей")
    void testGetAllMarketingCampaignsByVendorWithoutAllMarketingRoles() {
        setVendorUserRoles(Collections.singleton(marketing_banners_user), 100501, 321L);
        setVendorUserRoles(Collections.singleton(marketing_landings_user), 100501, 321L);
        setVendorUserRoles(Collections.singleton(marketing_shop_in_shop_user), 100501, 321L);
        String actual = FunctionalTestHelper.get(baseUrl + "/vendors/321/marketing/campaigns?uid=100501");
        String expected = getStringResource("/testGetAllMarketingCampaignsByVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка кампаний вендора с фильтром по типу услуги")
    void testGetMarketingCampaignsFilterByServiceType() {
        setVendorUserRoles(Collections.singleton(manager_user), 100500);
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100500&serviceType=LOGO"
        );
        String expected = getStringResource("/testGetMarketingCampaignsFilterByServiceType/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка кампаний вендора с фильтром по тексту")
    void testGetMarketingCampaignsFilterByText() {
        setVendorUserRoles(Collections.singleton(admin_user), 100501, 321L);
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100501&text=требует"
        );
        String expected = getStringResource("/testGetMarketingCampaignsFilterByText/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка кампаний вендора с фильтром по временному интервалу")
    void testGetMarketingCampaignsFilterByDate() {
        setVendorUserRoles(Collections.singleton(manager_user), 100500);
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100500&dateFrom=1589922000000&dateTo=1590354000000"
        );
        String expected = getStringResource("/testGetMarketingCampaignsFilterByDate/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка кампаний вендора с фильтром по статус ACTIVE")
    void testGetMarketingCampaignsFilterByStatusActive() {
        setVendorUserRoles(Collections.singleton(admin_user), 100501, 321L);
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100501&status=ACTIVE"
        );
        String expected = getStringResource("/testGetMarketingCampaignsFilterByStatusActive/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка кампаний вендора с фильтром по статус FINISHED")
    void testGetMarketingCampaignsFilterByStatusFinished() {
        setVendorUserRoles(Collections.singleton(manager_user), 100500);
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100500&status=FINISHED"
        );
        String expected = getStringResource("/testGetMarketingCampaignsFilterByStatusFinished/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка кампаний вендора с фильтром по статус NEED_APPROVE")
    void testGetMarketingCampaignsFilterByStatusNeedApprove() {
        setVendorUserRoles(Collections.singleton(admin_user), 100501, 321L);
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100501&status=NEED_APPROVE"
        );
        String expected = getStringResource("/testGetMarketingCampaignsFilterByStatusNeedApprove/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка кампаний вендора с фильтром по статус SCHEDULED")
    void testGetMarketingCampaignsFilterByStatusScheduled() {
        setVendorUserRoles(Collections.singleton(manager_user), 100500);
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100500&status=SCHEDULED"
        );
        String expected = getStringResource("/testGetMarketingCampaignsFilterByStatusScheduled/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка кампаний вендора с фильтром по подтверждению менеджером")
    void testGetMarketingCampaignsFilterByManagerApproval() {
        setVendorUserRoles(Collections.singleton(manager_user), 100500);
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100500&approved=true"
        );
        String expected = getStringResource("/testGetMarketingCampaignsFilterByManagerApproval/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка кампаний вендора с фильтром по НЕ подтверждению менеджером")
    void testGetMarketingCampaignsFilterByManagerDisapproval() {
        setVendorUserRoles(Collections.singleton(manager_user), 100500);
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100500&approved=false"
        );
        String expected = getStringResource("/testGetMarketingCampaignsFilterByManagerDisapproval/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка кампаний вендора с фильтром по подтверждению вендором")
    void testGetMarketingCampaignsFilterByVendorApproval() {
        setVendorUserRoles(Collections.singleton(admin_user), 100501L, 321L);
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100501&approved=true"
        );
        String expected = getStringResource("/testGetMarketingCampaignsFilterByVendorApproval/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Получение списка кампаний вендора с фильтром по НЕ подтверждению вендором")
    void testGetMarketingCampaignsFilterByVendorDisapproval() {
        setVendorUserRoles(Collections.singleton(admin_user), 100501L, 321L);
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100500&approved=false"
        );
        String expected = getStringResource("/testGetMarketingCampaignsFilterByVendorDisapproval/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Создание кампании менеджером")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/testCreateMarketingCampaignByManager/after.csv",
            dataSource = "vendorDataSource"
    )
    void testCreateMarketingCampaignByManager() {
        String balanseResponse = getStringResource("/testCreateMarketingCampaignByManager/serviceDatasourceBalanceResponse.json");
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1324/balance")
                .willReturn(WireMock.okJson(balanseResponse)));

        setVendorUserRoles(Collections.singleton(manager_user), 100500L);

        String request = getStringResource("/testCreateMarketingCampaignByManager/request.json");

        String actual = FunctionalTestHelper.post(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100500",
                request
        );
        String expected = getStringResource("/testCreateMarketingCampaignByManager/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Создание кампании менеджером (даты из разных месяцев)")
    void testCreateMarketingCampaignByManagerWithinMultipleMonths() {
        setVendorUserRoles(Collections.singleton(manager_user), 100500L);

        String request = getStringResource("/testCreateMarketingCampaignByManagerWithinMultipleMonths/request.json");

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        baseUrl + "/vendors/321/marketing/campaigns?uid=100500",
                        request
                )
        );

        String actual = exception.getResponseBodyAsString();
        String expected = getStringResource("/testCreateMarketingCampaignByManagerWithinMultipleMonths/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Создание кампании вендором")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/testCreateMarketingCampaignByVendor/after.csv",
            dataSource = "vendorDataSource"
    )
    void testCreateMarketingCampaignByVendor() {
        String balanseResponse = getStringResource("/testCreateMarketingCampaignByVendor/serviceDatasourceBalanceResponse.json");
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1324/balance")
                .willReturn(WireMock.okJson(balanseResponse)));

        setVendorUserRoles(Collections.singleton(admin_user), 100501L, 321L);

        String request = getStringResource("/testCreateMarketingCampaignByVendor/request.json");

        String actual = FunctionalTestHelper.post(
                baseUrl + "/vendors/321/marketing/campaigns?uid=100501",
                request
        );
        String expected = getStringResource("/testCreateMarketingCampaignByVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Изменение кампании менеджером")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/testUpdateMarketingCampaignByManager/after.csv",
            dataSource = "vendorDataSource"
    )
    void testUpdateMarketingCampaignByManager() {
        String balanseResponse = getStringResource("/testUpdateMarketingCampaignByManager/serviceDatasourceBalanceResponse.json");
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1324/balance")
                .willReturn(WireMock.okJson(balanseResponse)));

        setVendorUserRoles(Collections.singleton(manager_user), 100500L);
        String request = getStringResource("/testUpdateMarketingCampaignByManager/request.json");

        String actual = FunctionalTestHelper.put(
                baseUrl + "/vendors/321/marketing/campaigns/103?uid=100500",
                request
        );
        String expected = getStringResource("/testUpdateMarketingCampaignByManager/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Изменение бизнес модели кампании")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/testUpdateCampaignBusinessModel/after.csv",
            dataSource = "vendorDataSource"
    )
    void testUpdateCampaignBusinessModel() {
        String balanseResponse = getStringResource("/testUpdateCampaignBusinessModel/serviceDatasourceBalanceResponse.json");
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1326/balance")
                .willReturn(WireMock.okJson(balanseResponse)));

        setVendorUserRoles(Collections.singleton(manager_user), 100500L);

        String request = getStringResource("/testUpdateCampaignBusinessModel/request.json");

        String actual = FunctionalTestHelper.put(
                baseUrl + "/vendors/321/marketing/campaigns/102?uid=100500",
                request);

        String expected = getStringResource("/testUpdateCampaignBusinessModel/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));

    }

    @Test
    @DisplayName("Изменение кампании менеджером (валидация доступного баланса)")
    void testUpdateMarketingCampaignByManagerWithNotEnoughMoney() {
        String balanseResponse = getStringResource("/testUpdateMarketingCampaignByManagerWithNotEnoughMoney/serviceDatasourceBalanceResponse.json");
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1324/balance")
                .willReturn(WireMock.okJson(balanseResponse)));

        setVendorUserRoles(Collections.singleton(manager_user), 100500L);
        String request = getStringResource("/testUpdateMarketingCampaignByManagerWithNotEnoughMoney/request.json");

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        baseUrl + "/vendors/321/marketing/campaigns/103?uid=100500",
                        request
                )
        );
        String actual = exception.getResponseBodyAsString();
        String expected = getStringResource("/testUpdateMarketingCampaignByManagerWithNotEnoughMoney/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Изменение кампании вендором")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/testUpdateMarketingCampaignByVendor/after.csv",
            dataSource = "vendorDataSource"
    )
    void testUpdateMarketingCampaignByVendor() {
        String balanseResponse = getStringResource("/testUpdateMarketingCampaignByVendor/serviceDatasourceBalanceResponse.json");
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1324/balance")
                .willReturn(WireMock.okJson(balanseResponse)));

        setVendorUserRoles(Collections.singleton(admin_user), 100501L, 321L);
        String request = getStringResource("/testUpdateMarketingCampaignByVendor/request.json");

        String actual = FunctionalTestHelper.put(
                baseUrl + "/vendors/321/marketing/campaigns/103?uid=100501",
                request
        );
        String expected = getStringResource("/testUpdateMarketingCampaignByVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Удаление кампании по ID")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/testDeleteMarketingCampaignById/after.csv",
            dataSource = "vendorDataSource"
    )
    void testDeleteMarketingCampaignById() {
        String balanseResponse = getStringResource("/testDeleteMarketingCampaignById/serviceDatasourceBalanceResponse.json");
        csBillingApiMock.stubFor(WireMock.get("/service/132/datasource/1327/balance")
                .willReturn(WireMock.okJson(balanseResponse)));

        csBillingApiMock.stubFor(WireMock.put("/service/132/datasource/1327/dynamicCost?uid=100500")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/testDeleteMarketingCampaignById/csBillingApiRequest.json"), true, false))
                .willReturn(WireMock.ok()));

        setVendorUserRoles(Collections.singleton(manager_user), 100500L);
        String actual = FunctionalTestHelper.deleteWithAuth(baseUrl + "/vendors/321/marketing/campaigns/101?uid=100500");
        String expected = getStringResource("/testDeleteMarketingCampaignById/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Удаление не существующей кампании по ID")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/testDeleteMarketingCampaignByWrongId/after.csv",
            dataSource = "vendorDataSource"
    )
    void testDeleteMarketingCampaignByWrongId() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.delete(baseUrl + "/vendors/321/marketing/campaigns/1010?uid=100500")
        );
        String actual = exception.getResponseBodyAsString();
        String expected = getStringResource("/testDeleteMarketingCampaignByWrongId/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Удаление чужой кампании по ID")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/testDeleteWrongVendorMarketingCampaignById/after.csv",
            dataSource = "vendorDataSource"
    )
    void testDeleteWrongVendorMarketingCampaignById() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.delete(baseUrl + "/vendors/321/marketing/campaigns/105?uid=100500")
        );
        String actual = exception.getResponseBodyAsString();
        String expected = getStringResource("/testDeleteWrongVendorMarketingCampaignById/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Подтверждение кампании менеджером")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/testApproveMarketingCampaignByManager/after.csv",
            dataSource = "vendorDataSource"
    )
    void testApproveMarketingCampaignByManager() {
        csBillingApiMock.stubFor(WireMock.put("/service/132/datasource/1322/dynamicCost?uid=100555")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/testApproveMarketingCampaignByManager/csBillingApiRequest.json"), true, false))
                .willReturn(WireMock.ok()));

        setVendorUserRoles(Collections.singleton(manager_user), 100555L);
        String actual = FunctionalTestHelper.post(
                baseUrl + "/vendors/321/marketing/campaigns/103/approval?uid=100555");
        String expected = getStringResource("/testApproveMarketingCampaignByManager/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Отмена подтверждения кампании менеджером")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/testDisapproveMarketingCampaignByManager/after.csv",
            dataSource = "vendorDataSource"
    )
    void testDisapproveMarketingCampaignByManager() {
        csBillingApiMock.stubFor(WireMock.put("/service/132/datasource/1322/dynamicCost?uid=100555")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/testDisapproveMarketingCampaignByManager/csBillingApiRequest.json"), true, false))
                .willReturn(WireMock.ok()));

        setVendorUserRoles(Collections.singleton(manager_user), 100555L);
        String actual = FunctionalTestHelper.delete(
                baseUrl + "/vendors/321/marketing/campaigns/103/approval?uid=100555");
        String expected = getStringResource("/testDisapproveMarketingCampaignByManager/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Подтверждение кампании вендором")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/testApproveMarketingCampaignByVendor/after.csv",
            dataSource = "vendorDataSource"
    )
    void testApproveMarketingCampaignByVendor() {
        csBillingApiMock.stubFor(WireMock.put("/service/132/datasource/1322/dynamicCost?uid=100501")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/testApproveMarketingCampaignByVendor/csBillingApiRequest.json"), true, false))
                .willReturn(WireMock.ok()));

        setVendorUserRoles(Collections.singleton(admin_user), 100501L, 321L);
        String actual = FunctionalTestHelper.post(
                baseUrl + "/vendors/321/marketing/campaigns/103/approval?uid=100501");
        String expected = getStringResource("/testApproveMarketingCampaignByVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Отмена подтверждения кампании вендором")
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/marketing/MarketingServicesCampaignControllerFunctionalTest/testDisapproveMarketingCampaignByVendor/after.csv",
            dataSource = "vendorDataSource"
    )
    void testDisapproveMarketingCampaignByVendor() {
        csBillingApiMock.stubFor(WireMock.put("/service/132/datasource/1322/dynamicCost?uid=100501")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/testDisapproveMarketingCampaignByVendor/csBillingApiRequest.json"), true, false))
                .willReturn(WireMock.ok()));

        setVendorUserRoles(Collections.singleton(admin_user), 100501L, 321L);
        String actual = FunctionalTestHelper.delete(
                baseUrl + "/vendors/321/marketing/campaigns/103/approval?uid=100501");
        String expected = getStringResource("/testDisapproveMarketingCampaignByVendor/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, when(Option.IGNORING_ARRAY_ORDER));
    }

}
