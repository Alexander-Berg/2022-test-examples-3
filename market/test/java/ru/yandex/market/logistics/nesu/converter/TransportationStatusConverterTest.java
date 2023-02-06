package ru.yandex.market.logistics.nesu.converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.model.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.model.dto.StatusHistoryInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationStatusHistoryInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationUnitDto;
import ru.yandex.market.delivery.transport_manager.model.enums.MovementStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationUnitStatus;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.client.model.error.PartnerShipmentConfirmationError.PartnerShipmentConfirmationErrorType;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatus;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.dto.partner.PartnerShipmentConfirmDto;
import ru.yandex.market.logistics.nesu.service.transport_manager.model.TransportationStatus;
import ru.yandex.market.logistics4shops.client.model.MdsFilePath;
import ru.yandex.market.logistics4shops.client.model.Outbound;

@DisplayName("Конвертация в статус отгрузки дропшипа")
@ParametersAreNonnullByDefault
public class TransportationStatusConverterTest extends AbstractTest {

    private static final Instant CHANGED_EARLIER = Instant.parse("2021-05-04T04:00:00Z");
    private static final Instant CHANGED = Instant.parse("2021-05-04T05:00:00Z");
    private static final Instant CHANGED_LATER = Instant.parse("2021-05-04T06:00:00Z");

    private static final Instant NOW = Instant.parse("2021-05-04T09:01:00Z");
    private static final Instant FUTURE = Instant.parse("2021-05-04T10:00:00Z");
    private static final ZoneOffset MSK_ZONE_OFFSET = ZoneOffset.of("+03:00");

    private final FeatureProperties featureProperties = new FeatureProperties();

    private TransportationStatusConverter transportationStatusConverter;

    @BeforeEach
    void setup() {
        TestableClock clock = new TestableClock();
        clock.setFixed(NOW, ZoneOffset.UTC);
        featureProperties.setEnableAcceptedTransportationStatuses(true);
        transportationStatusConverter = new TransportationStatusConverter(clock, featureProperties);
    }

    @AfterEach
    void tearDown() {
        featureProperties.setEnableAcceptedTransportationStatuses(true);
    }

    @DisplayName("Статус отгрузки")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({
        "convertToPartnerShipmentStatusInProgress",
        "convertToPartnerShipmentStatusFinished",
        "convertToPartnerShipmentStatusAccepted",
    })
    void convertToPartnerShipmentStatus(
        @SuppressWarnings("unused") String name,
        @Nullable PartnerShipmentStatus status,
        TransportationDto transportation,
        List<StatusHistoryInfoDto> history,
        @Nullable Outbound outbound,
        PartnerShipmentConfirmDto confirmDto,
        ZoneOffset outboundWarehouseZoneOffset
    ) {
        PartnerShipmentStatus partnerShipmentStatus = transportationStatusConverter.toStatus(
            transportation,
            history,
            outbound,
            confirmDto,
            outboundWarehouseZoneOffset
        );
        softly.assertThat(partnerShipmentStatus).isEqualTo(status);
    }

