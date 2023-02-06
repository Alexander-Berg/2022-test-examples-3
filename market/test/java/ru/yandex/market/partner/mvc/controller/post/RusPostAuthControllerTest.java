package ru.yandex.market.partner.mvc.controller.post;

import java.time.Clock;
import java.time.Instant;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.post.RusPostAuthClient;
import ru.yandex.market.core.post.model.dto.AccessTokenDto;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Тесты на {@link RusPostAuthController}.
 */
@DbUnitDataSet(before = "/mvc/post/database/RusPostAuthControllerTest.before.csv")
class RusPostAuthControllerTest extends FunctionalTest {

    private static final String ACCESS_TOKEN = "wdawdwafa";
    private static final String CLIENT_ID = "IG9sVCeSFNagBG0nVmd2cpexIssa";
    private static final String SECRET = "secret";

    @Autowired
    private RusPostAuthClient authClient;

    @Autowired
    private Clock clock;

    @Autowired
    private EnvironmentService environmentService;

    @Test
    @DisplayName("Проверка аутентификации в первый раз")
    @DbUnitDataSet(after = "/mvc/post/database/RusPostAuthControllerTest.testAuthNewRecord.after.csv")
    void testAuthenticationCreateNewRecord() {
        String code = "rdgsEFA";
        String redirectUrl = "https://demofslb.market.yandex.ru";
        doReturn(Instant.parse("2020-05-01T01:01:01Z")).when(clock).instant();
        mockGetAccessToken(code, redirectUrl, createTestAccessTokenResponse());
        sendAuthenticationRequest(100, 50, code, redirectUrl);
    }

