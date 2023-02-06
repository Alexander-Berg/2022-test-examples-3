package ru.yandex.market.delivery.tracker;

import java.sql.Date;
import java.time.Clock;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.client.tracking.common.TrackerStatus;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.entity.TrackRequestMeta;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.MovementDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.RequestType;
import ru.yandex.market.delivery.tracker.domain.enums.SurveyType;
import ru.yandex.market.delivery.tracker.service.pushing.PushCheckpointLesQueueProducer;
import ru.yandex.market.delivery.tracker.service.pushing.PushTrackQueueProducer;
import ru.yandex.market.delivery.tracker.service.tracking.CheckpointsProcessingService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT;
import static ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_RFF_ARRIVED_FULFILLMENT;

class CheckpointsProcessingServiceTest extends AbstractContextualTest {

    @Autowired
    private CheckpointsProcessingService checkpointsProcessingService;

    @Autowired
    private Clock clock;

    @Autowired
    private PushTrackQueueProducer pushTrackQueueProducer;

    @Autowired
    private PushCheckpointLesQueueProducer pushCheckpointLesQueueProducer;

    @BeforeEach
    void setUp() {
        doNothing().when(pushTrackQueueProducer).enqueue(anyLong());
        doNothing().when(pushCheckpointLesQueueProducer).enqueue(anyLong(), anyList());
    }

