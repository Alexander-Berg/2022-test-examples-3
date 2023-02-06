package ru.yandex.market.core.abc;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.abc.model.AbcClientSyncEvent;

import static ru.yandex.market.core.abc.AbcClientSyncDao.MAX_LIMIT;

/**
 * Тест работы с бд.
 *
 * @see ru.yandex.market.core.abc.AbcClientSyncDao
 */
class AbcClientSyncDaoTest extends FunctionalTest {
    @Autowired
    private AbcClientSyncDao abcClientSyncDao;

    @Test
    @DisplayName("Добавление новых событий для выгрузки в ABC")
    @DbUnitDataSet(
            before = "csv/AbcClientSyncDao.insert.before.csv",
            after = "csv/AbcClientSyncDao.insert.after.csv"
    )
    void testAddEvent() {
        abcClientSyncDao.addEvent(2);
        abcClientSyncDao.addEvent(3);
    }

    @Test
    @DisplayName("Удаление событий для выгрузки в ABC")
    @DbUnitDataSet(
            before = "csv/AbcClientSyncDao.remove.before.csv",
            after = "csv/AbcClientSyncDao.remove.after.csv"
    )
    void testRemoveEvent() {
        abcClientSyncDao.removeEvent(1);
    }

    @Test
    @DisplayName("Удаление событий для выгрузки в ABC")
    @DbUnitDataSet(
            before = "csv/AbcClientSyncDao.remove.before.csv",
            after = "csv/AbcClientSyncDao.removeAll.after.csv"
    )
    void testRemoveAllEvents() {
        abcClientSyncDao.removeAllEvents();
    }

    @Test
    @DisplayName("Получение событий для выгрузки в ABC")
    @DbUnitDataSet(
            before = "csv/AbcClientSyncDao.get.before.csv"
    )
    void testGetEvents() {
        List<AbcClientSyncEvent> abcClientSyncEvents = abcClientSyncDao.getEvents(MAX_LIMIT);
        Assertions.assertEquals(3, abcClientSyncEvents.size());

        abcClientSyncEvents = abcClientSyncDao.getEvents(1);
        Assertions.assertEquals(1, abcClientSyncEvents.size());
        Assertions.assertEquals(-1, abcClientSyncEvents.get(0).getId());
    }
}
