package ru.yandex.market.abc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Тест для {@link AbcClientSyncEventService}.
 * проверяет процесс выгрузки в ABC.
 */
class AbcClientSyncEventServiceTest extends FunctionalTest {

    @Autowired
    AbcClientSyncEventService abcClientSyncEventService;

    @Test
    @DisplayName("Успешная выгрузка в ABC")
    @DbUnitDataSet(
            before = "AbcClientSyncEventService.processAll.before.csv",
            after = "AbcClientSyncEventService.processAll.after.csv"
    )
    void testProcessAll() {
        abcClientSyncEventService.processAll(record -> {
        });
    }

    @Test
    @DisplayName("Неуспешная выгрузка в ABC, события остаются в бд, для следующего запуска джоба")
    @DbUnitDataSet(
            before = "AbcClientSyncEventService.processAll.before.csv",
            after = "AbcClientSyncEventService.processAllError.after.csv"
    )
    void testProcessAllError() {
        abcClientSyncEventService.processAll(record -> {
            throw new RuntimeException("test");
        });
    }
}
