package ru.yandex.market.acw.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.Test;

import ru.yandex.market.acw.api.Image;
import ru.yandex.market.acw.api.ImageCache;
import ru.yandex.market.acw.api.ImageCacheBatch;
import ru.yandex.market.acw.api.RequestMode;
import ru.yandex.market.acw.api.Text;
import ru.yandex.market.acw.api.TextCache;
import ru.yandex.market.acw.api.TextCacheBatch;
import ru.yandex.market.acw.config.Base;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.acw.jooq.Tables.IMAGE_CACHE;
import static ru.yandex.market.acw.jooq.Tables.TEXT_CACHE;

class AcwMigrationApiServiceImplTest extends Base {

    @Test
    void textSave() {
        AcwMigrationApiServiceImpl acwMigrationApiService = new AcwMigrationApiServiceImpl(configuration);

        TextCache text1 = TextCache.newBuilder()
                .setText("title = Мобильная баскетбольная стойка DFC 80х58см п/э KIDSD2\n")
                .setVerdictResult(Text.TextVerdictResult.newBuilder()
                        .addVerdicts(Text.TextVerdict.TEXT_TOLOKA_CLEAN_TEXT).build())
                .setCreationTime(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .setRequestMode(RequestMode.DEFAULT)
                .build();

        TextCache text2 = TextCache.newBuilder()
                .setText("title = Мобильная баскетбольная стойка DFC 80х58см")
                .setVerdictResult(Text.TextVerdictResult.newBuilder()
                        .addVerdicts(Text.TextVerdict.TEXT_TOLOKA_CLEAN_TEXT)
                        .addVerdicts(Text.TextVerdict.TEXT_AUTO_GOOD)
                        .build())
                .setCreationTime(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .setRequestMode(RequestMode.DEFAULT)
                .build();

        TextCacheBatch batch = TextCacheBatch.newBuilder()
                .addResults(text1)
                .addResults(text2)
                .build();
        acwMigrationApiService.saveTextCacheResult(batch);

        Result<Record> fetch = configuration.dsl().select(TEXT_CACHE.asterisk()).from(TEXT_CACHE)
                .fetch();
        assertThat(fetch).hasSize(2);

        acwMigrationApiService.saveTextCacheResult(batch);
        Result<Record> fetchAfter = configuration.dsl().select(TEXT_CACHE.asterisk()).from(TEXT_CACHE)
                .fetch();
        assertThat(fetchAfter).hasSize(2);
    }

    @Test
    void imageSave() {
        AcwMigrationApiServiceImpl acwMigrationApiService = new AcwMigrationApiServiceImpl(configuration);

        ImageCache image1 = ImageCache.newBuilder()
                .setIdxUrl("url1")
                .setCacheDate(100L)
                .setVerdictResult(Image.ImageVerdictResult.newBuilder()
                        .addVerdicts(Image.ImageVerdict.CP_YANG_404)
                        .addVerdicts(Image.ImageVerdict.FASHION_BACKGROUND_BACKGROUND)
                        .build())
                .setRequestMode(RequestMode.DEFAULT)
                .build();
        ImageCache image2 = ImageCache.newBuilder()
                .setIdxUrl("url2")
                .setCacheDate(100L)
                .setVerdictResult(Image.ImageVerdictResult.newBuilder()
                        .addVerdicts(Image.ImageVerdict.CP_YANG_404)
                        .addVerdicts(Image.ImageVerdict.ACCESSORIES_ORIENTATION_GOOD)
                        .build())
                .setRequestMode(RequestMode.DEFAULT)
                .build();

        ImageCacheBatch batch = ImageCacheBatch.newBuilder()
                .addResults(image1)
                .addResults(image2)
                .build();
        acwMigrationApiService.saveImageCacheResult(batch);
        Result<Record> fetch = configuration.dsl().select(IMAGE_CACHE.asterisk()).from(IMAGE_CACHE)
                .fetch();
        assertThat(fetch).hasSize(2);

        acwMigrationApiService.saveImageCacheResult(batch);
        Result<Record> fetchAfter = configuration.dsl().select(IMAGE_CACHE.asterisk()).from(IMAGE_CACHE)
                .fetch();
        assertThat(fetchAfter).hasSize(2);
    }
}
