package ru.yandex.market.ir.skutcher2.meta.api;

import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.GeneratedMessageV3;
import org.junit.Test;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.ir.skutcher2.meta.ShardClient;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mbo.http.SkuBDApi.ReloadResponse.ReloadResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ir.skutcher2.meta.ShardClient.Method.GET_SKU;
import static ru.yandex.market.ir.skutcher2.meta.ShardClient.Method.PING;
import static ru.yandex.market.ir.skutcher2.meta.ShardClient.Method.RELOAD;
import static ru.yandex.market.mbo.http.SkuBDApi.SkutchType.BARCODE_SKUTCH;
import static ru.yandex.market.mbo.http.SkuBDApi.SkutchType.NO_SKUTCH;
import static ru.yandex.market.mbo.http.SkuBDApi.Status.DISABLED_FOR_CATEGORY;
import static ru.yandex.market.mbo.http.SkuBDApi.Status.NO_MODEL;
import static ru.yandex.market.mbo.http.SkuBDApi.Status.OK;

public class SkuBDApiServiceImplTest {

    private final ShardClient shardClient = mock(ShardClient.class);
    private final SkuBDApiServiceImpl skuBDApiService = new SkuBDApiServiceImpl(shardClient);

    @Test
    public void getSku_Test() {
        when(shardClient.sendPost(eq(GET_SKU), any(GeneratedMessageV3.class), any(Function.class)))
                .thenReturn(Arrays.asList(
                        SkuBDApi.GetSkuResponse.newBuilder()
                                .addSkuOffer(disableForCategory())
                                .addSkuOffer(SkuBDApi.SkuOffer.newBuilder()
                                        .setSkutchType(BARCODE_SKUTCH)
                                        .setStatus(OK))
                                .build(),
                        SkuBDApi.GetSkuResponse.newBuilder()
                                .addSkuOffer(noModelOffer())
                                .addSkuOffer(disableForCategory())
                                .build()));

        final SkuBDApi.GetSkuRequest request = SkuBDApi.GetSkuRequest.newBuilder()
                .addOffer(SkuBDApi.OfferInfo.newBuilder())
                .addOffer(SkuBDApi.OfferInfo.newBuilder())
                .build();
        final SkuBDApi.GetSkuResponse sku = skuBDApiService.getSku(request);
        assertEquals(2, sku.getSkuOfferList().size());

        final SkuBDApi.SkuOffer noSkutchOffer = sku.getSkuOfferList().get(0);
        assertEquals(NO_SKUTCH, noSkutchOffer.getSkutchType());
        assertEquals(NO_MODEL, noSkutchOffer.getStatus());

        final SkuBDApi.SkuOffer barcodeSkutchOffer = sku.getSkuOfferList().get(1);
        assertEquals(BARCODE_SKUTCH, barcodeSkutchOffer.getSkutchType());
        assertEquals(OK, barcodeSkutchOffer.getStatus());
    }

    @Test
    public void getSku_NO_SKUTCH() {
        when(shardClient.sendPost(eq(GET_SKU), any(GeneratedMessageV3.class), any(Function.class)))
                .thenReturn(Arrays.asList(
                        SkuBDApi.GetSkuResponse.newBuilder()
                                .addSkuOffer(disableForCategory())
                                .build(),
                        SkuBDApi.GetSkuResponse.newBuilder()
                                .addSkuOffer(noModelOffer())
                                .build()));

        final SkuBDApi.GetSkuRequest request = SkuBDApi.GetSkuRequest.newBuilder()
                .addOffer(SkuBDApi.OfferInfo.newBuilder())
                .build();
        final SkuBDApi.GetSkuResponse sku = skuBDApiService.getSku(request);
        assertEquals(1, sku.getSkuOfferList().size());
        final SkuBDApi.SkuOffer skuOffer = sku.getSkuOfferList().get(0);
        assertEquals(NO_SKUTCH, skuOffer.getSkutchType());
        assertEquals(NO_MODEL, skuOffer.getStatus());
    }

    @Test
    public void getSku_disabledForCategory() {
        when(shardClient.sendPost(eq(GET_SKU), any(GeneratedMessageV3.class), any(Function.class)))
                .thenReturn(Arrays.asList(
                        SkuBDApi.GetSkuResponse.newBuilder()
                                .addSkuOffer(disableForCategory())
                                .build(),
                        SkuBDApi.GetSkuResponse.newBuilder()
                                .addSkuOffer(disableForCategory())
                                .build()));

        final SkuBDApi.GetSkuRequest request = SkuBDApi.GetSkuRequest.newBuilder()
                .addOffer(SkuBDApi.OfferInfo.newBuilder())
                .build();
        final SkuBDApi.GetSkuResponse sku = skuBDApiService.getSku(request);
        assertEquals(1, sku.getSkuOfferList().size());
        final SkuBDApi.SkuOffer skuOffer = sku.getSkuOfferList().get(0);
        assertEquals(NO_SKUTCH, skuOffer.getSkutchType());
        assertEquals(DISABLED_FOR_CATEGORY, skuOffer.getStatus());
    }

    private SkuBDApi.SkuOffer.Builder disableForCategory() {
        return SkuBDApi.SkuOffer.newBuilder()
                .setSkutchType(NO_SKUTCH)
                .setStatus(DISABLED_FOR_CATEGORY);
    }

    private SkuBDApi.SkuOffer.Builder noModelOffer() {
        return SkuBDApi.SkuOffer.newBuilder()
                .setSkutchType(NO_SKUTCH)
                .setStatus(NO_MODEL);
    }

