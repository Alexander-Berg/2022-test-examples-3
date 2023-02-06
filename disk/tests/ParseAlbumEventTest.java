package ru.yandex.chemodan.eventlog.log.tests;

import org.junit.Test;

import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.MpfsAddress;
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
public class ParseAlbumEventTest extends AbstractParseEventTest {
    private static final MpfsAddress ADDRESS = MpfsAddress.parseFile(UID + ":/disk/target");

    private static final Album ALBUM = new Album("55f936cce9c1e3446a05b1ca", "album title");

    private static final String ALBUM_TSKV_LINE = "album_id=" + ALBUM.id + "\t" +
            "album_is_public=1\t" +
            "album_title=" + ALBUM.title;

    private static final AlbumItem RESOURCE_ITEM =
            new AlbumItem(AlbumItemType.RESOURCE, "55f98b71fe47cf116bd8d568", ALBUM);

    private static final AlbumItem CHILD_ALBUM_ITEM =
            new AlbumItem(AlbumItemType.ALBUM, "55f98b71fe47cf116bd8d568", ALBUM);

    private static final String RESOURCE_ITEM_TSKV_LINE =
            "album_id=" + RESOURCE_ITEM.album.id + "\t" +
                    "album_title=" + RESOURCE_ITEM.album.title + "\t" +
                    "album_item_id=" + RESOURCE_ITEM.itemId + "\t" +
                    "album_item_type=resource\t" +
                    "resource_address=" + ADDRESS + "\t" +
                    "resource_file_id=a57ff145d3ce1e7dc05cf16a01e318ed83c6e218ef887f1d765a5bccace42207\t"+
                    "owner_uid=" + UID + "\t" +
                    FILE_RESOURCE_LINE;

    private static final String CHILD_ALBUM_ITEM_TSKV_LINE =
            "album_id=" + CHILD_ALBUM_ITEM.album.id + "\t" +
                    "album_title=" + CHILD_ALBUM_ITEM.album.title + "\t" +
                    "album_item_id=" + CHILD_ALBUM_ITEM.itemId + "\t" +
                    "album_item_type=album\t" +
                    "child_album_id=55f936cce9c1e3446a05b1cb\t" +
                    "child_album_title=child_title";

    @Test
    public void testCreateWithoutItems() {
        assertParseEquals(UID, "album-create", ALBUM_TSKV_LINE + "\talbum_item_count=0",
                new CreateAlbumEvent(EVENT_METADATA, ALBUM, 0),
                EventType.ALBUM_CREATE);
    }

    @Test
    public void testCreateWithItems() {
        assertParseEquals(UID, "album-create", ALBUM_TSKV_LINE + "\talbum_item_count=5",
                new CreateAlbumEvent(EVENT_METADATA, ALBUM, 5),
                EventType.ALBUM_CREATE);
    }

    @Test
    public void testRemove() {
        assertParseEquals(UID, "album-remove", ALBUM_TSKV_LINE, new RemoveAlbumEvent(EVENT_METADATA, ALBUM));
    }

    @Test
    public void testChangeCover() {
        assertParseEquals(UID, "album-change-cover", ALBUM_TSKV_LINE, new ChangeCoverAlbumEvent(EVENT_METADATA, ALBUM));
    }

    @Test
    public void testChangeCoverOffset() {
        assertParseEquals(
                UID, "album-change-cover-offset", ALBUM_TSKV_LINE,
                new ChangeCoverOffsetAlbumEvent(EVENT_METADATA, ALBUM)
        );
    }

    @Test
    public void testChangePublicity() {
        assertParseEquals(
                UID, "album-change-publicity", ALBUM_TSKV_LINE,
                new ChangePublicityAlbumEvent(EVENT_METADATA, ALBUM, true)
        );
    }

    @Test
    public void testChangeTitle() {
        assertParseEquals(
                UID, "album-change-title", ALBUM_TSKV_LINE + "\tprev_album_title=oldtitle",
                new ChangeTitleAlbumEvent(EVENT_METADATA, ALBUM, "oldtitle")
        );
    }

    @Test
    public void testPostToSocial() {
        assertParseEquals(
                UID, "album-post-to-social", ALBUM_TSKV_LINE + "\tprovider=vkontakte",
                new PostToSocialAlbumEvent(EVENT_METADATA, ALBUM, "vkontakte")
        );
    }

    @Test
    public void testCreateWithResourceItem() {
        assertParseEquals(
                UID, "album-create-item", RESOURCE_ITEM_TSKV_LINE,
                new ResourceAlbumItemEvent(EVENT_METADATA, RESOURCE_ITEM,
                        AlbumItemEventType.CREATE, ADDRESS, FILE_RESOURCE)
        );
    }

    @Test
    public void testCreateWithChildAlbumItem() {
        assertParseEquals(
                UID, "album-create-item", CHILD_ALBUM_ITEM_TSKV_LINE,
                new ChildAlbumItemEvent(EVENT_METADATA, CHILD_ALBUM_ITEM,
                        AlbumItemEventType.CREATE, "55f936cce9c1e3446a05b1cb", "child_title")
        );
    }

    @Test
    public void testItemsAppend() {
        assertParseEquals(
                UID, "album-items-append", RESOURCE_ITEM_TSKV_LINE,
                new ResourceAlbumItemEvent(EVENT_METADATA, RESOURCE_ITEM,
                        AlbumItemEventType.APPEND, ADDRESS, FILE_RESOURCE));
    }

    @Test
    public void testItemsRemove() {
        assertParseEquals(
                UID, "album-items-remove", RESOURCE_ITEM_TSKV_LINE,
                new ResourceAlbumItemEvent(EVENT_METADATA, RESOURCE_ITEM,
                        AlbumItemEventType.REMOVE, ADDRESS, FILE_RESOURCE));
    }
}
