package ru.yandex.market.delivery.tracker;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.delivery.tracker.dao.repository.DeliveryTrackDao;
import ru.yandex.market.delivery.tracker.domain.entity.AssignedTrackingBatches;
import ru.yandex.market.delivery.tracker.service.tracking.batching.supplier.AvailabilityCheckingBatchSupplierService;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties =
    {"delivery.track.feature.assigned-tracking-batches.read-from-new=true",
    "delivery.track.feature.assigned-tracking-batches.read-from-old=false"}
)
public class AvailabilityCheckingBatchSupplierServiceWithAssignedBatchesIntegrationTest extends AbstractContextualTest {

    @Autowired
    @Qualifier("availabilityCheckingBatchSupplierService.ORDER")
    private AvailabilityCheckingBatchSupplierService orderAvailabilityCheckingBatchSupplierService;

    @Autowired
    private DeliveryTrackDao deliveryTrackDao;

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/with_assigned_tracking_batches/" +
            "before_supply_checker_without_method_batches_tracks_already_in_batch.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/with_assigned_tracking_batches/" +
                "after_supply_checker_without_method_batches_tracks_already_in_batch.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithoutMethodBatchesTracksAlreadyInBatch() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup("/database/states/batches/supply/checker/with_assigned_tracking_batches/" +
        "before_supply_checker_batches_mixed.xml")
    @ExpectedDatabase(
        value = "/database/expected/batches/supply/checker/with_assigned_tracking_batches/" +
            "after_supply_checker_batches_mixed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerBatchesMixed() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();

        List<AssignedTrackingBatches> assignedTrackingBatchesToTracks =
            deliveryTrackDao.getAssignedTrackingBatchesToTracks();

        assertThat(assignedTrackingBatchesToTracks.size()).isEqualTo(7);

        Map<Long, List<AssignedTrackingBatches>> mapOfAssignedTrackingBatches = assignedTrackingBatchesToTracks.stream()
            .collect(Collectors.groupingBy(AssignedTrackingBatches::getBatchId));
        assertThat(mapOfAssignedTrackingBatches.size()).isEqualTo(5);
    }
}
