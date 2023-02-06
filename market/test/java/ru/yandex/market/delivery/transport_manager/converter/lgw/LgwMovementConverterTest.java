package ru.yandex.market.delivery.transport_manager.converter.lgw;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.util.ProxyUtil;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;

class LgwMovementConverterTest {

    private LgwMovementConverter converter;

    @BeforeEach
    void setUp() {
        LgwCommonResourceIdConverter resourceIdConverter = new LgwCommonResourceIdConverter(new IdPrefixConverter());
        LgwLogisticPointConverter logisticPointConverter =
            new LgwLogisticPointConverter(new LgwLocationConverter(), resourceIdConverter, new LgwPhoneConverter());
        converter = ProxyUtil.createWithSelfReference(
            movementConverterProxy -> new LgwMovementConverter(
                resourceIdConverter,
                new LgwPartyConverter(resourceIdConverter, new LgwLegalEntityConverter(), logisticPointConverter),
                new WeightVolumeConverter(),
                new LgwTripConverter(resourceIdConverter, movementConverterProxy)
            ),
            LgwMovementConverter.class
        );
    }

    @ParameterizedTest(name = "Интервал перевозки для перемещения с типом {0}")
    @MethodSource("interwarehouseTypes")
    void dateTimeIntervalInterwarehouse(TransportationType transportationType) {
        Transportation linehaul = new Transportation()
            .setTransportationType(transportationType)
            .setOutboundUnit(new TransportationUnit()
                .setPlannedIntervalStart(localDateTime(14, 30))
                .setPlannedIntervalEnd(localDateTime(15, 30))
            )
            .setInboundUnit(new TransportationUnit()
                .setPlannedIntervalStart(localDateTime(14, 30))
                .setPlannedIntervalEnd(localDateTime(15, 30))
            );

        Assertions.assertThat(converter.dateTimeInterval(linehaul))
            .isEqualTo(new DateTimeInterval(
                offsetDateTime(14, 30),
                offsetDateTime(15, 30)
            ));
    }

    @ParameterizedTest(name = "Интервал перевозки для перемещения с типом {0} (время из movement-а)")
    @MethodSource("nonInterwarehouseTypes")
    void dateTimeIntervalNonInterwarehouseMovementPlan(TransportationType transportationType) {
        Transportation linehaul = new Transportation()
            .setTransportationType(transportationType)
            .setOutboundUnit(new TransportationUnit()
                .setPlannedIntervalStart(localDateTime(14, 30))
                .setPlannedIntervalEnd(localDateTime(15, 30))
            )
            .setInboundUnit(new TransportationUnit()
                .setPlannedIntervalStart(localDateTime(16, 30))
                .setPlannedIntervalEnd(localDateTime(17, 30))
            )
            .setMovement(new Movement()
                .setPlannedIntervalStart(localDateTime(14, 40))
                .setPlannedIntervalEnd(localDateTime(15, 20))
            );

        Assertions.assertThat(converter.dateTimeInterval(linehaul))
            .isEqualTo(new DateTimeInterval(
                offsetDateTime(14, 40),
                offsetDateTime(15, 20)
            ));
    }

    @ParameterizedTest(name = "Интервал перевозки для перемещения с типом {0} (время из отгрузки/приёмки)")
    @MethodSource("nonInterwarehouseTypes")
    void dateTimeIntervalNonInterwarehouse(TransportationType transportationType) {
        Transportation linehaul = new Transportation()
            .setTransportationType(transportationType)
            .setOutboundUnit(new TransportationUnit()
                .setPlannedIntervalStart(localDateTime(14, 30))
                .setPlannedIntervalEnd(localDateTime(15, 30))
            )
            .setInboundUnit(new TransportationUnit()
                .setPlannedIntervalStart(localDateTime(16, 30))
                .setPlannedIntervalEnd(localDateTime(17, 30))
            )
            .setMovement(new Movement());

        Assertions.assertThat(converter.dateTimeInterval(linehaul))
            .isEqualTo(new DateTimeInterval(
                offsetDateTime(15, 30),
                offsetDateTime(16, 30)
            ));
    }

    @NotNull
    private LocalDateTime localDateTime(int h, int m) {
        return LocalDateTime.of(2021, 11, 23, h, m);
    }

    @NotNull
    private OffsetDateTime offsetDateTime(int h, int m) {
        return OffsetDateTime.of(2021, 11, 23, h, m, 0, 0, ZoneOffset.of("+0300"));
    }

    static Stream<Arguments> interwarehouseTypes() {
        return Arrays.stream(TransportationType.values())
            .filter(TransportationType::isInterwarehouse)
            .map(Arguments::of);
    }

    static Stream<Arguments> nonInterwarehouseTypes() {
        return StreamEx.of(TransportationType.values())
            .remove(TransportationType::isInterwarehouse)
            .map(Arguments::of);
    }
}
