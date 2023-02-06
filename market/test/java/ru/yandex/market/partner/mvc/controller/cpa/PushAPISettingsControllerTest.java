package ru.yandex.market.partner.mvc.controller.cpa;

import java.util.Objects;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@DbUnitDataSet(before = "ActivatePushApiToken.before.csv")
class PushAPISettingsControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Активация временного токена для партнера SHOP")
    @DbUnitDataSet(after = "ActivatePushApiToken.after.csv")
    void testGetShopActivatePushAPIToken() {
        FunctionalTestHelper.post(baseUrl + "/push-api/activate-second-token?id=101&format=json");
    }

    @Test
    @DisplayName("Активация временного токена для партнера SUPPLIER")
    @DbUnitDataSet(after = "ActivateSupplierPushApiToken.after.csv")
    void testGetSupplierActivatePushAPIToken() {
        FunctionalTestHelper.post(baseUrl + "/push-api/activate-second-token?id=201&format=json");
    }

    @DisplayName("Получение списка всех токенов по бизнесу")
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("getArgsSearchByBusiness")
    void testGetPushAPIToken(String testName, String uid, String expected) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/business/999/push-api-tokens?_user_id=" + uid);
        String body = Objects.requireNonNull(response.getBody());
        JSONArray result = new JSONObject(body).getJSONArray("result");
        JSONAssert.assertEquals(expected, result, true);
    }

    static private Stream<Arguments> getArgsSearchByBusiness() {
        return Stream.of(
                Arguments.of(
                        "Токены для роли BUSINESS_ADMIN",
                        "100500",
                        "[{\"apiToken\":\"cur_token1\"},{\"apiToken\":\"cur_token2\"}]"),
                Arguments.of(
                        "Токены для роли SHOP_ADMIN",
                        "500100",
                        "[{\"apiToken\":\"cur_token1\"}]"),
                Arguments.of(
                        "Токены для роли SHOP_OPERATOR",
                        "111222",
                        "[]"));
    }
}