    @Test
    public void reload_successTest() {
        when(shardClient.sendPost(eq(RELOAD), any(GeneratedMessageV3.class), any(Function.class)))
                .thenReturn(Arrays.asList(
                        SkuBDApi.ReloadResponse.newBuilder()
                                .setReloadResult(ReloadResult.OK)
                                .addCategoryResponse(SkuBDApi.CategoryInfoResponse.newBuilder()
                                        .setHid(11111)
                                        .addCategoryFileInfo(SkuBDApi.CategoryFileInfo.newBuilder()
                                                .setFileName("test.pb")
                                                .setMd5("18927634812643")
                                                .setReloadTime(123456789)))
                                .build(),
                        SkuBDApi.ReloadResponse.newBuilder()
                                .setReloadResult(ReloadResult.OK)
                                .addCategoryResponse(SkuBDApi.CategoryInfoResponse.newBuilder()
                                        .setHid(22222)
                                        .addCategoryFileInfo(SkuBDApi.CategoryFileInfo.newBuilder()
                                                .setFileName("test_2.pb")
                                                .setMd5("23654321513215")
                                                .setReloadTime(987654321)))
                                .build()
                ));
        final SkuBDApi.ReloadResponse reload = skuBDApiService.reload(SkuBDApi.CategoryRequest.newBuilder().build());
        assertEquals(ReloadResult.OK, reload.getReloadResult());
        assertFalse(reload.hasReloadErrorMessage());
        assertEquals(2, reload.getCategoryResponseList().size());
        final List<Long> hids = reload.getCategoryResponseList()
                .stream()
                .map(SkuBDApi.CategoryInfoResponse::getHid)
                .collect(Collectors.toList());
        assertTrue(hids.contains(11111L));
        assertTrue(hids.contains(22222L));
    }

    @Test
    public void reload_errorTest() {
        when(shardClient.sendPost(eq(RELOAD), any(GeneratedMessageV3.class), any(Function.class)))
                .thenReturn(Arrays.asList(
                        SkuBDApi.ReloadResponse.newBuilder()
                                .setReloadResult(ReloadResult.OK)
                                .addCategoryResponse(SkuBDApi.CategoryInfoResponse.newBuilder()
                                        .setHid(11111)
                                        .addCategoryFileInfo(SkuBDApi.CategoryFileInfo.newBuilder()
                                                .setFileName("test.pb")
                                                .setMd5("18927634812643")
                                                .setReloadTime(123456789)))
                                .build(),
                        SkuBDApi.ReloadResponse.newBuilder()
                                .setReloadResult(ReloadResult.ERROR)
                                .setReloadErrorMessage("Error1")
                                .addCategoryResponse(SkuBDApi.CategoryInfoResponse.newBuilder()
                                        .setHid(22222)
                                        .addCategoryFileInfo(SkuBDApi.CategoryFileInfo.newBuilder()
                                                .setFileName("test_2.pb")
                                                .setMd5("23654321513215")
                                                .setReloadTime(987654321)))
                                .build(),
                        SkuBDApi.ReloadResponse.newBuilder()
                                .setReloadResult(ReloadResult.ERROR)
                                .setReloadErrorMessage("Error2")
                                .addCategoryResponse(SkuBDApi.CategoryInfoResponse.newBuilder()
                                        .setHid(33333)
                                        .addCategoryFileInfo(SkuBDApi.CategoryFileInfo.newBuilder()
                                                .setFileName("test_2.pb")
                                                .setMd5("23654321513215")
                                                .setReloadTime(987654321)))
                                .build()
                ));
        final SkuBDApi.ReloadResponse reload = skuBDApiService.reload(SkuBDApi.CategoryRequest.newBuilder().build());
        assertEquals(ReloadResult.ERROR, reload.getReloadResult());
        assertEquals("Error1;Error2", reload.getReloadErrorMessage());
        assertEquals(3, reload.getCategoryResponseList().size());
        final List<Long> hids = reload.getCategoryResponseList()
                .stream()
                .map(SkuBDApi.CategoryInfoResponse::getHid)
                .collect(Collectors.toList());
        assertTrue(hids.contains(11111L));
        assertTrue(hids.contains(22222L));
        assertTrue(hids.contains(33333L));
    }

    @Test
    public void ping_successTest() {
        HttpResponse<String> httpResponse1 = mock(HttpResponse.class);
        when(httpResponse1.statusCode()).thenReturn(200);
        when(httpResponse1.body()).thenReturn("0;OK");

        HttpResponse<String> httpResponse2 = mock(HttpResponse.class);
        when(httpResponse2.statusCode()).thenReturn(200);
        when(httpResponse2.body()).thenReturn("0;OK");

        when(shardClient.sendGet(eq(PING), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(Map.of(
                        "host1", httpResponse1,
                        "host2", httpResponse2));

        final MonitoringResult ping = skuBDApiService.ping();
        assertEquals(MonitoringResult.Status.OK, ping.getStatus());
        assertEquals("OK", ping.getMessage());
    }

    @Test
    public void ping_errorTest() {
        HttpResponse<String> httpResponse1 = mock(HttpResponse.class);
        when(httpResponse1.statusCode()).thenReturn(500);
        when(httpResponse1.body()).thenReturn("Error");

        HttpResponse<String> httpResponse2 = mock(HttpResponse.class);
        when(httpResponse2.statusCode()).thenReturn(200);
        when(httpResponse2.body()).thenReturn("0;OK");

        when(shardClient.sendGet(eq(PING), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(Map.of(
                        "host1", httpResponse1,
                        "host2", httpResponse2));

        final MonitoringResult ping = skuBDApiService.ping();
        assertEquals(MonitoringResult.Status.ERROR, ping.getStatus());
        assertEquals("host1: Error", ping.getMessage());
    }
}
