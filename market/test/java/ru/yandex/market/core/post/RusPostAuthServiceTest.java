package ru.yandex.market.core.post;

import java.time.Clock;
import java.time.Instant;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.post.exception.RusPostAuthenticationException;
import ru.yandex.market.core.post.model.dto.AccessTokenDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Тесты на {@link RusPostAuthService}
 */
class RusPostAuthServiceTest extends FunctionalTest {

    private static final String CLIENT_ID = "IG9sVCeSFNagBG0nVmd2cpexIssa";
    private static final String SECRET = "secret";

    private static final String ACCESS_TOKEN = "wdawdwafa";

    @Autowired
    private RusPostAuthService authService;

    @Autowired
    private RusPostAuthClient authClient;

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("Попытка получения токена, когда предварительно не была сделана аутентификация")
    void getAccessTokenNoToken() {
        assertThrows(RusPostAuthenticationException.class, () -> authService.getAccessToken(1, 1));
    }

    @Test
    @DisplayName("Нормальное получение токена")
    @DbUnitDataSet(before = "RusPostAuthService.getAccessToken.before.csv")
    void getAccessToken() {
        doReturn(Instant.parse("2020-05-01T01:20:01Z")).when(clock).instant();
        assertEquals(ACCESS_TOKEN, authService.getAccessToken(1000, 50));
    }

    @Test
    @DisplayName("Попытка получения токена, когда срок его действия истек, а refresh token отсутствует")
    @DbUnitDataSet(before = "RusPostAuthService.getAccessTokenOutdatedAccessTokenNoRefreshToken.before.csv",
            after = "RusPostAuthService.getAccessTokenOutdatedAccessTokenNoRefreshToken.after.csv")
    void getAccessTokenOutdatedAccessTokenNoRefreshToken() {
        doReturn(Instant.parse("2020-06-01T01:01:01Z")).when(clock).instant();
        assertThrows(RusPostAuthenticationException.class, () -> authService.getAccessToken(1000, 50));
    }

    @Test
    @DisplayName("Попытка получения токена, когда срок его действия истек")
    @DbUnitDataSet(before = "RusPostAuthService.getAccessTokenOutdatedAccessToken.before.csv",
            after = "RusPostAuthService.getAccessTokenOutdatedAccessToken.after.csv")
    void getAccessTokenOutdatedAccessToken() {
        doReturn(Instant.parse("2020-06-01T01:01:01Z")).when(clock).instant();
        when(authClient.refreshToken(CLIENT_ID, SECRET, "some_refresh_token")).thenReturn(createTestAccessTokenResponse());
        assertEquals(ACCESS_TOKEN, authService.getAccessToken(1000, 50));
    }

    @Nonnull
    private AccessTokenDto createTestAccessTokenResponse() {
        final AccessTokenDto accessToken = new AccessTokenDto();
        accessToken.setAccessToken("wdawdwafa");
        accessToken.setRefreshToken("wow_much_refresh_token");
        accessToken.setScope("openid");
        accessToken.setIdToken("this_is_user_id_token");
        accessToken.setTokenType("Bearer");
        accessToken.setExpiresIn(1800);
        return accessToken;
    }
}
