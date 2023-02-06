package ru.yandex.market.delivery.tracker.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.AbstractContextualTest;
import ru.yandex.market.delivery.tracker.dao.repository.DatabaseStatisticsDao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DatabaseStatisticsDaoTest extends AbstractContextualTest {

    @Autowired
    private DatabaseStatisticsDao statisticsDao;

    @Test
    void testRelationSize() {
        assertions().assertThat(statisticsDao.getRelationSizes()).isNotEmpty();
    }

    @Test
    void testDeadTupleSizes() {
        assertDoesNotThrow(() -> statisticsDao.getDeadTupleSizes());
    }

    @Test
    void testInvalidIndexesCount() {
        assertDoesNotThrow(() -> statisticsDao.getInvalidIndexesCount());
    }

    @Test
    void testAutovacuumTime() {
        assertDoesNotThrow(() -> statisticsDao.getAutovacuumTime());
    }
}
