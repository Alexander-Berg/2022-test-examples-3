package ru.yandex.direct.core.testing.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.BannerImageOpts;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.repository.old.mapper.BannerImageMappings;
import ru.yandex.direct.core.entity.banner.type.bannerimage.BannerImageConverterKt;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.dbschema.ppc.Tables;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesStatusshow;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerImagesFormatsRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerImagesRecord;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplierBuilder;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES_FORMATS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES_POOL;
import static ru.yandex.direct.dbschema.ppc.tables.BannerImages.BANNER_IMAGES;
import static ru.yandex.direct.jooqmapper.ReaderWriterBuilders.convertibleProperty;
import static ru.yandex.direct.jooqmapper.ReaderWriterBuilders.property;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class TestBannerImageRepository {
    private final ShardHelper shardHelper;
    private final JooqMapperWithSupplier<OldBannerImage> bannerImageMapper;
    private final JooqMapperWithSupplier<BannerImageFormat> bannerImageFormatMapper;
    private DslContextProvider dslContextProvider;

    @Autowired
    public TestBannerImageRepository(DslContextProvider dslContextProvider, ShardHelper shardHelper) {
        this.dslContextProvider = dslContextProvider;
        this.shardHelper = shardHelper;

        this.bannerImageMapper = createBannerImageMapper();
        this.bannerImageFormatMapper = createBannerImageFormatMapper();
    }

    /**
     * Сохранение форматов изображений баннеров
     *
     * @param shard        шард
     * @param imageFormats сохраняемые форматы изображений
     * @return кол-во сохраненных форматов
     */
    public int addBannerImageFormats(int shard, List<BannerImageFormat> imageFormats) {
        if (imageFormats.isEmpty()) {
            return 0;
        }
        InsertHelper<BannerImagesFormatsRecord> insertHelper =
                new InsertHelper<>(dslContext(shard), BANNER_IMAGES_FORMATS);
        for (BannerImageFormat banner : imageFormats) {
            insertHelper.add(bannerImageFormatMapper, banner).newRecord();
        }
        return insertHelper.execute();
    }

    /**
     * Сохраняет изображения баннеров
     * <p>
     * В переданных объектах происходит генерация id
     *
     * @param shard  шард
     * @param images сохраняемые изображения
     * @return список id сохраненных изображений
     */
    public List<Long> addBannerImages(int shard, List<OldBannerImage> images) {
        generateImageIds(images);
        insertImagesToDb(shard, images);
        return mapList(images, OldBannerImage::getId);
    }

    public OldBannerImage getBannerImage(int shard, long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(bannerImageMapper.getFieldsToRead())
                .from(BANNER_IMAGES)
                .where(BANNER_IMAGES.BID.eq(bannerId))
                .fetchOne(bannerImageMapper::fromDb);
    }

    public void updateImageOpts(int shard, long bannerId, Set<BannerImageOpts> bannerImageOpts) {
        dslContextProvider.ppc(shard)
                .update(Tables.BANNER_IMAGES)
                .set(Tables.BANNER_IMAGES.OPTS, BannerImageConverterKt.optsToDb(bannerImageOpts))
                .where(Tables.BANNER_IMAGES.BID.eq(bannerId))
                .execute();
    }

    /**
     * удаление записи из banner_images
     *
     * @param shard     шард
     * @param bannerIds список id баннеров
     */
    public void delete(int shard, Collection<Long> bannerIds) {
        dslContextProvider.ppc(shard)
                .deleteFrom(Tables.BANNER_IMAGES)
                .where(Tables.BANNER_IMAGES.BID.in(bannerIds))
                .execute();
    }

    /**
     * Обновить statusModerate для картинок баннеров
     *
     * @param shard     шард
     * @param bannerIds список id баннеров
     * @param status    устанавливаемый статус
     */
    public void updateStatusModerate(int shard, Collection<Long> bannerIds, OldStatusBannerImageModerate status) {
        dslContextProvider.ppc(shard)
                .update(Tables.BANNER_IMAGES)
                .set(Tables.BANNER_IMAGES.STATUS_MODERATE, OldStatusBannerImageModerate.toSource(status))
                .where(Tables.BANNER_IMAGES.BID.in(bannerIds))
                .execute();
    }

    /**
     * Обновить statusModerate для картинок баннеров
     *
     * @param shard      шард
     * @param bannerIds  список id баннеров
     * @param statusShow устанавливаемый статус: показывать картинку или нет.
     */
    public void updateStatusShow(int shard, Collection<Long> bannerIds, BannerImagesStatusshow statusShow) {
        dslContextProvider.ppc(shard)
                .update(Tables.BANNER_IMAGES)
                .set(Tables.BANNER_IMAGES.STATUS_SHOW, statusShow)
                .where(Tables.BANNER_IMAGES.BID.in(bannerIds))
                .execute();
    }

    /**
     * Обновить ID баннера по ID хэшу его картинки
     *
     * @param shard           Шард
     * @param bannerImageInfo Информация о баннере, включая ID и хэш его картинки
     * @param bsBannerId      Новый ID баннера
     */
    public void updateBannerIdByImageAndHash(int shard, BannerImageInfo<?> bannerImageInfo, Long bsBannerId) {
        dslContextProvider.ppc(shard)
                .update(Tables.BANNER_IMAGES)
                .set(Tables.BANNER_IMAGES.BANNER_ID, bsBannerId)
                .where(Tables.BANNER_IMAGES.IMAGE_ID.eq(bannerImageInfo.getBannerImageId()))
                .and(Tables.BANNER_IMAGES.IMAGE_HASH.eq(bannerImageInfo.getBannerImage().getImageHash()))
                .execute();
    }


    /**
     * Сгенерировать ID для всех картинок из списка
     *
     * @param images Список картинок для баннеров
     */
    private void generateImageIds(List<OldBannerImage> images) {
        Iterator<Long> imageIds =
                shardHelper.generateBannerIdsByBids(mapList(images, OldBannerImage::getBannerId)).iterator();
        images.forEach(image -> image.setId(imageIds.next()));
    }

    /**
     * Добавить картинки для баннеров в базу
     *
     * @param shard  Шард
     * @param images Список картинок
     */
    private void insertImagesToDb(int shard, List<OldBannerImage> images) {
        if (images.isEmpty()) {
            return;
        }
        InsertHelper<BannerImagesRecord> insertHelper = new InsertHelper<>(dslContext(shard), BANNER_IMAGES);
        for (OldBannerImage image : images) {
            insertHelper.add(bannerImageMapper, image).newRecord();
        }
        insertHelper.execute();
    }

    private DSLContext dslContext(int shard) {
        return dslContextProvider.ppc(shard);
    }

    private JooqMapperWithSupplier<OldBannerImage> createBannerImageMapper() {
        return JooqMapperWithSupplierBuilder.builder(OldBannerImage::new)
                .map(property(OldBannerImage.ID, BANNER_IMAGES.IMAGE_ID))
                .map(property(OldBannerImage.IMAGE_HASH, BANNER_IMAGES.IMAGE_HASH))
                .map(property(OldBannerImage.BANNER_ID, BANNER_IMAGES.BID))
                .map(property(OldBannerImage.BS_BANNER_ID, BANNER_IMAGES.BANNER_ID))
                .map(convertibleProperty(OldBannerImage.STATUS_MODERATE, BANNER_IMAGES.STATUS_MODERATE,
                        OldStatusBannerImageModerate::fromSource,
                        OldStatusBannerImageModerate::toSource))
                .map(convertibleProperty(OldBannerImage.STATUS_SHOW, BANNER_IMAGES.STATUS_SHOW,
                        BannerImageMappings::imageStatusShowFromDb,
                        BannerImageMappings::imageStatusShowToDb))
                .map(property(OldBannerImage.DATE_ADDED, BANNER_IMAGES.DATE_ADDED))
                .build();
    }

    private JooqMapperWithSupplier<BannerImageFormat> createBannerImageFormatMapper() {
        return JooqMapperWithSupplierBuilder.builder(BannerImageFormat::new)
                .map(property(BannerImageFormat.IMAGE_HASH, BANNER_IMAGES_FORMATS.IMAGE_HASH))
                .map(property(BannerImageFormat.MDS_GROUP_ID, BANNER_IMAGES_FORMATS.MDS_GROUP_ID))
                .map(convertibleProperty(BannerImageFormat.AVATAR_NAMESPACE, BANNER_IMAGES_FORMATS.NAMESPACE,
                        BannerImageFormat::avatarNamespaceFromDb,
                        BannerImageFormat::avatarNamespaceToDb))
                .map(convertibleProperty(BannerImageFormat.IMAGE_TYPE, BANNER_IMAGES_FORMATS.IMAGE_TYPE,
                        BannerImageFormat::imageTypeFromDb,
                        BannerImageFormat::imageTypeToDb))
                .map(property(BannerImageFormat.WIDTH, BANNER_IMAGES_FORMATS.WIDTH))
                .map(property(BannerImageFormat.HEIGHT, BANNER_IMAGES_FORMATS.HEIGHT))
                .map(property(BannerImageFormat.FORMATS_JSON, BANNER_IMAGES_FORMATS.FORMATS))
                .map(property(BannerImageFormat.MDS_META_JSON, BANNER_IMAGES_FORMATS.MDS_META))
                .map(convertibleProperty(BannerImageFormat.AVATAR_HOST, BANNER_IMAGES_FORMATS.AVATARS_HOST,
                        BannerImageFormat::avatarHostFromDb,
                        BannerImageFormat::avatarHostToDb))
                .build();
    }

    public LocalDateTime getImageCreateTime(int shard, Long clientId, String imageHash) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_IMAGES_POOL.CREATE_TIME)
                .from(BANNER_IMAGES_POOL)
                .where(BANNER_IMAGES_POOL.CLIENT_ID.eq(clientId).and(BANNER_IMAGES_POOL.IMAGE_HASH.eq(imageHash)))
                .fetchOne(BANNER_IMAGES_POOL.CREATE_TIME);
    }

    public void setImageCreateTime(int shard, Long clientId, String imageHash, LocalDateTime createTime) {
        dslContextProvider.ppc(shard)
                .update(BANNER_IMAGES_POOL)
                .set(BANNER_IMAGES_POOL.CREATE_TIME, createTime)
                .where(BANNER_IMAGES_POOL.CLIENT_ID.eq(clientId).and(BANNER_IMAGES_POOL.IMAGE_HASH.eq(imageHash)))
                .execute();
    }
}
