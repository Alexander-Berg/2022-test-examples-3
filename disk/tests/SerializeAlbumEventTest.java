package ru.yandex.chemodan.app.eventloader.serializer.tests;

import org.junit.Test;

import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.MpfsAddress;
import ru.yandex.chemodan.eventlog.events.Resource;
import ru.yandex.chemodan.eventlog.events.album.Album;
import ru.yandex.chemodan.eventlog.events.album.AlbumItem;
import ru.yandex.chemodan.eventlog.events.album.AlbumItemEventType;
import ru.yandex.chemodan.eventlog.events.album.AlbumItemType;
import ru.yandex.chemodan.eventlog.events.album.ChangeCoverAlbumEvent;
import ru.yandex.chemodan.eventlog.events.album.ChangeCoverOffsetAlbumEvent;
import ru.yandex.chemodan.eventlog.events.album.ChangePublicityAlbumEvent;
import ru.yandex.chemodan.eventlog.events.album.ChangeTitleAlbumEvent;
import ru.yandex.chemodan.eventlog.events.album.ChildAlbumItemEvent;
import ru.yandex.chemodan.eventlog.events.album.CreateAlbumEvent;
import ru.yandex.chemodan.eventlog.events.album.PostToSocialAlbumEvent;
import ru.yandex.chemodan.eventlog.events.album.RemoveAlbumEvent;
import ru.yandex.chemodan.eventlog.events.album.ResourceAlbumItemEvent;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class SerializeAlbumEventTest extends AbstractSerializeEventTest {
    private static final MpfsAddress IMAGE_ADDRESS = MpfsAddress.parseFile(UID + ":/disk/dir/image.jpg");

    private static final Resource IMAGE_RESOURCE = Resource.file("image", "123", UID);

    private static final Album ALBUM = new Album("55f936cce9c1e3446a05b1ca", "title");

    private static final AlbumItem CHILD_ALBUM_ITEM =
            new AlbumItem(AlbumItemType.ALBUM, "55f98b71fe47cf116bd8d568", ALBUM);

    private static final AlbumItem RESOURCE_ITEM =
            new AlbumItem(AlbumItemType.RESOURCE, "55f98b71fe47cf116bd8d568", ALBUM);

    private static final String CHILD_ALBUM_ID = "55f936cce9c1e3446a05b1cb";

    private static final String CHILD_ALBUM_TITLE = "child title";

    @Test
    public void testCreate() {
        new ExpectedJson()
                .withAlbum(ALBUM)
                .serializeAndCheck(new CreateAlbumEvent(METADATA, ALBUM));
    }

    @Test
    public void testRemove() {
        new ExpectedJson()
                .withAlbum(ALBUM)
                .serializeAndCheck(new RemoveAlbumEvent(METADATA, ALBUM));
    }

    @Test
    public void testChangeCover() {
        new ExpectedJson()
                .withAlbum(ALBUM)
                .serializeAndCheck(new ChangeCoverAlbumEvent(METADATA, ALBUM));
    }

    @Test
    public void testChangeCoverOffset() {
        new ExpectedJson()
                .withAlbum(ALBUM)
                .serializeAndCheck(new ChangeCoverOffsetAlbumEvent(METADATA, ALBUM));
    }

    @Test
    public void testChangePublicity() {
        new ExpectedJson()
                .withAlbum(ALBUM)
                .with("album_is_public", true)
                .serializeAndCheck(new ChangePublicityAlbumEvent(METADATA, ALBUM, true));
    }

    @Test
    public void testChangeTitle() {
        new ExpectedJson()
                .withAlbum(ALBUM)
                .with("album_prev_title", "oldtitle")
                .serializeAndCheck(new ChangeTitleAlbumEvent(METADATA, ALBUM, "oldtitle"));
    }

    @Test
    public void testPostToSocial() {
        new ExpectedJson()
                .withAlbum(ALBUM)
                .with("invite_service", "facebook")
                .serializeAndCheck(new PostToSocialAlbumEvent(METADATA, ALBUM, "facebook"));
    }

    @Test
    public void testCreateItem() {
        new ExpectedJson()
                .withAlbumItem(RESOURCE_ITEM)
                .withSource(IMAGE_ADDRESS)
                .withResource(IMAGE_RESOURCE)
                .withEventType(EventType.ALBUM_CREATE)
                .serializeAndCheck(
                        new ResourceAlbumItemEvent(METADATA, RESOURCE_ITEM, AlbumItemEventType.CREATE,
                                IMAGE_ADDRESS, IMAGE_RESOURCE)
                );
    }

    @Test
    public void testAppendItem() {
        new ExpectedJson()
                .withAlbumItem(CHILD_ALBUM_ITEM)
                .with("child_album_id", CHILD_ALBUM_ID)
                .with("child_album_title", CHILD_ALBUM_TITLE)
                .serializeAndCheck(
                        new ChildAlbumItemEvent(METADATA, CHILD_ALBUM_ITEM, AlbumItemEventType.APPEND,
                                CHILD_ALBUM_ID, CHILD_ALBUM_TITLE)
                );
    }
}
