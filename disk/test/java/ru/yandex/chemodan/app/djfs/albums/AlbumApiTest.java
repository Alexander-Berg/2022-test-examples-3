package ru.yandex.chemodan.app.djfs.albums;

import java.net.URLEncoder;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.SneakyThrows;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.joda.time.Instant;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.djfs.core.album.Album;
import ru.yandex.chemodan.app.djfs.core.album.AlbumItem;
import ru.yandex.chemodan.app.djfs.core.album.AlbumType;
import ru.yandex.chemodan.app.djfs.core.album.DjfsAlbumsTestBase;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.util.InstantUtils;
import ru.yandex.chemodan.queller.celery.job.CeleryJob;
import ru.yandex.chemodan.queller.worker.CeleryTaskManager;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.commune.json.JsonArray;
import ru.yandex.commune.json.JsonNumber;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.JsonValue;
import ru.yandex.misc.test.Assert;

@ContextConfiguration(classes = {
        DjfsAlbumsTestContextConfiguration.class,
})
public class AlbumApiTest extends DjfsAlbumsTestBase {

    @Autowired
    CeleryTaskManager celeryTaskManager;

    /**
     * Album Append Items Tests
     **/

    @Test
    public void appendItemsInPersonalAlbum() {

    }

    @Test
    public void appendItemsInGeoAlbum() {

    }

    @Test
    public void appendItemsInFavoritesAlbum() {

    }

    @Test
    public void appendItemsInFacesAlbum() {

    }

    /**
     * Album Item Remove Tests
     **/

