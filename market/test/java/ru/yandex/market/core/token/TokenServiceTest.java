package ru.yandex.market.core.token;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.token.model.MarketToken;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест на логику работы {@link TokenService}.
 *
 * @author fbokovikov
 */
class TokenServiceTest extends FunctionalTest {

    private static final long USER_ID = 100L;
    private static final String APPLICATION_ID = "100ABC";
    private static final String ACCESS_TOKEN = "qwerty";
    private static final MarketToken EXPECTED_TOKEN = new MarketToken(
            USER_ID,
            ACCESS_TOKEN,
            APPLICATION_ID,
            Instant.ofEpochSecond(1L)
    );

    @Autowired
    private TokenService tokenService;

    /**
     * Проверка сценария, когда по связке userId + applicationId {@link MarketToken токен}
     * {@link TokenService#getToken(long, String) не найден}.
     */
    @Test
    void testTokenNotFound() {
        assertThat(tokenService.getToken(1L, "123T")).isNull();
    }

    /**
     * Положительный сценарий {@link TokenService#getToken(long, String)}
     */
    @Test
    @DbUnitDataSet(before = "testGetToken.csv")
    void testGetToken() {
        var token = tokenService.getToken(100L, "100ABC");
        assertThat(token).usingRecursiveComparison().isEqualTo(EXPECTED_TOKEN);
    }

    /**
     * Тест на {@link TokenService#saveToken(MarketToken)}
     */
    @Test
    @DbUnitDataSet(after = "testSaveToken.csv")
    void testSaveNewToken() {
        tokenService.saveToken(EXPECTED_TOKEN);
    }

    /**
     * Тест на {@link TokenService#saveToken(MarketToken)}
     */
    @Test
    @DbUnitDataSet(before = "testGetToken.csv", after = "testUpdateToken.csv")
    void testUpdateToken() {
        var token = new MarketToken(
                EXPECTED_TOKEN.getUserId(),
                "newToken",
                EXPECTED_TOKEN.getApplicationId(),
                Instant.ofEpochSecond(2L)
        );
        tokenService.saveToken(token);
    }

    /**
     * Тест на {@link TokenService#deleteToken(long, String)
     */
    @Test
    @DbUnitDataSet
    void testDeleteToken() {
        tokenService.saveToken(EXPECTED_TOKEN);
        assertThat(tokenService.getToken(EXPECTED_TOKEN.getUserId(), EXPECTED_TOKEN.getApplicationId())).isNotNull();

        tokenService.deleteToken(EXPECTED_TOKEN.getUserId(), EXPECTED_TOKEN.getApplicationId());
        assertThat(tokenService.getToken(EXPECTED_TOKEN.getUserId(), EXPECTED_TOKEN.getApplicationId())).isNull();
    }


}
