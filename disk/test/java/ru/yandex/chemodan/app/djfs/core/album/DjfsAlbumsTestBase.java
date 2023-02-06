package ru.yandex.chemodan.app.djfs.core.album;

import org.bson.types.ObjectId;
import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSinglePostgresUserTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.misc.geo.Coordinates;

public abstract class DjfsAlbumsTestBase extends DjfsSinglePostgresUserTestBase {

    protected final int CITY_REGION_ID = 123;
    protected final String CITY_NAME = "Moscow";
    protected final Coordinates POINT_COORDINATES = new Coordinates(55.751244, 37.618423);

    protected final int CITY_MICRODISTRICT_REGION_ID = 456;
    protected final String CITY_MICRODISTRICT_NAME = "Khamovniki";

    protected final int FEDERAL_SUBJECT_REGION_ID = 321;
    protected final String FEDERAL_SUBJECT_NAME = "Moscow and Moscow Area";

    protected final Coordinates OTHER_POINT_COORDINATES = new Coordinates(12.751244, 47.618423);

    @Autowired
    protected GeoAlbumManager geoAlbumManager;

    @Autowired
    protected PersonalAlbumManager personalAlbumManager;

    @Autowired
    protected AlbumDao albumDao;

    @Autowired
    protected AlbumItemDao albumItemDao;

    @Autowired
    protected AlbumDeltaDao albumDeltaDao;

    private Album.AlbumBuilder buildDefaultAlbum(DjfsUid uid, String title) {
        return Album.builder()
                .id(new ObjectId())
                .uid(uid)
                .title(title)
                .coverId(Option.empty())
                .coverOffsetY(Option.of(12.5))
                .description(Option.empty())

                .publicKey(Option.empty())
                .publicUrl(Option.empty())
                .shortUrl(Option.empty())

                .isPublic(false)
                .isBlocked(false)

                .blockReason(Option.empty())
                .flags(Option.empty())
                .layout(Option.of(AlbumLayoutType.WATERFALL))
                .dateCreated(Option.of(Instant.now()))
                .dateModified(Option.of(Instant.now()))

                .socialCoverStid(Option.empty())
                .fotkiAlbumId(Option.empty())

                .revision(Option.empty())
                .geoId(Option.empty())
                .hidden(false)
                .isDescSorting(Option.empty())
                .albumItemsSorting(Option.empty())
                .type(AlbumType.PERSONAL);
    }

    public Album createAlbum(DjfsUid uid, String title, String description, Option<ObjectId> coverFileId) {
        return createAlbum(uid, title, x -> x.description(Option.of(description)).coverId(coverFileId));
    }

    public Album createAlbum(DjfsUid uid, String title, Function<Album.AlbumBuilder, Album.AlbumBuilder> initialize) {
        return initialize.apply(buildDefaultAlbum(uid, title)).build();
    }

    public Album createAlbum(DjfsUid uid, String title, String description) {
        return createAlbum(uid, title, description, Option.empty());
    }

    public Album createAlbum(DjfsUid uid, String title) {
        return createAlbum(uid, title, "", Option.empty());
    }

    public Album createGeoAlbum(DjfsUid uid, String title) {
        return createAlbum(uid, title, "", Option.empty()).toBuilder().type(AlbumType.GEO).build();
    }

    public Album createFacesAlbum(DjfsUid uid, String title) {
        return createAlbum(uid, title, "", Option.empty()).toBuilder().type(AlbumType.FACES).build();
    }

    public Album createFavoritesAlbum(DjfsUid uid, String title) {
        return createAlbum(uid, title, "", Option.empty()).toBuilder().type(AlbumType.FAVORITES).build();
    }

    private AlbumItem.AlbumItemBuilder buildDefaultAlbumItem(Album album) {
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
                .dateCreated(Option.of(Instant.now()));
    }

    public AlbumItem createItem(Album album, double orderIndex) {
        return buildDefaultAlbumItem(album).orderIndex(Option.of(orderIndex)).build();
    }

    public AlbumItem createItem(Album album) {
        return buildDefaultAlbumItem(album).build();
    }

    public AlbumItem createItem(Album album,
            Function<AlbumItem.AlbumItemBuilder, AlbumItem.AlbumItemBuilder> initialize)
    {
        return initialize.apply(buildDefaultAlbumItem(album)).build();
    }

    public AlbumDelta createInsertDelta(Album album, AlbumItem item, long revision) {
        return AlbumUtils.createInsertAlbumItemDelta(album, item, revision);
    }
}
