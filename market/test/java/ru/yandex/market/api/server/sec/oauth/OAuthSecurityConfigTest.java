package ru.yandex.market.api.server.sec.oauth;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.server.sec.AuthorizationType;
import ru.yandex.market.api.server.sec.client.Client;

/**
 * Провреяем что указанны корректные проперти в production
 * Работает за счет
 * partners.auth.byClientId.enabled=true в intergration_test
 * partners.auth.byClientId.enabled=false в testing
 * Таким образом везде настройки продовые, просто в тестинге выключена проверка
 * Если нужно вкючать в тестинге свое, то надо явно затирать продовые настройки так как пропретиз наследуются
 */
@ActiveProfiles(OAuthSecurityConfigTest.PROFILE)
public class OAuthSecurityConfigTest extends BaseTest {
    static final String PROFILE = "OAuthSecurityConfigTest";

    @Inject
    private OAuthSecurityConfig config;

    @Test
    public void testParseProperties() {
        Assert.assertTrue(config.isEnabled());

        Assert.assertTrue(config.isAvailableType(clientWithId("14252"), AuthorizationType.SberLog));
        Assert.assertTrue(config.isAvailableType(clientWithId("14252"), AuthorizationType.OAuth));
        Assert.assertTrue(config.isAvailableType(clientWithId("14252"), AuthorizationType.MarketUid));

        Assert.assertTrue(config.isAvailableType(clientWithId("18932"), AuthorizationType.OAuth));
        Assert.assertTrue(config.isAvailableType(clientWithId("18932"), AuthorizationType.MarketUid));

        Assert.assertEquals(23, config.getClientsWithAuthPermission().size());
    }

    private static Client clientWithId(String id) {
        Client client = new Client();
        client.setId(id);
        return client;
    }

}