    @Test
    @SneakyThrows
    public void itemRemoveFromPersonalAlbum() {

        ArgumentCaptor<CeleryJob> jobCaptor = ArgumentCaptor.forClass(CeleryJob.class);
        Mockito.doNothing().when(celeryTaskManager).submit(jobCaptor.capture());

        // Create album with two photo
        Album album = createFirstPersonalAlbum("My personal album");
        addItemToAlbum(album, "image-1.jpg");
        addItemToAlbum(album, "image-2.jpg");

        ListF<AlbumItem> albumItems = albumItemDao.getAllAlbumItems(UID, album.getId());
        Assert.equals(2, albumItems.length());

        /**
         * Remove Item from Album
         **/

        AlbumItem item = albumItems.get(1);
        HttpResponse response = a3TestHelper.delete("/api/v1/albums/album_item_remove"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&item_id=" + item.getId().toString());

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check database
        albumItems = albumItemDao.getAllAlbumItems(UID, album.getId());
        Assert.equals(1, albumItems.length());

        // Check push
        List<CeleryJob> pushes = jobCaptor.getAllValues();
        Assert.equals(2, pushes.size());

        checkDatabaseChangedPush(pushes.get(0).getKwargs(), "2");
        checkItemRemovePush(pushes.get(1).getKwargs(), album, item);

    }

    @Test
    @SneakyThrows
    public void deleteItemOfCoverFromPersonalAlbum() {

        //TODO: мигация теста test_remove_cover_item(), добавить в т.ч. для других альбомов, для которых нужно
        //      (не тут заглушка только для personal)

    }

    @Test
    public void itemRemoveFromGeoAlbum() {

    }

    @Test
    public void itemRemoveFromFavoritesAlbum() {

    }

    @Test
    public void itemRemoveFromFacesAlbum() {

    }

    /**
     * Album Remove Tests
     **/

    @Test
    public void removePersonalAlbum() {

    }

    @Test
    public void removeGeoAlbum() {

    }

    @Test
    public void removeFavoritesAlbum() {

    }

    @Test
    public void removeFacesAlbum() {

    }

    /**
     * Album Set Attributes Tests
     **/

    private void checkDatabaseChangedPush(MapF<String, JsonValue> push, String revision) {

        Assert.equals(JsonString.valueOf(UID.asString()), push.getO("uid").get());
        Assert.equals(JsonString.valueOf("album_deltas_updated"), push.getO("class").get());

        JsonObject xivaData = (JsonObject)push.getO("xiva_data").get();
        Assert.equals(JsonString.valueOf("album_deltas_updated"), xivaData.getByPath("t"));
        Assert.equals(JsonString.valueOf(UID.asString()), xivaData.getByPath("uid"));
        Assert.equals(revision, xivaData.getByPath("revision").toString());

        JsonObject repack = (JsonObject)push.getO("repack").get();
        List<JsonValue> list = ((JsonArray)repack.getByPath("other", "repack_payload")).getArray();
        Assert.isTrue(list.contains(JsonString.valueOf("t")));
        Assert.isTrue(list.contains(JsonString.valueOf("uid")));
        Assert.isTrue(list.contains(JsonString.valueOf("revision")));

    }

    private void checkTitleChangedPush(MapF<String, JsonValue> push, Album album) {

        Assert.equals(JsonString.valueOf(UID.asString()), push.getO("uid").get());
        Assert.equals(JsonString.valueOf("albums"), push.getO("class").get());

        JsonObject root = (JsonObject)((JsonObject)push.getO("xiva_data").get()).get("root");
        Assert.equals(JsonString.valueOf("album"), root.getO("tag").get());
        Assert.equals(
                JsonString.valueOf("title_change"),
                root.getByPath("parameters", "type"));
        Assert.equals(
                JsonString.valueOf(album.getPublicKey().getOrElse("")),
                root.getByPath("parameters", "public_key"));
        Assert.equals(
                JsonString.valueOf(album.getId().toString()),
                root.getByPath("parameters", "id"));

    }

    private void checkCoverChangedPush(MapF<String, JsonValue> push, Album album) {

        Assert.equals(JsonString.valueOf(UID.asString()), push.getO("uid").get());
        Assert.equals(JsonString.valueOf("albums"), push.getO("class").get());

        JsonObject root = (JsonObject)((JsonObject)push.getO("xiva_data").get()).get("root");
        Assert.equals(JsonString.valueOf("album"), root.getO("tag").get());
        Assert.equals(
                JsonString.valueOf("cover_change"),
                root.getByPath("parameters", "type"));
        Assert.equals(
                JsonString.valueOf(album.getPublicKey().getOrElse("")),
                root.getByPath("parameters", "public_key"));
        Assert.equals(
                JsonString.valueOf(album.getId().toString()),
                root.getByPath("parameters", "id"));

    }

    private void checkItemRemovePush(MapF<String, JsonValue> push, Album album, AlbumItem item) {

        Assert.equals(JsonString.valueOf(UID.asString()), push.getO("uid").get());
        Assert.equals(JsonString.valueOf("albums"), push.getO("class").get());

        JsonObject root = (JsonObject)((JsonObject)push.getO("xiva_data").get()).get("root");
        Assert.equals(JsonString.valueOf("album"), root.getO("tag").get());
        Assert.equals(
                JsonString.valueOf("items_remove"),
                root.getByPath("parameters", "type"));
        Assert.equals(
                JsonNumber.valueOf(0),
                root.getByPath("parameters", "auto_handle"));
        Assert.equals(
                JsonString.valueOf(album.getPublicKey().getOrElse("")),
                root.getByPath("parameters", "public_key"));

        List<JsonValue> values = ((JsonArray)((JsonObject)push.getO("xiva_data").get()).get("values")).getArray();
        Assert.equals(1, values.size());

        JsonObject value = (JsonObject)values.get(0);
        Assert.equals(
                JsonString.valueOf("item"),
                value.getByPath("tag"));
        Assert.equals(
                JsonString.valueOf(item.getId().toString()),
                value.getByPath("parameters", "id"));

    }

    @Test
    @SneakyThrows
    public void setTitleForPersonalAlbum() {

        ArgumentCaptor<CeleryJob> jobCaptor = ArgumentCaptor.forClass(CeleryJob.class);
        Mockito.doNothing().when(celeryTaskManager).submit(jobCaptor.capture());

        // Create album without photo
        Album album = createFirstPersonalAlbum("My personal album");

        /**
         * Change Title
         **/

        // Title change
        String newTitle = "My ugly personal album";
        HttpResponse response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&title=" + URLEncoder.encode(newTitle, "UTF-8"), "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(newTitle, jsonObject.get("title"));

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(newTitle, album.getTitle());

        // Check push
        List<CeleryJob> pushes = jobCaptor.getAllValues();
        Assert.equals(2, pushes.size());

        checkDatabaseChangedPush(pushes.get(0).getKwargs(), "1");
        checkTitleChangedPush(pushes.get(1).getKwargs(), album);

        // Title change to empty
        response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&title=", "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(newTitle, jsonObject.get("title"));

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(newTitle, album.getTitle());

    }

    @Test
    @SneakyThrows
    public void setTitleForFacesAlbum() {

        ArgumentCaptor<CeleryJob> jobCaptor = ArgumentCaptor.forClass(CeleryJob.class);
        Mockito.doNothing().when(celeryTaskManager).submit(jobCaptor.capture());

        // Create album without photo
        Album album = createFirstFacesAlbum("My faces album");

        /**
         * Change Title
         **/

        // Title change
        String newTitle = "My ugly faces album";
        HttpResponse response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&title=" + URLEncoder.encode(newTitle, "UTF-8"), "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(newTitle, jsonObject.get("title"));

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(newTitle, album.getTitle());

        // Check push
        List<CeleryJob> pushes = jobCaptor.getAllValues();
        Assert.equals(2, pushes.size());

        checkDatabaseChangedPush(pushes.get(0).getKwargs(), "1");
        checkTitleChangedPush(pushes.get(1).getKwargs(), album);

        // Title change to empty
        response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&title=", "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(newTitle, jsonObject.get("title"));

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(newTitle, album.getTitle());

    }

    @Test
    @SneakyThrows
    public void setCoverForPersonalAlbum() {

        ArgumentCaptor<CeleryJob> jobCaptor = ArgumentCaptor.forClass(CeleryJob.class);
        Mockito.doNothing().when(celeryTaskManager).submit(jobCaptor.capture());

        // Create album with two photo
        Album album = createFirstPersonalAlbum("My personal album");
        addItemToAlbum(album, "image-1.jpg");
        addItemToAlbum(album, "image-2.jpg");

        ListF<AlbumItem> albumItems = albumItemDao.getAllAlbumItems(UID, album.getId());
        Assert.equals(2, albumItems.length());

        /**
         * Change Cover
         **/

        // Cover change by index
        String coverId = albumItems.get(1).getId().toString();
        HttpResponse response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&cover=1", "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(coverId, jsonObject.getJSONObject("cover").get("id"));

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(coverId, album.getCoverId().get().toString());

        // Check push
        List<CeleryJob> pushes = jobCaptor.getAllValues();
        Assert.equals(2, pushes.size());

        checkDatabaseChangedPush(pushes.get(0).getKwargs(), "1");
        checkCoverChangedPush(pushes.get(1).getKwargs(), album);

        // Cover change by id
        coverId = albumItems.get(0).getId().toString();
        response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&cover=" + coverId, "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(coverId, jsonObject.getJSONObject("cover").get("id"));

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(coverId, album.getCoverId().get().toString());

    }

    @Test
    @SneakyThrows
    public void setCoverForGeoAlbum() {

        ArgumentCaptor<CeleryJob> jobCaptor = ArgumentCaptor.forClass(CeleryJob.class);
        Mockito.doNothing().when(celeryTaskManager).submit(jobCaptor.capture());

        // Create album with two photo
        Album album = createFirstGeoAlbum("My geo album");
        addItemToAlbum(album, "image-1.jpg");
        addItemToAlbum(album, "image-2.jpg");

        ListF<AlbumItem> albumItems = albumItemDao.getAllAlbumItems(UID, album.getId());
        Assert.equals(2, albumItems.length());

        /**
         * Change Cover
         **/

        // Cover change by index
        String coverId = albumItems.get(1).getId().toString();
        HttpResponse response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&cover=1", "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(coverId, jsonObject.getJSONObject("cover").get("id"));

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(coverId, album.getCoverId().get().toString());

        // Check push
        List<CeleryJob> pushes = jobCaptor.getAllValues();
        Assert.equals(2, pushes.size());

        checkDatabaseChangedPush(pushes.get(0).getKwargs(), "1");
        checkCoverChangedPush(pushes.get(1).getKwargs(), album);

        // Cover change by id
        coverId = albumItems.get(0).getId().toString();
        response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&cover=" + coverId, "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(coverId, jsonObject.getJSONObject("cover").get("id"));

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(coverId, album.getCoverId().get().toString());

    }

    @Test
    @SneakyThrows
    public void setCoverForFacesAlbum() {

        ArgumentCaptor<CeleryJob> jobCaptor = ArgumentCaptor.forClass(CeleryJob.class);
        Mockito.doNothing().when(celeryTaskManager).submit(jobCaptor.capture());

        // Create album with two photo
        Album album = createFirstGeoAlbum("My faces album");
        addItemToAlbum(album, "image-1.jpg");
        addItemToAlbum(album, "image-2.jpg");

        ListF<AlbumItem> albumItems = albumItemDao.getAllAlbumItems(UID, album.getId());
        Assert.equals(2, albumItems.length());

        /**
         * Change Cover
         **/

        // Cover change by index
        String coverId = albumItems.get(1).getId().toString();
        HttpResponse response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&cover=1", "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(coverId, jsonObject.getJSONObject("cover").get("id"));

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(coverId, album.getCoverId().get().toString());

        // Check push
        List<CeleryJob> pushes = jobCaptor.getAllValues();
        Assert.equals(2, pushes.size());

        checkDatabaseChangedPush(pushes.get(0).getKwargs(), "1");
        checkCoverChangedPush(pushes.get(1).getKwargs(), album);

        // Cover change by id
        coverId = albumItems.get(0).getId().toString();
        response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&cover=" + coverId, "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(coverId, jsonObject.getJSONObject("cover").get("id"));

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(coverId, album.getCoverId().get().toString());

    }

    @Test
    @SneakyThrows
    public void setLayoutForPersonalAlbum() {

        ArgumentCaptor<CeleryJob> jobCaptor = ArgumentCaptor.forClass(CeleryJob.class);
        Mockito.doNothing().when(celeryTaskManager).submit(jobCaptor.capture());

        // Create album without photo
        Album album = createFirstPersonalAlbum("My personal album");
        Instant oldDateModified = album.getDateModified().get();

        /**
         * Change Layout
         **/

        // Layout change
        String newLayout = "waterfall";
        HttpResponse response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&layout=" + newLayout, "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(newLayout, jsonObject.get("layout").toString().toLowerCase());
        Assert.equals(Long.toString(InstantUtils.toSecondsHalfDown(oldDateModified)), jsonObject.get("mtime").toString());

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(newLayout, album.getLayout().get().toString().toLowerCase());
        Assert.equals(oldDateModified, album.getDateModified().get());

        // Check push
        List<CeleryJob> pushes = jobCaptor.getAllValues();
        Assert.equals(1, pushes.size());

        checkDatabaseChangedPush(pushes.get(0).getKwargs(), "1");

    }

    @Test
    @SneakyThrows
    public void setLayoutForGeoAlbum() {

        ArgumentCaptor<CeleryJob> jobCaptor = ArgumentCaptor.forClass(CeleryJob.class);
        Mockito.doNothing().when(celeryTaskManager).submit(jobCaptor.capture());

        // Create album without photo
        Album album = createFirstGeoAlbum("Geo");
        Instant oldDateModified = album.getDateModified().get();

        /**
         * Change Layout
         **/

        // Layout change
        String newLayout = "waterfall";
        HttpResponse response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&layout=" + newLayout, "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(newLayout, jsonObject.get("layout").toString().toLowerCase());
        Assert.equals(Long.toString(InstantUtils.toSecondsHalfDown(oldDateModified)), jsonObject.get("mtime").toString());

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(newLayout, album.getLayout().get().toString().toLowerCase());
        Assert.equals(oldDateModified, album.getDateModified().get());

        // Check push
        List<CeleryJob> pushes = jobCaptor.getAllValues();
        Assert.equals(1, pushes.size());

        checkDatabaseChangedPush(pushes.get(0).getKwargs(), "1");

    }

    @Test
    @SneakyThrows
    public void setLayoutForFacesAlbum() {

        ArgumentCaptor<CeleryJob> jobCaptor = ArgumentCaptor.forClass(CeleryJob.class);
        Mockito.doNothing().when(celeryTaskManager).submit(jobCaptor.capture());

        // Create album without photo
        Album album = createFirstFacesAlbum("Faces");
        Instant oldDateModified = album.getDateModified().get();

        /**
         * Change Layout
         **/

        // Layout change
        String newLayout = "waterfall";
        HttpResponse response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&layout=" + newLayout, "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(newLayout, jsonObject.get("layout").toString().toLowerCase());
        Assert.equals(Long.toString(InstantUtils.toSecondsHalfDown(oldDateModified)), jsonObject.get("mtime").toString());

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(newLayout, album.getLayout().get().toString().toLowerCase());
        Assert.equals(oldDateModified, album.getDateModified().get());

        // Check push
        List<CeleryJob> pushes = jobCaptor.getAllValues();
        Assert.equals(1, pushes.size());

        checkDatabaseChangedPush(pushes.get(0).getKwargs(), "1");

    }

    @Test
    @SneakyThrows
    public void setLayoutForFavoritesAlbum() {

        ArgumentCaptor<CeleryJob> jobCaptor = ArgumentCaptor.forClass(CeleryJob.class);
        Mockito.doNothing().when(celeryTaskManager).submit(jobCaptor.capture());

        // Create album without photo
        Album album = createFirstFavoritesAlbum("Favorites");
        Instant oldDateModified = album.getDateModified().get();

        /**
         * Change Layout
         **/

        // Layout change
        String newLayout = "waterfall";
        HttpResponse response = a3TestHelper.patch("/api/v1/albums/album_set_attr"
                + "?uid=" + UID.asString()
                + "&album_id=" + album.getId()
                + "&layout=" + newLayout, "");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check response
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
        Assert.equals(newLayout, jsonObject.get("layout").toString().toLowerCase());
        Assert.equals(Long.toString(InstantUtils.toSecondsHalfDown(oldDateModified)), jsonObject.get("mtime").toString());

        // Check database
        album = albumDao.findAlbum(UID, album.getId()).get();
        Assert.equals(newLayout, album.getLayout().get().toString().toLowerCase());
        Assert.equals(oldDateModified, album.getDateModified().get());

        // Check push
        List<CeleryJob> pushes = jobCaptor.getAllValues();
        Assert.equals(1, pushes.size());

        checkDatabaseChangedPush(pushes.get(0).getKwargs(), "1");

    }

    @Test
    @SneakyThrows
    public void socialCoverUpdateByTitleForPersonalAlbum() {

        //TODO: мигация теста test_social_cover_update(), добавить в т.ч. для других альбомов, для которых нужно
        //      (тут заглушка только для personal)

    }

    /**
     * Album Publish Tests
     **/

    @Test
    public void publishPersonalAlbum() {

    }

    @Test
    public void publishGeoAlbum() {

    }

    @Test
    public void publishFavoritesAlbum() {

    }

    @Test
    public void publishFacesAlbum() {

    }

    /**
     * Album UnPublish Tests
     **/

    @Test
    public void unPublishPersonalAlbum() {

    }

    @Test
    public void unPublishGeoAlbum() {

    }

    @Test
    public void unPublishFavoritesAlbum() {

    }

    @Test
    public void unPublishFacesAlbum() {

    }

    @Value("${a3.port}")
    private int port;

    private A3TestHelper a3TestHelper;

    @PostConstruct
    public void startServers() {
        this.a3TestHelper = new A3TestHelper(port);
        this.a3TestHelper.startServers(applicationContext);
    }

    private Album createFirstPersonalAlbum(String title) {

        Album album = createAlbum(UID, title);
        albumDao.insert(album);
        albumDeltaDao.tryInitializeCurrentRevision(UID);

        ListF<Album> albums = albumDao.getAlbums(UID, AlbumType.PERSONAL);
        Assert.equals(1, albums.length());
        Assert.equals(title, albums.get(0).getTitle());

        return albums.get(0);

    }

    private Album createFirstGeoAlbum(String title) {

        Album album = createGeoAlbum(UID, title);
        albumDao.insert(album);
        albumDeltaDao.tryInitializeCurrentRevision(UID);

        ListF<Album> albums = albumDao.getAlbums(UID, AlbumType.GEO);
        Assert.equals(1, albums.length());
        Assert.equals(title, albums.get(0).getTitle());

        return albums.get(0);

    }

    private Album createFirstFacesAlbum(String title) {

        Album album = createFacesAlbum(UID, title);
        albumDao.insert(album);
        albumDeltaDao.tryInitializeCurrentRevision(UID);

        ListF<Album> albums = albumDao.getAlbums(UID, AlbumType.FACES);
        Assert.equals(1, albums.length());
        Assert.equals(title, albums.get(0).getTitle());

        return albums.get(0);

    }

    private Album createFirstFavoritesAlbum(String title) {

        Album album = createFavoritesAlbum(UID, title);
        albumDao.insert(album);
        albumDeltaDao.tryInitializeCurrentRevision(UID);

        ListF<Album> albums = albumDao.getAlbums(UID, AlbumType.FAVORITES);
        Assert.equals(1, albums.length());
        Assert.equals(title, albums.get(0).getTitle());

        return albums.get(0);

    }

    private AlbumItem addItemToAlbum(Album album, String name) {

        AlbumItem albumItem = createItem(album);
        filesystem.createFile(
                PRINCIPAL,
                DjfsResourcePath.cons(UID, "/disk").getChildPath(name),
                f -> f.fileId(DjfsFileId.cons(albumItem.getObjectId()))
        );
        albumItemDao.insert(albumItem);

        return albumItem;

    }

}
