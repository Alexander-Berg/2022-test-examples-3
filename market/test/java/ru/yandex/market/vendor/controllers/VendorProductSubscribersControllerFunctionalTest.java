package ru.yandex.market.vendor.controllers;

import com.github.tomakehurst.wiremock.WireMockServer;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class VendorProductSubscribersControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    @Autowired
    private WireMockServer blackboxMock;

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testGetSubscribers/before.csv",
            dataSource = "vendorDataSource"
    )
    void testGetSubscribers() {
        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetSubscribers/blackbox_response.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/subscribers?uid=0");
        String expected = getStringResource("/testGetSubscribers/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testPostAllProductsEmailSubscriber() {
        String request = getStringResource("/testPostAllProductsEmailSubscriber/request.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/subscribers?subscriberType=EMAIL&uid=0", request);
        String expected = getStringResource("/testPostAllProductsEmailSubscriber/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testPostAllProductsEmailSubscriberWithoutId() {
        String request = getStringResource("/testPostAllProductsEmailSubscriberWithoutId/request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/321/subscribers?subscriberType=EMAIL&uid=0", request)
        );
        String response = exception.getResponseBodyAsString();
        String expected = getStringResource("/testPostAllProductsEmailSubscriberWithoutId/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testDeleteAllProductsEmailSubscriber/before.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testDeleteAllProductsEmailSubscriber/after.csv",
            dataSource = "vendorDataSource"
    )
    void testDeleteAllProductsEmailSubscriber() {
        String request = getStringResource("/testDeleteAllProductsEmailSubscriber/request.json");
        String response = FunctionalTestHelper.delete(baseUrl + "/vendors/321/subscribers?subscriberType=EMAIL&uid=0", request);
        String expected = getStringResource("/testDeleteAllProductsEmailSubscriber/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testDeleteAllProductsEmailSubscriberWithoutId/before.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testDeleteAllProductsEmailSubscriberWithoutId/after.csv",
            dataSource = "vendorDataSource"
    )
    void testDeleteAllProductsEmailSubscriberWithoutId() {
        String request = getStringResource("/testDeleteAllProductsEmailSubscriberWithoutId/request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.delete(baseUrl + "/vendors/321/subscribers?subscriberType=EMAIL&uid=0", request)
        );
        String response = exception.getResponseBodyAsString();
        String expected = getStringResource("/testDeleteAllProductsEmailSubscriberWithoutId/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testPostAllProductsNotificationCenterSubscriber() {
        String request = getStringResource("/testPostAllProductsNotificationCenterSubscriber/request_post.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/subscribers?subscriberType=LOGIN&uid=0", request);
        String expected = getStringResource("/testPostAllProductsNotificationCenterSubscriber/expected_post.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testPostAllProductsNotificationCenterSubscriberWithoutId() {
        String request = getStringResource("/testPostAllProductsNotificationCenterSubscriberWithoutId/request_post.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/321/subscribers?subscriberType=LOGIN&uid=0", request)
        );
        String response = exception.getResponseBodyAsString();
        String expected = getStringResource("/testPostAllProductsNotificationCenterSubscriberWithoutId/expected_post.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testDeleteAllProductsNotificationCenterSubscriber/before.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testDeleteAllProductsNotificationCenterSubscriber/after.csv",
            dataSource = "vendorDataSource"
    )
    void testDeleteAllProductsNotificationCenterSubscriber() {
        String request = getStringResource("/testDeleteAllProductsNotificationCenterSubscriber/request.json");
        String response = FunctionalTestHelper.delete(baseUrl + "/vendors/321/subscribers?subscriberType=LOGIN&uid=0", request);
        String expected = getStringResource("/testDeleteAllProductsNotificationCenterSubscriber/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testDeleteAllProductsNotificationCenterSubscriberWithoutId/before.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testDeleteAllProductsNotificationCenterSubscriberWithoutId/after.csv",
            dataSource = "vendorDataSource"
    )
    void testDeleteAllProductsNotificationCenterSubscriberWithoutId() {
        String request = getStringResource("/testDeleteAllProductsNotificationCenterSubscriberWithoutId/request.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.delete(baseUrl + "/vendors/321/subscribers?subscriberType=LOGIN&uid=0", request)
        );
        String response = exception.getResponseBodyAsString();
        String expected = getStringResource("/testDeleteAllProductsNotificationCenterSubscriberWithoutId/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"recommended", "modelbids", "brandzone", "marketing"})
    void testPostPaidProductEmailSubscriber(String product) {
        String requestPost = getStringResource("/testPostPaidProductEmailSubscriber/request_post.json");
        String responsePost = FunctionalTestHelper.post(baseUrl + "/vendors/321/" + product + "/subscribers?subscriberType=EMAIL&uid=0", requestPost);
        String expected = getStringResource("/testPostPaidProductEmailSubscriber/expected_post.json");
        JsonAssert.assertJsonEquals(expected, responsePost, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"recommended", "modelbids", "brandzone", "marketing"})
    void testPostPaidProductEmailSubscriberWithoutId(String product) {
        String requestPost = getStringResource("/testPostPaidProductEmailSubscriberWithoutId/request_post.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/321/" + product + "/subscribers?subscriberType=EMAIL&uid=0", requestPost)
        );
        String responsePost = exception.getResponseBodyAsString();
        String expected = getStringResource("/testPostPaidProductEmailSubscriberWithoutId/expected_post.json");
        JsonAssert.assertJsonEquals(expected, responsePost, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"recommended", "modelbids", "brandzone", "marketing"})
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testDeletePaidProductEmailSubscriber/before.csv",
            dataSource = "vendorDataSource"
    )
    void testDeletePaidProductEmailSubscriber(String product) {
        String request = getStringResource("/testDeletePaidProductEmailSubscriber/request_delete.json");
        String response = FunctionalTestHelper.delete(baseUrl + "/vendors/321/" + product + "/subscribers?subscriberType=EMAIL&uid=0", request);
        String expected = getStringResource("/testDeletePaidProductEmailSubscriber/expected_delete.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"recommended", "modelbids", "brandzone", "marketing"})
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testDeletePaidProductEmailSubscriberWithoutId/before.csv",
            dataSource = "vendorDataSource"
    )
    void testDeletePaidProductEmailSubscriberWithoutId(String product) {
        String request = getStringResource("/testDeletePaidProductEmailSubscriberWithoutId/request_delete.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.delete(baseUrl + "/vendors/321/" + product + "/subscribers?subscriberType=EMAIL&uid=0", request)
        );
        String response = exception.getResponseBodyAsString();
        String expected = getStringResource("/testDeletePaidProductEmailSubscriberWithoutId/expected_delete.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"recommended", "modelbids", "brandzone", "marketing"})
    void testPostPaidProductNotificationCenterSubscriber(String product) {
        String request = getStringResource("/testPostPaidProductNotificationCenterSubscriber/request_post.json");
        String response = FunctionalTestHelper.post(baseUrl + "/vendors/321/" + product + "/subscribers?subscriberType=LOGIN&uid=0", request);
        String expected = getStringResource("/testPostPaidProductNotificationCenterSubscriber/expected_post.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"recommended", "modelbids", "brandzone", "marketing"})
    void testPostPaidProductNotificationCenterSubscriberWithoutId(String product) {
        String request = getStringResource("/testPostPaidProductNotificationCenterSubscriberWithoutId/request_post.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/321/" + product + "/subscribers?subscriberType=LOGIN&uid=0", request)
        );
        String response = exception.getResponseBodyAsString();
        String expected = getStringResource("/testPostPaidProductNotificationCenterSubscriberWithoutId/expected_post.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"recommended", "modelbids", "brandzone", "marketing"})
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testDeletePaidProductNotificationCenterSubscriber/before.csv",
            dataSource = "vendorDataSource"
    )
    void testDeletePaidProductNotificationCenterSubscriber(String product) {
        String request = getStringResource("/testDeletePaidProductNotificationCenterSubscriber/request_delete.json");
        String response = FunctionalTestHelper.delete(baseUrl + "/vendors/321/" + product + "/subscribers?subscriberType=LOGIN&uid=0", request);
        String expected = getStringResource("/testDeletePaidProductNotificationCenterSubscriber/expected_delete.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"recommended", "modelbids", "brandzone", "marketing"})
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testDeletePaidProductNotificationCenterSubscriberWithoutId/before.csv",
            dataSource = "vendorDataSource"
    )
    void testDeletePaidProductNotificationCenterSubscriberWithoutId(String product) {
        String request = getStringResource("/testDeletePaidProductNotificationCenterSubscriberWithoutId/request_delete.json");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.delete(baseUrl + "/vendors/321/" + product + "/subscribers?subscriberType=LOGIN&uid=0", request)
        );
        String response = exception.getResponseBodyAsString();
        String expected = getStringResource("/testDeletePaidProductNotificationCenterSubscriberWithoutId/expected_delete.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testGetAvailableSubscribersUsers/before.csv",
            dataSource = "vendorDataSource"
    )
    void testGetAvailableSubscribersUsers() {
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100500"))
                .willReturn(aResponse().withBody(getStringResource("/testGetAvailableSubscribersUsers/blackbox_response_100500.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/subscribers/users?uid=0");
        String expected = getStringResource("/testGetAvailableSubscribersUsers/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Проверяет, что фильтр возвращает логины, которые именно НАЧИНАЮТСЯ на log
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testGetAvailableSubscribersUsersWithLoginFilter/before.csv",
            dataSource = "vendorDataSource"
    )
    void testGetAvailableSubscribersUsersWithLoginFilter() {
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100500"))
                .willReturn(aResponse().withBody(getStringResource("/testGetAvailableSubscribersUsersWithLoginFilter/blackbox_response_100500.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/subscribers/users?uid=0&login=og");
        String expected = getStringResource("/testGetAvailableSubscribersUsersWithLoginFilter/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Один user c логином на log
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testGetAvailableSubscribersUsersWithLoginFilterForOneLogin/before.csv",
            dataSource = "vendorDataSource"
    )
    void testGetAvailableSubscribersUsersWithLoginFilterForOneLogin() {
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100500"))
                .willReturn(aResponse().withBody(getStringResource("/testGetAvailableSubscribersUsersWithLoginFilterForOneLogin/blackbox_response_100500.json"))));
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100501"))
                .willReturn(aResponse().withBody(getStringResource("/testGetAvailableSubscribersUsersWithLoginFilterForOneLogin/blackbox_response_100501.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/subscribers/users?uid=0&login=log");
        String expected = getStringResource("/testGetAvailableSubscribersUsersWithLoginFilterForOneLogin/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Логины у юзеров имеют одинаковый префикс выбираются оба
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testGetAvailableSubscribersUsersWithLoginFilterForTwoLogins/before.csv",
            dataSource = "vendorDataSource"
    )
    void testGetAvailableSubscribersUsersWithLoginFilterForTwoLogins() {
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100500"))
                .willReturn(aResponse().withBody(getStringResource("/testGetAvailableSubscribersUsersWithLoginFilterForTwoLogins/blackbox_response_100500.json"))));
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100501"))
                .willReturn(aResponse().withBody(getStringResource("/testGetAvailableSubscribersUsersWithLoginFilterForTwoLogins/blackbox_response_100501.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/subscribers/users?uid=0&login=log");
        String expected = getStringResource("/testGetAvailableSubscribersUsersWithLoginFilterForTwoLogins/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Логины у юзеров имеют разные префиксы, выбирается только один
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testGetAvailableSubscribersUsersWithLoginFilterForTwoDifferentLogins/before.csv",
            dataSource = "vendorDataSource"
    )
    void testGetAvailableSubscribersUsersWithLoginFilterForTwoDifferentLogins() {
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100500"))
                .willReturn(aResponse().withBody(getStringResource("/testGetAvailableSubscribersUsersWithLoginFilterForTwoDifferentLogins/blackbox_response_100500.json"))));
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100501"))
                .willReturn(aResponse().withBody(getStringResource("/testGetAvailableSubscribersUsersWithLoginFilterForTwoDifferentLogins/blackbox_response_100501.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/subscribers/users?uid=0&login=Petya");
        String expected = getStringResource("/testGetAvailableSubscribersUsersWithLoginFilterForTwoDifferentLogins/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * На вход фильтра пришла пустая строка должны прийти все
     */
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testGetAvailableSubscribersUsersWithLoginWithEmptyFilter/before.csv",
            dataSource = "vendorDataSource"
    )
    void testGetAvailableSubscribersUsersWithLoginWithEmptyFilter() {
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100500"))
                .willReturn(aResponse().withBody(getStringResource("/testGetAvailableSubscribersUsersWithLoginWithEmptyFilter/blackbox_response_100500.json"))));
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100501"))
                .willReturn(aResponse().withBody(getStringResource("/testGetAvailableSubscribersUsersWithLoginWithEmptyFilter/blackbox_response_100501.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/subscribers/users?uid=0&login=");
        String expected = getStringResource("/testGetAvailableSubscribersUsersWithLoginWithEmptyFilter/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @DisplayName("Возвращает доступных пользователей, не обладающих ролью model_charge_free_user")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductSubscribersControllerFunctionalTest/testGetAvailableSubscribersUsersWithLoginNoModelChargeFreeUser/before.csv",
            dataSource = "vendorDataSource"
    )
    void testGetAvailableSubscribersUsersWithLoginNoModelChargeFreeUser() {
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100500"))
                .willReturn(aResponse().withBody(getStringResource("/testGetAvailableSubscribersUsersWithLoginNoModelChargeFreeUser/blackbox_response_100500.json"))));
        blackboxMock.stubFor(get(anyUrl())
                .withQueryParam("uid", equalTo("100501"))
                .willReturn(aResponse().withBody(getStringResource("/testGetAvailableSubscribersUsersWithLoginNoModelChargeFreeUser/blackbox_response_100501.json"))));

        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/subscribers/users?uid=0&login=");
        String expected = getStringResource("/testGetAvailableSubscribersUsersWithLoginNoModelChargeFreeUser/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }
}
