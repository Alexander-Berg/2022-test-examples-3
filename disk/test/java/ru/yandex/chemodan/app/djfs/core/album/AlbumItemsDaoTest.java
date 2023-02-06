package ru.yandex.chemodan.app.djfs.core.album;

import java.util.Random;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.misc.test.Assert;

public class AlbumItemsDaoTest extends DjfsAlbumsTestBase {
    @Autowired
    protected AlbumDao albumDao;

    @Autowired
    protected AlbumItemDao albumItemDao;

    @Test
    public void fetchWholeAlbumItems() {
        Album album = createAlbum(UID,"Test");
        albumDao.insert(album);

        int itemCount = 10;
        for (int i = 0; i < itemCount; i++) {
            AlbumItem item = createItem(album);
            albumItemDao.insert(item);
        }

        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), 100);
        Assert.sizeIs(itemCount, items);
    }

    @Test
    public void fetchSeveralAlbums() {
        Album album1 = createAlbum(UID,"Album1");
        albumDao.insert(album1);

        int itemCount1 = 10;
        for (int i = 0; i < itemCount1; i++) {
            AlbumItem item = createItem(album1);
            albumItemDao.insert(item);
        }

        Album album2 = createAlbum(UID,"Album2");
        albumDao.insert(album2);

        int itemCount2 = 17;
        for (int i = 0; i < itemCount2; i++) {
            AlbumItem item = createItem(album2);
            albumItemDao.insert(item);
        }

        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album1.getId(), album2.getId()), 100);
        Assert.sizeIs(itemCount1 + itemCount2, items);

        SetF<ObjectId> albumIds = Cf.toSet(items.map(AlbumItem::getAlbumId));
        Assert.equals(Cf.set(album1.getId(), album2.getId()), albumIds);
    }

    @Test
    public void limitWithMoreItems() {
        Album album = createAlbum(UID,"Test");
        albumDao.insert(album);

        int itemCount = 10;
        for (int i = 0; i < itemCount; i++) {
            AlbumItem item = createItem(album);
            albumItemDao.insert(item);
        }

        int limit = 4;
        ListF<AlbumItem> items = albumItemDao.getAllAlbumItems(UID, Cf.list(album.getId()), limit);
        Assert.sizeIs(limit, items);
    }

    @Test
    public void testItemsOrderByOrderIndexAsc() {
        ListF<Album> albums = Cf.arrayList();
        for (int i = 0; i < 3; i++) {
            Album album = createAlbum(UID, "Album " + i);
            albums.add(album);
            albumDao.insert(album);
        }

        Random rand = new Random();

        for (int i = 0; i < 100; i++) {
            int num = rand.nextInt(albums.size());
            AlbumItem item = createItem(albums.get(num), rand.nextLong());
            albumItemDao.insert(item);
        }

        ListF<AlbumItem> albumItems = albumItemDao.getAllAlbumItems(UID, albums.map(Album::getId), 100500);
        MapF<ObjectId, ListF<AlbumItem>> itemsByAlbum = albumItems.groupBy(AlbumItem::getAlbumId);
        for (ListF<AlbumItem> items : itemsByAlbum.values()) {
            ListF<Double> indices = items.map(x -> x.getOrderIndex().get());
            for (int i = 1; i < indices.size(); i++) {
                Assert.le(indices.get(i - 1), indices.get(i));
            }
        }
    }
}
