package ru.yandex.market.delivery.transport_manager.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.TimeSlot;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.service.StatusHistoryService;

class TimeUtilTest extends AbstractContextualTest {
    public static final Function<Transportation, TransportationUnit> GET_OUTBOUND_UNIT = new TransportationUnitGetter(
        Transportation::getOutboundUnit,
        TransportationUnitType.OUTBOUND
    );
    public static final Function<Transportation, TransportationUnit> GET_INBOUND_UNIT = new TransportationUnitGetter(
        Transportation::getOutboundUnit,
        TransportationUnitType.INBOUND
    );

    @Autowired
    private StatusHistoryService statusHistoryService;

    @DisplayName("Для не ручных перемещений дату не корректируем")
    @ParameterizedTest
    @MethodSource("transportationUnitGetters")
    void getMinTransportationUnitTimeForCorrectionNotManual(Function<Transportation, TransportationUnit> unitGetter) {
        final Transportation transportation = new Transportation()
            .setTransportationSource(TransportationSource.FFWF)
            .setTransportationType(TransportationType.XDOC_TRANSPORT);

        softly.assertThat(
                TimeUtil.getMinTransportationUnitTimeForCorrection(
                    transportation,
                    unitGetter.apply(transportation),
                    statusHistoryService
                )
            )
            .isEmpty();
    }

    @DisplayName("Для ручных не межскладских перемещений в будущем дату не корректируем")
    @ParameterizedTest
    @MethodSource("transportationUnitGetters")
    void getMinTransportationUnitTimeForCorrectionNonInterwarehouse(
        Function<Transportation, TransportationUnit> unitGetter
    ) {
        final Transportation transportation = new Transportation()
            .setTransportationSource(TransportationSource.TM_MANUAL)
            .setTransportationType(TransportationType.ORDERS_OPERATION)
            .setOutboundUnit(new TransportationUnit())
            .setMovement(new Movement().setId(1L))
            .setInboundUnit(new TransportationUnit());

        softly.assertThat(
                TimeUtil.getMinTransportationUnitTimeForCorrection(
                    transportation,
                    unitGetter.apply(transportation),
                    statusHistoryService
                )
            )
            .isEmpty();
    }

    @DisplayName("Для ручных межскладских перемещений дату не корректируем, если она больше, чем confirmed")
    @DatabaseSetup("/repository/movement/movement_status_history.xml")
    @ParameterizedTest
    @MethodSource("transportationUnitGetters")
    void getMinTransportationUnitTimeForCorrectionInFuture(
        Function<Transportation, TransportationUnit> unitGetter
    ) {
        final Transportation transportation = new Transportation()
            .setTransportationSource(TransportationSource.TM_MANUAL)
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setOutboundUnit(new TransportationUnit())
            .setMovement(new Movement().setId(1L))
            .setInboundUnit(new TransportationUnit());

        unitGetter.apply(transportation).setPlannedIntervalStart(LocalDateTime.of(
            2021, 7, 15, 12, 0
        ));

        softly.assertThat(
                TimeUtil.getMinTransportationUnitTimeForCorrection(
                    transportation,
                    unitGetter.apply(transportation),
                    statusHistoryService
                )
            )
            .isEmpty();
    }

    @DisplayName("Для ручных межскладских перемещений дату не корректируем, если дата из слота больше, чем confirmed")
    @DatabaseSetup("/repository/movement/movement_status_history.xml")
    @ParameterizedTest
    @MethodSource("transportationUnitGetters")
    void getMinTransportationUnitTimeForCorrectionInFutureWithBooking(
        Function<Transportation, TransportationUnit> unitGetter
    ) {
        final Transportation transportation = new Transportation()
            .setTransportationSource(TransportationSource.TM_MANUAL)
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setOutboundUnit(new TransportationUnit())
            .setMovement(new Movement().setId(1L))
            .setInboundUnit(new TransportationUnit());

        TransportationUnit unit = unitGetter.apply(transportation);
        unit.setPlannedIntervalStart(LocalDateTime.of(
            2021, 7, 13, 12, 0
        ));
        unit.setBookedTimeSlot(new TimeSlot()
            .setFromDate(ZonedDateTime.of(
                2021, 7, 13, 18, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET
            ).toLocalDateTime())
            .setZoneId(TimeUtil.DEFAULT_ZONE_OFFSET.getId())
        );

        softly.assertThat(
                TimeUtil.getMinTransportationUnitTimeForCorrection(
                    transportation,
                    unit,
                    statusHistoryService
                )
            )
            .isEmpty();
    }

    @DisplayName("Если нет CONFIRMED в истории статусов movement-а, дату не корректируем")
    @ParameterizedTest
    @MethodSource("transportationUnitGetters")
    void getMinTransportationUnitTimeForCorrectionNoHistory(Function<Transportation, TransportationUnit> unitGetter) {
        final Transportation transportation = new Transportation()
            .setTransportationSource(TransportationSource.TM_MANUAL)
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setOutboundUnit(new TransportationUnit())
            .setMovement(new Movement().setId(1L))
            .setInboundUnit(new TransportationUnit());

        unitGetter.apply(transportation).setPlannedIntervalStart(LocalDateTime.of(
            2021, 7, 9, 12, 0
        ));

        softly.assertThat(
                TimeUtil.getMinTransportationUnitTimeForCorrection(
                    transportation,
                    unitGetter.apply(transportation),
                    statusHistoryService
                )
            )
            .isEmpty();
    }

