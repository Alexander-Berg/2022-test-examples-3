package ru.yandex.market.vendors.analytics.platform.controller.partner.registration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.common.framework.user.UserInfo;
import ru.yandex.common.framework.user.UserInfoService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.service.startrek.StartrekClient;
import ru.yandex.market.vendors.analytics.platform.controller.billing.BalanceFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Functional tests for {@link VendorController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "vendorRegistration.before.csv")
class VendorControllerTest extends BalanceFunctionalTest {

    @Autowired
    private StartrekClient startrekClient;
    @MockBean(name = "userInfoService")
    private UserInfoService userInfoService;

    @BeforeEach
    void setUp() {
        reset(startrekClient, userInfoService);
    }

    @Test
    @DisplayName("Подключение вендора")
    @DbUnitDataSet(after = "registerVendor.after.csv")
    void registerVendor() {
        String expected = ""
                + "{"
                + "  \"partner\": {"
                + "    \"id\": 100,"
                + "    \"type\": \"VENDOR\""
                + "  },"
                + "  \"categories\": ["
                + "    {"
                + "      \"hid\": 14,"
                + "      \"name\": \"14_имя\""
                + "    },"
                + "    {"
                + "      \"hid\": 20,"
                + "      \"name\": \"20_имя\""
                + "    },"
                + "    {"
                + "      \"hid\": 25,"
                + "      \"name\": \"25_имя\""
                + "    }"
                + "  ]"
                + "}";
        String requestBody = "{"
                + "  \"vendorName\": \"Вендор\","
                + "  \"contacts\": ["
                + "    {"
                + "      \"roles\": ["
                + "        \"ANALYTICS\""
                + "      ],"
                + "      \"uid\": 1500"
                + "    }"
                + "  ],"
                + "  \"hids\": ["
                + "    14, 20, 25"
                + "  ],"
                + "  \"brands\": ["
                + "    3, 12"
                + "  ]"
                + "}";
        String response = registerVendor(100L, requestBody);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Подключение вендора по оферте")
    @DbUnitDataSet(after = "registerOfferVendor.after.csv")
    void registerOfferVendor() {
        var userInfo = mock(UserInfo.class);
        when(userInfo.getLogin()).thenReturn("");
        when(userInfoService.getUserInfo(10001L)).thenReturn(userInfo);
        String expected = ""
                + "{"
                + "  \"partner\": {"
                + "    \"id\": 100,"
                + "    \"type\": \"VENDOR\""
                + "  },"
                + "  \"categories\":[]"
                + "}";
        String requestBody = "{"
                + "  \"contacts\": ["
                + "    {"
                + "      \"roles\": ["
                + "        \"ANALYTICS\""
                + "      ],"
                + "      \"uid\": 1500"
                + "    }"
                + "  ],"
                + "  \"brands\": ["
                + "    3, 12"
                + "  ],"
                + "  \"offer\": true,"
                + "  \"advertisingAgreement\": true"
                + "}";
        mockVendorDatasource(100, 1);
        String response = registerVendor(100L, 10001, requestBody);
        JsonTestUtil.assertEquals(expected, response);
        verify(startrekClient, times(1)).createTicket(any());
    }

    @Test
    @DisplayName("Подключение вендора к услуге без контактов (старый метод)")
    @DbUnitDataSet(after = "registerVendorWithoutContacts.after.csv")
    void registerVendorWithoutContacts() {
        String expected = ""
                + "{"
                + "  \"partner\": {"
                + "    \"id\": 40,"
                + "    \"type\": \"VENDOR\""
                + "  },"
                + "  \"categories\": ["
                + "    {"
                + "      \"hid\": 45,"
                + "      \"name\": \"45_имя\""
                + "    },"
                + "    {"
                + "      \"hid\": 90,"
                + "      \"name\": \"90_имя\""
                + "    }"
                + "  ]"
                + "}";
        String body = "{\"hids\" : [45, 90], \"brands\" : [10,20]}";
        String response = registerVendor(40L, body);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Попытка подключения вендора без категорий")
    void registerVendorWithoutHids() {
        String body = "{\"hids\" : [], \"brands\" : [10]}";
        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> registerVendor(40L, body)
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                "{\"code\":\"BAD_REQUEST\",\"message\":\"Categories doest't set for non-offer vendor\"}",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Попытка подключения вендора без брендов")
    void registerVendorWithoutBrands() {
        String body = "{\"hids\" : [10], \"brands\" : []}";
        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> registerVendor(40L, body)
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                "{\"code\":\"BAD_REQUEST\",\"message\":\"[brands can not be empty]\"}",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DbUnitDataSet(before = "registerExistedVendor.before.csv")
    @DisplayName("Попытка подключения уже существующего вендора")
    void registerExistedVendor() {
        String body = "{\"hids\" : [10], \"brands\" : [10]}";
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> registerVendor(4000L, body)
        );
        JsonTestUtil.assertEquals(
                "{\"code\":\"BAD_REQUEST\",\"message\":\"Partner already exists\"}",
                exception.getResponseBodyAsString()
        );
    }

    private String partnerRegistrationUrl(long partnerId) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/partners/{partnerId}")
                .buildAndExpand(partnerId)
                .toUriString();
    }

    private String registerVendor(long partnerId, String body) {
        return registerVendor(partnerId, 1, body);
    }

    private String registerVendor(long partnerId, long uid, String body) {
        String analyticsUrl = partnerRegistrationUrl(partnerId) + "?uid=" + uid;
        return FunctionalTestHelper.postForJson(analyticsUrl, body);
    }
}
