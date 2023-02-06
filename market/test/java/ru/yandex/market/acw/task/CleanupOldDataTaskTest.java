package ru.yandex.market.acw.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.acw.api.Image;
import ru.yandex.market.acw.api.Text;
import ru.yandex.market.acw.config.Base;
import ru.yandex.market.acw.jooq.tables.pojos.ImageCache;
import ru.yandex.market.acw.jooq.tables.pojos.TextCache;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.acw.db.utils.CacheConstants.BATCH_SIZE_FOR_CLEAN_UP;
import static ru.yandex.market.acw.db.utils.CacheConstants.CACHE_RETENTION_MONTHS;

public class CleanupOldDataTaskTest extends Base {

    private CleanupOldTextTask cleanupOldTextTask;
    private CleanupOldImagesTask cleanupOldImagesTask;

    @BeforeEach
    void setup() {
        cleanupOldImagesTask = new CleanupOldImagesTask(imageCacheDao, imageCacheDao, 1);
        cleanupOldTextTask = new CleanupOldTextTask(textCacheDao, textCacheDao, 1);
    }

    @Test
    @DisplayName("clean up images")
    void cleanupImages() {
        imageCacheDao.insert(List.of(
                new ImageCache(1L, "idx_url1", "mbo_url1", null,
                        LocalDateTime.now().minusMonths(CACHE_RETENTION_MONTHS + 1),
                        Image.ImageVerdictResult.getDefaultInstance()),
                new ImageCache(2L, "idx_url2", "mbo_url2", null,
                        LocalDateTime.now().minusMonths(CACHE_RETENTION_MONTHS - 1),
                        Image.ImageVerdictResult.getDefaultInstance())));

        var oldImages = imageCacheDao.getOldImageCache(BATCH_SIZE_FOR_CLEAN_UP, 0L);

        assertThat(oldImages.size()).isOne();
        assertThat(oldImages.stream().allMatch(image -> image.equals(1L)));

        cleanupOldImagesTask.execute(null);

        assertThat(imageCacheDao.getOldImageCache(BATCH_SIZE_FOR_CLEAN_UP, 0L)).isEmpty();
    }

    @Test
    @DisplayName("clean up text")
    void cleanupText() {
        textCacheDao.insert(List.of(
                new TextCache(1L, UUID.randomUUID(), LocalDateTime.now().minusMonths(CACHE_RETENTION_MONTHS + 1),
                        Text.TextVerdictResult.getDefaultInstance()),
                new TextCache(2L, UUID.randomUUID(), LocalDateTime.now().minusMonths(CACHE_RETENTION_MONTHS - 1),
                        Text.TextVerdictResult.getDefaultInstance())));

        var oldText = textCacheDao.getOldTextCache(BATCH_SIZE_FOR_CLEAN_UP, 0L);

        assertThat(oldText.size()).isOne();
        assertThat(oldText.stream().allMatch(text -> text.equals(1L)));

        cleanupOldTextTask.execute(null);
        assertThat(textCacheDao.getOldTextCache(BATCH_SIZE_FOR_CLEAN_UP, 0L)).isEmpty();
    }
}
