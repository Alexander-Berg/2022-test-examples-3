package ru.yandex.market.tsum.clients.aqua;

import com.google.common.base.CharMatcher;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.aqua.AquaApiUtils;

@Ignore("integration test")
public class AquaServiceIntegrationTest {

    private static final String PACK_ID = "54d33c74e4b02864887bbbae";
    private static final String AQUA_YANDEX_TEAM_DOMAIN = "https://aqua.yandex-team.ru";
    private static final String API_RELATIVE_URL = "/aqua-api/services/";
    private static final String AQUA_URL =
        CharMatcher.is('/').trimTrailingFrom(AQUA_YANDEX_TEAM_DOMAIN) + API_RELATIVE_URL;

    /**
     * Тест на проверку успешного выполнения запроса в Aqua
     * (был написан после падения ошибок NoClassDefFoundError)
     */
    @Test
    public void getPackById() {
        Assertions.assertThatNoException().isThrownBy(() -> AquaApiUtils.getPackById(PACK_ID, AQUA_URL));
    }

}
