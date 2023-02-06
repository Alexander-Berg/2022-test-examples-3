package ru.yandex.market.pers.grade.core.util;

import org.apache.http.client.HttpClient;
import org.junit.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.pers.grade.core.util.datasync.DataSyncAddress;
import ru.yandex.market.pers.grade.core.util.datasync.DataSyncClient;
import ru.yandex.passport.tvmauth.TvmClient;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.pers.grade.client.mock.HttpClientMockHelpers.mockResponseWithFile;

public class DataSyncClientTest {
    public static final int UID = 1;
    private final HttpClient httpClient = mock(HttpClient.class);
    private final TvmClient tvmClient = mock(TvmClient.class);
    private final DataSyncClient dataSyncClient =
            new DataSyncClient("https://localhost:1234", 1234,  tvmClient,
                    new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)));

    @Test
    public void testGetAddressesWith200Code() {
        mockResponseWithFile(httpClient, 200, "/data/datasync_get_market_delivery_addresses.json");
        List<DataSyncAddress> addresses = dataSyncClient.getMarketDeliveryAddresses(123L);
        assertEquals(2, addresses.size());
    }

    @Test(expected = RuntimeException.class)
    public void testGetAddressesWith400Code() {
        mockResponseWithFile(httpClient, 400, "/data/datasync_get_market_delivery_addresses.json");
        List<DataSyncAddress> addresses = dataSyncClient.getMarketDeliveryAddresses(123L);
    }
}
