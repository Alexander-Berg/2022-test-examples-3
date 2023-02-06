package ru.yandex.market.delivery.tracker.dao;

import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.delivery.tracker.AbstractContextualTest;
import ru.yandex.market.delivery.tracker.dao.repository.BatchDao;
import ru.yandex.market.delivery.tracker.dao.repository.DatabaseStatisticsDao.NamedValue;
import ru.yandex.market.delivery.tracker.domain.entity.UnprocessedBatchesCount;

public class BatchDaoTest extends AbstractContextualTest {

    @Autowired
    private BatchDao batchDao;

    @Test
    @DatabaseSetup("/database/states/batches/dao/unprocessed_batches.xml")
    public void testUnprocessedBatchesStatistics() {
        List<UnprocessedBatchesCount> actual = batchDao.getUnprocessedBatchesStatistics();
        // (requestType, priority) -> batchesCount
        Map<Pair<Integer, Integer>, Long> expectedCounts = Map.of(
            new Pair<>(0, 0), 4L,
            new Pair<>(1, 0), 1L,
            new Pair<>(3, 0), 2L,
            new Pair<>(0, 10), 2L,
            new Pair<>(1, 10), 1L
        );
        Assertions.assertEquals(expectedCounts.size(), actual.size());
        for (UnprocessedBatchesCount count : actual) {
            Pair<Integer, Integer> key = new Pair<>(count.getRequestType(), count.getPriority());
            Assertions.assertTrue(expectedCounts.containsKey(key));
            Assertions.assertEquals(expectedCounts.get(key), count.getBatchesCount());
        }
    }

    @Test
    @DatabaseSetup("/database/states/batches/dao/unprocessed_batches.xml")
    public void testServicesWithUnprocessedBatchesStatistics() {
        assertions().assertThatThrownBy(() -> batchDao.getServicesWithUnprocessedBatchesStatistics(false, false))
            .as("Asserting that thrown exception is valid")
            .hasRootCause(new IllegalArgumentException("'Group by' should have at least one parameter"));

        List<NamedValue> groupedByServiceAndRequestType =
            batchDao.getServicesWithUnprocessedBatchesStatistics(true, true);

        Assertions.assertTrue(checkArrayEquality(
            List.of(
                new NamedValue("5", 2L),
                new NamedValue("1", 5L)
            ),
            groupedByServiceAndRequestType
        ), "Not expected groupedByServiceAndRequestType array");

        List<NamedValue> groupedByService =
            batchDao.getServicesWithUnprocessedBatchesStatistics(true, false);

        Assertions.assertTrue(checkArrayEquality(
            List.of(
                new NamedValue("5", 3L)
            ),
            groupedByService
        ), "Not expected groupedByService array");

        List<NamedValue> groupedByRequestType =
            batchDao.getServicesWithUnprocessedBatchesStatistics(false, true);

        Assertions.assertTrue(checkArrayEquality(
            List.of(
                new NamedValue("10", 1L),
                new NamedValue("5", 2L)
            ),
            groupedByRequestType
        ), "Not expected groupedByRequestType array");
    }

    private static <T> boolean checkArrayEquality(List<T> expected, List<T> actual) {
        if (expected.size() != actual.size()) {
            return false;
        }
        for (T element : actual) {
            if (expected.stream().noneMatch(el -> el.equals(element))) {
                return false;
            }
        }
        return true;
    }
}
