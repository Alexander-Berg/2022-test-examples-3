package ru.yandex.chemodan.app.djfs.albums.notification;

import org.bson.types.ObjectId;
import org.junit.Test;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.album.Album;
import ru.yandex.chemodan.app.djfs.core.notification.XivaPushGeneratorAlbumUtils;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.JsonValue;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class XivaDataTest {

    @Test
    public void mergeAlbumsTest() {
        DjfsUid uid = DjfsUid.cons(123);
        Album srcAlbum = Album.builder().id(new ObjectId()).uid(uid).title("Album with name").build();
        Album dstAlbum = Album.builder().id(new ObjectId()).uid(uid).title("").build();

        MapF<String, JsonValue> kwargs = XivaPushGeneratorAlbumUtils
                .albumsMerged(uid, srcAlbum, dstAlbum, Option.of(srcAlbum.getTitle())).toCeleryKwargs();
        Assert.equals(JsonString.valueOf(uid.toString()), kwargs.getTs("uid"));
        Assert.equals(JsonString.valueOf("albums"), kwargs.getTs("class"));
        JsonValue body = kwargs.getTs("xiva_data");
        Assert.equals("{ \"root\": { \"tag\": \"album\", \"parameters\": { \"src_id\": \"" +
                        srcAlbum.getId().toHexString() + "\", " +
                "\"dst_id\": \"" +
                        dstAlbum.getId().toHexString() + "\", \"type\": \"albums_merged\", " +
                "\"new_dst_title\": \"Album with name\" } } }",
                body.serialize());
    }

    @Test
    public void removeAlbumTest() {
        DjfsUid uid = DjfsUid.cons(123);
        Album album = Album.builder()
                .id(new ObjectId()).uid(uid).publicKey(Option.of("some_public_key")).build();

        MapF<String, JsonValue> kwargs = XivaPushGeneratorAlbumUtils.albumRemoved(uid, album).toCeleryKwargs();
        Assert.equals(JsonString.valueOf(uid.toString()), kwargs.getTs("uid"));
        Assert.equals(JsonString.valueOf("albums"), kwargs.getTs("class"));
        JsonValue body = kwargs.getTs("xiva_data");
        Assert.equals("{ \"root\": { \"tag\": \"album\", \"parameters\": { \"id\": \"" + album.getId().toHexString()
                + "\", \"type\": \"album_remove\", \"public_key\": \"some_public_key\" } } }", body.serialize());
    }
}
