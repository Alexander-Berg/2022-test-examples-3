package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.config.properties.FeatureProperties;
import ru.yandex.market.logistics.lrm.converter.EnumConverter;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnBoxStatusChangedPayload;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatusChangedPayload;
import ru.yandex.market.logistics.lrm.model.entity.ControlPoint;
import ru.yandex.market.logistics.lrm.model.entity.ReturnBoxEntity;
import ru.yandex.market.logistics.lrm.model.entity.ReturnBoxStatusHistoryEntity;
import ru.yandex.market.logistics.lrm.model.entity.ReturnEntity;
import ru.yandex.market.logistics.lrm.model.entity.ReturnEventEntity;
import ru.yandex.market.logistics.lrm.model.entity.ReturnSegmentEntity;
import ru.yandex.market.logistics.lrm.model.entity.ReturnSegmentStatusHistoryEntity;
import ru.yandex.market.logistics.lrm.model.entity.ReturnStatusHistoryEntity;
import ru.yandex.market.logistics.lrm.model.entity.enums.ControlPointStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ControlPointType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnBoxStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnEventType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSegmentStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnStatus;
import ru.yandex.market.logistics.lrm.queue.payload.ChangeClientReturnStatusPayload;
import ru.yandex.market.logistics.lrm.queue.payload.UpdateReturnStatusesPayload;
import ru.yandex.market.logistics.lrm.queue.processor.UpdateReturnStatusesProcessor;
import ru.yandex.market.logistics.lrm.repository.ControlPointRepository;
import ru.yandex.market.logistics.lrm.repository.ReturnEventRepository;
import ru.yandex.market.logistics.lrm.repository.ReturnSegmentRepository;
import ru.yandex.market.logistics.lrm.service.ReturnSegmentStatusHistoryService;
import ru.yandex.market.logistics.lrm.utils.QueueTasksChecker;

import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@DisplayName("Обновление статусов у сущностей возврата при обновлении статуса возвратного сегмента")
@DatabaseSetup("/database/tasks/return-segment/update-statuses/before/common.xml")
class UpdateReturnStatusesProcessorTest extends AbstractIntegrationTest {
    private static final long PICKUP_SEGMENT_ID = 101L;
    private static final long SC_MIDDLE_MILE_SEGMENT_ID = 102L;
    private static final long SC_LAST_MILE_TO_SHOP_SEGMENT_ID = 103L;
    private static final long SC_LAST_MILE_TO_FF_SEGMENT_ID = 104L;
    private static final long SC_LAST_MILE_TO_UTILIZATION_SEGMENT_ID = 108L;
    private static final long FF_SEGMENT_ID_ALL_BOXES_FF_RECIEVED = 105L;
    private static final long FF_SEGMENT_ID_NOT_ALL_BOXES_FF_RECEIVED = 106L;
    private static final long COURIER_SEGMENT_ID = 107L;
    private static final long DROPOFF_LAST_MILE_SEGMENT_ID = 103L;
    private static final long PARALLEL_LAST_MILE_SEGMENT = 110L;
    private static final Instant NOW = Instant.parse("2022-01-02T03:04:05.00Z");
    private static final String CHANGE_CLIENT_RETURN_STATUS_QUEUE_NAME = "CHANGE_CLIENT_RETURN_STATUS";

    @Autowired
    private ReturnSegmentRepository returnSegmentRepository;

    @Autowired
    private ReturnSegmentStatusHistoryService statusHistoryService;

    @Autowired
    private UpdateReturnStatusesProcessor updateReturnStatusesProcessor;

    @Autowired
    private ReturnEventRepository returnEventRepository;

    @Autowired
    private ControlPointRepository controlPointRepository;

    @Autowired
    private EnumConverter enumConverter;

