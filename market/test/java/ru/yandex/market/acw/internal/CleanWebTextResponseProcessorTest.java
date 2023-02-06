package ru.yandex.market.acw.internal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.acw.api.RequestMode;
import ru.yandex.market.acw.api.Text;
import ru.yandex.market.acw.config.Base;
import ru.yandex.market.acw.exceptions.CleanWebServiceException;
import ru.yandex.market.acw.jooq.enums.Status;
import ru.yandex.market.acw.jooq.tables.pojos.TextCache;
import ru.yandex.market.acw.jooq.tables.pojos.TextQueue;
import ru.yandex.market.acw.json.CWRawVerdict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.acw.api.Text.TextVerdict.CLEAN_WEB_MODERATION_END;
import static ru.yandex.market.acw.api.Text.TextVerdict.TEXT_AUTO_GOOD;
import static ru.yandex.market.acw.api.Text.TextVerdict.TEXT_TOLOKA_CLEAN_TEXT;

public class CleanWebTextResponseProcessorTest extends Base {

    private CleanWebTextResponseProcessor processor;

    @BeforeEach
    void setup() {
        processor = new CleanWebTextResponseProcessor(textCacheDao, textQueueDao);
    }

    @Test
    @DisplayName("throw error if not valid CW input")
    void throwErrorIfNotValidCWInput() {
        var map = Map.of(1L, List.of(new CWRawVerdict("1", "name", "false", null, null, null)));
        CleanWebServiceException exception = assertThrows(
                CleanWebServiceException.class,
                () -> processor.processAsyncTextResponse(map, "tableName"));
        assertThat(exception.getMessage()).contains("Incorrect verdict");
    }

    @Test
    @DisplayName("happy path when no cache exists")
    void happyPathWhenNoCacheExists() {
        var uuid = UUID.randomUUID();
        textQueueDao.insert(List.of(
                new TextQueue(1L, uuid,  LocalDateTime.now(), Status.NEW,
                        RequestMode.DEFAULT.name(), "text1")));

        var map = Map.of(1L, List.of(
                new CWRawVerdict("1", "text_auto_good", "true", null, null, null),
                new CWRawVerdict("1", "clean_web_moderation_end", "true", null, null, null)));

        processor.processAsyncTextResponse(map, "tableName");

        assertThat(textQueueDao.existsById(1L)).isFalse();

        var cache = textCacheDao.fetchByHash(uuid);
        assertThat(cache.size()).isOne();
        assertThat(cache.get(0).getData().getVerdictsList()).containsExactlyInAnyOrder(TEXT_AUTO_GOOD, CLEAN_WEB_MODERATION_END);
    }

    @Test
    @DisplayName("happy path when cache does exist")
    void happyPathWhenCacheDoesExist() {
        var uuid = UUID.randomUUID();
        textQueueDao.insert(List.of(
                new TextQueue(1L, uuid,  LocalDateTime.now(), Status.NEW,
                        RequestMode.DEFAULT.name(), "text1")));

        textCacheDao.insert(List.of(
                new TextCache(1L, uuid, LocalDateTime.now(),
                        Text.TextVerdictResult.newBuilder()
                                .addAllVerdicts(List.of(TEXT_TOLOKA_CLEAN_TEXT))
                                .build())));

        var map = Map.of(1L, List.of(
                new CWRawVerdict("1", "text_auto_good", "true", null, null, null),
                new CWRawVerdict("1", "clean_web_moderation_end", "true", null, null, null)));

        var cache = textCacheDao.fetchByHash(uuid);
        assertThat(cache.size()).isOne();
        assertThat(cache.get(0).getData().getVerdictsList()).containsExactlyInAnyOrder(TEXT_TOLOKA_CLEAN_TEXT);

        processor.processAsyncTextResponse(map, "tableName");

        assertThat(textQueueDao.existsById(1L)).isFalse();

        var updatedCache = textCacheDao.fetchByHash(uuid);
        assertThat(updatedCache.size()).isOne();
        assertThat(updatedCache.get(0).getHash()).isEqualTo(uuid);
        assertThat(updatedCache.get(0).getData().getVerdictsList()).containsExactlyInAnyOrder(TEXT_TOLOKA_CLEAN_TEXT, TEXT_AUTO_GOOD, CLEAN_WEB_MODERATION_END);
    }
}
