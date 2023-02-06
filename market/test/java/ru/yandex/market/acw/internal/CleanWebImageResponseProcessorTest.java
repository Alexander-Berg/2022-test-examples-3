package ru.yandex.market.acw.internal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.acw.api.CheckImageParameters;
import ru.yandex.market.acw.api.CheckImagePayload;
import ru.yandex.market.acw.api.Image;
import ru.yandex.market.acw.api.RequestMode;
import ru.yandex.market.acw.config.Base;
import ru.yandex.market.acw.exceptions.CleanWebServiceException;
import ru.yandex.market.acw.jooq.enums.Status;
import ru.yandex.market.acw.jooq.tables.pojos.ImageCache;
import ru.yandex.market.acw.jooq.tables.pojos.ImageQueue;
import ru.yandex.market.acw.json.CWRawVerdict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.acw.api.Image.ImageVerdict.CLEAN_WEB_MODERATION_END;
import static ru.yandex.market.acw.api.Image.ImageVerdict.IS_GOOD;
import static ru.yandex.market.acw.api.Image.ImageVerdict.WATERMARK_AUTO_CLEAN;
import static ru.yandex.market.acw.api.Image.ImageVerdict.WATERMARK_CLEAN;

public class CleanWebImageResponseProcessorTest extends Base {

    private CleanWebImageResponseProcessor processor;

    @BeforeEach
    void setup() {
        processor = new CleanWebImageResponseProcessor(imageCacheDao, imageQueueDao);
    }

    @Test
    @DisplayName("throw error if not valid CW input")
    void throwErrorIfNotValidCWInput() {
        var map = Map.of(1L, List.of(new CWRawVerdict("1", "name", "false", null, null, null)));
        CleanWebServiceException exception = assertThrows(
                CleanWebServiceException.class,
                () -> processor.processAsyncImageResponse(map, "tableName"));
        assertThat(exception.getMessage()).contains("Incorrect verdict");
    }

    @Test
    @DisplayName("happy path when no cache exists")
    void happyPathWhenNoCacheExists() {
        imageQueueDao.insert(List.of(
                new ImageQueue(1L, "idx_url1", "mbo_url1", null, LocalDateTime.now(), Status.NEW,
                        RequestMode.DEFAULT.name(), CheckImagePayload.newBuilder().setTitle("Title1").build())));

        var map = Map.of(1L, List.of(
                new CWRawVerdict("1", "is_good", "true", null, null, null),
                new CWRawVerdict("1", "clean_web_moderation_end", "true", null, null, null)));

        processor.processAsyncImageResponse(map, "tableName");

        assertThat(imageQueueDao.existsById(1L)).isFalse();

        var cache = imageCacheDao.fetchByIdxUrl("idx_url1");
        assertThat(cache.size()).isOne();
        assertThat(cache.get(0).getData().getVerdictsList()).containsExactlyInAnyOrder(IS_GOOD, CLEAN_WEB_MODERATION_END);
    }

    @Test
    @DisplayName("happy path when cache does exist")
    void happyPathWhenCacheDoesExist() {
        imageQueueDao.insert(List.of(
                new ImageQueue(1L, "idx_url1", "mbo_url1", null, LocalDateTime.now(), Status.NEW,
                        RequestMode.DEFAULT.name(), CheckImagePayload.newBuilder().setTitle("Title1").build())));

        imageCacheDao.insert(List.of(
                new ImageCache(1L, "idx_url1", "mbo_url1", null, LocalDateTime.now(),
                        Image.ImageVerdictResult.newBuilder()
                                .addAllVerdicts(List.of(WATERMARK_CLEAN))
                                .build())));

        var map = Map.of(1L, List.of(
                new CWRawVerdict("1", "is_good", "true", null, null, null),
                new CWRawVerdict("1", "clean_web_moderation_end", "true", null, null, null)));

        var cache = imageCacheDao.fetchById(1L);
        assertThat(cache.size()).isOne();
        assertThat(cache.get(0).getData().getVerdictsList()).containsExactly(WATERMARK_CLEAN);


        processor.processAsyncImageResponse(map, "tableName");

        assertThat(imageQueueDao.existsById(1L)).isFalse();

        var updatedCache = imageCacheDao.fetchById(1L);
        assertThat(updatedCache.size()).isOne();
        assertThat(updatedCache.get(0).getIdxUrl()).isEqualTo("idx_url1");
        assertThat(updatedCache.get(0).getData().getVerdictsList()).containsExactlyInAnyOrder(WATERMARK_CLEAN, IS_GOOD, CLEAN_WEB_MODERATION_END);
    }

    @Test
    @DisplayName("happy path when 2 cache with different verdicts")
    void happyPathWhen2Cache() {
        var hash = UUID.randomUUID();
        imageQueueDao.insert(List.of(
                new ImageQueue(1L, "idx_url1", "mbo_url1", hash, LocalDateTime.now(), Status.WAITING_RESPONSE,
                        RequestMode.DEFAULT.name(), CheckImagePayload.newBuilder().setTitle("Title1").build())));

        imageCacheDao.insert(List.of(
                new ImageCache(1L, "idx_url1", "mbo_url2", null, LocalDateTime.now(),
                        Image.ImageVerdictResult.newBuilder()
                                .addAllVerdicts(List.of(WATERMARK_CLEAN))
                                .build()),
                new ImageCache(2L, "idx_url2", null, hash, LocalDateTime.now(),
                        Image.ImageVerdictResult.newBuilder()
                                .addAllVerdicts(List.of(WATERMARK_AUTO_CLEAN))
                                .build())));

        var map = Map.of(1L, List.of(
                new CWRawVerdict("1", "watermark_clean", "true", null, null, null),
                new CWRawVerdict("1", "watermark_auto_clean", "true", null, null, null),
                new CWRawVerdict("1", "is_good", "true", null, null, null),
                new CWRawVerdict("1", "clean_web_moderation_end", "true", null, null, null)));

        processor.processAsyncImageResponse(map, "tableName");

        assertThat(imageQueueDao.existsById(1L)).isFalse();

        var updatedCache = imageCacheDao.getImageVerdicts(CheckImageParameters.newBuilder()
                .setIdxUrl("idx_url1")
                .setMboUrl("mbo_url1")
                .setMboUrlHash(hash.toString().replace("-", ""))
                .setRequestMode(RequestMode.DEFAULT)
                .build());

        var allVerdicts = List.of(WATERMARK_CLEAN, WATERMARK_AUTO_CLEAN, IS_GOOD, CLEAN_WEB_MODERATION_END);

        assertThat(updatedCache.size()).isEqualTo(2);
        assertThat(updatedCache.stream().allMatch(cache -> cache.getData().getVerdictsList().containsAll(allVerdicts)));
    }
}