    @Nonnull
    private static Stream<Arguments> convertToPartnerShipmentStatusInProgress() {
        return Stream.of(
            Arguments.of(
                "Пустая история статусов",
                null,
                transportation(),
                List.of(),
                null,
                notReachedCutoffConfirmDto(),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Перемещение в статусе NEW",
                PartnerShipmentStatus.OUTBOUND_CREATED,
                transportation(),
                List.of(historyStatus("NEW", CHANGED)),
                outbound(null),
                notReachedCutoffConfirmDto(),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка запланирована",
                PartnerShipmentStatus.OUTBOUND_CREATED,
                transportation(),
                List.of(historyStatus("SCHEDULED", CHANGED)),
                outbound(null),
                notReachedCutoffConfirmDto(),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка создана",
                PartnerShipmentStatus.OUTBOUND_CREATED,
                transportation(),
                List.of(historyStatus("OUTBOUND_CREATED", CHANGED)),
                outbound(null),
                notReachedCutoffConfirmDto(),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка создана, доступно подтверждение отгрузки",
                PartnerShipmentStatus.OUTBOUND_READY_FOR_CONFIRMATION,
                transportation(),
                List.of(historyStatus("OUTBOUND_CREATED", CHANGED)),
                outbound(null),
                reachedCutoffConfirmDto(CHANGED_LATER),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка создана, подтверждение отгрузки недоступно из-за отсутствия заказов",
                PartnerShipmentStatus.OUTBOUND_CREATED,
                transportation(),
                List.of(historyStatus("OUTBOUND_CREATED", CHANGED)),
                outbound(null),
                PartnerShipmentConfirmDto.builder()
                    .confirmAvailableTime(OffsetDateTime.ofInstant(CHANGED_LATER, ZoneOffset.UTC))
                    .outboundPlanCount(0)
                    .build(),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка подтверждена",
                PartnerShipmentStatus.OUTBOUND_CONFIRMED,
                transportation(),
                List.of(),
                outbound(CHANGED),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Ошибка процессинга перемещения",
                PartnerShipmentStatus.ERROR,
                transportation(),
                List.of(historyStatus("ERROR", CHANGED)),
                outbound(null),
                notReachedCutoffConfirmDto(),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Ошибка во время проверки transportation-а",
                PartnerShipmentStatus.ERROR,
                transportation(),
                List.of(historyStatus("CHECK_FAILED", CHANGED)),
                outbound(null),
                notReachedCutoffConfirmDto(),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Несколько статусов в истории",
                PartnerShipmentStatus.OUTBOUND_CREATED,
                transportation(),
                List.of(
                    historyStatus("SCHEDULED", CHANGED),
                    historyStatus("OUTBOUND_CREATED", CHANGED_LATER)
                ),
                outbound(null),
                notReachedCutoffConfirmDto(),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Есть история статусов и отгрузка подтверждена",
                PartnerShipmentStatus.OUTBOUND_CONFIRMED,
                transportation(),
                List.of(historyStatus("OUTBOUND_CREATED", CHANGED)),
                outbound(CHANGED_LATER),
                reachedCutoffConfirmDto(CHANGED),
                ZoneOffset.UTC
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> convertToPartnerShipmentStatusFinished() {
        return Stream.of(
            Arguments.of(
                "Таймаут отгрузки",
                PartnerShipmentStatus.FINISHED,
                transportation(),
                List.of(historyStatus("OUTBOUND_CREATED", CHANGED)),
                outbound(null),
                reachedCutoffConfirmDto(CHANGED_LATER),
                MSK_ZONE_OFFSET
            ),
            Arguments.of(
                "Отгрузка подтверждена после завершения",
                PartnerShipmentStatus.FINISHED,
                transportation(),
                List.of(historyStatus("DEPARTED", CHANGED)),
                outbound(CHANGED_LATER),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Завершено без сверки реестров",
                PartnerShipmentStatus.FINISHED,
                transportation(),
                List.of(historyStatus("COULD_NOT_BE_MATCHED", CHANGED)),
                outbound(null),
                notReachedCutoffConfirmDto(),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка завершена, так как inbound unit в процессе, и отгрузка подтверждена",
                PartnerShipmentStatus.FINISHED,
                transportation(MovementStatus.NEW, TransportationUnitStatus.IN_PROGRESS),
                List.of(historyStatus("OUTBOUND_CREATED", CHANGED)),
                outbound(CHANGED_LATER),
                reachedCutoffConfirmDto(CHANGED_LATER),
                ZoneOffset.UTC
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> convertToPartnerShipmentStatusAccepted() {
        return Stream.of(
            Arguments.of(
                "Отгрузка принята без расхождений",
                PartnerShipmentStatus.ACCEPTED,
                transportation(),
                List.of(),
                outbound(null).discrepancyActIsReady(true),
                reachedCutoffConfirmDto(CHANGED_LATER),
                MSK_ZONE_OFFSET
            ),
            Arguments.of(
                "Отгрузка принята с расхождениями",
                PartnerShipmentStatus.ACCEPTED_WITH_DISCREPANCIES,
                transportation(),
                List.of(),
                outbound(null)
                    .discrepancyActIsReady(true)
                    .discrepancyActPath(new MdsFilePath().bucket("bucket").filename("filename")),
                reachedCutoffConfirmDto(CHANGED_LATER),
                MSK_ZONE_OFFSET
            )
        );
    }


    @DisplayName("Статус отгрузки - передача статуса 'Принят' запрещена")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void convertToPartnerShipmentStatusAcceptedRestricted(
        @SuppressWarnings("unused") String name,
        @Nullable PartnerShipmentStatus status,
        TransportationDto transportation,
        List<StatusHistoryInfoDto> history,
        @Nullable Outbound outbound,
        PartnerShipmentConfirmDto confirmDto,
        ZoneOffset outboundWarehouseZoneOffset
    ) {
        featureProperties.setEnableAcceptedTransportationStatuses(false);
        PartnerShipmentStatus partnerShipmentStatus = transportationStatusConverter.toStatus(
            transportation,
            history,
            outbound,
            confirmDto,
            outboundWarehouseZoneOffset
        );
        softly.assertThat(partnerShipmentStatus).isEqualTo(status);
    }


    @Nonnull
    private static Stream<Arguments> convertToPartnerShipmentStatusAcceptedRestricted() {
        return Stream.of(
            Arguments.of(
                "Отгрузка принята без расхождений",
                PartnerShipmentStatus.FINISHED,
                transportation(),
                List.of(),
                outbound(null).discrepancyActIsReady(true),
                reachedCutoffConfirmDto(CHANGED_LATER),
                MSK_ZONE_OFFSET
            ),
            Arguments.of(
                "Отгрузка принята с расхождениями",
                PartnerShipmentStatus.FINISHED,
                transportation(),
                List.of(),
                outbound(null)
                    .discrepancyActIsReady(true)
                    .discrepancyActPath(new MdsFilePath().bucket("bucket").filename("filename")),
                reachedCutoffConfirmDto(CHANGED_LATER),
                MSK_ZONE_OFFSET
            )
        );
    }

    @DisplayName("Статус и время отгрузки")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({
        "convertToTransportationStatusInProgress",
        "convertToTransportationStatusFinished",
        "convertToTransportationStatusAccepted",
    })
    void convertToTransportationStatus(
        @SuppressWarnings("unused") String name,
        TransportationStatus expectedTransportationStatus,
        TransportationDto transportation,
        TransportationStatusHistoryInfoDto history,
        @Nullable Outbound outbound,
        PartnerShipmentConfirmDto confirmDto,
        ZoneOffset outboundWarehouseZoneOffset
    ) {
        TransportationStatus transportationStatus = transportationStatusConverter.toStatus(
            transportation,
            history,
            outbound,
            confirmDto,
            outboundWarehouseZoneOffset
        );
        softly.assertThat(transportationStatus).isEqualTo(expectedTransportationStatus);
    }

    @Nonnull
    private static Stream<Arguments> convertToTransportationStatusInProgress() {
        return Stream.of(
            Arguments.of(
                "История не найдена",
                new TransportationStatus(null, null),
                transportation(),
                null,
                null,
                notReachedCutoffConfirmDto(),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка создана",
                new TransportationStatus(CHANGED, PartnerShipmentStatus.OUTBOUND_CREATED),
                transportation(),
                history(List.of(historyStatus("OUTBOUND_CREATED", CHANGED))),
                outbound(null),
                notReachedCutoffConfirmDto(),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка создана, доступно подтверждение отгрузки",
                new TransportationStatus(CHANGED, PartnerShipmentStatus.OUTBOUND_READY_FOR_CONFIRMATION),
                transportation(),
                history(List.of(historyStatus("OUTBOUND_CREATED", CHANGED_EARLIER))),
                outbound(null),
                reachedCutoffConfirmDto(CHANGED),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка создана, подтверждение отгрузки недоступно из-за отсутствия заказов",
                new TransportationStatus(CHANGED_EARLIER, PartnerShipmentStatus.OUTBOUND_CREATED),
                transportation(),
                history(List.of(historyStatus("OUTBOUND_CREATED", CHANGED_EARLIER))),
                outbound(null),
                PartnerShipmentConfirmDto.builder()
                    .confirmAvailableTime(OffsetDateTime.ofInstant(CHANGED_LATER, ZoneOffset.UTC))
                    .outboundPlanCount(0)
                    .build(),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка подтверждена",
                new TransportationStatus(CHANGED_LATER, PartnerShipmentStatus.OUTBOUND_CONFIRMED),
                transportation(),
                history(List.of(historyStatus("OUTBOUND_CREATED", CHANGED_EARLIER))),
                outbound(CHANGED_LATER),
                reachedCutoffConfirmDto(CHANGED),
                ZoneOffset.UTC
            )
        );
    }

    private static Stream<Arguments> convertToTransportationStatusFinished() {
        return Stream.of(
            Arguments.of(
                "Таймаут отгрузки",
                new TransportationStatus(
                    LocalDateTime.ofInstant(Instant.parse("2021-05-04T12:00:00Z"), ZoneOffset.UTC)
                        .toInstant(MSK_ZONE_OFFSET),
                    PartnerShipmentStatus.FINISHED
                ),
                transportation(),
                history(List.of(historyStatus("OUTBOUND_CREATED", CHANGED))),
                outbound(null),
                reachedCutoffConfirmDto(CHANGED),
                MSK_ZONE_OFFSET
            ),
            Arguments.of(
                "Отгрузка подтверждена после завершения",
                new TransportationStatus(CHANGED, PartnerShipmentStatus.FINISHED),
                transportation(),
                history(List.of(historyStatus("DEPARTED", CHANGED))),
                outbound(CHANGED_LATER),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Несколько статусов отвечающих за завершенность отгрузки: DEPARTED после movement.IN_PROGRESS",
                new TransportationStatus(CHANGED, PartnerShipmentStatus.FINISHED),
                transportation(MovementStatus.IN_PROGRESS, TransportationUnitStatus.NEW),
                history(
                    List.of(historyStatus("DEPARTED", CHANGED_LATER)),
                    List.of(historyStatus("IN_PROGRESS", CHANGED)),
                    List.of()
                ),
                outbound(CHANGED_LATER),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Несколько статусов отвечающих за завершенность отгрузки: несколько статусов в истории movement-а",
                new TransportationStatus(CHANGED, PartnerShipmentStatus.FINISHED),
                transportation(MovementStatus.ON_INBOUND_POINT, TransportationUnitStatus.NEW),
                history(
                    List.of(),
                    List.of(
                        historyStatus("NEW", CHANGED_EARLIER),
                        historyStatus("IN_PROGRESS", CHANGED),
                        historyStatus("ON_INBOUND_POINT", CHANGED_LATER)
                    ),
                    List.of()
                ),
                outbound(CHANGED),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> convertToTransportationStatusAccepted() {
        return Stream.of(
            Arguments.of(
                "Отгрузка принята без расхождений, время достаётся из истории inbound-a",
                new TransportationStatus(CHANGED_EARLIER, PartnerShipmentStatus.ACCEPTED),
                transportation(),
                history(
                    List.of(),
                    List.of(historyStatus("IN_PROGRESS", CHANGED)),
                    List.of(historyStatus("PROCESSED", CHANGED_EARLIER))
                ),
                outbound(CHANGED).discrepancyActIsReady(true),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка принята без расхождений, время достаётся из общей истории",
                new TransportationStatus(CHANGED, PartnerShipmentStatus.ACCEPTED),
                transportation(),
                history(
                    List.of(),
                    List.of(historyStatus("IN_PROGRESS", CHANGED)),
                    List.of(historyStatus("IN_PROGRESS", CHANGED_EARLIER))
                ),
                outbound(CHANGED).discrepancyActIsReady(true),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка принята с расхождениями, время достаётся из истории inbound-a",
                new TransportationStatus(CHANGED_EARLIER, PartnerShipmentStatus.ACCEPTED_WITH_DISCREPANCIES),
                transportation(),
                history(
                    List.of(),
                    List.of(historyStatus("IN_PROGRESS", CHANGED)),
                    List.of(historyStatus("PROCESSED", CHANGED_EARLIER))
                ),
                outbound(CHANGED)
                    .discrepancyActIsReady(true)
                    .discrepancyActPath(new MdsFilePath().bucket("bucket").filename("filename")),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка принята c расхождениями, время достаётся из общей истории",
                new TransportationStatus(CHANGED, PartnerShipmentStatus.ACCEPTED_WITH_DISCREPANCIES),
                transportation(),
                history(
                    List.of(),
                    List.of(historyStatus("IN_PROGRESS", CHANGED)),
                    List.of(historyStatus("IN_PROGRESS", CHANGED_EARLIER))
                ),
                outbound(CHANGED)
                    .discrepancyActIsReady(true)
                    .discrepancyActPath(new MdsFilePath().bucket("bucket").filename("filename")),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            )
        );
    }

    @DisplayName("Статус и время отгрузки")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void convertToTransportationStatusAcceptedRestricted(
        @SuppressWarnings("unused") String name,
        TransportationStatus expectedTransportationStatus,
        TransportationDto transportation,
        TransportationStatusHistoryInfoDto history,
        @Nullable Outbound outbound,
        PartnerShipmentConfirmDto confirmDto,
        ZoneOffset outboundWarehouseZoneOffset
    ) {
        featureProperties.setEnableAcceptedTransportationStatuses(false);
        TransportationStatus transportationStatus = transportationStatusConverter.toStatus(
            transportation,
            history,
            outbound,
            confirmDto,
            outboundWarehouseZoneOffset
        );
        softly.assertThat(transportationStatus).isEqualTo(expectedTransportationStatus);
    }


    @Nonnull
    private static Stream<Arguments> convertToTransportationStatusAcceptedRestricted() {
        return Stream.of(
            Arguments.of(
                "Отгрузка принята без расхождений, время достаётся из истории inbound-a",
                new TransportationStatus(CHANGED, PartnerShipmentStatus.FINISHED),
                transportation(),
                history(
                    List.of(),
                    List.of(historyStatus("IN_PROGRESS", CHANGED)),
                    List.of(historyStatus("PROCESSED", CHANGED_EARLIER))
                ),
                outbound(CHANGED).discrepancyActIsReady(true),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка принята без расхождений, время достаётся из общей истории",
                new TransportationStatus(CHANGED_EARLIER, PartnerShipmentStatus.FINISHED),
                transportation(),
                history(
                    List.of(),
                    List.of(historyStatus("IN_PROGRESS", CHANGED)),
                    List.of(historyStatus("IN_PROGRESS", CHANGED_EARLIER))
                ),
                outbound(CHANGED).discrepancyActIsReady(true),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка принята с расхождениями, время достаётся из истории inbound-a",
                new TransportationStatus(CHANGED, PartnerShipmentStatus.FINISHED),
                transportation(),
                history(
                    List.of(),
                    List.of(historyStatus("IN_PROGRESS", CHANGED)),
                    List.of(historyStatus("PROCESSED", CHANGED_EARLIER))
                ),
                outbound(CHANGED)
                    .discrepancyActIsReady(true)
                    .discrepancyActPath(new MdsFilePath().bucket("bucket").filename("filename")),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            ),
            Arguments.of(
                "Отгрузка принята c расхождениями, время достаётся из общей истории",
                new TransportationStatus(CHANGED_EARLIER, PartnerShipmentStatus.FINISHED),
                transportation(),
                history(
                    List.of(),
                    List.of(historyStatus("IN_PROGRESS", CHANGED)),
                    List.of(historyStatus("IN_PROGRESS", CHANGED_EARLIER))
                ),
                outbound(CHANGED)
                    .discrepancyActIsReady(true)
                    .discrepancyActPath(new MdsFilePath().bucket("bucket").filename("filename")),
                reachedCutoffConfirmDto(CHANGED_EARLIER),
                ZoneOffset.UTC
            )
        );
    }

    @Nonnull
    private static TransportationDto transportation() {
        return transportation(MovementStatus.DRAFT, TransportationUnitStatus.NEW);
    }

    @Nonnull
    private static TransportationDto transportation(
        MovementStatus movementStatus,
        TransportationUnitStatus inboundStatus
    ) {
        return new TransportationDto()
            .setOutbound(
                TransportationUnitDto.builder()
                    .plannedIntervalStart(
                        LocalDateTime.ofInstant(CHANGED_EARLIER, ZoneId.ofOffset("UTC", ZoneOffset.UTC))
                    )
                    .plannedIntervalEnd(
                        LocalDateTime.ofInstant(CHANGED_LATER, ZoneId.ofOffset("UTC", ZoneOffset.UTC))
                    )
                    .build()
            )
            .setMovement(MovementDto.builder().status(movementStatus).build())
            .setInbound(TransportationUnitDto.builder().status(inboundStatus).build());
    }

    @Nonnull
    private static TransportationStatusHistoryInfoDto history(List<StatusHistoryInfoDto> transportationStatusHistory) {
        return history(transportationStatusHistory, List.of(), List.of());
    }

    @Nonnull
    private static TransportationStatusHistoryInfoDto history(
        List<StatusHistoryInfoDto> transportationStatusHistory,
        List<StatusHistoryInfoDto> movementStatusHistory,
        List<StatusHistoryInfoDto> inboundStatusHistory
    ) {
        return new TransportationStatusHistoryInfoDto()
            .setStatusHistoryList(transportationStatusHistory)
            .setMovementStatusHistoryList(movementStatusHistory)
            .setInboundStatusHistoryList(inboundStatusHistory);
    }

    @Nonnull
    private static StatusHistoryInfoDto historyStatus(String status, Instant changedAt) {
        return new StatusHistoryInfoDto()
            .setNewStatus(status)
            .setChangedAt(changedAt);
    }

    @Nonnull
    private static Outbound outbound(@Nullable Instant confirmed) {
        return new Outbound().confirmed(confirmed);
    }

    @Nonnull
    private static PartnerShipmentConfirmDto notReachedCutoffConfirmDto() {
        return PartnerShipmentConfirmDto.builder()
            .confirmAvailableTime(OffsetDateTime.ofInstant(FUTURE, ZoneOffset.UTC))
            .confirmDisabledErrorType(PartnerShipmentConfirmationErrorType.CUTOFF_NOT_REACHED)
            .build();
    }

    @Nonnull
    private static PartnerShipmentConfirmDto reachedCutoffConfirmDto(Instant confirmAvailableTime) {
        return PartnerShipmentConfirmDto.builder()
            .confirmAvailableTime(OffsetDateTime.ofInstant(confirmAvailableTime, ZoneOffset.UTC))
            .outboundPlanCount(1)
            .build();
    }
}
