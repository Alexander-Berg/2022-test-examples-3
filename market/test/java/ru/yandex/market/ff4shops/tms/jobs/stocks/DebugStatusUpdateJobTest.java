package ru.yandex.market.ff4shops.tms.jobs.stocks;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.json.AbstractJsonControllerFunctionalTest;
import ru.yandex.market.ff4shops.delivery.stocks.StocksRequestStatusService;

public class DebugStatusUpdateJobTest extends AbstractJsonControllerFunctionalTest {

    private final long supplierId = 1;
    private final long deliveryServiceId = 11;

    @Autowired
    private StocksRequestStatusService stocksRequestStatusService;

    @Autowired
    private DebugStatusUpdateJob debugStatusUpdateJob;

    @Test
    @DbUnitDataSet(before = "DebugStatusUpdateJob.before.csv", after = "DebugStatusUpdateJob.after.csv")
    void testDontUpdateDebugStatus() {
        stocksRequestStatusService.updateTimeOfRequestStocks(Set.of(supplierId));
        mockSearchPartners(deliveryServiceId, false);
        debugStatusUpdateJob.doJob(null);
    }

    @Test
    @DbUnitDataSet(before = "DebugStatusUpdateJob.null.before.csv", after = "DebugStatusUpdateJob.null.after.csv")
    void testDontUpdateDebugStatusFromNull() {
        stocksRequestStatusService.updateTimeOfRequestStocks(Set.of(supplierId));
        mockSearchPartners(deliveryServiceId, true);
        debugStatusUpdateJob.doJob(null);
    }

    @Test
    @DbUnitDataSet(before = "DebugStatusUpdateJob.lms.before.csv", after = "DebugStatusUpdateJob.lms.after.csv")
    void testUpdateDebugStatusBecauseLmsSyncEnabled() {
        stocksRequestStatusService.updateTimeOfRequestStocks(Set.of(supplierId));
        mockSearchPartners(deliveryServiceId, true);
        debugStatusUpdateJob.doJob(null);
    }

    @Test
    @DbUnitDataSet(before = "DebugStatusUpdateJob.stocks.before.csv", after = "DebugStatusUpdateJob.stocks.after.csv")
    void testUpdateDebugStatusBecauseStocksOutdated() {
        mockSearchPartners(deliveryServiceId, false);
        debugStatusUpdateJob.doJob(null);
    }
}
