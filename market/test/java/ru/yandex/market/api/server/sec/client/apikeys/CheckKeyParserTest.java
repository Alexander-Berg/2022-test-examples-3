package ru.yandex.market.api.server.sec.client.apikeys;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.sec.client.AuthorizationType;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.internal.ApiTariffService;
import ru.yandex.market.api.server.sec.client.internal.Tariffs;
import ru.yandex.market.api.server.sec.client.internal.TestTariffs;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class CheckKeyParserTest extends UnitTestBase {

    ApiTariffService apiTariffService;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        apiTariffService = Mockito.mock(ApiTariffService.class);
        Mockito.when(apiTariffService.toApiTariff(Mockito.eq("market_middle"))).thenReturn(TestTariffs.VENDOR);
        Mockito.when(apiTariffService.get(Tariffs.BASE)).thenReturn(TestTariffs.BASE);
    }

    @Test
    public void shouldNotParseClientWithNoTariff() {
        Client client = new CheckKeyParser(apiTariffService).parse(ResourceHelpers.getResource("no-tariff.json"));

        assertThat(client, hasProperty("authorizationType", equalTo(AuthorizationType.API_KEYS)));
        assertThat(client, hasProperty("status", equalTo(Client.Status.BLOCKED)));
        assertThat(client, hasProperty("id", equalTo("e53694ab-6423-419c-8eeb-9f579495d948")));
        assertThat(client, hasProperty("secret", equalTo("e53694ab-6423-419c-8eeb-9f579495d947")));
        assertThat(client, hasProperty("tariff", equalTo(TestTariffs.BASE)));
    }

    @Test
    public void shouldParseClientWithTariff() {
        Client client = new CheckKeyParser(apiTariffService).parse(ResourceHelpers.getResource("tariff.json"));

        assertThat(client, hasProperty("authorizationType", equalTo(AuthorizationType.API_KEYS)));
        assertThat(client, hasProperty("status", equalTo(Client.Status.ENABLED)));
        assertThat(client, hasProperty("id", equalTo("f7b5e974-e36f-4688-ae7e-f2263f439088")));
        assertThat(client, hasProperty("secret", equalTo("f7b5e974-e36f-4688-ae7e-f2263f439084")));
        assertThat(client, hasProperty("tariff", equalTo(TestTariffs.VENDOR)));
    }
}