    @DisplayName("Корректируем дату межсклада")
    @DatabaseSetup("/repository/movement/movement_status_history.xml")
    @ParameterizedTest
    @MethodSource("transportationUnitGettersAndSources")
    void getMinInterwarehouseTransportationUnitTimeForCorrection(
        Function<Transportation, TransportationUnit> unitGetter,
        TransportationSource source
    ) {
        final Transportation transportation = new Transportation()
            .setTransportationSource(source)
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setOutboundUnit(new TransportationUnit())
            .setMovement(new Movement().setId(1L))
            .setInboundUnit(new TransportationUnit());

        unitGetter.apply(transportation).setPlannedIntervalStart(LocalDateTime.of(
            2021, 7, 9, 12, 0
        ));

        softly.assertThat(
                TimeUtil.getMinTransportationUnitTimeForCorrection(
                    transportation,
                    unitGetter.apply(transportation),
                    statusHistoryService
                )
            )
            .map(d -> d.atZoneSameInstant(ZoneOffset.UTC))
            .contains(ZonedDateTime.of(2021, 7, 13, 15, 50, 40, 515453000, ZoneOffset.UTC));
    }

    @DisplayName("Корректируем дату ORDERS_OPRATION 3PL")
    @DatabaseSetup("/repository/movement/movement_status_history.xml")
    @ParameterizedTest
    @MethodSource("transportationUnitGetters")
    void getMinOrders3PLTransportationUnitTimeForCorrection(
        Function<Transportation, TransportationUnit> unitGetter
    ) {
        final Transportation transportation = new Transportation()
            .setTransportationSource(TransportationSource.LMS_TM_MOVEMENT)
            .setTransportationType(TransportationType.ORDERS_OPERATION)
            .setOutboundUnit(new TransportationUnit().setPartnerId(1L))
            .setMovement(new Movement().setId(1L).setPartnerId(2L))
            .setInboundUnit(new TransportationUnit().setPartnerId(3L));

        unitGetter.apply(transportation).setPlannedIntervalStart(LocalDateTime.of(
            2021, 7, 9, 12, 0
        ));

        softly.assertThat(
                TimeUtil.getMinTransportationUnitTimeForCorrection(
                    transportation,
                    unitGetter.apply(transportation),
                    statusHistoryService
                )
            )
            .map(d -> d.atZoneSameInstant(ZoneOffset.UTC))
            .contains(ZonedDateTime.of(2021, 7, 13, 15, 50, 40, 515453000, ZoneOffset.UTC));
    }

    @DisplayName("Не корректируем дату ORDERS_OPRATION")
    @DatabaseSetup("/repository/movement/movement_status_history.xml")
    @ParameterizedTest
    @MethodSource("transportationUnitGetters")
    void getMinOrdersTransportationUnitTimeForCorrection(
        Function<Transportation, TransportationUnit> unitGetter
    ) {
        final Transportation transportation = new Transportation()
            .setTransportationSource(TransportationSource.LMS_TM_MOVEMENT)
            .setTransportationType(TransportationType.ORDERS_OPERATION)
            .setOutboundUnit(new TransportationUnit().setPartnerId(1L))
            .setMovement(new Movement().setId(1L).setPartnerId(1L))
            .setInboundUnit(new TransportationUnit().setPartnerId(3L));

        unitGetter.apply(transportation).setPlannedIntervalStart(LocalDateTime.of(
            2021, 7, 9, 12, 0
        ));

        softly.assertThat(
                TimeUtil.getMinTransportationUnitTimeForCorrection(
                    transportation,
                    unitGetter.apply(transportation),
                    statusHistoryService
                )
            )
            .map(d -> d.atZoneSameInstant(ZoneOffset.UTC))
            .isEmpty();
    }

    static Stream<Arguments> transportationUnitGetters() {
        return Stream.of(
            Arguments.of(GET_OUTBOUND_UNIT),
            Arguments.of(GET_INBOUND_UNIT)
        );
    }

    static Stream<Arguments> transportationUnitGettersAndSources() {
        return Stream.of(
            Arguments.of(
                GET_OUTBOUND_UNIT,
                TransportationSource.TM_MANUAL
            ),
            Arguments.of(
                GET_INBOUND_UNIT,
                TransportationSource.TM_MANUAL
            ),
            Arguments.of(
                GET_OUTBOUND_UNIT,
                TransportationSource.TM_GENERATED
            ),
            Arguments.of(
                GET_INBOUND_UNIT,
                TransportationSource.TM_GENERATED
            )
        );
    }

    @Value
    private static class TransportationUnitGetter implements Function<Transportation, TransportationUnit> {
        Function<Transportation, TransportationUnit> getter;
        TransportationUnitType name;

        @Override
        public TransportationUnit apply(Transportation transportation) {
            return getter.apply(transportation);
        }

        @Override
        public String toString() {
            return name.name();
        }
    }
}