    @Test
    @DatabaseSetup("/database/states/single_track_registered.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_with_checkpoint_added.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreateCheckpoints() {
        DeliveryTrackMeta meta = new DeliveryTrackMeta();
        meta.setId(1L);
        meta.setEntityId("ORDER_1");
        meta.setDeliveryServiceId(101L);

        List<TrackerStatus> statuses = ImmutableList.of(
            TrackerStatus.builder()
                .code(45)
                .date(Date.from(clock.instant().minus(Period.ofDays(2))))
                .country("Россия")
                .city("Moscow")
                .location("Sklad")
                .zipCode("zip")
                .message("moved to sklad")
                .build()
        );

        TrackRequestMeta requestMeta = new TrackRequestMeta()
            .setServiceId(101L)
            .setToken("")
            .setUrl("")
            .setName("")
            .setType(RequestType.ORDER_HISTORY)
            .setVersion(ApiVersion.DS);

        checkpointsProcessingService.createNewCheckPointsIfAchieved(
            meta,
            statuses,
            SurveyType.PULL,
            requestMeta.getType()
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_track_registered.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_with_checkpoint_added.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreateCheckpointsWithMilliseconds() {
        DeliveryTrackMeta meta = new DeliveryTrackMeta();
        meta.setId(1L);
        meta.setEntityId("ORDER_1");
        meta.setDeliveryServiceId(101L);

        List<TrackerStatus> statuses = ImmutableList.of(
            TrackerStatus.builder()
                .code(45)
                .date(Date.from(clock.instant().minus(Period.ofDays(2)).plusMillis(954)))
                .country("Россия")
                .city("Moscow")
                .location("Sklad")
                .zipCode("zip")
                .message("moved to sklad")
                .build()
        );

        TrackRequestMeta requestMeta = new TrackRequestMeta()
            .setServiceId(101L)
            .setToken("")
            .setUrl("")
            .setName("")
            .setType(RequestType.ORDER_HISTORY)
            .setVersion(ApiVersion.DS);

        checkpointsProcessingService.createNewCheckPointsIfAchieved(
            meta,
            statuses,
            SurveyType.PULL,
            requestMeta.getType()
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_track_registered.xml")
    void createCheckpointWithInvalidCheckpointStatus() {
        assertions()
            .assertThatThrownBy(() -> {
                checkpointsProcessingService.createNewCheckPointsIfAchieved(
                    new DeliveryTrackMeta().setId(1L).setDeliveryServiceId(101L),
                    List.of(TrackerStatus.builder().code(105).build()), // incorrect code
                    SurveyType.PULL,
                    (new TrackRequestMeta()).getType()
                );
            })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SORTING_CENTER_CANCELED is not acceptable for DELIVERY with API version DS");
    }

    @Test
    @DatabaseSetup("/database/states/single_track_registered.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_with_fake_checkpoint_added.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreationFakeCheckpointMakesQueuePush() {
        DeliveryTrackCheckpoint checkpoint = new DeliveryTrackCheckpoint()
            .setTrackId(1L)
            .setDeliveryCheckpointStatus(DELIVERY_ARRIVED_PICKUP_POINT)
            .setZipCode("zip")
            .setMessage("moved to sklad");

        checkpointsProcessingService.addNewFakeCheckpoint(checkpoint.getTrackId(), checkpoint);
    }

    @Test
    @DatabaseSetup("/database/states/single_track_registered.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_with_fake_checkpoint_added_with_checkpoint_date.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreationFakeCheckpointWithCheckpointDate() {
        DeliveryTrackCheckpoint checkpoint = new DeliveryTrackCheckpoint()
            .setTrackId(1L)
            .setDeliveryCheckpointStatus(DELIVERY_ARRIVED_PICKUP_POINT)
            .setZipCode("zip")
            .setMessage("moved to sklad")
            .setCheckpointDate(Date.from(ZonedDateTime.parse("2021-06-01T16:32:00.000+03:00").toInstant()));

        checkpointsProcessingService.addNewFakeCheckpoint(checkpoint.getTrackId(), checkpoint);
    }

    @Test
    @DatabaseSetup("/database/states/single_track_registered.xml")
    public void createFakeCheckpointWithInvalidCheckpointStatus() throws IllegalArgumentException {
        DeliveryTrackCheckpoint checkpoint = new DeliveryTrackCheckpoint()
            .setTrackId(1L)
            .setDeliveryCheckpointStatus(SORTING_CENTER_RETURN_RFF_ARRIVED_FULFILLMENT);

        assertions()
            .assertThatThrownBy(() -> {
                checkpointsProcessingService.addNewFakeCheckpoint(checkpoint.getTrackId(), checkpoint);
            })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(
                "SORTING_CENTER_RETURN_RFF_ARRIVED_FULFILLMENT is not acceptable for DELIVERY with API version DS"
            );
    }

    @Test
    @DatabaseSetup("/database/states/single_track_for_delivery_with_ff_api.xml")
    @ExpectedDatabase(
        value = "/database/states/single_track_for_delivery_with_ff_api_with_fake_checkpoint_added.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    public void createFakeCheckpointWithForDeliveryWithFfApi() throws IllegalArgumentException {
        DeliveryTrackCheckpoint checkpoint = new DeliveryTrackCheckpoint()
            .setTrackId(1L)
            .setDeliveryCheckpointStatus(SORTING_CENTER_RETURN_RFF_ARRIVED_FULFILLMENT);

        checkpointsProcessingService.addNewFakeCheckpoint(checkpoint.getTrackId(), checkpoint);
    }

    @Test
    @DatabaseSetup("/database/states/single_track_for_delivery_with_ff_api.xml")
    public void createFakeCheckpointForDeliveryWithFfApiWithInvalidCheckpointStatus() throws IllegalArgumentException {
        DeliveryTrackCheckpoint checkpoint = new DeliveryTrackCheckpoint()
            .setTrackId(1L)
            .setDeliveryCheckpointStatus(DELIVERY_ARRIVED_PICKUP_POINT);

        assertions()
            .assertThatThrownBy(() -> {
                checkpointsProcessingService.addNewFakeCheckpoint(checkpoint.getTrackId(), checkpoint);
            })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(
                "DELIVERY_ARRIVED_PICKUP_POINT is not acceptable for DELIVERY with API version FF"
            );
    }

    @Test
    @DatabaseSetup("/database/states/single_track_registered.xml")
    public void createFakeCheckpointWithInvalidEntityType() throws IllegalArgumentException {
        DeliveryTrackCheckpoint checkpoint = new DeliveryTrackCheckpoint()
            .setTrackId(1L)
            .setDeliveryCheckpointStatus(MovementDeliveryCheckpointStatus.HANDED_OVER)
            .setEntityType(EntityType.MOVEMENT);

        assertions()
            .assertThatThrownBy(() -> {
                checkpointsProcessingService.addNewFakeCheckpoint(checkpoint.getTrackId(), checkpoint);
            })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Delivery checkpoint entity type was different from the track's one");
    }

    @Test
    @DatabaseSetup("/database/states/single_movement_track_registered.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_movement_track_with_checkpoint_added.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCreateCheckpointsForMovement() {
        DeliveryTrackMeta meta = new DeliveryTrackMeta();
        meta.setId(1L);
        meta.setEntityId("MOVEMENT_1");
        meta.setEntityType(EntityType.MOVEMENT);
        meta.setDeliveryServiceId(101L);

        List<TrackerStatus> statuses = ImmutableList.of(
            TrackerStatus.builder()
                .code(100)
                .date(Date.from(clock.instant().minus(Period.ofDays(2))))
                .message("courier is here, bro")
                .build()
        );

        TrackRequestMeta requestMeta = new TrackRequestMeta()
            .setServiceId(101L)
            .setToken("")
            .setUrl("")
            .setName("")
            .setType(RequestType.MOVEMENT_STATUS_HISTORY)
            .setVersion(ApiVersion.DS);

        checkpointsProcessingService.createNewCheckPointsIfAchieved(
            meta,
            statuses,
            SurveyType.PULL,
            requestMeta.getType()
        );
    }

    @Test
    @DatabaseSetup("/database/states/single_movement_track_registered.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_movement_track_with_unknown_checkpoint_added.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createCheckpointWithOrderCheckpointStatusForMovement() {
        DeliveryTrackMeta meta = new DeliveryTrackMeta();
        meta.setId(1L);
        meta.setEntityId("MOVEMENT_1");
        meta.setEntityType(EntityType.MOVEMENT);
        meta.setDeliveryServiceId(101L);

        List<TrackerStatus> statuses = ImmutableList.of(
            TrackerStatus.builder()
                .code(45) // Order status code, invalid for movements.
                .date(Date.from(clock.instant().minus(Period.ofDays(2))))
                .message("courier is here, bro")
                .build()
        );

        TrackRequestMeta requestMeta = new TrackRequestMeta()
            .setServiceId(101L)
            .setToken("")
            .setUrl("")
            .setName("")
            .setType(RequestType.MOVEMENT_STATUS_HISTORY)
            .setVersion(ApiVersion.DS);

        checkpointsProcessingService.createNewCheckPointsIfAchieved(
            meta,
            statuses,
            SurveyType.PULL,
            requestMeta.getType()
        );
    }
}
