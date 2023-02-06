package ru.yandex.market.wms.autostart.modules.autostartlogic.service.util.dispenser;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.autostartlogic.orderbatching.VanillaOrderBatchingLogic;
import ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching.RandomOrdersGenerator;
import ru.yandex.market.wms.autostart.util.dispenser.Dispenser;
import ru.yandex.market.wms.autostart.util.dispenser.SingleAccumulatedLimitChecker;
import ru.yandex.market.wms.autostart.util.dispenser.SubSequencesDispenser;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;
import ru.yandex.market.wms.common.spring.dao.entity.SubBatch;
import ru.yandex.market.wms.common.utils.IteratorUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubSequenceDispenserTest {
    @Test
    void test() {
        final int minOrdersPerPutWall = 1;
        final int maxOrdersPerPutWall = 10;
        final int maxItemsPerPutwall = 90;

        final HashMap<String, Integer> candidateStations = new HashMap<>();
        candidateStations.put("STATION_1", 1000);
        candidateStations.put("STATION_2", 1000);
        candidateStations.put("STATION_3", 1000);

        VanillaOrderBatchingLogic batchingLogic =
                new VanillaOrderBatchingLogic(minOrdersPerPutWall, maxOrdersPerPutWall, candidateStations);

        RandomOrdersGenerator generator = new RandomOrdersGenerator();

        List<OrderWithDetails> orders = generator.genOwd(30, 3, 3, 10, LocalDateTime.now());
        Dispenser<OrderWithDetails> ordersDispenser =
                new SubSequencesDispenser<>(orders,
                        () -> new SingleAccumulatedLimitChecker<>(OrderWithDetails::itemCount, maxItemsPerPutwall)
                );

        List<SubBatch<OrderWithDetails>> subBatches =
                IteratorUtil.takeWhileNonNull(() -> batchingLogic.nextSubBatch(ordersDispenser));
        assertEquals(3, subBatches.size());
        subBatches.forEach(subBatch -> assertEquals(10, subBatch.getOrders().size()));
    }
}
