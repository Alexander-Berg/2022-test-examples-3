package ru.yandex.chemodan.app.djfs.core.album;


import org.bson.types.ObjectId;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.misc.test.Assert;

public class AlbumDaoTest extends DjfsAlbumsTestBase {
    @Autowired
    protected AlbumDao albumDao;

    @Autowired
    protected AlbumItemDao albumItemDao;

    private AlbumItem createAlbumItem(Album album) {
        return AlbumItem.builder()
                .id(new ObjectId())
                .uid(album.getUid())
                .albumId(album.getId())
                .objectId(DjfsFileId.random().getValue())
                .objectType(AlbumItemType.RESOURCE)
                .description(Option.empty())
                .orderIndex(Option.empty())
                .groupId(Option.empty())
                .faceInfo(Option.empty())
                .dateCreated(Option.of(Instant.now()))
                .build();
    }

    @Test
    public void fetchAllAlbums() {
        Album album = createAlbum(UID, "My lovely album", "Here I keep all my great photos");
        albumDao.insert(album);

        ListF<Album> albums = albumDao.getAlbums(UID, AlbumType.PERSONAL);
        Assert.sizeIs(1, albums);

        Assert.equals(album.getUid(), albums.get(0).getUid());
        Assert.equals(album.getId(), albums.get(0).getId());
    }

    @Test
    public void fetchAlbumWithItemsCount() {
        Album album = createAlbum(UID, "Test", "Description");
        albumDao.insert(album);

        int itemsCount = 12;
        for (int i = 0; i < itemsCount; i++) {
            AlbumItem item = createAlbumItem(album);
            albumItemDao.insert(item);
        }

        ListF<ExtendedAlbum> albumsWithMeta = albumDao.getExtendedAlbums(UID, Cf.list(AlbumType.PERSONAL));
        Assert.sizeIs(1, albumsWithMeta);

        Assert.equals(itemsCount, albumsWithMeta.get(0).itemsCount);
    }

    @Test
    public void fetchAlbumWithCover() {
        Album album = createAlbum(UID, "Test", "Description");

        AlbumItem coverItem = createAlbumItem(album);
        Album albumWithCover = album.toBuilder().coverId(Option.of(coverItem.getId())).build();

        transactionUtils.executeInNewOrCurrentTransaction(UID, () -> {
            albumItemDao.insert(coverItem);
            albumDao.insert(albumWithCover);
        });

        int itemsCount = 12;
        for (int i = 0; i < itemsCount; i++) {
            albumItemDao.insert(createAlbumItem(albumWithCover));
        }

        ListF<ExtendedAlbum> albumsWithMeta = albumDao.getExtendedAlbums(UID, Cf.list(AlbumType.PERSONAL));
        Assert.sizeIs(1, albumsWithMeta);

        Assert.some(albumsWithMeta.get(0).coverResourceId);
        DjfsResourceId coverResourceId = albumsWithMeta.get(0).coverResourceId.get();
        Assert.equals(coverItem.getObjectId(), coverResourceId.getFileId().getValue());
        Assert.equals(UID, coverResourceId.getUid());
    }

    @Test
    public void maxAlbumsVersion() {
        long revision1 = 100499;
        Album album1 = createAlbum(UID, "Test1").toBuilder().revision(Option.of(revision1)).build();
        albumDao.insert(album1);
        long revision2 = 100500;
        Album album2 = createAlbum(UID, "Test2").toBuilder().revision(Option.of(revision2)).build();
        albumDao.insert(album2);
        long revision3 = 42;
        Album album3 = createAlbum(UID, "Test3").toBuilder().revision(Option.of(revision3)).build();
        albumDao.insert(album3);

        Assert.equals(revision2, albumDao.getMaxAlbumVersion(UID));
    }
}
