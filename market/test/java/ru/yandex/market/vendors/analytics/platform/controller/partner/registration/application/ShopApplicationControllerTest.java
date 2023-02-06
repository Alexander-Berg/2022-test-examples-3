package ru.yandex.market.vendors.analytics.platform.controller.partner.registration.application;

import java.util.Optional;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.model.partner.application.ApplicationStatus;
import ru.yandex.market.vendors.analytics.core.model.partner.shop.ShopDataSourceType;
import ru.yandex.market.vendors.analytics.core.security.AnalyticsTvmClient;
import ru.yandex.market.vendors.analytics.core.service.ga.GaProfileService;
import ru.yandex.market.vendors.analytics.core.service.startrek.StartrekClient;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.utils.json.JsonUnitUtils;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.vendors.analytics.core.model.partner.application.ApplicationStatus.APPROVED;
import static ru.yandex.market.vendors.analytics.core.model.partner.application.ApplicationStatus.METRIKA_NO_PERMISSION;
import static ru.yandex.market.vendors.analytics.core.model.partner.application.ApplicationStatus.NEED_ECOM;
import static ru.yandex.market.vendors.analytics.core.model.partner.shop.ShopDataSourceType.APP_METRIKA;
import static ru.yandex.market.vendors.analytics.core.model.partner.shop.ShopDataSourceType.METRIKA;
import static ru.yandex.market.vendors.analytics.core.utils.GaTestUtils.profile;
import static ru.yandex.market.vendors.analytics.core.utils.GaTestUtils.profiles;

/**
 * Functional tests for {@link ShopApplicationController}.
 *
 * @author sergeymironov
 */
@DbUnitDataSet(before = "ShopApplicationControllerTest.before.csv")
class ShopApplicationControllerTest extends FunctionalTest {

    private static final String HEADER_SERVICE_TICKET = "X-Ya-Service-Ticket";

    @Autowired
    private StartrekClient startrekClient;
    @Autowired
    private AnalyticsTvmClient analyticsTvmClient;
    @Autowired
    private RestTemplate metricsRestTemplate;
    @Autowired
    private NamedParameterJdbcTemplate postgresJdbcTemplate;
    @SpyBean
    private GaProfileService gaProfileService;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void resetMocks() {
        reset(startrekClient);
        reset(analyticsTvmClient);
        mockRestServiceServer = MockRestServiceServer.createServer(metricsRestTemplate);
        postgresJdbcTemplate.update(
                "ALTER SEQUENCE analytics.s_partner_shopid RESTART WITH 1000000",
                new MapSqlParameterSource()
        );
    }

