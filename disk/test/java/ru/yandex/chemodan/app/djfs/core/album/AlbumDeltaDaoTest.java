package ru.yandex.chemodan.app.djfs.core.album;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;

public class AlbumDeltaDaoTest extends DjfsAlbumsTestBase {
    @Autowired
    protected AlbumDeltaDao albumDeltaDao;

    @Test
    public void insertAlbumItemInsertDelta() {
        Album album = createAlbum(UID, "Test album");
        AlbumItem item = createItem(album);

        AlbumDelta delta = createInsertDelta(album, item, 1);
        albumDeltaDao.insert(delta);

        Option<AlbumDeltaRaw> deltaO = albumDeltaDao.findRaw(UID, delta.getRevision());
        Assert.some(deltaO);
    }
}
