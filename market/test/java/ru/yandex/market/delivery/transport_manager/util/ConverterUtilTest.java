package ru.yandex.market.delivery.transport_manager.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.transport_manager.domain.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.TransportationUnitDto;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Schedule;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationAdditionalData;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationConfig;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationRoutingConfig;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.ConfigTransportationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.DimensionsClass;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

public class ConverterUtilTest {
    @Test
    void convertMovementFromDtoTest() {
        String externalId = "externalId";
        MovementStatus status = MovementStatus.NEW;
        Integer weight = 5;
        Integer volume = 7;

        MovementDto movementDto = new MovementDto()
            .setExternalId(externalId)
            .setStatus(status)
            .setWeight(weight)
            .setVolume(volume);

        Movement expected = new Movement()
            .setExternalId(externalId)
            .setStatus(MovementStatus.NEW)
            .setWeight(weight)
            .setVolume(volume);

        Movement movement = ConverterUtil.convertMovementFromDto(movementDto);
        assertThat(movement).isEqualTo(expected);
    }

    @Test
    void initTransportationTest() {
        TransportationUnit inboundUnitId = new TransportationUnit().setId(9L);
        TransportationUnit outboundUnitId = new TransportationUnit().setId(19L);
        Movement movementId = new Movement().setId(46L);
        String hash = "hash";

        Transportation expected = new Transportation()
            .setInboundUnit(inboundUnitId)
            .setOutboundUnit(outboundUnitId)
            .setMovement(movementId)
            .setStatus(TransportationStatus.NEW)
            .setHash(hash)
            .setTransportationSource(TransportationSource.LMS_TM_MOVEMENT)
            .setTransportationType(TransportationType.ORDERS_OPERATION)
            .setRegular(true)
            .setTargetPartnerId(172L)
            .setTargetLogisticsPointId(10000004403L)
            .setMovementSegmentId(100L)
            .setAdditionalData(new TransportationAdditionalData(new TransportationRoutingConfig(
                true, DimensionsClass.MEDIUM_SIZE_CARGO, 1.1D, false, "DEFAULT"
            )));

        Transportation transportation = ConverterUtil.initTransportation(
            inboundUnitId,
            outboundUnitId,
            movementId,
            hash,
            TransportationStatus.NEW,
            TransportationSource.LMS_TM_MOVEMENT,
            TransportationType.ORDERS_OPERATION,
            null,
            null,
            true,
            172L,
            10000004403L,
            100L,
            new TransportationAdditionalData(new TransportationRoutingConfig(
                true, DimensionsClass.MEDIUM_SIZE_CARGO, 1.1D, false, "DEFAULT"
            ))
        );

        assertThat(transportation).isEqualTo(expected);
    }

    @Test
    void convertFromDtoTest() {
        String externalId = "externalId";
        long requestId = 123L;
        TransportationUnitStatus status = TransportationUnitStatus.NEW;
        TransportationUnitType type = TransportationUnitType.INBOUND;
        Long logisticPointId = 10L;
        Long partnerId = 15L;
        LocalDateTime plannedIntervalStart = now();
        LocalDateTime plannedIntervalEnd = now().plus(2, ChronoUnit.HOURS);
        LocalDateTime actualDateTime = now();

        TransportationUnitDto transportationUnitDto = new TransportationUnitDto()
            .setExternalId(externalId)
            .setRequestId(requestId)
            .setStatus(status)
            .setType(type)
            .setLogisticPointId(logisticPointId)
            .setPartnerId(partnerId)
            .setPlannedIntervalStart(plannedIntervalStart)
            .setPlannedIntervalEnd(plannedIntervalEnd)
            .setActualDateTime(actualDateTime);

        TransportationUnit expected = new TransportationUnit()
            .setExternalId(externalId)
            .setRequestId(requestId)
            .setStatus(TransportationUnitStatus.NEW)
            .setType(type)
            .setLogisticPointId(logisticPointId)
            .setPartnerId(partnerId)
            .setPlannedIntervalStart(plannedIntervalStart)
            .setPlannedIntervalEnd(plannedIntervalEnd)
            .setActualDateTime(actualDateTime);

        assertThat(ConverterUtil.convertTransportationUnitFromDto(transportationUnitDto))
            .isEqualTo(expected);
    }

