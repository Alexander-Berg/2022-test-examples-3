package ru.yandex.market.core.fulfillment.report;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsReportQueueServiceTest extends FunctionalTest {

    @Autowired
    private StatisticReportQueueService monthlyStatisticReportQueueService;

    @Test
    @DbUnitDataSet(before = "StatisticsReportQueueServiceTest.adding.before.csv",
            after = "StatisticsReportQueueServiceTest.adding.after.csv")
    void testAdding() {
        monthlyStatisticReportQueueService.addToQueue(List.of(42342L, 11323L, 145245L, 4564574L));
    }

    @Test
    @DbUnitDataSet(before = "StatisticsReportQueueServiceTest.remove.before.csv",
            after = "StatisticsReportQueueServiceTest.remove.after.csv")
    void testRemoving() {
        monthlyStatisticReportQueueService.removePartnerFromExcelQueue(List.of(145245L, 4564574L));
    }

    @Test
    @DbUnitDataSet(before = "StatisticsReportQueueServiceTest.adding.before.csv")
    void testGetFromNewTable() {
        var expected = List.of(33L, 44L);
        assertThat(expected).hasSameElementsAs(monthlyStatisticReportQueueService.getSuppliersForOEBS());
    }
}
