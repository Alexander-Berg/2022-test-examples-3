package util;

import java.util.Date;
import java.util.List;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;

import static ru.yandex.market.delivery.tracker.util.CheckpointsProcessingUtils.extractLastCheckpointStatus;

class CheckpointsProcessingUtilsTest {

    @RegisterExtension
    JUnitJupiterSoftAssertions assertions = new JUnitJupiterSoftAssertions();

    @Test
    void testExtractLastCheckpointStatusEmpty() {
        assertions.assertThat(extractLastCheckpointStatus(List.of())).isNull();
    }


    @Test
    void testExtractLastCheckpointStatusOne() {
        List<DeliveryTrackCheckpoint> checkpoints = List.of(
            checkpoint(1, 2, 3, EntityType.ORDER)
        );
        assertions.assertThat(extractLastCheckpointStatus(checkpoints)).isEqualTo(1);
    }

    @Test
    void testExtractLastCheckpointStatusDifferentTimes() {
        List<DeliveryTrackCheckpoint> checkpoints = List.of(
            checkpoint(101, 101, 100, EntityType.ORDER),
            checkpoint(102, 301, 300, EntityType.ORDER),
            checkpoint(100, 201, 200, EntityType.ORDER)
        );
        assertions.assertThat(extractLastCheckpointStatus(checkpoints)).isEqualTo(102);
    }

    @Test
    void testExtractLastCheckpointStatusSameTrackerTimes() {
        List<DeliveryTrackCheckpoint> checkpoints = List.of(
            checkpoint(100, 1000, 900, EntityType.ORDER),
            checkpoint(120, 2000, 1990, EntityType.ORDER),
            checkpoint(110, 2000, 1950, EntityType.ORDER)
        );
        assertions.assertThat(extractLastCheckpointStatus(checkpoints)).isEqualTo(120);
    }

    @Test
    void testExtractLastCheckpointStatusBothTrackerCheckpointTimes() {
        List<DeliveryTrackCheckpoint> checkpoints = List.of(
                checkpoint(100, 1000, 900, EntityType.ORDER),
                checkpoint(120, 2000, 1990, EntityType.ORDER),
                checkpoint(110, 2000, 1950, EntityType.ORDER),
                checkpoint(175, 5000, 4000, EntityType.ORDER),
                checkpoint(170, 6000, 3900, EntityType.ORDER),
                checkpoint(160, 6000, 3900, EntityType.ORDER),
                checkpoint(130, 3000, 1950, EntityType.ORDER)
        );
        assertions.assertThat(extractLastCheckpointStatus(checkpoints)).isEqualTo(170);
    }

    @Test
    void testExtractLastCheckpointStatusBothTrackerCheckpointTimesForMovement() {
        List<DeliveryTrackCheckpoint> checkpoints = List.of(
            checkpoint(0, 1000, 900, EntityType.MOVEMENT),
            checkpoint(1, 2000, 1990, EntityType.MOVEMENT),
            checkpoint(100, 2000, 1950, EntityType.MOVEMENT),
            checkpoint(250, 6000, 3900, EntityType.MOVEMENT),
            checkpoint(200, 6000, 3900, EntityType.MOVEMENT),
            checkpoint(150, 3000, 1950, EntityType.MOVEMENT)
        );
        assertions.assertThat(extractLastCheckpointStatus(checkpoints)).isEqualTo(250);
    }

    private DeliveryTrackCheckpoint checkpoint(
        int status,
        long acquiredByTrackerTime,
        long checkpointTime,
        EntityType entityType
    ) {

        Date acquiredByTrackerDate = new Date(acquiredByTrackerTime);
        Date checkpointDate = new Date(checkpointTime);
        DeliveryCheckpointStatus checkpointStatus = DeliveryCheckpointStatus.findByIdAndEntityType(status, entityType);

        return new DeliveryTrackCheckpoint()
                .setAcquiredByTrackerDate(acquiredByTrackerDate)
                .setCheckpointDate(checkpointDate)
                .setDeliveryCheckpointStatus(checkpointStatus);
    }
}
