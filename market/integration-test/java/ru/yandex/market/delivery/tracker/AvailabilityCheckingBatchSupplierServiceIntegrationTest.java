package ru.yandex.market.delivery.tracker;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.delivery.tracker.dao.repository.DeliveryTrackDao;
import ru.yandex.market.delivery.tracker.domain.entity.AssignedTrackingBatches;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.service.tracking.batching.supplier.AvailabilityCheckingBatchSupplierService;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityCheckingBatchSupplierServiceIntegrationTest extends AbstractContextualTest {

    @Autowired
    @Qualifier("availabilityCheckingBatchSupplierService.ORDER")
    private AvailabilityCheckingBatchSupplierService orderAvailabilityCheckingBatchSupplierService;

    @Autowired
    @Qualifier("availabilityCheckingBatchSupplierService.MOVEMENT")
    private AvailabilityCheckingBatchSupplierService movementAvailabilityCheckingBatchSupplierService;

    @Autowired
    private DeliveryTrackDao deliveryTrackDao;

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_supply_checker_batches_with_method_batches.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/after_supply_checker_batches_with_method_batches.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithMethodBatches() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_check_that_track_with_next_request_ts_gt_now_not_in_batch.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_check_that_track_with_next_request_ts_gt_now_not_in_batch.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void checkThatTrackWithNextRequestTSGreaterThanNowNotInBatch() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_supply_checker_batches_without_method_batches.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/after_supply_checker_batches_without_method_batches.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithoutMethodBatches() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/"
            + "before_supply_checker_batches_without_method_batches_disabled_pull_by_get_history.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/"
                + "after_supply_checker_batches_without_method_batches_disabled_pull_by_get_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithoutMethodBatchesAndDisabledPullByGetHistory() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/"
            + "before_supply_checker_batches_without_method_batches_enabled_pull_by_get_history.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/"
                + "after_supply_checker_batches_without_method_batches_enabled_pull_by_get_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithoutMethodBatchesAndEnabledPullByGetHistory() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/" +
            "before_supply_checker_with_method_batches_more_tracks_than_batch_limit.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_with_method_batches_more_tracks_than_batch_limit.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithMethodBatchesMoreTracksThanBatchLimit() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();

        List<AssignedTrackingBatches> assignedTrackingBatchesToTracks =
            deliveryTrackDao.getAssignedTrackingBatchesToTracks();
        assertThat(assignedTrackingBatchesToTracks).hasSize(4);

        Map<Long, List<AssignedTrackingBatches>> mapOfAssignedTrackingBatches = assignedTrackingBatchesToTracks.stream()
            .collect(Collectors.groupingBy(AssignedTrackingBatches::getBatchId));
        assertThat(mapOfAssignedTrackingBatches).hasSize(2);

        assertThat(mapOfAssignedTrackingBatches.entrySet().stream()
            .filter(e -> e.getValue().size() == 1).findAny()).isNotEmpty();
        assertThat(mapOfAssignedTrackingBatches.entrySet().stream()
            .filter(e -> e.getValue().size() == 3).findAny()).isNotEmpty();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/" +
            "before_supply_checker_without_method_batches_more_tracks_than_batch_limit.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_without_method_batches_more_tracks_than_batch_limit.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithoutMethodBatchesMoreTracksThanBatchLimit() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();

        List<AssignedTrackingBatches> assignedTrackingBatchesToTracks =
            deliveryTrackDao.getAssignedTrackingBatchesToTracks();
        assertThat(assignedTrackingBatchesToTracks).hasSize(4);

        Map<Long, List<AssignedTrackingBatches>> mapOfAssignedTrackingBatches = assignedTrackingBatchesToTracks.stream()
            .collect(Collectors.groupingBy(AssignedTrackingBatches::getBatchId));
        assertThat(mapOfAssignedTrackingBatches).hasSize(2);

        assertThat(mapOfAssignedTrackingBatches.entrySet()
            .stream().filter(e -> e.getValue().size() == 1).findAny()).isNotEmpty();
        assertThat(mapOfAssignedTrackingBatches.entrySet()
            .stream().filter(e -> e.getValue().size() == 3).findAny()).isNotEmpty();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/" +
            "before_supply_checker_with_method_batches_tracks_in_incorrect_state.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_with_method_batches_tracks_in_incorrect_state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithMethodBatchesTracksInIncorrectState() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/" +
            "before_supply_checker_without_method_batches_tracks_in_incorrect_state.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_without_method_batches_tracks_in_incorrect_state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithoutMethodBatchesTracksInIncorrectState() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/" +
            "before_supply_checker_with_method_batches_tracks_with_incorrect_status.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_with_method_batches_tracks_with_incorrect_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithMethodBatchesTracksWithIncorrectStatus() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/" +
            "before_supply_checker_without_method_batches_tracks_with_incorrect_status.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_without_method_batches_tracks_with_incorrect_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithoutMethodBatchesTracksWithIncorrectStatus() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_supply_checker_with_method_batches_tracks_already_in_batch.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_with_method_batches_tracks_already_in_batch.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithMethodBatchesTracksAlreadyInBatch() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/" +
            "before_supply_checker_without_method_batches_tracks_already_in_batch.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_without_method_batches_tracks_already_in_batch.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithoutMethodBatchesTracksAlreadyInBatch() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_supply_checker_batches_with_method_batches_no_tracks.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/after_supply_checker_batches_with_method_batches_no_tracks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithMethodBatchesNoTracks() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_supply_checker_batches_without_method_batches_no_tracks.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_batches_without_method_batches_no_tracks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithoutMethodBatchesNoTracks() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_supply_checker_with_method_batches_different_ds.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/after_supply_checker_with_method_batches_different_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithMethodBatchesDifferentDs() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();

        List<AssignedTrackingBatches> assignedTrackingBatchesToTracks =
            deliveryTrackDao.getAssignedTrackingBatchesToTracks();
        assertThat(assignedTrackingBatchesToTracks).hasSize(2);
        assertThat(assignedTrackingBatchesToTracks.get(0).getBatchId())
            .isNotEqualTo(assignedTrackingBatchesToTracks.get(1).getBatchId());
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_supply_checker_without_method_batches_different_ds.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/after_supply_checker_without_method_batches_different_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithoutMethodBatchesDifferentDs() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();

        List<AssignedTrackingBatches> assignedTrackingBatchesToTracks =
            deliveryTrackDao.getAssignedTrackingBatchesToTracks();
        assertThat(assignedTrackingBatchesToTracks).hasSize(2);
        assertThat(assignedTrackingBatchesToTracks.get(0).getBatchId())
            .isNotEqualTo(assignedTrackingBatchesToTracks.get(1).getBatchId());
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_supply_checker_mixed_method_batches_different_ds.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/after_supply_checker_mixed_method_batches_different_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerMixedMethodBatchesDifferentDs() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();

        List<AssignedTrackingBatches> assignedTrackingBatchesToTracks =
            deliveryTrackDao.getAssignedTrackingBatchesToTracks();
        assertThat(assignedTrackingBatchesToTracks).hasSize(2);
        assertThat(assignedTrackingBatchesToTracks.get(0).getBatchId())
            .isNotEqualTo(assignedTrackingBatchesToTracks.get(1).getBatchId());
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_supply_checker_different_method_batches_different_ds.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/after_supply_checker_different_method_batches_different_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerDifferentMethodBatchesDifferentDs() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();

        List<AssignedTrackingBatches> assignedTrackingBatchesToTracks =
            deliveryTrackDao.getAssignedTrackingBatchesToTracks();
        assertThat(assignedTrackingBatchesToTracks).hasSize(2);
        assertThat(assignedTrackingBatchesToTracks.get(0).getBatchId())
            .isNotEqualTo(assignedTrackingBatchesToTracks.get(1).getBatchId());
    }

    @Test
    @DatabaseSetup("/database/states/batches/supply/checker/before_supply_checker_batches_mixed.xml")
    @ExpectedDatabase(
        value = "/database/expected/batches/supply/checker/after_supply_checker_batches_mixed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerBatchesMixed() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();

        List<AssignedTrackingBatches> assignedTrackingBatchesToTracks =
            deliveryTrackDao.getAssignedTrackingBatchesToTracks();
        assertThat(assignedTrackingBatchesToTracks).hasSize(4);

        Map<Long, List<AssignedTrackingBatches>> mapOfAssignedTrackingBatches =
            assignedTrackingBatchesToTracks.stream()
                .collect(Collectors.groupingBy(AssignedTrackingBatches::getBatchId));

        assertThat(mapOfAssignedTrackingBatches).hasSize(2);
        assertThat(mapOfAssignedTrackingBatches).containsKey(1L);
        assertThat(mapOfAssignedTrackingBatches.get(1L)).hasSize(2);
        assertThat(mapOfAssignedTrackingBatches).containsKey(2L);
        assertThat(mapOfAssignedTrackingBatches.get(2L)).hasSize(2);
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_supply_checker_batches_with_method_batches_for_movement.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_batches_with_method_batches_for_movement.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithMethodBatchesForMovement() {
        movementAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/" +
            "before_supply_checker_batches_with_method_batches_only_for_movements.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_batches_with_method_batches_only_for_movements.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithMethodBatchesOnlyForMovements() {
        movementAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_supply_checker_batches_with_method_batches_only_for_orders.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_batches_with_method_batches_only_for_orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithMethodBatchesOnlyForOrders() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
    }

    @Test
    @DatabaseSetup("/database/states/batches/supply/checker/before_supply_checker_batches_with_multiple_api.xml")
    void testSupplyCheckerWithMultiplePartnerApi() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();
        // Батчи могут создаться в произвольном порядке, ссылки на них не получится проверить через @ExpectedDatabase
        assertThat(deliveryTrackDao.getBatchedTracks()).containsExactly(
            Pair.of(1L, ApiVersion.DS),
            Pair.of(2L, ApiVersion.DS),
            Pair.of(3L, ApiVersion.FF)
        );
    }

    @Test
    @DatabaseSetup(
        "/database/states/batches/supply/checker/before_supply_checker_batches_without_method_multiple_api.xml"
    )
    @ExpectedDatabase(
        value =
            "/database/expected/batches/supply/checker/" +
                "after_supply_checker_batches_without_method_multiple_api.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSupplyCheckerWithoutMethodsMultiplePartnerApi() {
        orderAvailabilityCheckingBatchSupplierService.supplyBatches();

        assertThat(deliveryTrackDao.getBatchedTracks()).containsExactly(
            Pair.of(1L, ApiVersion.DS),
            Pair.of(2L, ApiVersion.DS),
            Pair.of(3L, ApiVersion.FF)
        );

        List<AssignedTrackingBatches> assignedTrackingBatchesToTracks =
            deliveryTrackDao.getAssignedTrackingBatchesToTracks();
        assertThat(assignedTrackingBatchesToTracks).hasSize(3);

        Map<Long, List<AssignedTrackingBatches>> mapOfAssignedTrackingBatches =
            assignedTrackingBatchesToTracks.stream()
                .collect(Collectors.groupingBy(AssignedTrackingBatches::getBatchId));
        assertThat(mapOfAssignedTrackingBatches).hasSize(2);

        assertThat(mapOfAssignedTrackingBatches.entrySet().stream()
            .filter(e -> e.getValue().size() == 1).findAny()).isNotEmpty();
        assertThat(mapOfAssignedTrackingBatches.entrySet().stream()
            .filter(e -> e.getValue().size() == 2).findAny()).isNotEmpty();
    }
}
