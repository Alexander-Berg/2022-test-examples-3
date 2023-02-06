package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.old.Image;
import ru.yandex.direct.core.entity.banner.model.old.StatusImageModerate;
import ru.yandex.direct.dbschema.ppc.tables.records.ImagesRecord;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplierBuilder;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.dbschema.ppc.Tables.IMAGES;
import static ru.yandex.direct.jooqmapper.ReaderWriterBuilders.convertibleProperty;
import static ru.yandex.direct.jooqmapper.ReaderWriterBuilders.property;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
public class TestImageRepository {

    private final DslContextProvider dslContextProvider;
    private final ShardHelper shardHelper;

    private final JooqMapperWithSupplier<Image> imageMapper;

    @Autowired
    public TestImageRepository(DslContextProvider dslContextProvider,
                               ShardHelper shardHelper) {
        this.dslContextProvider = dslContextProvider;
        this.shardHelper = shardHelper;

        this.imageMapper = createImageMapper();
    }

    public String getImageText(int shard, long bid) {
        return dslContextProvider.ppc(shard)
                .select(IMAGES.IMAGE_TEXT)
                .from(IMAGES)
                .where(IMAGES.BID.eq(bid))
                .fetchOne(Record1::value1);
    }

    /**
     * Сохраняет изображения баннеров
     * <p>
     * В переданных объектах происходит генерация id
     *
     * @param images сохраняемые изображения
     * @param shard  шард
     * @return список id сохраненных изображений
     */
    public List<Long> addImages(int shard, List<Image> images) {
        generateImageIds(images);
        insertImagesToDb(shard, images);
        return mapList(images, Image::getId);
    }

    /**
     * Удаление записей из images
     *
     * @param shard     шард
     * @param bannerIds список id баннеров
     */
    public void delete(int shard, Collection<Long> bannerIds) {
        dslContextProvider.ppc(shard)
                .deleteFrom(IMAGES)
                .where(IMAGES.BID.in(bannerIds))
                .execute();
    }

    private void generateImageIds(Collection<Image> images) {
        List<Long> ids = shardHelper.generateImageIds(images.size());
        StreamEx.of(images).zipWith(ids.stream())
                .forKeyValue(Image::setId);
    }

    private void insertImagesToDb(int shard, List<Image> images) {
        if (images.isEmpty()) {
            return;
        }
        InsertHelper<ImagesRecord> insertHelper = new InsertHelper<>(dslContext(shard), IMAGES);
        for (Image image : images) {
            insertHelper.add(imageMapper, image).newRecord();
        }
        insertHelper.execute();
    }

    public Map<Long, Long> getBannerIdToImageId(int shard, Long... bannerIds) {
        return dslContextProvider.ppc(shard)
                .select(IMAGES.BID, IMAGES.IMAGE_ID)
                .from(IMAGES)
                .where(IMAGES.BID.in(bannerIds))
                .fetchMap(IMAGES.BID, IMAGES.IMAGE_ID);
    }

    private DSLContext dslContext(int shard) {
        return dslContextProvider.ppc(shard);
    }

    private JooqMapperWithSupplier<Image> createImageMapper() {
        return JooqMapperWithSupplierBuilder.builder(Image::new)
                .map(property(Image.ID, IMAGES.IMAGE_ID))
                .map(property(Image.IMAGE_HASH, IMAGES.IMAGE_HASH))
                .map(property(Image.BANNER_ID, IMAGES.BID))
                .map(property(Image.AD_GROUP_ID, IMAGES.PID))
                .map(property(Image.CAMPAIGN_ID, IMAGES.CID))
                .map(property(Image.IMAGE_TEXT, IMAGES.IMAGE_TEXT))
                .map(property(Image.DISCLAIMER_TEXT, IMAGES.DISCLAIMER_TEXT))
                .map(convertibleProperty(Image.STATUS_MODERATE, IMAGES.STATUS_MODERATE,
                        StatusImageModerate::fromSource,
                        StatusImageModerate::toSource))
                .build();
    }

}
