package ru.yandex.market.api.server.sec.client.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.api.internal.distribution.DistributionReportClient;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.util.cache.FileCacheFactory;
import ru.yandex.market.api.util.concurrent.Pipelines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiClientServiceMemTest {

    @Test
    public void testUpdateClidsByUserLogin() {
        DistributionReportClient mockDistributionReportClient = mock(DistributionReportClient.class);
        when(mockDistributionReportClient.getUserLoginByClids()).thenReturn(mockReportResult());

        ApiClientServiceMem service = new ApiClientServiceMem(
                mock(FileCacheFactory.class), mock(ApiTariffService.class),
                "unused", mock(ApiClientsDatabaseSupplier.class),
                mockDistributionReportClient);
        List<Client> clients = getClients();
        service.updateClidsByUserLogin(clients);
        assertEquals(3, clients.get(0).getThisLoginClids().size());
        assertThat(clients.get(0).getThisLoginClids(), Matchers.containsInAnyOrder("1", "2", "5"));
        assertEquals(3, clients.get(1).getThisLoginClids().size());
        assertThat(clients.get(1).getThisLoginClids(), Matchers.containsInAnyOrder("1", "2", "5"));
        assertEquals(1, clients.get(2).getThisLoginClids().size());
        assertThat(clients.get(2).getThisLoginClids(), Matchers.containsInAnyOrder("3"));
        assertEquals(1, clients.get(3).getThisLoginClids().size());
        assertThat(clients.get(3).getThisLoginClids(), Matchers.containsInAnyOrder("4"));
    }

    private static Pipelines.Pipeline<Map<Long, String>> mockReportResult() {
        Map<Long, String> result = new HashMap<>();
        result.put(1L, "user_login1");
        result.put(2L, "user_login1");
        result.put(5L, "user_login1");
        result.put(3L, "user_login2");
        return Pipelines.startWithValue(result);
    }

    private static List<Client> getClients() {
        return Arrays.asList(
                newClient(1L, "user_login1"),
                newClient(2L, "user_login1"),
                newClient(3L, "user_login2"),
                newClient(4L, "user_login3")
        );
    }

    private static Client newClient(long clid, String userLogin) {
        Client client = new Client();
        client.setClid(String.valueOf(clid));
        client.setUserLogin(userLogin);
        return client;
    }
}
