package ru.yandex.market.api.server.limits.standalone;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.web.bind.annotation.RequestMethod;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.domain.GlobalResource;
import ru.yandex.market.api.server.domain.MethodResource;
import ru.yandex.market.api.server.sec.client.AuthorizationType;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.internal.Tariff;
import ru.yandex.market.api.server.sec.client.internal.TestTariffs;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithMocks
public class ResourceLimitServiceTest extends UnitTestBase {

    @Mock
    ResourceDataSupplier resourceDataSupplier;

    @Mock
    ResourceDataSupplier.Data data;

    @InjectMocks
    private ResourceLimitService resourceLimitService;

    @Before
    public void setUp() throws Exception {
        when(resourceDataSupplier.get()).thenReturn(data);
        when(data.getLimit(eq("test-tariff"), anyString())).thenReturn(100);
        when(data.getTariff(anyInt())).thenReturn("test-tariff");
    }

    @Test
    public void shouldResolveLimitsFromApiKeysInfo() throws Exception {
        Client client = new Client();
        client.setAuthorizationType(AuthorizationType.API_KEYS);
        client.setTariff(TestTariffs.VENDOR);
        client.setSecret("test");

        MethodResource methodResource = new MethodResource(RequestMethod.GET, "test", MethodResource.Group.HEAVY, "test");

        int limit = resourceLimitService.getLimit(client, methodResource);

        assertEquals(100, limit);
    }

    @Test
    public void shouldResolveGlobalLimitsFromApiKeysInfo() throws Exception {
        Client client = new Client();
        client.setAuthorizationType(AuthorizationType.API_KEYS);
        client.setTariff(TestTariffs.VENDOR);
        client.setSecret("test");

        int limit = resourceLimitService.getLimit(client, GlobalResource.GLOBAL);

        assertEquals(100, limit);
    }
}
