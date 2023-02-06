package ru.yandex.market.delivery.tracker.service.tracking;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.SurveyType;
import ru.yandex.market.delivery.tracker.service.logger.CheckpointsAcquiringLogger;
import ru.yandex.market.delivery.tracker.service.logger.data.track_events.TrackEventsLoggingService;
import ru.yandex.market.delivery.tracker.service.pushing.PushCheckpointLesQueueProducer;
import ru.yandex.market.delivery.tracker.service.pushing.PushTrackQueueProducer;

import static java.util.stream.Collectors.toList;

class DeliveryCheckpointsFilterTest {

    @RegisterExtension
    JUnitJupiterSoftAssertions assertions = new JUnitJupiterSoftAssertions();

    private CheckpointsProcessingService checkpointService = new CheckpointsProcessingService(
        Mockito.mock(DeliveryTrackService.class),
        Mockito.mock(TrackEventsLoggingService.class),
        Mockito.mock(CheckpointsAcquiringLogger.class),
        Mockito.mock(PushTrackQueueProducer.class),
        Mockito.mock(PushCheckpointLesQueueProducer.class),
        Mockito.mock(TrackingDurationService.class),
        Mockito.mock(DeliveryServiceFetcher.class),
        Mockito.mock(Clock.class),
        false
    );