    @Autowired
    private QueueTasksChecker queueTasksChecker;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        clock.setFixed(NOW, DateTimeUtils.MOSCOW_ZONE);
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Попытка обновить статус сегмента на неактуальный для сегмента статус")
    void notActualSegmentStatus(Long segmentId, ReturnSegmentStatus status) {
        updateStatuses(segmentId, status);

        verifyLog(
            "ERROR",
            "Trying to update not actual status for segment %d, current status null, status in payload %s".formatted(
                segmentId,
                status
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> notActualSegmentStatus() {
        return Stream.of(
            Arguments.of(PICKUP_SEGMENT_ID, ReturnSegmentStatus.CANCELLED),
            Arguments.of(SC_MIDDLE_MILE_SEGMENT_ID, ReturnSegmentStatus.CANCELLED),
            Arguments.of(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.CANCELLED),
            Arguments.of(SC_LAST_MILE_TO_FF_SEGMENT_ID, ReturnSegmentStatus.CANCELLED)
        );
    }

    @MethodSource({
        "statusUpdatingForPickupSegment",
        "statusUpdatingForCourierSegment",
        "statusUpdatingForScMiddleMileSegment",
        "statusUpdatingForScLastMileToShopSegment",
        "statusUpdatingForScLastMileToFFSegment",
        "statusUpdatingForScLastMileToUtilizationSegment",
        "statusUpdatingForFFSegment",
    })
    @ParameterizedTest
    @DisplayName("Обновление статусов возврата через сегменты: статусы обновляются у грузоместа и у возврата")
    void statusUpdatingForSegment(
        @SuppressWarnings("unused") String segmentType,
        ReturnSegmentStatusChangingInfo statusChangeInfo
    ) {
        statusUpdatingForSegmentCheck(statusChangeInfo);
    }

    private void checkReturnStatus(ReturnEntity returnEntity, ReturnStatus returnStatus) {
        softly.assertThat(returnEntity.getStatus()).isEqualTo(returnStatus);
        softly.assertThat(returnEntity.getStatusHistory()).extracting(
            ReturnStatusHistoryEntity::getId,
            ReturnStatusHistoryEntity::getStatus,
            ReturnStatusHistoryEntity::getDatetime
        ).containsExactly(Tuple.tuple(1L, returnStatus, NOW));
    }

    private void checkBoxStatus(ReturnBoxEntity returnBox, ReturnBoxStatus boxStatus) {
        softly.assertThat(returnBox.getStatus()).isEqualTo(boxStatus);
        softly.assertThat(returnBox.getStatusHistory()).extracting(
            ReturnBoxStatusHistoryEntity::getId,
            ReturnBoxStatusHistoryEntity::getStatus,
            ReturnBoxStatusHistoryEntity::getDatetime
        ).containsExactly(Tuple.tuple(1L, boxStatus, NOW));
    }

    private void checkForQueueTasks(long returnId, ReturnStatus returnStatus, boolean clientReturnStatusUpdate) {
        if (clientReturnStatusUpdate) {
            queueTasksChecker.assertSingleQueueTaskPayload(
                CHANGE_CLIENT_RETURN_STATUS_QUEUE_NAME,
                ChangeClientReturnStatusPayload.builder()
                    .returnId(returnId)
                    .status(returnStatus)
                    .build()
            );
        } else {
            queueTasksChecker.assertNoQueueTasksCreated(CHANGE_CLIENT_RETURN_STATUS_QUEUE_NAME);
        }
    }

    private void checkEvents(
        Long returnId,
        String boxExternalId,
        ReturnSegmentStatusChangingInfo statusChangeInfo
    ) {
        Tuple boxStatusChangedEvent = Tuple.tuple(
            ReturnEventType.RETURN_BOX_STATUS_CHANGED,
            returnId,
            new ReturnBoxStatusChangedPayload()
                .setBoxExternalId(boxExternalId)
                .setStatus(enumConverter.convert(
                    statusChangeInfo.getBoxStatus(),
                    ru.yandex.market.logistics.lrm.event_model.payload.ReturnBoxStatus.class
                ))
        );
        Tuple returnStatusChangedEvent = Tuple.tuple(
            ReturnEventType.RETURN_STATUS_CHANGED,
            returnId,
            new ReturnStatusChangedPayload()
                .setStatus(enumConverter.convert(
                    statusChangeInfo.getReturnStatus(),
                    ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatus.class
                ))
        );
        if (statusChangeInfo.isReturnStatusChanges()) {
            softly.assertThat(returnEventRepository.findAll())
                .extracting(
                    ReturnEventEntity::getType,
                    event -> event.getReturnEntity().getId(),
                    ReturnEventEntity::getPayload
                )
                .containsExactlyInAnyOrder(boxStatusChangedEvent, returnStatusChangedEvent);
        } else {
            softly.assertThat(returnEventRepository.findAll())
                .extracting(
                    ReturnEventEntity::getType,
                    event -> event.getReturnEntity().getId(),
                    ReturnEventEntity::getPayload
                )
                .containsExactlyInAnyOrder(boxStatusChangedEvent);
        }
    }

    @Nonnull
    private static Stream<Arguments> statusUpdatingForPickupSegment() {
        return Stream.of(
            Arguments.of(
                "ПВЗ",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(PICKUP_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CREATED)
                    .setBoxStatus(ReturnBoxStatus.CREATED)
                    .setReturnStatus(ReturnStatus.CREATED)
                    .setClientReturnStatusUpdate(false)
            ),
            Arguments.of(
                "ПВЗ",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(PICKUP_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.IN)
                    .setBoxStatus(ReturnBoxStatus.RECEIVED)
                    .setReturnStatus(ReturnStatus.RECEIVED)
                    .setClientReturnStatusUpdate(false)
            ),
            Arguments.of(
                "ПВЗ",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(PICKUP_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.OUT)
                    .setBoxStatus(ReturnBoxStatus.IN_TRANSIT)
                    .setReturnStatus(ReturnStatus.IN_TRANSIT)
                    .setClientReturnStatusUpdate(false)
            ),
            Arguments.of(
                "ПВЗ",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(PICKUP_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.EXPIRED)
                    .setBoxStatus(ReturnBoxStatus.EXPIRED)
                    .setReturnStatus(ReturnStatus.EXPIRED)
                    .setClientReturnStatusUpdate(false)
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> statusUpdatingForCourierSegment() {
        return Stream.of(
            Arguments.of(
                "Курьер",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(COURIER_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CREATED)
                    .setBoxStatus(ReturnBoxStatus.CREATED)
                    .setReturnStatus(ReturnStatus.CREATED)
            ),
            Arguments.of(
                "Курьер",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(COURIER_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.IN)
                    .setBoxStatus(ReturnBoxStatus.RECEIVED)
                    .setReturnStatus(ReturnStatus.RECEIVED)
            ),
            Arguments.of(
                "Курьер",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(COURIER_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.OUT)
                    .setBoxStatus(ReturnBoxStatus.IN_TRANSIT)
                    .setReturnStatus(ReturnStatus.IN_TRANSIT)
            ),
            Arguments.of(
                "Курьер",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(COURIER_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.EXPIRED)
                    .setBoxStatus(ReturnBoxStatus.EXPIRED)
                    .setReturnStatus(ReturnStatus.EXPIRED)
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> statusUpdatingForScMiddleMileSegment() {
        return Stream.of(
            Arguments.of(
                "СЦ средняя миля",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_MIDDLE_MILE_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CREATED)
                    .setBoxStatus(ReturnBoxStatus.CREATED)
                    .setReturnStatus(ReturnStatus.CREATED)
            ),
            Arguments.of(
                "СЦ средняя миля",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_MIDDLE_MILE_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.OUT)
                    .setBoxStatus(ReturnBoxStatus.IN_TRANSIT)
                    .setReturnStatus(ReturnStatus.IN_TRANSIT)
            ),
            Arguments.of(
                "СЦ средняя миля",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_MIDDLE_MILE_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.TRANSIT_PREPARED)
                    .setBoxStatus(ReturnBoxStatus.IN_TRANSIT)
                    .setReturnStatus(ReturnStatus.IN_TRANSIT)
            ),
            Arguments.of(
                "СЦ средняя миля",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_MIDDLE_MILE_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CANCELLED)
                    .setBoxStatus(ReturnBoxStatus.CANCELLED)
                    .setReturnStatus(ReturnStatus.CANCELLED)
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> statusUpdatingForScLastMileToShopSegment() {
        return Stream.of(
            Arguments.of(
                "СЦ последняя миля в SHOP",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_SHOP_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CREATED)
                    .setBoxStatus(ReturnBoxStatus.CREATED)
                    .setReturnStatus(ReturnStatus.CREATED)
            ),
            Arguments.of(
                "СЦ последняя миля в SHOP",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_SHOP_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.IN)
                    .setBoxStatus(ReturnBoxStatus.DESTINATION_POINT_RECEIVED)
                    .setReturnStatus(ReturnStatus.DESTINATION_POINT_RECEIVED)
            ),
            Arguments.of(
                "СЦ последняя миля в SHOP",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_SHOP_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.TRANSIT_PREPARED)
                    .setBoxStatus(ReturnBoxStatus.READY_FOR_RETURN)
                    .setReturnStatus(ReturnStatus.READY_FOR_IM)
            ),
            Arguments.of(
                "СЦ последняя миля в SHOP",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_SHOP_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.OUT)
                    .setBoxStatus(ReturnBoxStatus.DELIVERED)
                    .setReturnStatus(ReturnStatus.DELIVERED)
            ),
            Arguments.of(
                "СЦ последняя миля в SHOP",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_SHOP_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CANCELLED)
                    .setBoxStatus(ReturnBoxStatus.CANCELLED)
                    .setReturnStatus(ReturnStatus.CANCELLED)
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> statusUpdatingForScLastMileToFFSegment() {
        return Stream.of(
            Arguments.of(
                "СЦ последняя миля в FULFILLMENT",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_FF_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CREATED)
                    .setBoxStatus(ReturnBoxStatus.CREATED)
                    .setReturnStatus(ReturnStatus.CREATED)
            ),
            Arguments.of(
                "СЦ последняя миля в FULFILLMENT",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_FF_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.OUT)
                    .setBoxStatus(ReturnBoxStatus.IN_TRANSIT)
                    .setReturnStatus(ReturnStatus.IN_TRANSIT)
            ),
            Arguments.of(
                "СЦ последняя миля в FULFILLMENT",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_FF_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.TRANSIT_PREPARED)
                    .setBoxStatus(ReturnBoxStatus.IN_TRANSIT)
                    .setReturnStatus(ReturnStatus.IN_TRANSIT)
            ),
            Arguments.of(
                "СЦ последняя миля в FULFILLMENT",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_FF_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CANCELLED)
                    .setBoxStatus(ReturnBoxStatus.CANCELLED)
                    .setReturnStatus(ReturnStatus.CANCELLED)
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> statusUpdatingForScLastMileToUtilizationSegment() {
        return Stream.of(
            Arguments.of(
                "СЦ последняя миля в UTILIZATION",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_UTILIZATION_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CREATED)
                    .setBoxStatus(ReturnBoxStatus.CREATED)
                    .setReturnStatus(ReturnStatus.CREATED)
            ),
            Arguments.of(
                "СЦ последняя миля в UTILIZATION",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_UTILIZATION_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.OUT)
                    .setBoxStatus(ReturnBoxStatus.READY_FOR_UTILIZATION)
                    .setReturnStatus(ReturnStatus.READY_FOR_UTILIZATION)
            ),
            Arguments.of(
                "СЦ последняя миля в UTILIZATION",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_UTILIZATION_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.TRANSIT_PREPARED)
                    .setBoxStatus(ReturnBoxStatus.READY_FOR_UTILIZATION)
                    .setReturnStatus(ReturnStatus.READY_FOR_UTILIZATION)
            ),
            Arguments.of(
                "СЦ последняя миля в UTILIZATION",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_UTILIZATION_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CANCELLED)
                    .setBoxStatus(ReturnBoxStatus.CANCELLED)
                    .setReturnStatus(ReturnStatus.CANCELLED)
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> statusUpdatingForFFSegment() {
        return Stream.of(
            Arguments.of(
                "FULFILLMENT сегмент: вторичная приемка",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(FF_SEGMENT_ID_ALL_BOXES_FF_RECIEVED)
                    .setSegmentStatus(ReturnSegmentStatus.IN)
                    .setBoxStatus(ReturnBoxStatus.FULFILMENT_RECEIVED)
                    .setReturnStatus(ReturnStatus.FULFILMENT_RECEIVED)
            ),
            Arguments.of(
                "FULFILLMENT сегмент: вторичная приемка, статус возврата не обновляется",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(FF_SEGMENT_ID_NOT_ALL_BOXES_FF_RECEIVED)
                    .setSegmentStatus(ReturnSegmentStatus.IN)
                    .setBoxStatus(ReturnBoxStatus.FULFILMENT_RECEIVED)
                    .setReturnStatus(ReturnStatus.IN_TRANSIT)
                    .setClientReturnStatusUpdate(false)
                    .setReturnStatusChanges(false)
            ),
            Arguments.of(
                "FULFILLMENT сегмент: отменён",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(FF_SEGMENT_ID_ALL_BOXES_FF_RECIEVED)
                    .setSegmentStatus(ReturnSegmentStatus.CANCELLED)
                    .setBoxStatus(ReturnBoxStatus.CANCELLED)
                    .setReturnStatus(ReturnStatus.CANCELLED)
            )
        );
    }

    @MethodSource({
        "statusUpdatingForDropOffLastMileSegment",
    })
    @ParameterizedTest
    @DisplayName("Обновление статусов возврата через сегменты для ДО: статусы обновляются у грузоместа и у возврата")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/dropoff.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/return_only_version.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void statusUpdatingForDropOffSegment(
        @SuppressWarnings("unused") String segmentType,
        ReturnSegmentStatusChangingInfo statusChangingInfo
    ) {
        statusUpdatingForSegmentCheck(statusChangingInfo);
    }

    @Nonnull
    private static Stream<Arguments> statusUpdatingForDropOffLastMileSegment() {
        return Stream.of(
            Arguments.of(
                "ДО последняя миля",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(DROPOFF_LAST_MILE_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CREATED)
                    .setBoxStatus(ReturnBoxStatus.CREATED)
                    .setReturnStatus(ReturnStatus.CREATED)
            ),
            Arguments.of(
                "ДО последняя миля",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(DROPOFF_LAST_MILE_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.IN)
                    .setBoxStatus(ReturnBoxStatus.DESTINATION_POINT_RECEIVED)
                    .setReturnStatus(ReturnStatus.DESTINATION_POINT_RECEIVED)
            ),
            Arguments.of(
                "ДО последняя миля",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(DROPOFF_LAST_MILE_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.TRANSIT_PREPARED)
                    .setBoxStatus(ReturnBoxStatus.READY_FOR_RETURN)
                    .setReturnStatus(ReturnStatus.READY_FOR_IM)
            ),
            Arguments.of(
                "ДО последняя миля",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(DROPOFF_LAST_MILE_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.OUT)
                    .setBoxStatus(ReturnBoxStatus.DELIVERED)
                    .setReturnStatus(ReturnStatus.DELIVERED)
            ),
            Arguments.of(
                "ДО последняя миля",
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(DROPOFF_LAST_MILE_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CANCELLED)
                    .setBoxStatus(ReturnBoxStatus.CANCELLED)
                    .setReturnStatus(ReturnStatus.CANCELLED)
            )
        );
    }

    private void statusUpdatingForSegmentCheck(ReturnSegmentStatusChangingInfo returnSegmentStatusChangingInfo) {
        setupReturnSegmentStatus(returnSegmentStatusChangingInfo);

        updateStatuses(
            returnSegmentStatusChangingInfo.getReturnSegmentId(),
            returnSegmentStatusChangingInfo.getSegmentStatus()
        );

        checkReturnSegmentStatusChanged(returnSegmentStatusChangingInfo);
    }

    private void setupReturnSegmentStatus(ReturnSegmentStatusChangingInfo statusChangeInfo) {
        ReturnSegmentStatus segmentStatus = statusChangeInfo.getSegmentStatus();
        transactionTemplate.execute(status -> {
            ReturnSegmentEntity returnSegment = returnSegmentRepository.getById(statusChangeInfo.getReturnSegmentId());
            softly.assertThat(returnSegment.getStatus()).isNotEqualTo(segmentStatus);
            softly.assertThat(returnSegment.getReturnBox().getStatus()).isNotEqualTo(statusChangeInfo.getBoxStatus());
            softly.assertThat(Objects.equals(
                    returnSegment.getReturnEntity().getStatus(),
                    statusChangeInfo.getReturnStatus()
                ))
                .isNotEqualTo(statusChangeInfo.isReturnStatusChanges());
            statusHistoryService.save(
                new ReturnSegmentStatusHistoryEntity()
                    .setReturnSegment(returnSegment)
                    .setStatus(segmentStatus)
                    .setDatetime(clock.instant())
            );
            return returnSegment.setStatusWithoutHistory(segmentStatus);
        });
    }

    private void checkReturnSegmentStatusChanged(ReturnSegmentStatusChangingInfo statusChangeInfo) {
        ReturnSegmentStatus segmentStatus = statusChangeInfo.getSegmentStatus();
        ReturnStatus returnStatus = statusChangeInfo.getReturnStatus();
        ReturnBoxStatus boxStatus = statusChangeInfo.getBoxStatus();
        boolean returnStatusChanged = statusChangeInfo.isReturnStatusChanges();

        transactionTemplate.execute(status -> {
            ReturnSegmentEntity updatedSegment = returnSegmentRepository.findById(statusChangeInfo.getReturnSegmentId())
                .orElseThrow();
            ReturnEntity returnEntity = updatedSegment.getReturnEntity();
            Long returnId = returnEntity.getId();

            softly.assertThat(updatedSegment.getStatus()).isEqualTo(segmentStatus);
            if (returnStatusChanged) {
                checkReturnStatus(returnEntity, returnStatus);
            } else {
                softly.assertThat(returnEntity.getStatus()).isEqualTo(returnStatus);
            }
            checkBoxStatus(updatedSegment.getReturnBox(), boxStatus);
            checkForQueueTasks(returnId, returnStatus, statusChangeInfo.isClientReturnStatusUpdate());
            checkEvents(returnId, "box-external-id-" + returnId, statusChangeInfo);
            return null;
        });
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Обновление статусов для возврата: статусы не обновляются")
    void statusesNotUpdating(
        @SuppressWarnings("unused") String segmentType,
        long returnSegmentId,
        ReturnSegmentStatus ignoredSegmentStatus
    ) {

        transactionTemplate.execute(status -> {
            ReturnSegmentEntity returnSegment = returnSegmentRepository.getById(returnSegmentId);
            returnSegment.setStatusWithoutHistory(ignoredSegmentStatus);
            returnSegment.getReturnEntity().setStatus(ReturnStatus.CREATED, clock);
            return null;
        });

        updateStatuses(returnSegmentId, ignoredSegmentStatus);

        transactionTemplate.execute(status -> {
            ReturnSegmentEntity returnSegment = returnSegmentRepository.findById(returnSegmentId).orElseThrow();
            checkReturnStatus(returnSegment.getReturnEntity(), ReturnStatus.CREATED);
            return null;
        });
    }

    @Nonnull
    private static Stream<Arguments> statusesNotUpdating() {
        return Stream.of(
            Arguments.of(
                "ПВЗ",
                PICKUP_SEGMENT_ID,
                ReturnSegmentStatus.TRANSIT_PREPARED
            ),
            Arguments.of(
                "СЦ средняя миля",
                SC_MIDDLE_MILE_SEGMENT_ID,
                ReturnSegmentStatus.EXPIRED
            ),
            Arguments.of(
                "СЦ последняя миля",
                SC_LAST_MILE_TO_SHOP_SEGMENT_ID,
                ReturnSegmentStatus.EXPIRED
            ),
            Arguments.of(
                "СЦ последняя миля в утилизацию",
                SC_LAST_MILE_TO_UTILIZATION_SEGMENT_ID,
                ReturnSegmentStatus.EXPIRED
            )
        );
    }

    @Test
    @DisplayName("Статус коробки не обновляется")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/return_box_status_exists.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/return_box_status_exists.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void existingBoxStatus() {
        updateStatuses(SC_MIDDLE_MILE_SEGMENT_ID, ReturnSegmentStatus.OUT);
    }

    @Test
    @DisplayName("Статус возврата не обновляется")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/return_status_exists.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/return_status_exists.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void existingReturnStatus() {
        updateStatuses(SC_MIDDLE_MILE_SEGMENT_ID, ReturnSegmentStatus.OUT);
    }

    @Test
    @DisplayName("Вызываем обновление статуса в неправильном порядке")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/incorrect_order.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/incorrect_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void incorrectOrder() {
        updateStatuses(SC_MIDDLE_MILE_SEGMENT_ID, ReturnSegmentStatus.OUT);
    }

    @Test
    @DisplayName("Проставление статуса средней мили произошло позже, чем у последней")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/incorrect_order.xml")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/update-statuses/before/set_date_of_middle_mile_later_then_last.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/incorrect_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dateOfMiddleMileIsLaterThenLastMile() {
        updateStatuses(SC_MIDDLE_MILE_SEGMENT_ID, ReturnSegmentStatus.OUT);
    }

    @Test
    @DisplayName("Статусы средней мили отсутствуют, но есть у первой и последней")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/four_segments.xml")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/without_middle_statuses.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/ready_for_return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void middleMileWithoutStatuses() {
        updateStatuses(PICKUP_SEGMENT_ID, ReturnSegmentStatus.OUT);
    }

    @Test
    @DisplayName("Последний статус из параллельной ветки 'создан' не влияет на статус грузоместа")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/four_segments.xml")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/created_at_parallel.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/ready_for_return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createdAtParallelDoNotAffectBoxStatus() {
        updateStatuses(PARALLEL_LAST_MILE_SEGMENT, ReturnSegmentStatus.CREATED);
    }

    @Test
    @DisplayName("Позволяем сбрасывать статус физического местоположения в рамках сегмента")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/four_segments.xml")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/update-statuses/before/update/reset_segment_statuses.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/reset_tracking_position_status.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void resetTrackingPositionStatus() {
        updateStatuses(PICKUP_SEGMENT_ID, ReturnSegmentStatus.CREATED);
    }

    @Test
    @DisplayName("Не позволяем сбрасывать в CREATED")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/in_transit_got_created.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/in_transit_got_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void inTransitReturnCreatedStatus() {
        updateStatuses(SC_MIDDLE_MILE_SEGMENT_ID, ReturnSegmentStatus.CREATED);
    }

    @Test
    @DisplayName("Не позволяем сбрасывать из FULFILMENT_RECEIVED")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/fulfillment_received_got_in_transit.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/before/fulfillment_received_got_in_transit.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void fulfilmentReceivedReturnBoxStatus() {
        updateStatuses(SC_MIDDLE_MILE_SEGMENT_ID, ReturnSegmentStatus.OUT);
    }

    @EnumSource(
        value = ReturnSegmentStatus.class,
        names = {"IN", "CREATED", "CANCELLED"},
        mode = EnumSource.Mode.EXCLUDE
    )
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Обновление статуса ФФ сегмента в неподдерживаемый статус")
    void incorrectStatusForFFSegment(ReturnSegmentStatus unsupportedSegmentStatus) {
        transactionTemplate.execute(status -> {
            ReturnSegmentEntity returnSegment = returnSegmentRepository.findById(FF_SEGMENT_ID_ALL_BOXES_FF_RECIEVED)
                .orElseThrow();
            statusHistoryService.save(
                new ReturnSegmentStatusHistoryEntity()
                    .setReturnSegment(returnSegment)
                    .setStatus(unsupportedSegmentStatus)
                    .setDatetime(clock.instant())
            );
            return returnSegment.setStatusWithoutHistory(unsupportedSegmentStatus);
        });

        softly.assertThatCode(
                () -> updateReturnStatusesProcessor.execute(updateReturnStatusesPayload(
                    FF_SEGMENT_ID_ALL_BOXES_FF_RECIEVED,
                    unsupportedSegmentStatus
                ))
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unsupported status %s for updating FF segment 105".formatted(unsupportedSegmentStatus));
    }

    @Test
    @DisplayName("Обновление одного из нескольких сегментов СЦ в статус CANCELLED")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/multiple_sc.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/multiple_sc_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void multipleScSegmentsCancel() {
        updateStatuses(12L, ReturnSegmentStatus.CANCELLED);
    }

    @MethodSource
    @ParameterizedTest
    @DisplayName("Флаг проставление статусов утилизации выключен")
    void setUtilizationStatusesDisabled(ReturnSegmentStatusChangingInfo statusChangeInfo) {
        when(featureProperties.isEnableSetUtilizationStatuses()).thenReturn(false);

        statusUpdatingForSegmentCheck(statusChangeInfo);
    }

    @Nonnull
    private static Stream<Arguments> setUtilizationStatusesDisabled() {
        return Stream.of(
            Arguments.of(
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_UTILIZATION_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CREATED)
                    .setBoxStatus(ReturnBoxStatus.CREATED)
                    .setReturnStatus(ReturnStatus.CREATED)
            ),
            Arguments.of(
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_UTILIZATION_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.OUT)
                    .setBoxStatus(ReturnBoxStatus.IN_TRANSIT)
                    .setReturnStatus(ReturnStatus.IN_TRANSIT)
            ),
            Arguments.of(
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_UTILIZATION_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.TRANSIT_PREPARED)
                    .setBoxStatus(ReturnBoxStatus.IN_TRANSIT)
                    .setReturnStatus(ReturnStatus.IN_TRANSIT)
            ),
            Arguments.of(
                new ReturnSegmentStatusChangingInfo()
                    .setReturnSegmentId(SC_LAST_MILE_TO_UTILIZATION_SEGMENT_ID)
                    .setSegmentStatus(ReturnSegmentStatus.CANCELLED)
                    .setBoxStatus(ReturnBoxStatus.CANCELLED)
                    .setReturnStatus(ReturnStatus.CANCELLED)
            )
        );
    }

    @MethodSource
    @ParameterizedTest
    @DisplayName("Запуск контрольной точки")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_created.xml")
    void startControlPoint(ControlPointType type, Instant expireAt) {
        updateControlPoint(controlPoint -> controlPoint.setType(type));

        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.TRANSIT_PREPARED);

        verifyControlPoint(ControlPointStatus.STARTED, expireAt);
    }

    @MethodSource("startControlPoint")
    @ParameterizedTest
    @DisplayName("Форсированный запуск контрольной точки")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_created.xml")
    void forceStartControlPoint(ControlPointType type, Instant expireAt) {
        when(featureProperties.isEnableControlPointStarting()).thenReturn(false);
        featureProperties.setAllowUtilizationFlowReturnIds(Set.of(3L));

        updateControlPoint(controlPoint -> controlPoint.setType(type));

        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.TRANSIT_PREPARED);

        verifyControlPoint(ControlPointStatus.STARTED, expireAt);

        featureProperties.setAllowUtilizationFlowReturnIds(Set.of(1L));
    }

    static Stream<Arguments> startControlPoint() {
        return Stream.of(
            Arguments.of(ControlPointType.SHORT_TERM_STORAGE, NOW.plus(5, ChronoUnit.DAYS)),
            Arguments.of(ControlPointType.LONG_TERM_STORAGE, NOW.plus(25, ChronoUnit.DAYS)),
            Arguments.of(ControlPointType.EXTRA_LONG_TERM_STORAGE, NOW.plus(30, ChronoUnit.DAYS))
        );
    }

    @Test
    @DisplayName("Не запускаем контрольную точку, т.к. тип UTILIZATION")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_created.xml")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/update-statuses/before/control_point_utilization.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/control_point_still_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notStartControlPointBecauseUtilization() {
        setSegmentStatusAndExecute(SC_LAST_MILE_TO_UTILIZATION_SEGMENT_ID, ReturnSegmentStatus.TRANSIT_PREPARED);
    }

    @Test
    @DisplayName("Не запускаем контрольную точку т.к. выключен фича-флаг")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_created.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/control_point_still_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notStartControlPointBecauseFlagIsOff() {
        when(featureProperties.isEnableControlPointStarting()).thenReturn(false);

        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.TRANSIT_PREPARED);
    }

    @Test
    @DisplayName("Не запускаем контрольную точку т.к. неверная лог точка")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_created.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/control_point_still_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notStartControlPointBecauseDifferentLogisticPoint() {
        updateControlPoint(controlPoint -> controlPoint.setLogisticPointId(200L));

        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.TRANSIT_PREPARED);

        verifyLog("ERROR", "Control point was not started because logisticPointId is 345 instead of 200");
    }

    @EnumSource(value = ControlPointStatus.class, names = "CREATED", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    @DisplayName("Не запускаем контрольную точку т.к. она не в статусе CREATED")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_created.xml")
    void notStartControlPointBecauseAlreadyStarted(ControlPointStatus status) {
        updateControlPoint(controlPoint -> controlPoint.setStatus(status, clock));

        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.TRANSIT_PREPARED);

        verifyControlPoint(status, null);
    }

    @Test
    @DisplayName("Не запускаем контрольную точку т.к. нет контрольной точки")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/no_control_points.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/before/no_control_points.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notStartControlPointBecauseNoControlPoint() {
        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.TRANSIT_PREPARED);

        verifyLog("WARN", "No controlPoint in status CREATED for return 3");
    }

    @EnumSource(value = ReturnSegmentStatus.class, names = "TRANSIT_PREPARED", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    @DisplayName("Не запускаем контрольную точку т.к. возврат не достиг нужного статуса")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_created.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/control_point_still_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notStartControlPointBecauseReturnStatusNotReached(ReturnSegmentStatus segmentStatus) {
        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, segmentStatus);
    }

    @Test
    @DisplayName("Не запускаем контрольную точку т.к. несколько точек в статусе CREATED")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_points_created.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/before/control_points_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notStartControlPointBecauseMultipleInStatusCreated() {
        softly.assertThatThrownBy(() ->
                setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.TRANSIT_PREPARED)
            )
            .hasMessage("Found multiple controlPoints with status CREATED for return 3");
    }

    @EnumSource(value = ControlPointType.class, names = "UTILIZATION", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    @DisplayName("Завершение контрольной точки")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_started.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/control_point_finished.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void finishControlPoint(ControlPointType controlPointType) {
        updateControlPoint(controlPoint -> controlPoint.setType(controlPointType));

        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.OUT);
    }

    @EnumSource(value = ControlPointType.class, names = "UTILIZATION", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    @DisplayName("Форсированное завершение контрольной точки")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_started.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/control_point_finished.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void forceFinishControlPoint(ControlPointType controlPointType) {
        when(featureProperties.isEnableControlPointFinishing()).thenReturn(false);
        featureProperties.setAllowUtilizationFlowReturnIds(Set.of(3L));

        updateControlPoint(controlPoint -> controlPoint.setType(controlPointType));

        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.OUT);

        featureProperties.setAllowUtilizationFlowReturnIds(Set.of(1L));
    }

    @Test
    @DisplayName("Не завершаем контрольную точку, т.к. тип UTILIZATION")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_started.xml")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/update-statuses/before/control_point_utilization.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/control_point_still_started.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void finishControlPointUtilization() {
        setSegmentStatusAndExecute(SC_LAST_MILE_TO_UTILIZATION_SEGMENT_ID, ReturnSegmentStatus.OUT);
    }

    @Test
    @DisplayName("Не завершаем контрольную точку, т.к. выключен фича-флаг")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_started.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/before/control_point_started.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void notFinishControlPointBecauseFlagIsOff() {
        when(featureProperties.isEnableControlPointFinishing()).thenReturn(false);

        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.OUT);
    }

    @EnumSource(value = ReturnSegmentStatus.class, names = "OUT", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    @DisplayName("Не завершаем контрольную точку т.к. возврат не достиг нужного статуса")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_started.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/before/control_point_started.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void notFinishControlPointBecauseReturnStatusNotReached(ReturnSegmentStatus segmentStatus) {
        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, segmentStatus);
    }

    @Test
    @DisplayName("Не завершаем контрольную точку т.к. выдали возврат не тому партнеру")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_started.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/after/control_point_still_started.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void notFinishControlPointBecauseWrongDestinationPartner() {
        updateControlPoint(controlPoint -> controlPoint.setToPartnerId(200));

        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.OUT);

        verifyLog(
            "ERROR",
            "Control point was not finished because delivered to wrong partner. Expected partnerId 200 but was 172"
        );
    }

    @Test
    @DisplayName("Не завершаем контрольную точку, т.к. нет контрольной точки")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/no_control_points.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/before/no_control_points.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notFinishControlPointBecauseNoControlPoint() {
        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.OUT);

        verifyLog("WARN", "No controlPoint in status STARTED for return 3");
    }

    @Test
    @DisplayName("Не завершаем контрольную точку т.к. несколько точек в статусе STARTED")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_points_started.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/update-statuses/before/control_points_started.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notFinishControlPointBecauseMultipleInStatusCreated() {
        softly.assertThatThrownBy(() ->
                setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.OUT)
            )
            .hasMessage("Found multiple controlPoints with status STARTED for return 3");
    }

    @EnumSource(value = ControlPointStatus.class, names = "STARTED", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    @DisplayName("Не завершаем контрольную точку, т.к. она не в статусе STARTED")
    @DatabaseSetup("/database/tasks/return-segment/update-statuses/before/control_point_created.xml")
    void notFinishControlPointBeacuseNotStarted(ControlPointStatus status) {
        updateControlPoint(controlPoint -> controlPoint.setStatus(status, clock));

        setSegmentStatusAndExecute(SC_LAST_MILE_TO_SHOP_SEGMENT_ID, ReturnSegmentStatus.OUT);

        verifyControlPoint(status, null);
    }

    private void verifyLog(String level, String payload) {
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=%s\t\
                    format=plain\t\
                    payload=%s\t\
                    request_id=test-request-id
                    """
                    .formatted(level, payload)
            );
    }

    private void setSegmentStatusAndExecute(Long segmentId, ReturnSegmentStatus segmentStatus) {
        transactionTemplate.execute(status -> {
            ReturnSegmentEntity returnSegment = returnSegmentRepository.findById(segmentId)
                .orElseThrow();
            return returnSegment.setStatus(segmentStatus, clock);
        });
        updateReturnStatusesProcessor.execute(updateReturnStatusesPayload(
            segmentId,
            segmentStatus
        ));
    }

    private void updateControlPoint(Consumer<ControlPoint> controlPointUpdater) {
        transactionTemplate.execute(status -> {
            ControlPoint controlPoint = controlPointRepository.findById(1L).orElseThrow();
            controlPointUpdater.accept(controlPoint);
            return null;
        });
    }

    private void verifyControlPoint(ControlPointStatus status, @Nullable Instant expireAt) {
        ControlPoint controlPoint = controlPointRepository.findById(1L).orElseThrow();
        softly.assertThat(controlPoint.getStatus()).isEqualTo(status);
        softly.assertThat(controlPoint.getExpireAt()).isEqualTo(expireAt);
    }

    private void updateStatuses(Long segmentId, ReturnSegmentStatus segmentStatus) {
        softly.assertThat(updateReturnStatusesProcessor.execute(updateReturnStatusesPayload(segmentId, segmentStatus)))
            .isEqualTo(TaskExecutionResult.finish());
    }

    @Nonnull
    private UpdateReturnStatusesPayload updateReturnStatusesPayload(
        Long segmentId,
        ReturnSegmentStatus segmentStatus
    ) {
        return UpdateReturnStatusesPayload.builder()
            .segmentId(segmentId)
            .segmentStatus(segmentStatus)
            .build();
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Accessors(chain = true)
    private static class ReturnSegmentStatusChangingInfo {
        long returnSegmentId;

        ReturnSegmentStatus segmentStatus;

        ReturnBoxStatus boxStatus;

        ReturnStatus returnStatus;

        //признак обновления статуса клиентского возврата в чекаутере
        boolean clientReturnStatusUpdate = true;

        //признак обновления статуса возврата при обновлении статуса сегмента
        boolean returnStatusChanges = true;
    }

}
