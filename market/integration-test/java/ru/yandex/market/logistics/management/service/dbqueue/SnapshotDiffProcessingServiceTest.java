package ru.yandex.market.logistics.management.service.dbqueue;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.queue.model.SnapshotDiffPayload;
import ru.yandex.market.logistics.management.queue.processor.SnapshotDiffProcessingService;

@DatabaseSetup("/data/service/dbqueue/deliveryIntervalSnapshots/prepare_data.xml")
class SnapshotDiffProcessingServiceTest extends AbstractContextualTest {

    @Autowired
    private SnapshotDiffProcessingService snapshotDiffProcessingService;

    @Test
    void testNoLastSnapshotNoExcept() {
        snapshotDiffProcessingService.processPayload(new SnapshotDiffPayload("", "", 1L));
    }

    @ExpectedDatabase(
        value = "/data/service/dbqueue/deliveryIntervalSnapshots/no_diff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testNoDiff() {
        snapshotDiffProcessingService.processPayload(new SnapshotDiffPayload("", "", 2L));
    }

    @ExpectedDatabase(
        value = "/data/service/dbqueue/deliveryIntervalSnapshots/diff_location.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testHasDiffLocation() {
        snapshotDiffProcessingService.processPayload(new SnapshotDiffPayload("", "", 4L));
    }

    @ExpectedDatabase(
        value = "/data/service/dbqueue/deliveryIntervalSnapshots/diff_intervals.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testHasDiffIntervals() {
        snapshotDiffProcessingService.processPayload(new SnapshotDiffPayload("", "", 3L));

    }
}
