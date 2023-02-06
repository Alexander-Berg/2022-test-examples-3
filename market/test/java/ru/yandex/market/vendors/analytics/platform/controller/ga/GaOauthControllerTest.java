package ru.yandex.market.vendors.analytics.platform.controller.ga;

import java.io.IOException;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.model.ga.GoogleScope;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 */
class GaOauthControllerTest extends FunctionalTest {

    @SpyBean
    private GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow;

    @Test
    @DisplayName("Получение authLink")
    void getAuthLink() {
        long uid = 1;
        var expectedRedirectUrl = expectedRedirectUri();
        var expectedAuthLink = UriComponentsBuilder.fromUriString("https://accounts.google.com")
                .pathSegment("o", "oauth2", "auth")
                .queryParam("access_type", "offline")
                .queryParam("approval_prompt", "force")
                .queryParam("client_id", "id")
                .queryParam("redirect_uri", expectedRedirectUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", GoogleScope.ANALYTICS_READONLY.getScopeName())
                .toUriString();
        assertEquals(expectedAuthLink, getAuthLink(uid));
    }

    @Test
    @DisplayName("Пользователь отменил предоставление доступа")
    void userCancel() {
        var clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> exchangeTokenError(1, "error")
        );
        assertEquals(HttpStatus.BAD_REQUEST, clientException.getStatusCode());
        var expected = ""
                + "{\n"
                + "  \"code\": \"GA_USER_DONT_GRANT_PERMISSION\",\n"
                + "  \"message\": \"${json-unit.ignore}\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, clientException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Пользователь не предоставил доступ к чтению профилей")
    void userDontGrantRequiredScopes() {
        var clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> exchangeCode(1, "code", "unknownScope1 unknownScope2")
        );
        assertEquals(HttpStatus.BAD_REQUEST, clientException.getStatusCode());
        var expected = ""
                + "{\n"
                + "  \"code\": \"GA_USER_DONT_GRANT_PERMISSION\",\n"
                + "  \"message\": \"${json-unit.ignore}\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, clientException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Проверка обмена кода на токен")
    @DbUnitDataSet(
            before = "GaOauthControllerTest.exchangeCode.before.csv",
            after = "GaOauthControllerTest.exchangeCode.after.csv"
    )
    void exchangeCode() throws IOException {
        long uid = 1;
        var request = mock(GoogleAuthorizationCodeTokenRequest.class);
        when(request.setRedirectUri(eq(expectedRedirectUri())))
                .thenReturn(request);
        when(request.execute()).thenReturn(new GoogleTokenResponse()
                .setAccessToken("newToken")
                .setRefreshToken("newRefreshToken")
                .setExpiresInSeconds(3600L)
        );
        when(googleAuthorizationCodeFlow.newTokenRequest("codeForExchange"))
                .thenReturn(request);
        exchangeCode(uid, "codeForExchange", GoogleScope.ANALYTICS_READONLY.getScopeName());
    }

    private String expectedRedirectUri() {
        return UriComponentsBuilder.fromUriString("http://localhost:8080")
                .pathSegment("ga", "oauth2callback")
                .toUriString();
    }

    private String getAuthLink(long userId) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("ga", "authLink")
                .queryParam("uid", userId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

    private void exchangeCode(long userId, String code, String scopes) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("ga", "oauth2callback")
                .queryParam("uid", userId)
                .queryParam("code", code)
                .queryParam("scope", scopes)
                .toUriString();
        FunctionalTestHelper.get(url);
    }

    private void exchangeTokenError(long userId, String error) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("ga", "oauth2callback")
                .queryParam("uid", userId)
                .queryParam("error", error)
                .toUriString();
        FunctionalTestHelper.get(url);
    }
}