    @Test
    void convertFirstSegmentFromConfigTest() {

        TransportationUnitType type = TransportationUnitType.OUTBOUND;
        Long logisticPointId = 10L;
        Long partnerId = 15L;
        // ближайший вторник
        LocalDateTime plannedIntervalStart = LocalDateTime.of(2020, 8, 25, 10, 0, 0);
        LocalDateTime plannedIntervalEnd = LocalDateTime.of(2020, 8, 25, 15, 0, 0);

        // 24 августа, понедельник
        TestableClock clock = new TestableClock();
        clock.setFixed(
            LocalDateTime.of(2020, 8, 24, 15, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
        );

        List<TransportationUnitDto> expected = List.of(new TransportationUnitDto()
            .setType(type)
            .setLogisticPointId(logisticPointId)
            .setPartnerId(partnerId)
            .setPlannedIntervalStart(plannedIntervalStart)
            .setPlannedIntervalEnd(plannedIntervalEnd)
            .setActualDateTime(null));

        TransportationConfig config = new TransportationConfig()
            .setTransportationSchedule(List.of(
                new Schedule().setDay(2).setTimeFrom(LocalTime.of(10, 0, 0)).setTimeTo(LocalTime.of(15, 0, 0))
            ))
            .setTransportationType(ConfigTransportationType.ORDERS_OPERATION)
            .setOutboundLogisticPointId(10L)
            .setOutboundPartnerId(15L);

        List<TransportationUnitDto> outboundDto = ConverterUtil.convertOutbound(config, LocalDateTime.now(clock), 7);
        assertThat(outboundDto).isEqualTo(expected);
    }

    @Test
    void convertFirstSegmentFromConfigFor3PLTest() {

        TransportationUnitType type = TransportationUnitType.OUTBOUND;
        Long logisticPointId = 10L;
        Long partnerId = 15L;
        Long movingPartnerId = 9001L;
        // ближайший вторник
        LocalDateTime plannedIntervalStart = LocalDateTime.of(2020, 8, 25, 17, 0, 0);
        LocalDateTime plannedIntervalEnd = LocalDateTime.of(2020, 8, 25, 18, 0, 0);

        // 24 августа, понедельник
        TestableClock clock = new TestableClock();
        clock.setFixed(
                LocalDateTime.of(2020, 8, 24, 15, 0, 0).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
        );

        int duration = 360;
        List<TransportationUnitDto> expected = List.of(new TransportationUnitDto()
                .setType(type)
                .setLogisticPointId(logisticPointId)
                .setPartnerId(partnerId)
                .setPlannedIntervalStart(plannedIntervalStart)
                .setPlannedIntervalEnd(plannedIntervalEnd)
                .setActualDateTime(null));

        TransportationConfig config = new TransportationConfig()
                .setTransportationSchedule(List.of(
                        new Schedule().setDay(2)
                                .setTimeFrom(LocalTime.of(17, 0, 0))
                                .setTimeTo(LocalTime.of(18, 0, 0))
                ))
                .setTransportationType(ConfigTransportationType.ORDERS_OPERATION)
                .setOutboundLogisticPointId(10L)
                .setInboundPartnerId(16L)
                .setOutboundPartnerId(15L)
                .setDuration(duration)
                .setMovingPartnerId(movingPartnerId);

        List<TransportationUnitDto> outboundDto = ConverterUtil.convertOutbound(config, LocalDateTime.now(clock), 7);
        assertThat(outboundDto).isEqualTo(expected);
    }

    @Test
    void convertMovementFromConfigTest() {

        TestableClock clock = new TestableClock();
        clock.setFixed(
            LocalDateTime.of(2021, 3, 16, 15, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
        );

        long partnerId = 15L;
        int weight = 10;
        int volume = 100;
        int durationInMinutes = 600;

        // ближайшая пятница + 10 часов на duration
        LocalDateTime plannedIntervalStart = LocalDateTime.of(2021, 3, 19, 12, 20, 0);
        LocalDateTime plannedIntervalEnd = LocalDateTime.of(2021, 3, 19, 20, 30, 0);

        List<MovementDto> expected = List.of(
            new MovementDto()
                .setVolume(volume)
                .setWeight(weight)
                .setPartnerId(partnerId)
                .setPlannedIntervalStart(plannedIntervalStart)
                .setPlannedIntervalEnd(plannedIntervalEnd)
        );

        TransportationConfig config = new TransportationConfig()
            .setDuration(durationInMinutes)
            .setInboundPartnerId(partnerId)
            .setMovingPartnerId(partnerId)
            .setVolume(volume)
            .setWeight(weight)
            .setTransportationType(ConfigTransportationType.ORDERS_OPERATION)
            .setTransportationSchedule(List.of(
                new Schedule().setDay(5).setTimeFrom(LocalTime.of(10, 30, 0)).setTimeTo(LocalTime.of(12, 20, 0))
            ));

        List<MovementDto> movementDto = ConverterUtil.convertMovement(config, LocalDateTime.now(clock), 7, false);
        assertThat(movementDto).isEqualTo(expected);
    }

    @Test
    void convertMovementFor3PLFromConfigTest() {

        TestableClock clock = new TestableClock();
        clock.setFixed(
                LocalDateTime.of(2021, 3, 16, 15, 0, 0).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
        );

        long partnerId = 15L;
        long movingPartnerId = 9001L;
        int weight = 10;
        int volume = 100;
        int durationInMinutes = 360;

        // ближайшая пятница + 10 часов на duration
        LocalDateTime plannedIntervalStart = LocalDateTime.of(2021, 3, 19, 18, 0, 0);
        LocalDateTime plannedIntervalEnd = LocalDateTime.of(2021, 3, 19, 23, 0, 0);

        List<MovementDto> expected = List.of(
                new MovementDto()
                        .setVolume(volume)
                        .setWeight(weight)
                        .setPartnerId(movingPartnerId)
                        .setPlannedIntervalStart(plannedIntervalStart)
                        .setPlannedIntervalEnd(plannedIntervalEnd)
        );

        TransportationConfig config = new TransportationConfig()
                .setDuration(durationInMinutes)
                .setInboundPartnerId(partnerId)
                .setMovingPartnerId(movingPartnerId)
                .setVolume(volume)
                .setWeight(weight)
                .setTransportationType(ConfigTransportationType.ORDERS_OPERATION)
                .setTransportationSchedule(List.of(
                        new Schedule().setDay(5).setTimeFrom(LocalTime.of(17, 0, 0)).setTimeTo(LocalTime.of(18, 0, 0))
                ));

        List<MovementDto> movementDto = ConverterUtil.convertMovement(config, LocalDateTime.now(clock), 7, false);
        assertThat(movementDto).isEqualTo(expected);
    }

    @Test
    void convertLastSegmentFromConfigTest() {

        // ближайший вторник + сутки и 1 час на duration
        LocalDateTime plannedIntervalStart = LocalDateTime.of(2020, 8, 26, 11, 0, 0);
        LocalDateTime plannedIntervalEnd = LocalDateTime.of(2020, 8, 26, 16, 0, 0);

        // 24 августа, понедельник
        TestableClock clock = new TestableClock();
        clock.setFixed(
            LocalDateTime.of(2020, 8, 24, 15, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
        );

        TransportationConfig config = new TransportationConfig()
            .setDuration(1500)
            .setMovingPartnerId(15L)
            .setInboundLogisticPointId(10L)
            .setInboundPartnerId(15L)
            .setTransportationType(ConfigTransportationType.ORDERS_OPERATION)
            .setTransportationSchedule(List.of(
                new Schedule().setDay(2).setTimeFrom(LocalTime.of(10, 0, 0)).setTimeTo(LocalTime.of(15, 0, 0))
            ));

        TransportationUnitType type = TransportationUnitType.INBOUND;
        Long logisticPointId = 10L;
        Long partnerId = 15L;

        List<TransportationUnitDto> expected = List.of(new TransportationUnitDto()
            .setType(type)
            .setLogisticPointId(logisticPointId)
            .setPlannedIntervalStart(plannedIntervalStart)
            .setPlannedIntervalEnd(plannedIntervalEnd)
            .setPartnerId(partnerId));

        List<TransportationUnitDto> outboundDto = ConverterUtil.convertInbound(config, LocalDateTime.now(clock), 7);
        assertThat(outboundDto).isEqualTo(expected);
    }

    @Test
    void convertLastSegmentFromConfigFor3plTest() {

        // ближайший вторник + 6 часов и 1 час на duration
        LocalDateTime plannedIntervalStart = LocalDateTime.of(2020, 8, 25, 23, 0, 0);
        LocalDateTime plannedIntervalEnd = LocalDateTime.of(2020, 8, 26, 0, 0, 0);

        // 24 августа, понедельник
        TestableClock clock = new TestableClock();
        clock.setFixed(
                LocalDateTime.of(2020, 8, 24, 15, 0, 0).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
        );

        long movingPartnerId = 9001L;
        int duration = 360;
        TransportationConfig config = new TransportationConfig()
                .setDuration(duration)
                .setMovingPartnerId(movingPartnerId)
                .setInboundLogisticPointId(10L)
                .setInboundPartnerId(15L)
                .setTransportationType(ConfigTransportationType.ORDERS_OPERATION)
                .setTransportationSchedule(List.of(
                        new Schedule().setDay(2).setTimeFrom(LocalTime.of(17, 0, 0)).setTimeTo(LocalTime.of(18, 0, 0))
                ));

        TransportationUnitType type = TransportationUnitType.INBOUND;
        Long logisticPointId = 10L;
        Long partnerId = 15L;

        List<TransportationUnitDto> expected = List.of(new TransportationUnitDto()
                .setType(type)
                .setLogisticPointId(logisticPointId)
                .setPlannedIntervalStart(plannedIntervalStart)
                .setPlannedIntervalEnd(plannedIntervalEnd)
                .setPartnerId(partnerId)
        );

        List<TransportationUnitDto> outboundDto = ConverterUtil.convertInbound(config, LocalDateTime.now(clock), 7);
        assertThat(outboundDto).isEqualTo(expected);
    }

    @Test
    void correctlyDetectPlusWeek() {

        // ближайшая среда
        LocalDateTime plannedWedIntervalStart = LocalDateTime.of(2020, 8, 26, 10, 0, 0);
        LocalDateTime plannedWedIntervalEnd = LocalDateTime.of(2020, 8, 26, 15, 0, 0);

        // ближайший четверг
        LocalDateTime plannedThuIntervalStart = LocalDateTime.of(2020, 8, 20, 10, 0, 0);
        LocalDateTime plannedThuIntervalEnd = LocalDateTime.of(2020, 8, 20, 15, 0, 0);

        // 16 августа, среда
        TestableClock clock = new TestableClock();
        clock.setFixed(
            LocalDateTime.of(2020, 8, 19, 15, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
        );

        TransportationUnitType type = TransportationUnitType.OUTBOUND;
        Long logisticPointId = 10L;
        Long partnerId = 15L;

        TransportationConfig config = new TransportationConfig()
            .setDuration(1500)
            .setOutboundLogisticPointId(10L)
            .setOutboundPartnerId(15L)
            .setTransportationType(ConfigTransportationType.ORDERS_OPERATION)
            .setTransportationSchedule(List.of(
                new Schedule().setDay(3).setTimeFrom(LocalTime.of(10, 0, 0)).setTimeTo(LocalTime.of(15, 0, 0)),
                new Schedule().setDay(4).setTimeFrom(LocalTime.of(10, 0, 0)).setTimeTo(LocalTime.of(15, 0, 0))
            ));

        List<TransportationUnitDto> expected = List.of(
            new TransportationUnitDto()
                .setType(type)
                .setLogisticPointId(logisticPointId)
                .setPlannedIntervalStart(plannedWedIntervalStart)
                .setPlannedIntervalEnd(plannedWedIntervalEnd)
                .setPartnerId(partnerId),
            new TransportationUnitDto()
                .setType(type)
                .setLogisticPointId(logisticPointId)
                .setPlannedIntervalStart(plannedThuIntervalStart)
                .setPlannedIntervalEnd(plannedThuIntervalEnd)
                .setPartnerId(partnerId)
        );

        List<TransportationUnitDto> outboundDtos = ConverterUtil.convertOutbound(config, LocalDateTime.now(clock), 7);
        assertThat(outboundDtos).isEqualTo(expected);
    }

    @Test
    void getShipmentInterval() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        TransportationUnit unit = new TransportationUnit()
            .setPlannedIntervalStart(start)
            .setPlannedIntervalEnd(end);
        DateTimeInterval actual = ConverterUtil.getShipmentInterval(unit);
        DateTimeInterval expected = new DateTimeInterval(
            ZonedDateTime.of(start, DateTimeUtils.MOSCOW_ZONE).toOffsetDateTime(),
            ZonedDateTime.of(end, DateTimeUtils.MOSCOW_ZONE).toOffsetDateTime()
        );

        assertThat(expected).isEqualTo(actual);
    }

}
