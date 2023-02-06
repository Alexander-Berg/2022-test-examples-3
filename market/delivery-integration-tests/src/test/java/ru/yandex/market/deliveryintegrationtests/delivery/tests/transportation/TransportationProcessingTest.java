package ru.yandex.market.deliveryintegrationtests.delivery.tests.transportation;

import java.time.LocalDate;

import dto.responses.tm.TmCheckpointStatus;
import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationStatus;

@Slf4j
@Epic("TM")
@DisplayName("TM Test")
public class TransportationProcessingTest extends AbstractTransportationTest {

    /**
     * Константы с набором данных ниже нужны только в этом тесте, уносить их наружу нет смысла
     **/
    private static final long OUTBOUND_PARTNER_ID = 48099;
    private static final long INBOUND_SC_PARTNER_ID = 100136;

    private static final long ANOTHER_OUTBOUND_DROPSHIP_PARTNER_ID = 49363;
    private static final long INBOUND_DROPOFF_PARTNER_ID = 1005555;

    private static final String ARRIVED_STATUS = "ARRIVED";

    @Test
    @DisplayName("ТМ: Старт перемещения")
    void startTransportationTest() {
        log.info("Start Transportation...");

        long transportationId = TM_STEPS.getTransportationIdForDay(
            OUTBOUND_PARTNER_ID,
            INBOUND_SC_PARTNER_ID,
            LocalDate.now().plusDays(2),
            TransportationStatus.SCHEDULED
        );
        TM_STEPS.startTransportation(transportationId);
        TM_STEPS.getMovementExternalId(transportationId);
        long movementTrackerId =
            DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(TM_STEPS.getMovementIdWithPrefix(transportationId)).getId();
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            movementTrackerId,
            TmCheckpointStatus.MOVEMENT_COURIER_FOUND,
            EntityType.MOVEMENT
        );
        Long movementId = TM_STEPS.getMovementId(transportationId);
        TM_STEPS.verifyCourierInMovement(movementId, "Саддам", "Хусейн", "У145АТ", "+70000000713");
        TM_STEPS.getOutboundExternalId(transportationId);
        long outboundTrackerId = DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(
                TM_STEPS.getOutboundIdWithPrefix(transportationId),
                EntityType.OUTBOUND.getId()
            )
            .getId();
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            outboundTrackerId,
            TmCheckpointStatus.OUTBOUND_TRANSFERRED,
            EntityType.OUTBOUND
        );
        TM_STEPS.verifyOutboundStatus(transportationId, "PROCESSED");
        TM_STEPS.verifyOutboundFactRegister(transportationId, 128, 2);
    }

    @Test
    @DisplayName("ТМ: Приемка от дропшипа на дропоффе, фиксируется время приезда")
    void getTransportationTest() {
        log.info("Start Transportation...");

        long transportationId = TM_STEPS.getTransportationIdForDay(
            ANOTHER_OUTBOUND_DROPSHIP_PARTNER_ID,
            INBOUND_DROPOFF_PARTNER_ID,
            LocalDate.now().plusDays(3),
            TransportationStatus.SCHEDULED
        );
        TM_STEPS.startTransportation(transportationId);
        TM_STEPS.getMovementExternalId(transportationId);
        long movementTrackerId =
            DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(TM_STEPS.getMovementIdWithPrefix(transportationId)).getId();
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            movementTrackerId,
            TmCheckpointStatus.MOVEMENT_COURIER_FOUND,
            EntityType.MOVEMENT
        );
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            movementTrackerId,
            TmCheckpointStatus.MOVEMENT_OUTBOUND_WAREHOUSE_REACHED,
            EntityType.MOVEMENT
        );
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            movementTrackerId,
            TmCheckpointStatus.MOVEMENT_DELIVERING,
            EntityType.MOVEMENT
        );
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            movementTrackerId,
            TmCheckpointStatus.MOVEMENT_INBOUND_WAREHOUSE_REACHED,
            EntityType.MOVEMENT
        );
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            movementTrackerId,
            TmCheckpointStatus.MOVEMENT_DELIVERED,
            EntityType.MOVEMENT
        );
        TM_STEPS.getMovementId(transportationId);
        TM_STEPS.getOutboundExternalId(transportationId);
        long outboundTrackerId = DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(
                TM_STEPS.getOutboundIdWithPrefix(transportationId),
                EntityType.OUTBOUND.getId()
            )
            .getId();
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            outboundTrackerId,
            TmCheckpointStatus.OUTBOUND_TRANSFERRED,
            EntityType.OUTBOUND
        );
        TM_STEPS.verifyOutboundStatus(transportationId, "PROCESSED");

        long inboundTrackerId = DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(
                TM_STEPS.getInboundIdWithPrefix(transportationId),
                EntityType.INBOUND.getId()
            )
            .getId();

        TM_STEPS.verifyInboundStatus(transportationId, "ACCEPTED");

        TM_STEPS.verifyInboundArrivalTime(
            ARRIVED_STATUS,
            EntityType.INBOUND.name().toLowerCase(),
            transportationId,
            Assertions::assertNull
        );

        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            inboundTrackerId,
            TmCheckpointStatus.INBOUND_ARRIVED,
            EntityType.INBOUND
        );

        TM_STEPS.verifyInboundArrivalTime(
            ARRIVED_STATUS,
            EntityType.INBOUND.name().toLowerCase(),
            transportationId,
            Assertions::assertNotNull
        );
    }
}
