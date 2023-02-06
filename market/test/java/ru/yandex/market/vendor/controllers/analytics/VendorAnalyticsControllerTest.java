package ru.yandex.market.vendor.controllers.analytics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.cs.billing.balance.BalanceService;
import ru.yandex.cs.billing.balance.model.BalanceClientUser;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.balance.xmlrpc.model.ClientUserStructure;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.market.vendor.util.VendorUrlBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Functional tests for {@link VendorAnalyticsController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/analytics/VendorAnalyticsControllerTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/analytics/VendorAnalyticsControllerTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class VendorAnalyticsControllerTest extends AbstractVendorPartnerFunctionalTest {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private Clock clock;

    @Autowired
    private WireMockServer csBillingApiMock;

    @BeforeEach
    void initMocks() {
        LocalDateTime testCaseNow = LocalDateTime.of(2019, 1, 26, 0, 0, 0);
        when(clock.instant()).thenReturn(TimeUtil.toInstant(testCaseNow));
        reset(balanceService);
    }

    private static final long UID = 7L;
    private static final long VENDOR_ID = 1991L;
    private static final long VENDOR_WITH_CUTOFF_ID = 1993L;
    private static final BalanceClientUser BALANCE_USER = createBalanceUser();

    @Test
    @DisplayName("Услуга не найдена")
    void notFound() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> getAnalytics(100500L)
        );
        Assertions.assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }

    @Test
    @DisplayName("Услуга найдена")
    void found() {
        JsonAssert.assertJsonEquals(
                getStringResource("/found/expected.json"),
                getAnalytics(VENDOR_ID)
        );
    }

    @Test
    @DisplayName("Услуга аналитики уже подключена")
    void alreadyCompleted() {
        String body = getStringResource("/alreadyCompleted/request.json");
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> createAnalytics(VENDOR_ID, body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @Test
    @DisplayName("Попытка подключить офертную услугу")
    void createOfferProduct() {
        assertThrows(
                HttpClientErrorException.class,
                () -> createAnalytics(
                        VENDOR_ID,
                        getStringResource("/createOfferProduct/request.json")
                )
        );
    }

    @Test
    @DisplayName("Подключение услуги Маркет.Аналитика")
    void enableAnalytics() {
        when(balanceService.getClientUsers(1006540))
                .thenReturn(Collections.singletonList(BALANCE_USER));
        String body = getStringResource("/enableAnalytics/request.json");

        csBillingApiMock.stubFor(WireMock.get("/api/v1/tariffs/params/search?tariffParamNames=IS_DEFAULT_ANALYTICS")
                .willReturn(aResponse().withBody(getStringResource("/enableAnalytics/retrofit2_response.json"))));

        //Падает из-за вызова функции CS_BILLING.CREATE_CAMPAIGN
        assertThrows(
                HttpServerErrorException.class,
                () -> createAnalytics(VENDOR_ID + 1, body)
        );
    }

    @Test
    @DisplayName("Попытка сделать услугу оффертной")
    void updateIsOfferTrue() {
        assertThrows(
                HttpClientErrorException.class,
                () -> updateAnalytics(
                        VENDOR_ID,
                        getStringResource("/updateIsOfferTrue/request.json")
                )
        );
    }

    @Test
    @DisplayName("Обновление услуги Маркет.Аналитика")
    void updateMarketAnalytics() {
        when(balanceService.getClientUsers(1006540))
                .thenReturn(Collections.singletonList(BALANCE_USER));
        String body = getStringResource("/updateMarketAnalytics/request.json");
        JsonAssert.assertJsonEquals(
                getStringResource("/updateMarketAnalytics/expected.json"),
                updateAnalytics(VENDOR_ID, body)
        );
    }

    @Test
    @DisplayName("Обновление услуги Маркет.Аналитика с лишним id контракта")
    void updateMarketAnalyticsBadContract() {

        String body = getStringResource("/updateMarketAnalyticsBadContract/request.json");
        assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> updateAnalytics(VENDOR_ID, body)
        );
    }

    @Test
    @DisplayName("Обновление услуги Маркет.Аналитика с неверным типом контракта")
    void updateMarketAnalyticsBadType() {
        String body = getStringResource("/updateMarketAnalyticsBadType/request.json");
        assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> updateAnalytics(VENDOR_ID, body)
        );
    }

    @Test
    @DisplayName("Обновление услуги без id нтракта")
    void updateMarketAnalyticsWithoutContract() {
        String body = getStringResource("/updateMarketAnalyticsWithoutContract/request.json");
        assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> updateAnalytics(VENDOR_ID, body)
        );
    }

    @Test
    @DisplayName("Обновление даты подключения услуги Маркет.Аналитика")
    void updateMarketAnalyticsStartDate() {
        when(balanceService.getClientUsers(1006540))
                .thenReturn(Collections.singletonList(BALANCE_USER));

        String newStartDate = DATE_FORMAT.format(DateUtil.addDay(TimeUtil.toDate(clock.instant()), 5));

        String body = getStringResourceWithStartDate("/updateMarketAnalyticsStartDate/request.json", newStartDate);
        JsonAssert.assertJsonEquals(
                getStringResourceWithStartDate("/updateMarketAnalyticsStartDate/expected.json", newStartDate),
                updateAnalytics(VENDOR_WITH_CUTOFF_ID, body)
        );
    }

    @Test
    @DisplayName("Попытка сделать дату запуска раньше даты создания кампании")
    void updateMarketAnalyticsStartDateBeforeCampaignStart() {
        String body = getStringResource("/updateMarketAnalyticsStartDateBeforeCampaignStart/request.json");
        assertThrows(
                HttpClientErrorException.class,
                () -> updateAnalytics(VENDOR_WITH_CUTOFF_ID, body)
        );
    }

    @Test
    @DisplayName("Попытка сдвинуть дату запуска уже запущенной услуги")
    void updateMarketAnalyticsStartDateAlreadyPassed() {
        String body = getStringResource("/updateMarketAnalyticsStartDateAlreadyPassed/request.json");
        assertThrows(
                HttpClientErrorException.class,
                () -> updateAnalytics(VENDOR_ID, body)
        );
    }

    private String getAnalytics(long vendorId) {
        String analyticsUrl = VendorUrlBuilder.analyticsUrl(baseUrl, vendorId, UID);
        return FunctionalTestHelper.get(analyticsUrl, String.class);
    }

    private ResponseEntity<String> createAnalytics(long vendorId, String body) {
        String analyticsUrl = VendorUrlBuilder.analyticsUrl(baseUrl, vendorId, UID);
        return FunctionalTestHelper.postForEntity(analyticsUrl, body);
    }

    private String updateAnalytics(long vendorId, String body) {
        String analyticsUrl = VendorUrlBuilder.analyticsUrl(baseUrl, vendorId, UID);
        return FunctionalTestHelper.put(analyticsUrl, body);
    }

    private String getStringResourceWithStartDate(String path, String startDate) {
        return String.format(getStringResource(path), startDate);
    }

    private static BalanceClientUser createBalanceUser() {
        ClientUserStructure clientUserStructure = new ClientUserStructure();
        clientUserStructure.setPassportId(123456789L);
        return new BalanceClientUser(clientUserStructure);
    }
}
