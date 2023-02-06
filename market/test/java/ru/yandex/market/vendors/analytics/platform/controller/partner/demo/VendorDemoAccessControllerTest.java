package ru.yandex.market.vendors.analytics.platform.controller.partner.demo;

import java.time.Clock;
import java.time.LocalDate;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.utils.DateUtils;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "VendorDemoAccessControllerTest.before.csv")
class VendorDemoAccessControllerTest extends FunctionalTest {

    @MockBean(name = "clock")
    private Clock clock;

    @Autowired
    private NamedParameterJdbcTemplate postgresJdbcTemplate;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(DateUtils.toInstant(LocalDate.of(2020, 6, 18)));
        postgresJdbcTemplate.update(
                "ALTER SEQUENCE analytics.s_partner_shopid RESTART WITH 1000000",
                new MapSqlParameterSource()
        );
    }

    @Test
    @DisplayName("Предоставить демо-доступ")
    @DbUnitDataSet(after = "VendorDemoAccessControllerTest.grantAccess.after.csv")
    void grantAccess() {
        var request = "{"
                + "  \"requestedBy\": 1,"
                + "  \"uids\": [1, 2],"
                + "  \"hid\": 91122,"
                + "  \"brandId\": 1,"
                + "  \"advertisingAgreement\": true"
                + "}";
        var response = grantAccess(1L, request);
        var expected = "{"
                + "  \"vendorId\": 1,"
                + "  \"requestedBy\": 1,"
                + "  \"grantedDate\": \"2020-06-18\","
                + "  \"expiredDate\": \"2020-06-25\","
                + "  \"active\": true,"
                + "  \"hid\": 91122,"
                + "    \"categoryName\": \"Игровые приставки\""
                + "}";
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Предоставить демо-доступ без подписки на рассылку")
    @DbUnitDataSet(after = "VendorDemoAccessControllerTest.grantAccessWithoutSubscription.after.csv")
    void grantAccessWithoutSubscription() {
        var request = "{"
                + "  \"requestedBy\": 1,"
                + "  \"uids\": [1, 2],"
                + "  \"advertisingAgreement\": false"
                + "}";
        var response = grantAccess(1L, request);
        var expected = "{"
                + "  \"vendorId\": 1,"
                + "  \"requestedBy\": 1,"
                + "  \"grantedDate\": \"2020-06-18\","
                + "  \"expiredDate\": \"2020-06-25\","
                + "  \"active\": true,"
                + "  \"hid\": 91122,"
                + "    \"categoryName\": \"Игровые приставки\""
                + "}";
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Демо-доступ уже был использован")
    @DbUnitDataSet(after = "VendorDemoAccessControllerTest.before.csv")
    void failedGrantAccess() {
        var request = "{"
                + "  \"requestedBy\": 1,"
                + "  \"uids\": [1, 2]"
                + "}";
        var exception = assertThrows(HttpClientErrorException.class, () -> grantAccess(2L, request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        var expected = "{"
                + "  \"code\": \"DEMO_ACCESS_ALREADY_USED\","
                + "  \"message\": \"Vendor 2 already used demo access\""
                + "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }


    @Test
    @DisplayName("Получить информацию о демо-доступе: активен")
    void getAccessInfoActive() {
        var response = getAccessInfo(3L);
        var expected = "{"
                + "  \"vendorId\": 3,"
                + "  \"requestedBy\": 5,"
                + "  \"grantedDate\": \"2020-06-11\","
                + "  \"expiredDate\": \"2020-06-18\","
                + "  \"active\": false,"
                + "  \"hid\": 91122,"
                + "    \"categoryName\": \"Игровые приставки\""
                + "}";
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получить информацию о демо-доступе: неактивен")
    void getAccessInfoInactive() {
        var response = getAccessInfo(2L);
        var expected = "{"
                + "  \"vendorId\": 2,"
                + "  \"requestedBy\": 4,"
                + "  \"grantedDate\": \"2020-06-10\","
                + "  \"expiredDate\": \"2020-06-17\","
                + "  \"active\": false,"
                + "  \"hid\": 91122,"
                + "    \"categoryName\": \"Игровые приставки\""
                + "}";
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получить информацию о демо-доступе: не был использован")
    void getAccessInfoNull() {
        var response = getAccessInfo(1L);
        assertJsonEquals(null, response);
    }

    @Nullable
    private String grantAccess(long vendorId, String request) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("vendors", "{vendorId}", "demo")
                .buildAndExpand(vendorId)
                .toUriString();
        return FunctionalTestHelper.postForJson(url, request);
    }

    @Nullable
    private String getAccessInfo(long vendorId) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("vendors", "{vendorId}", "demo")
                .buildAndExpand(vendorId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }
}