    @Test
    @DisplayName("Регистрация заявки без настроенного ecom, статус NEED_ECOM")
    void registeredWithoutEcomAndInProgress() {
        mockStTicket("APIMPORT-3");

        int counterId = 1003;
        long uid = 1;
        String requestBody = getRequestBodyWithMetrika(uid, counterId, METRIKA);

        mockTvmAndMetrika(counterId, uid, true);

        String actual = registerApplications(requestBody);
        String expected = getExpectedResponseWithMetrika(uid, counterId, NEED_ECOM, METRIKA);

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Регистрация заявки с доступом к Я.Метрика и ecom")
    @DbUnitDataSet(after = "approveApplication.after.csv")
    @ClickhouseDbUnitDataSet(before = "ShopApplicationControllerTest.approve.clickhouse.csv")
    void approveApplication() {
        mockStTicket("APIMPORT-3");

        int counterId = 1000;
        long uid = 1;
        String requestBody = getRequestBodyWithMetrika(uid, counterId, METRIKA);

        mockTvmAndMetrika(counterId, uid, true);

        String actual = registerApplications(requestBody);
        String expected = getExpectedResponseWithMetrika(uid, counterId, APPROVED, METRIKA);

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Регистрация заявки с доступом к Апп.Метрике и ecom")
    void approveAppMetrikaApplication() {
        mockStTicket("APIMPORT-3");

        int counterId = 1000;
        long uid = 1;
        String requestBody = getRequestBodyWithMetrika(uid, counterId, APP_METRIKA);

        mockTvmAndAppMetrika(counterId, uid, true);

        String actual = registerApplications(requestBody);
        String expected = getExpectedResponseWithMetrika(uid, counterId, APPROVED, APP_METRIKA);

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Регистрация заявки с GA")
    @DbUnitDataSet(after = "ShopApplicationController.ga.after.csv")
    void gaApplication() {
        mockStTicket("APSALESIM-1");

        String requestBody = loadFromFile("ShopApplicationController.ga.request.json");
        String actual = registerApplications(requestBody);
        String expected = loadFromFile("ShopApplicationController.ga.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Регистрация заявки с GA без счётчика")
    void gaWithoutCounterApplication() {
        String requestBody = loadFromFile("ShopApplicationController.ga.bad.request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> registerApplications(requestBody)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        String expected = "{\n" +
                "  \"code\": \"NO_DATA\",\n" +
                "  \"message\": \"NO_ID_COUNTER\"\n" +
                "}";

        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Регистрация нескольких заявок с CSV")
    @DbUnitDataSet(after = "ShopApplicationController.csvApplications.after.csv")
    void csvApplications() {
        mockStTicket("APIMPORT-3");

        String requestBody = loadFromFile("ShopApplicationController.csvApplications.request.json");
        String actual = registerApplications(requestBody);
        String expected = loadFromFile("ShopApplicationController.csvApplications.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Отказ в добавлении заявки, т.к. заявка с таким uid + counterId Яндекс.Метрика уже существует")
    void applicationAlreadyExists() {
        int counterId = 1001;
        long uid = 1;
        String requestBody = getRequestBodyWithMetrika(uid, counterId, METRIKA);

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> registerApplications(requestBody)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        String expected = "{\n" +
                "  \"code\": \"ENTITY_ALREADY_EXISTS\",\n" +
                "  \"message\": \"APPLICATION_EXISTS\"\n" +
                "}";

        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Получить все заявки магазина пользователя")
    @DbUnitDataSet(before = "ShopApplicationControllerTest.getApplications.before.csv")
    void getApplications() {
        String actual = getApplications(1);
        var expected = loadFromFile("ShopApplicationController.getApplications.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Получить все заявки магазина пользователя вместе с профилями GA")
    @DbUnitDataSet(before = "ShopApplicationControllerTest.getApplications.before.csv")
    void getApplicationsWithGaProfiles() {
        long uid = 5;
        when(gaProfileService.loadProfilesSafe(eq(uid)))
                .thenReturn(Optional.of(profiles(
                        profile("0", "0", "UA-0-0", "Not ecom", "http://yandex.ru", false),
                        profile("77", "1", "UA-1-1", "Основной", "http://yandex.ru", true)
                )));
        String actual = getApplications(uid);
        var expected = loadFromFile("ShopApplicationController.getApplicationsWithGaProfiles.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Регистрация заявки, когда у пользователя нет прав доступа к счетчику Яндекс.метрика.")
    void noEditPermission() {
        mockStTicket("APIMPORT-3");

        int counterId = 55;
        long uid = 1;
        String requestBody = getRequestBodyWithMetrika(uid, counterId, METRIKA);

        mockTvmAndMetrika(counterId, uid, false);

        String actual = registerApplications(requestBody);
        String expected = getExpectedResponseWithMetrika(uid, counterId, METRIKA_NO_PERMISSION, METRIKA);

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Регистрация заявки, когда у пользователя нет прав доступа к счетчику Апп.метрики.")
    void noEditPermissionAppMetrika() {
        mockStTicket("APIMPORT-3");

        int counterId = 55;
        long uid = 1;
        String requestBody = getRequestBodyWithMetrika(uid, counterId, APP_METRIKA);

        mockTvmAndAppMetrika(counterId, uid, false);

        String actual = registerApplications(requestBody);
        String expected = getExpectedResponseWithMetrika(uid, counterId, METRIKA_NO_PERMISSION, APP_METRIKA);

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Регистрация заявки с созданием startrek_ticket, статус CSV_PROCESSING")
    @DbUnitDataSet(after = "sendTicketSaveApplication.after.csv")
    void sendTicketAndSaveApplication() {
        String requestBody = "[{\n" +
                "  \"passportInfo\": {\n" +
                "    \"uid\": 10\n" +
                "  },\n" +
                "  \"contactInfo\": {\n" +
                "    \"businessName\": \"ООО Яндекс\",\n" +
                "    \"fullName\": \"Другой Тикет Стартрек\",\n" +
                "    \"email\": \"ticket@yandex.ru\",\n" +
                "    \"phoneNumber\": \"89991234567\"\n" +
                "  },\n" +
                "  \"shopInfo\": {\n" +
                "    \"shopDataSource\": \"CSV\",\n" +
                "    \"domain\":\"domain.ru\"\n" +
                "  },\n" +
                "  \"serviceInfo\": {\n" +
                "    \"offerAccepted\": true\n" +
                "  }\n" +
                "}]";

        mockStTicket("APIMPORT-3");

        String actual = registerApplications(requestBody);
        String expected = "[{\n" +
                "   \"passportInfo\":{\n" +
                "      \"uid\":10\n" +
                "   },\n" +
                "   \"contactInfo\":{\n" +
                "      \"businessName\":\"ООО Яндекс\",\n" +
                "      \"fullName\":\"Другой Тикет Стартрек\",\n" +
                "      \"email\":\"ticket@yandex.ru\",\n" +
                "      \"phoneNumber\":\"89991234567\"\n" +
                "   },\n" +
                "   \"shopInfo\":{\n" +
                "      \"shopDataSource\":\"CSV\",\n" +
                "      \"counterId\":null,\n" +
                "      \"supplierId\": null," +
                "      \"domain\":\"domain.ru\"\n" +
                "   },\n" +
                "   \"serviceInfo\":{\n" +
                "      \"applicationStatus\":\"CSV_PROCESSING\",\n" +
                "      \"creationTime\": \"${json-unit.ignore}\",\n" +
                "      \"modificationTime\": \"${json-unit.ignore}\",\n" +
                "      \"applicationId\": \"${json-unit.ignore}\",\n" +
                "      \"offerAccepted\": true\n" +
                "   }\n" +
                "}]";
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Получить список доменов с ecom, зарегистрированных за партнером пользователя")
    @DbUnitDataSet(before = "ShopApplicationController.getEcomDomains.before.csv")
    void getEcomDomains() {
        long uid = 5;
        String actual = getEcomDomainsResponse(uid);
        var expected = loadFromFile("ShopApplicationController.getEcomDomains.response.json");
        JsonUnitUtils.assertJsonEqualsIgnoreArrayOrder(expected, actual);
    }

    private String registerApplications(String requestBody) {
        String analyticsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("shops", "applications")
                .toUriString();
        return FunctionalTestHelper.postForJson(analyticsUrl, requestBody);
    }

    private String getApplications(long uid) {
        String analyticsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("shops", "applications")
                .queryParam("uid", uid)
                .toUriString();
        return FunctionalTestHelper.get(analyticsUrl).getBody();
    }

    private String getEcomDomainsResponse(long uid) {
        String analyticsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("shops", "domains")
                .queryParam("uid", uid)
                .toUriString();
        return FunctionalTestHelper.get(analyticsUrl).getBody();
    }

    private void mockTvmAndAppMetrika(int counterId, long uid, boolean hasAccess) {
        String url = "http://mobmet-intapi-test.metrika.yandex.net/market_analytics/check_access";
        String serviceTicketValue = "service_ticket_for_test";
        when(analyticsTvmClient.getServiceTicketForAppMetrika()).thenReturn(serviceTicketValue);

        String uidString = hasAccess ? String.valueOf(uid) : "";
        String response = "{\n"
                + "  \"result\": [\n"
                + "    {\n"
                + "      \"application_id\": " + counterId + ",\n"
                + "      \"uids\": [\n"
                + "        " + uidString + "\n"
                + "      ]\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        mockRestServiceServer.expect(
                ExpectedCount.once(),
                requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(HEADER_SERVICE_TICKET, serviceTicketValue))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response)
                );
    }

    private void mockTvmAndMetrika(int counterId, long uid, boolean hasAccess) {
        String url = "http://internalapi-test.metrika.yandex.ru:8096/market_analytics/check_access";
        String serviceTicketValue = "service_ticket_for_test";
        when(analyticsTvmClient.getServiceTicketForYandexMetrika()).thenReturn(serviceTicketValue);

        String uidString = hasAccess ? String.valueOf(uid) : "";
        String response = "{\n"
                + "  \"result\": [\n"
                + "    {\n"
                + "      \"counter_id\": " + counterId + ",\n"
                + "      \"uids\": [\n"
                + "        " + uidString + "\n"
                + "      ]\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        mockRestServiceServer.expect(
                ExpectedCount.once(),
                requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(HEADER_SERVICE_TICKET, serviceTicketValue))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response)
                );
    }

    private String getRequestBodyWithMetrika(long uid, int counterId, ShopDataSourceType dataSourceType) {
        return "[{\n" +
                "  \"passportInfo\": {\n" +
                "    \"uid\": " + uid + "\n" +
                "  },\n" +
                "  \"contactInfo\": {\n" +
                "    \"businessName\": \"ООО Яндекс\",\n" +
                "    \"fullName\": \"Петров Петр Петрович\",\n" +
                "    \"email\": \"yandex@yandex.ru\",\n" +
                "    \"phoneNumber\": \"89991234567\"\n" +
                "  },\n" +
                "  \"shopInfo\": {\n" +
                "    \"shopDataSource\": \"" + dataSourceType + "\",\n" +
                "    \"counterId\": " + counterId + ",\n" +
                "    \"domain\": \"domain.ru\"\n" +
                "  },\n" +
                "  \"serviceInfo\": {\n" +
                "    \"offerAccepted\": true\n" +
                "  }\n" +
                "}]";
    }

    private String getExpectedResponseWithMetrika(
            long uid,
            long counterId,
            ApplicationStatus applicationStatus,
            ShopDataSourceType dataSourceType
    ) {
        return "[{\n" +
                "   \"passportInfo\":{\n" +
                "     \"uid\":" + uid + "\n" +
                "   },\n" +
                "   \"contactInfo\":{\n" +
                "      \"businessName\":\"ООО Яндекс\",\n" +
                "      \"fullName\":\"Петров Петр Петрович\",\n" +
                "      \"email\":\"yandex@yandex.ru\",\n" +
                "      \"phoneNumber\":\"89991234567\"\n" +
                "   },\n" +
                "   \"shopInfo\":{\n" +
                "    \"shopDataSource\": \"" + dataSourceType + "\",\n" +
                "      \"counterId\":" + counterId + ",\n" +
                "      \"supplierId\":null,\n" +
                "      \"domain\":\"domain.ru\"\n" +
                "   },\n" +
                "   \"serviceInfo\":{\n" +
                "      \"applicationStatus\":\"" + applicationStatus.toString() + "\",\n" +
                "      \"creationTime\": \"${json-unit.ignore}\",\n" +
                "      \"modificationTime\": \"${json-unit.ignore}\",\n" +
                "      \"applicationId\": \"${json-unit.ignore}\",\n" +
                "      \"offerAccepted\": true\n" +
                "   }\n" +
                "}]";
    }

    private void mockStTicket(String key) {
        var issue = mock(Issue.class);
        when(issue.getKey()).thenReturn(key);
        Mockito.when(startrekClient.createTicket(any())).thenReturn(issue);
    }
}
