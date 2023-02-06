package ru.yandex.market.partner.mvc.controller.fmcg;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@DbUnitDataSet(before = "data/csv/FmcgControllerFunctionalTest.before.csv")
class FmcgInfoControllerTest extends FunctionalTest {
    private static final long ONLINE_SHOP_CAMPAIGN_ID = 477;
    private static final long OFFLINE_SHOP_CAMPAIGN_ID = 577;

    @Test
    @DisplayName("Создание настроек интеграции корзины")
    @DbUnitDataSet(after = "data/csv/newCartSettingsCreationTest.after.csv")
    void createCartSettingsTest() {
        ResponseEntity<String> response = sendRequestFromFile(
                getPostEditUrl(ONLINE_SHOP_CAMPAIGN_ID),
                "data/json/create-cart-settings-request.json"
        );
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Создание настроек для оффлайн магазина")
    void createCartSettingsForOfflineShopTest() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> sendRequestFromFile(
                        getPostEditUrl(OFFLINE_SHOP_CAMPAIGN_ID),
                        "data/json/create-cart-settings-request.json")
        );
        MatcherAssert.assertThat(
                httpClientErrorException,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.FORBIDDEN),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors",
                                        MbiMatchers.jsonArrayEquals(
                                                "{" +
                                                        "\"code\":\"UNAUTHORIZED\"," +
                                                        "\"details\":{" +
                                                        "\"subcode\":\"FORBIDDEN_OPERATION\"," +
                                                        "\"reason\":\"Shopping cart cannot be configured for offline shop\"" +
                                                        "}" +
                                                        "}")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Создание настроек для не-FMCG магазина")
    @DbUnitDataSet(before = "data/csv/createCartSettingsForNonFmcgTest.before.csv")
    void createCartSettingsForNonFmcgTest() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> sendRequestFromFile(
                        getPostEditUrl(677L),
                        "data/json/create-cart-settings-request.json")
        );
        MatcherAssert.assertThat(
                httpClientErrorException,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.FORBIDDEN),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches("errors",
                                        MbiMatchers.jsonArrayEquals(
                                                "{" +
                                                        "\"code\":\"UNAUTHORIZED\"," +
                                                        "\"details\":{" +
                                                        "\"subcode\":\"FORBIDDEN_OPERATION\"," +
                                                        "\"reason\":\"Shopping cart cannot be configured for non-fmcg shop\"" +
                                                        "}" +
                                                        "}")
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Обновление настроек интеграции корзины")
    @DbUnitDataSet(before = "data/csv/updateCartSettingsTest.before.csv",
            after = "data/csv/updateCartSettingsTest.after.csv")
    void updateCartSettingsTest() {
        ResponseEntity<String> response = sendRequestFromFile(
                getPostEditUrl(ONLINE_SHOP_CAMPAIGN_ID),
                "data/json/update-cart-settings-request.json"
        );
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Получение пустых настроек")
    void getEmptyCartSettingsTest() {
        ResponseEntity<String> response = FunctionalTestHelper.get(getGetSettingsUrl(ONLINE_SHOP_CAMPAIGN_ID));
        JsonTestUtil.assertEquals(response, "{}");
    }

    @Test
    @DisplayName("Получение настроек интеграции корзины")
    @DbUnitDataSet(before = "data/csv/getCartSettings.before.csv")
    void getCartSettingsTest() {
        ResponseEntity<String> response = FunctionalTestHelper.get(getGetSettingsUrl(ONLINE_SHOP_CAMPAIGN_ID));
        JsonTestUtil.assertEquals(response, getClass(), "data/json/get-cart-settings-response.json");
    }

    private ResponseEntity<String> sendRequestFromFile(final String url, final String filename) {
        final HttpEntity request = JsonTestUtil.getJsonHttpEntity(getClass(), filename);
        return FunctionalTestHelper.post(url, request);
    }

    private String getPostEditUrl(long campaignId) {
        return baseUrl + String.format("/fmcg/shopping-cart/edits?id=%d", campaignId);
    }

    private String getGetSettingsUrl(long campaignId) {
        return baseUrl + String.format("/fmcg/shopping-cart?id=%d", campaignId);
    }
}