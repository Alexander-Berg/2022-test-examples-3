package ru.yandex.market.partner.mvc.controller.token;

import java.time.Instant;
import java.util.Date;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.unitils.reflectionassert.ReflectionAssert;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.token.TokenService;
import ru.yandex.market.core.token.model.MarketToken;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Функциональные тесты на {@link TokenController}
 *
 * @author fbokovikov
 */
class TokenControllerFunctionalTest extends FunctionalTest {
    private static final long DATE_IN_SECS = 1508334830L;
    private static final long USER_ID = 100L;
    private static final long EUID = 200L;
    private static final String APPLICATION_ID = "100ABC";
    private static final String ACCESS_TOKEN = "qwerty";
    private static final Instant EXPIRED_DATE = Instant.ofEpochSecond(DATE_IN_SECS);
    private static final MarketToken EXPECTED_TOKEN = new MarketToken(
            USER_ID, ACCESS_TOKEN, APPLICATION_ID, EXPIRED_DATE);

    @Autowired
    private TokenService tokenService;

    /**
     * Тест на получение токена.
     */
    @Test
    @DbUnitDataSet(before = "testGetToken.csv")
    void testGetToken() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(baseUrl + "/token?" + buildSaveUrl(), HttpMethod.GET);
        assertResultEquals(responseEntity.getBody(), "expected-uid-token.json");
    }

    /**
     * Тест на получение токена при переданном euid.
     */
    @Test
    @DbUnitDataSet(before = "testGetEuidToken.csv")
    void testGetEuidToken() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(baseUrl + "/token?" + buildSaveUrlWithEuid(), HttpMethod.GET);
        assertResultEquals(responseEntity.getBody(), "expected-euid-token.json");
    }

    private void assertResultEquals(String actualBody, String expectedJsonResourceName) {
        JsonElement actualResult = JsonTestUtil.parseJson(actualBody).getAsJsonObject().get("result");
        JsonElement expectedResult = JsonTestUtil.parseJson(this.getClass(), expectedJsonResourceName);
        assertThat(actualResult, equalTo(expectedResult));
    }

    /**
     * Тест на ручку {@link TokenController#saveMarketToken(String, Date, String, Long, PartnerDefaultRequestHandler.PartnerHttpServRequest)}
     */
    @Test
    void testSaveToken() {
        FunctionalTestHelper.post(baseUrl + "/token?" + buildSaveUrl());
        ReflectionAssert.assertReflectionEquals(
                EXPECTED_TOKEN,
                tokenService.getToken(USER_ID, APPLICATION_ID),
                ReflectionComparatorMode.LENIENT_DATES
        );
    }

    /**
     * Тест на ручку POST /token при переданном euid.
     */
    @Test
    void testSaveTokenWithEuid() {
        FunctionalTestHelper.post(baseUrl + "/token?" + buildSaveUrlWithEuid());
        ReflectionAssert.assertReflectionEquals(
                new MarketToken(EUID, ACCESS_TOKEN, APPLICATION_ID, EXPIRED_DATE),
                tokenService.getToken(EUID, APPLICATION_ID),
                ReflectionComparatorMode.LENIENT_DATES
        );
    }

    /**
     * Тест на ручку {@link TokenController#deleteToken(String, Long, PartnerDefaultRequestHandler.PartnerHttpServRequest)}
     */
    @Test
    @DbUnitDataSet(
            before = "testDeleteToken.before.csv",
            after = "testDeleteToken.after.csv"
    )
    void testDeleteToken() {
        FunctionalTestHelper.delete(baseUrl + "/token?" + buildDeleteUrl());
    }

    private static String buildDeleteUrl() {
        return "&_user_id=" + USER_ID +
                "&application_id=" + APPLICATION_ID;
    }

    private static String buildSaveUrl() {
        return "&_user_id=" + USER_ID +
                "&application_id=" + APPLICATION_ID +
                "&access_token=" + ACCESS_TOKEN +
                "&expire_date=" + DATE_IN_SECS;
    }

    private static String buildSaveUrlWithEuid() {
        return buildSaveUrl() + "&euid=" + EUID;
    }
}
