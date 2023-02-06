package ru.yandex.direct.core.testing.repository;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesFormatsImageType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES_FORMATS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES_POOL;
import static ru.yandex.direct.dbschema.ppc.enums.BannerImagesFormatsAvatarsHost.avatars_mds_yandex_net;

/**
 * Работа с описаниями размеров баннерных картинок в аватарнице
 */
public class TestBannerImageFormatRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    /**
     * Добавляет новый типоразмер картинки для аватарницы
     *
     * @param shard Шард
     * @param hash  Хеш картинки
     * @param type  Тип картинки
     */
    public void create(int shard, String hash, BannerImagesFormatsImageType type) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNER_IMAGES_FORMATS)
                .set(BANNER_IMAGES_FORMATS.IMAGE_HASH, hash)
                .set(BANNER_IMAGES_FORMATS.MDS_GROUP_ID, 4699L)
                .set(BANNER_IMAGES_FORMATS.IMAGE_TYPE, type)
                .set(BANNER_IMAGES_FORMATS.FORMATS, "{}")
                .set(BANNER_IMAGES_FORMATS.AVATARS_HOST, avatars_mds_yandex_net)
                .execute();
    }

    public int updateSize(int shard, String hash, ImageSize size) {
        return dslContextProvider.ppc(shard)
                .update(BANNER_IMAGES_FORMATS)
                .set(BANNER_IMAGES_FORMATS.WIDTH, size.getWidth().longValue())
                .set(BANNER_IMAGES_FORMATS.HEIGHT, size.getHeight().longValue())
                .where(BANNER_IMAGES_FORMATS.IMAGE_HASH.eq(hash))
                .execute();
    }

    public int updateCreateTime(int shard, String hash, LocalDateTime dateTime) {
        return dslContextProvider.ppc(shard)
                .update(BANNER_IMAGES_POOL)
                .set(BANNER_IMAGES_POOL.CREATE_TIME, dateTime)
                .where(BANNER_IMAGES_POOL.IMAGE_HASH.eq(hash))
                .execute();
    }

    public int updateName(int shard, String hash, String name) {
        return dslContextProvider.ppc(shard)
                .update(BANNER_IMAGES_POOL)
                .set(BANNER_IMAGES_POOL.NAME, name)
                .where(BANNER_IMAGES_POOL.IMAGE_HASH.eq(hash))
                .execute();
    }
}