    private DeliveryTrackCheckpoint checkpoint1 = new DeliveryTrackCheckpoint(
        1,
        Date.from(LocalDateTime.of(2016, 1, 20, 16, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.OUT_FOR_DELIVERY,
        OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint2 = new DeliveryTrackCheckpoint(
        2,
        Date.from(LocalDateTime.of(2016, 1, 21, 12, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.ATTEMPT_FAIL,
        OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint3 = new DeliveryTrackCheckpoint(
        3,
        Date.from(LocalDateTime.of(2016, 1, 21, 21, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.IN_TRANSIT,
        OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint4 = new DeliveryTrackCheckpoint(
        4,
        Date.from(LocalDateTime.of(2016, 1, 22, 9, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.DELIVERED,
        OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint5 = new DeliveryTrackCheckpoint(
        5,
        Date.from(LocalDateTime.of(2016, 1, 22, 9, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.PENDING,
        OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint6 = new DeliveryTrackCheckpoint(
        6,
        Date.from(LocalDateTime.of(2016, 1, 22, 9, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.EXPIRED,
        OrderDeliveryCheckpointStatus.ERROR_NOT_FOUND,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint7 = new DeliveryTrackCheckpoint(
        7,
        Date.from(LocalDateTime.of(2018, 1, 22, 9, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.EXPIRED,
        OrderDeliveryCheckpointStatus.ERROR_NOT_FOUND,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint8 = new DeliveryTrackCheckpoint(
        8,
        Date.from(LocalDateTime.of(2019, 1, 22, 9, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.UNKNOWN,
        OrderDeliveryCheckpointStatus.ERROR_NOT_FOUND,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint9 = new DeliveryTrackCheckpoint(
        9,
        Date.from(LocalDateTime.of(2019, 1, 22, 15, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.IN_TRANSIT,
        OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint10 = new DeliveryTrackCheckpoint(
        10,
        Date.from(LocalDateTime.of(2019, 1, 22, 18, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.IN_TRANSIT,
        OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_RECIPIENT,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint11 = new DeliveryTrackCheckpoint(
        11,
        Date.from(LocalDateTime.of(2019, 1, 22, 9, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.IN_TRANSIT,
        OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint12 = new DeliveryTrackCheckpoint(
        12,
        Date.from(LocalDateTime.of(2019, 1, 23, 15, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.IN_TRANSIT,
        OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint13 = new DeliveryTrackCheckpoint(
        13,
        Date.from(LocalDateTime.of(2019, 1, 23, 18, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.IN_TRANSIT,
        OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_RECIPIENT,
        SurveyType.PULL);
    private DeliveryTrackCheckpoint checkpoint14 = new DeliveryTrackCheckpoint(
        14,
        Date.from(LocalDateTime.of(2016, 1, 20, 16, 0, 0).atZone(ZoneId.systemDefault()).toInstant()),
        CheckpointStatus.OUT_FOR_DELIVERY,
        OrderDeliveryCheckpointStatus.DELIVERY_CUSTOMS_ARRIVED,
        SurveyType.PULL);

    @Test
    void testExpiredCheckpointNotFilteredEmptyOldCheckpoints() {

        List<DeliveryTrackCheckpoint> oldCheckpoints = Collections.emptyList();
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint7);

        long filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .count();

        assertions.assertThat(filtered)
            .as("testExpiredCheckpointNotFilteredEmptyOldCheckpoints: asserting that filtered count equals 1")
            .isEqualTo(1);
    }

    @Test
    void testCheckpointNotFilteredIfOldCheckpointsContainOnlyExpired() {

        List<DeliveryTrackCheckpoint> oldCheckpoints = Collections.singletonList(checkpoint7);
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint3);

        long filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .count();

        assertions.assertThat(filtered)
            .as("testCheckpointNotFilteredIfOldCheckpointsContainOnlyExpired:" +
                " asserting that filtered count equals 1")
            .isEqualTo(1);
    }

    @Test
    void testExpiredCheckpointNotFilteredIfOldCheckpointsContainOnlyUnknown() {

        List<DeliveryTrackCheckpoint> oldCheckpoints = Collections.singletonList(checkpoint8);
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint7);

        long filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .count();

        assertions.assertThat(filtered)
            .as("testExpiredCheckpointNotFilteredIfOldCheckpointsContainOnlyUnknown:" +
                " asserting that filtered count equals 1")
            .isEqualTo(1);
    }

    @Test
    void testExpiredCheckpointFilteredIfOldCheckpointsAlreadyContainExpired() {

        List<DeliveryTrackCheckpoint> oldCheckpoints = Collections.singletonList(checkpoint6);
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint7);

        long filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .count();

        assertions.assertThat(filtered)
            .as("testExpiredCheckpointFilteredWhenOldCheckpointsAlreadyContainsExpired:" +
                " asserting that filtered count equals 0")
            .isEqualTo(0);
    }

    @Test
    void testExpiredCheckpointFilteredIfOldCheckpointsAlreadyContainExpiredReverseOrder() {

        List<DeliveryTrackCheckpoint> oldCheckpoints = Collections.singletonList(checkpoint7);
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint6);

        long filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .count();

        assertions.assertThat(filtered)
            .as("testExpiredCheckpointFilteredWhenOldCheckpointsAlreadyContainsExpiredReverseOrder: " +
                "asserting that filtered count equals 0")
            .isEqualTo(0);
    }

    @Test
    void testEmptyStartSetCheckpoints() {

        List<DeliveryTrackCheckpoint> oldCheckpoints = new ArrayList<>();
        List<DeliveryTrackCheckpoint> newCheckpoints = Arrays.asList(checkpoint1, checkpoint2, checkpoint3,
            checkpoint4);

        long filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .count();

        assertions.assertThat(filtered)
            .as("testEmptyStartSetCheckpoints: asserting that filtered count equals 4")
            .isEqualTo(4);
    }

    @Test
    void testCheckpointsIntersection() {
        List<DeliveryTrackCheckpoint> oldCheckpoints = Arrays.asList(checkpoint1, checkpoint2);
        List<DeliveryTrackCheckpoint> newCheckpoints = Arrays.asList(checkpoint1, checkpoint2, checkpoint3,
            checkpoint4);

        List<DeliveryTrackCheckpoint> filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .collect(toList());

        List<DeliveryTrackCheckpoint> expected = Arrays.asList(checkpoint3, checkpoint4);

        assertions.assertThat(filtered)
            .as("testCheckpointsIntersection: asserting that filtered equals expected")
            .isEqualTo(expected);
    }

    @Test
    void testPendingStatusIgnoring() {
        List<DeliveryTrackCheckpoint> oldCheckpoints = Collections.singletonList(checkpoint5);
        List<DeliveryTrackCheckpoint> newCheckpoints = Arrays.asList(checkpoint1, checkpoint3, checkpoint5);

        List<DeliveryTrackCheckpoint> filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .collect(toList());

        List<DeliveryTrackCheckpoint> expected = Arrays.asList(checkpoint1, checkpoint3);

        assertions.assertThat(filtered)
            .as("testPendingStatusIgnoring: asserting that filtered equals expected")
            .isEqualTo(expected);
    }

    @Test
    void testGetDeliveredStatusAfterDeliveryUpdatedByDelivery() {
        List<DeliveryTrackCheckpoint> oldCheckpoints = Arrays.asList(checkpoint3, checkpoint9);
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint11);

        List<DeliveryTrackCheckpoint> filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .collect(toList());

        List<DeliveryTrackCheckpoint> expected = Collections.singletonList(checkpoint11);

        assertions.assertThat(filtered)
            .as("testGetDeliveredStatusAfterDeliveryUpdatedByDelivery: asserting that filtered equals expected")
            .isEqualTo(expected);
    }

    @Test
    void testGetDeliveredStatusAfterDeliveryUpdatedByRecipient() {
        List<DeliveryTrackCheckpoint> oldCheckpoints = Arrays.asList(checkpoint3, checkpoint10);
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint11);

        List<DeliveryTrackCheckpoint> filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .collect(toList());

        List<DeliveryTrackCheckpoint> expected = Collections.singletonList(checkpoint11);

        assertions.assertThat(filtered)
            .as("testGetDeliveredStatusAfterDeliveryUpdatedByRecipient: asserting that filtered equals expected")
            .isEqualTo(expected);
    }

    @Test
    void testDeliveryUpdatedByDeliveryNotDuplicated() {
        List<DeliveryTrackCheckpoint> oldCheckpoints = Arrays.asList(checkpoint3, checkpoint9);
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint9);

        List<DeliveryTrackCheckpoint> filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .collect(toList());

        List<DeliveryTrackCheckpoint> expected = Collections.emptyList();

        assertions.assertThat(filtered)
            .as("testDeliveryUpdatedByDeliveryNotDuplicated: asserting that filtered equals expected")
            .isEqualTo(expected);
    }

    @Test
    void testDeliveryUpdatedByRecipientNotDuplicated() {
        List<DeliveryTrackCheckpoint> oldCheckpoints = Arrays.asList(checkpoint3, checkpoint10);
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint10);

        List<DeliveryTrackCheckpoint> filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .collect(toList());

        List<DeliveryTrackCheckpoint> expected = Collections.emptyList();

        assertions.assertThat(filtered)
            .as("testDeliveryUpdatedByRecipientNotDuplicated: asserting that filtered equals expected")
            .isEqualTo(expected);
    }

    @Test
    void testTwoDeliveryUpdatedByDelivery() {
        List<DeliveryTrackCheckpoint> oldCheckpoints = Arrays.asList(checkpoint3, checkpoint9);
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint12);

        List<DeliveryTrackCheckpoint> filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .collect(toList());

        List<DeliveryTrackCheckpoint> expected = Collections.singletonList(checkpoint12);

        assertions.assertThat(filtered)
            .as("testTwoDeliveryUpdatedByDelivery: asserting that filtered equals expected")
            .isEqualTo(expected);
    }

    @Test
    void testTwoDeliveryUpdatedByRecipient() {
        List<DeliveryTrackCheckpoint> oldCheckpoints = Arrays.asList(checkpoint3, checkpoint10);
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint13);

        List<DeliveryTrackCheckpoint> filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .collect(toList());

        List<DeliveryTrackCheckpoint> expected = Collections.singletonList(checkpoint13);

        assertions.assertThat(filtered)
            .as("testTwoDeliveryUpdatedByRecipient: asserting that filtered equals expected")
            .isEqualTo(expected);
    }

    @Test
    void testThatWeAcceptCheckpointWithTheSameTimeAndAnotherCode() {
        List<DeliveryTrackCheckpoint> oldCheckpoints = Collections.singletonList(checkpoint1);
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint14);

        List<DeliveryTrackCheckpoint> filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .collect(toList());

        List<DeliveryTrackCheckpoint> expected = Collections.singletonList(checkpoint14);

        assertions.assertThat(filtered)
            .as("testThatWeAcceptCheckpointWithTheSameTimeAndAnotherCode: asserting that filtered equals expected")
            .isEqualTo(expected);
    }

    @Test
    void testThatWeDoNotAcceptCheckpointWithTheSameTimeAndTheSameCode() {
        List<DeliveryTrackCheckpoint> oldCheckpoints = Collections.singletonList(checkpoint1);
        List<DeliveryTrackCheckpoint> newCheckpoints = Collections.singletonList(checkpoint1);

        List<DeliveryTrackCheckpoint> filtered = newCheckpoints.stream()
            .filter(checkpointService.getNewCheckPointsPredicate(oldCheckpoints))
            .collect(toList());

        List<DeliveryTrackCheckpoint> expected = Collections.emptyList();

        assertions.assertThat(filtered)
            .as("testThatWeDoNotAcceptCheckpointWithTheSameTimeAndTheSameCode: asserting that filtered equals expected")
            .isEqualTo(expected);
    }
}
