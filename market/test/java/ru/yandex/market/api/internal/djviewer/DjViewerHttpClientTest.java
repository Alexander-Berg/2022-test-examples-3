package ru.yandex.market.api.internal.djviewer;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.Uuid;
import ru.yandex.market.api.server.sec.YandexUid;

public class DjViewerHttpClientTest extends BaseTest {

    private static final String DEFAULT_EXPERIMENT = "default";
    private static final byte[] EMPTY_RESPONSE = "{\"items\": []}".getBytes();

    @Inject
    DjViewerHttpClient client;

    @Inject
    DjViewerTestClient mockServer;

    @Test
    public void testEmptyTolokaModelsList() throws ExecutionException, InterruptedException {
        mockServer.doRequest(x -> x.get().serverMethod("/api/toloka"))
                .ok()
                .body(EMPTY_RESPONSE);

        List<DjViewerTolokaModelResponse> result = client.tolokaModelsList(DEFAULT_EXPERIMENT, null).get();
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testTolokaModelsList() throws ExecutionException, InterruptedException {
        mockServer.doRequest(x -> x.get().serverMethod("/api/toloka"))
                .ok()
                .body("djviewer_toloka_models_list.json");

        List<DjViewerTolokaModelResponse> items = client.tolokaModelsList(DEFAULT_EXPERIMENT, null).get();
        Assert.assertEquals(2, items.size());
        checkTolokaModel(items.get(0), 13334879L,
                "//avatars.mds.yandex.net/get-mpic/1538707/img_id6552763020559934720.jpeg/orig",
                "Корм для кошек Royal Canin для вывода шерсти 12шт. х 85 г (кусочки в соусе)",
                "http://market.yandex.ru/product/13334879");
        checkTolokaModel(items.get(1), 93725115L,
                "//avatars.mds.yandex.net/get-mpic/1726038/img_id3243942002522229780.jpeg/orig",
                "Автомобильная шина Hankook Tire Winter i*Pike RS2 W429 225/50 R17 98T зимняя шипованная",
                "http://market.yandex.ru/product/93725115");
    }

    private void checkTolokaModel(DjViewerTolokaModelResponse model, Long id, String picture, String title,
                                  String url) {
        Assert.assertEquals(id, model.getId());
        Assert.assertEquals(picture, model.getPicture());
        Assert.assertEquals(title, model.getTitle());
        Assert.assertEquals(url, model.getUrl());
    }

    @Test
    public void testTolokaModelsList_experiment() throws ExecutionException, InterruptedException {
        mockServer.doRequest(x -> x.get().serverMethod("/api/toloka")
                .param(DjViewerHttpClient.Param.EXPERIMENT, DEFAULT_EXPERIMENT)
        ).ok().body(EMPTY_RESPONSE);
        client.tolokaModelsList(DEFAULT_EXPERIMENT, null).get();
    }

    @Test
    public void testTolokaModelsList_deviceId() throws ExecutionException, InterruptedException {
        String deviceId = "123413214";
        mockServer.doRequest(x -> x.get().serverMethod("/api/toloka")
                .param(DjViewerHttpClient.Param.DEVICEID, deviceId)
        ).ok().body(EMPTY_RESPONSE);
        client.tolokaModelsList(DEFAULT_EXPERIMENT, deviceId).get();
    }

    @Test
    public void testTolokaModelsList_puid() throws ExecutionException, InterruptedException {
        long uid = 871478986L;
        mockServer.doRequest(x -> x.get().serverMethod("/api/toloka")
                .param(DjViewerHttpClient.Param.PUID, String.valueOf(uid))
        ).ok().body(EMPTY_RESPONSE);

        ContextHolder.update(x -> x.setUser(new User(new OauthUser(uid), null, null, null)));
        client.tolokaModelsList(DEFAULT_EXPERIMENT, null).get();
    }

    @Test
    public void testTolokaModelsList_uuid() throws ExecutionException, InterruptedException {
        String uuid = "12345678901234567890123456789012";
        mockServer.doRequest(x -> x.get().serverMethod("/api/toloka")
                .param(DjViewerHttpClient.Param.UUID, uuid)
        ).ok().body(EMPTY_RESPONSE);

        ContextHolder.update(x -> x.setUser(new User(null, null, new Uuid(uuid), null)));
        client.tolokaModelsList(DEFAULT_EXPERIMENT, null).get();
    }

    @Test
    public void testTolokaModelsList_yandexUid() throws ExecutionException, InterruptedException {
        String yandexUid = "6602559030123456789";
        mockServer.doRequest(x -> x.get().serverMethod("/api/toloka")
                .param(DjViewerHttpClient.Param.YANDEX_UID, yandexUid)
        ).ok().body(EMPTY_RESPONSE);

        ContextHolder.update(x -> x.setUser(new User(null, null, null, new YandexUid(yandexUid))));
        client.tolokaModelsList(DEFAULT_EXPERIMENT, null).get();
    }

}