    @Test
    @DisplayName("Проверка повторной аутентификации")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostAuthControllerTest.testAuthUpdateRecord.before.csv",
            after = "/mvc/post/database/RusPostAuthControllerTest.testAuthNewRecord.after.csv")
    void testAuthenticationUpdateRecord() {
        String code = "rdgsEFA";
        String redirectUrl = "https://demofslb.market.yandex.ru";
        doReturn(Instant.parse("2020-05-01T01:01:01Z")).when(clock).instant();
        mockGetAccessToken(code, redirectUrl, createTestAccessTokenResponse());
        sendAuthenticationRequest(100, 50, code, redirectUrl);
    }

    @Test
    @DisplayName("Проверка аутентификации в первый раз с успешной валидацией токена")
    void testAuthenticationCreateNewRecordWithValidation() {
        environmentService.setValue("rus.post.token.validation.enabled", "true");
        String code = "rdgsEFA";
        String redirectUrl = "https://demofslb.market.yandex.ru";
        doReturn(Instant.parse("2020-05-01T01:01:01Z")).when(clock).instant();
        mockGetAccessToken(code, redirectUrl, createTestAccessTokenResponseWithOkToken());
        sendAuthenticationRequest(100, 50, code, redirectUrl);
    }

    @Test
    @DisplayName("Проверка аутентификации в первый раз с валидацией токена: токен не валиден")
    void testAuthenticationCreateNewRecordWithFailedValidation() {
        environmentService.setValue("rus.post.token.validation.enabled", "true");
        String code = "rdgsEFA";
        String redirectUrl = "https://demofslb.market.yandex.ru";
        doReturn(Instant.parse("2020-05-01T01:01:01Z")).when(clock).instant();
        mockGetAccessToken(code, redirectUrl, createTestAccessTokenResponse());
        final HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> sendAuthenticationRequest(100, 50, code, redirectUrl));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    @DisplayName("Получение аутентификационной информации")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostAuthControllerTest.testAuthUpdateRecord.before.csv")
    void testGetInfo() {
        final ResponseEntity<String> response = getAuthInfo(100, 50);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonTestUtil.assertEquals(response, "{\"identificationToken\":\"id_tok\"}");
    }

    @Test
    @DisplayName("Получение аутентификационной информации: не найдено")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostAuthControllerTest.testAuthUpdateRecord.before.csv")
    void testGetInfoNotFound() {
        final HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> getAuthInfo(1000, 60));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("Logout: проверка удаления токенов")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostAuthControllerTest.logout.before.csv",
            after = "/mvc/post/database/RusPostAuthControllerTest.logout.after.csv")
    void testLogout() {
        logout(100, 50);
    }

    @Test
    @DisplayName("Logout: токены не найдены")
    @DbUnitDataSet(before = "/mvc/post/database/RusPostAuthControllerTest.logout.no_token.before.csv",
            after = "/mvc/post/database/RusPostAuthControllerTest.logout.no_token.after.csv")
    void testLogoutNoToken() {
        logout(100, 50);
    }

    private void mockGetAccessToken(String code, String redirectUrl, AccessTokenDto response) {
        when(authClient.getAccessToken(CLIENT_ID, SECRET, redirectUrl, code)).thenReturn(response);
    }

    private void sendAuthenticationRequest(long campaignId, long uid, String code, String redirectUrl) {
        FunctionalTestHelper.post(baseUrl + "/ruspost/authenticate?id={id}&_user_id={uid}&redirect_url={url}&code" +
                        "={code}", null,
                campaignId, uid, redirectUrl, code);
    }

    private ResponseEntity<String> getAuthInfo(long campaignId, long uid) {
        return FunctionalTestHelper.get(baseUrl + "/ruspost/authentication-info?id={id}&_user_id={uid}", campaignId, uid);
    }

    private ResponseEntity<String> logout(long campaignId, long uid) {
        return FunctionalTestHelper.post(baseUrl + "/ruspost/logout?id={id}&_user_id={uid}", null, campaignId, uid);
    }

    @Nonnull
    private AccessTokenDto createTestAccessTokenResponse() {
        final AccessTokenDto accessToken = new AccessTokenDto();
        accessToken.setAccessToken(ACCESS_TOKEN);
        accessToken.setRefreshToken("wow_much_refresh_token");
        accessToken.setScope("openid");
        accessToken.setIdToken("this_is_user_id_token");
        accessToken.setTokenType("Bearer");
        accessToken.setExpiresIn(1800);
        return accessToken;
    }

    @Nonnull
    private AccessTokenDto createTestAccessTokenResponseWithOkToken() {
        //рандомные данные, подписанные случайным ключом
        //внутри sub: "123abcde-1abc-1a2a-a123-12345abcdefgh"
        String idToken = "eyJhbGciOiJSUzUxMiJ9.ewogICJpc3MiOiAiaHR0cHM6Ly9wYXNzcG9ydC50ZXN0LnJ1c3NpYW5wb3N0LnJ1L3BjLyIsCiAgInN1YiI6ICIxMjNhYmNkZS0xYWJjLTFhMmEtYTEyMy0xMjM0NWFiY2RlZmdoIiwKICAiYXVkIjogWwogICAgIjEyM2FiY2RlMTIzYWJjZGUxMjNhYmNkZTEyMzQiCiAgXSwKICAiZXhwIjogMTU4ODA5OTI2NCwKICAiaWF0IjogMTU4ODA5NzQ2NCwKICAiYXV0aF90aW1lIjogMTU4ODA5NzQ2NCwKICAiYW1yIjogWwogICAgIlBXRCIKICBdLAogICJhenAiOiAiMTIzYWJjZGUxMjNhYmNkZTEyM2FiY2RlMTIzNCIsCiAgImF0X2hhc2giOiAiMTIzYWJjZGUxMjNhYmNkZTEyM2FiY2RlMTIzNCIKfQ.SliMol2ENtK6Oy4DBq8H1ZWv5Ds0liJpYHxA3wX2SEQeOLgPulZH5MzyGXWZUc4eGUlbbq-LmdvURcb4ZRofMygmLvD8tFUeaCe4yDH3vdS7dx-peDbF7D8RCWMtgBFENDsidVieMvqVyZVdilQcYzArxWE2xthx1ReKK2IqQIpdtWRZ_TorOh9YG-QiQ8slYC64Y0yjv0sccSH5ZqL5sUBgZHbvUt3pWkm0cZcgv_uTsbTVCAfjMpAnuoEAgUdE2eYZQx1JaNb9OzmV0tHIJxpwLxLDgUl5CEezU7rdrmj1-eg7ouJep4m29hiLzapML0P7ZXNGBsFR2H0KXMRnkg";
        final AccessTokenDto accessToken = new AccessTokenDto();
        accessToken.setAccessToken(ACCESS_TOKEN);
        accessToken.setRefreshToken("wow_much_refresh_token");
        accessToken.setScope("openid");
        accessToken.setIdToken(idToken);
        accessToken.setTokenType("Bearer");
        accessToken.setExpiresIn(1800);
        return accessToken;
    }
}
