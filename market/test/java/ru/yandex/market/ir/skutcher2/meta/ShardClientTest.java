package ru.yandex.market.ir.skutcher2.meta;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.sharding.nanny.service.ShardNodesHolderService;
import ru.yandex.market.mbo.http.SkuBDApi;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShardClientTest {

    private ShardClient shardClient;

    private final HttpClient httpClient = mock(HttpClient.class);
    private final ShardNodesHolderService shardNodesHolderService = mock(ShardNodesHolderService.class);
    private final HttpResponse<byte[]> responseByte = mock(HttpResponse.class);
    private final HttpResponse<String> responseString = mock(HttpResponse.class);

    @Before
    public void setUp() {
        when(shardNodesHolderService.getShardHosts()).thenReturn(Collections.singletonList(
                String.format("http://%s:%d", "localhost", 8000)
        ));
        when(responseByte.body()).thenReturn(SkuBDApi.GetSkuResponse.newBuilder()
                .addSkuOffer(SkuBDApi.SkuOffer.newBuilder()
                        .setStatus(SkuBDApi.Status.DISABLED_FOR_CATEGORY)
                        .setSkutchType(SkuBDApi.SkutchType.NO_SKUTCH))
                .build().toByteArray());
        when(httpClient.sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray()))).thenReturn(CompletableFuture.completedFuture(responseByte));

        when(responseString.body()).thenReturn("test");
        when(httpClient.sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(CompletableFuture.completedFuture(responseString));

        shardClient = new ShardClient(httpClient, shardNodesHolderService);
    }

    @Test
    public void sendPostTest() {
        final List<SkuBDApi.GetSkuResponse> result = shardClient.sendPost(ShardClient.Method.GET_SKU,
                SkuBDApi.GetSkuRequest.newBuilder().build(), httpResponse -> {
                    try {
                        return SkuBDApi.GetSkuResponse.newBuilder().mergeFrom(httpResponse.body()).build();
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                });

        assertEquals(1, result.size());
        final SkuBDApi.GetSkuResponse getSkuResponse = result.get(0);
        final SkuBDApi.SkuOffer skuOffer = getSkuResponse.getSkuOffer(0);
        assertEquals(SkuBDApi.Status.DISABLED_FOR_CATEGORY, skuOffer.getStatus());
        assertEquals(SkuBDApi.SkutchType.NO_SKUTCH, skuOffer.getSkutchType());
    }

    @Test
    public void sendGetTest() {
        final Map<String, HttpResponse<String>> result = shardClient.sendGet(ShardClient.Method.GET_SKU,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(1, result.size());
    }
}
