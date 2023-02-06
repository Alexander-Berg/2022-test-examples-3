package ru.yandex.market.pricelabs.tms.processing.offers;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LargeOffersProcessorTest extends AbstractOffersProcessorTest {

    @Test
    void testFullAndLargeTasks() {
        var blueShop = initShop(ApiConst.VIRTUAL_SHOP_BLUE, Utils.emptyConsumer());
        saveAsVirtualShop(blueShop);

        executors.modelBids().clearTargetTable();
        TimingUtils.setTime(Utils.parseDateTimeAsInstant("2019-12-01T01:02:03"));
        executor.insert(TmsTestUtils.map(readSourceList(), executor::copySource, offer -> {
            offer.setModel_id(1);
            offer.setModel_published_on_market(true);
        }), List.of());

        doFullJob();

        var task = startScheduledTask(JobType.SHOP_LOOP_FULL);
        var blueTask = startScheduledTask(JobType.SHOP_LOOP_FULL_LARGE);
        controller.processTask(blueTask);
        controller.processTask(task);
        checkNoScheduledTasks();

        assertEquals(1, testControls.getShopTasks(SHOP_ID).size());
        assertEquals(1, testControls.getShopTasks(ApiConst.VIRTUAL_SHOP_BLUE).size());

        testControls.taskMonitoringJob();
        checkNoScheduledTasks();

        testControls.taskMonitoringJob();
        checkNoScheduledTasks();
    }

}
