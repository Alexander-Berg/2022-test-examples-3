package ru.yandex.market.acw;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.acw.api.RequestMode;
import ru.yandex.market.acw.api.Text;
import ru.yandex.market.acw.api.TextCacheBatch;
import ru.yandex.market.acw.config.Base;
import ru.yandex.market.acw.jooq.enums.Status;
import ru.yandex.market.acw.jooq.tables.pojos.ImageQueue;
import ru.yandex.market.acw.jooq.tables.pojos.TextCache;
import ru.yandex.market.acw.service.AcwMigrationApiServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AcwTest extends Base {

    @Test
    void test() {
        //Just a test example
        assertTrue(true);
    }

    @Test
    void test1() {
        imageQueueDao.insert(new ImageQueue(null, "url1", "url2", UUID.nameUUIDFromBytes("hash1".getBytes()), LocalDateTime.now(),
                Status.NEW, "default", null));
    }

    @Test
    void test2() {
        textCacheDao.insert(new TextCache(
                1L,
                UUID.nameUUIDFromBytes("hash1".getBytes()),
                LocalDateTime.now(),
                Text.TextVerdictResult.newBuilder().addVerdicts(Text.TextVerdict.CLEAN_WEB_MODERATION_END).build()
        ));

        assertThat(textCacheDao.findAll()).hasSize(1);
    }

    @Test
    void test3() {
        AcwMigrationApiServiceImpl acwMigrationApiService = new AcwMigrationApiServiceImpl(configuration);
        acwMigrationApiService.saveTextCacheResult(
                TextCacheBatch.newBuilder()
                        .addResults(ru.yandex.market.acw.api.TextCache.newBuilder()
                                .setCreationTime(Timestamp.valueOf(LocalDateTime.now()).getTime())
                                .setRequestMode(RequestMode.DEFAULT)
                                .setText("Sample Text")
                                .setVerdictResult(Text.TextVerdictResult.newBuilder().addVerdicts(Text.TextVerdict.TEXT_TOLOKA_ADVERT)))
                        .build()
        );

        acwMigrationApiService.saveTextCacheResult(
                TextCacheBatch.newBuilder()
                        .addResults(ru.yandex.market.acw.api.TextCache.newBuilder()
                                .setCreationTime(Timestamp.valueOf(LocalDateTime.now()).getTime())
                                .setRequestMode(RequestMode.DEFAULT)
                                .setText("Sample Text")
                                .setVerdictResult(Text.TextVerdictResult.newBuilder().addVerdicts(Text.TextVerdict.TEXT_TOLOKA_ADVERT)))
                        .build()
        );
    }
}

