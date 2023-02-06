package ru.yandex.market.delivery.transport_manager.facade.trip;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.dto.IdAndCount;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteSchedule;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.Trip;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.TripPoint;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterUnitMapper;
import ru.yandex.market.delivery.transport_manager.service.route_schedule.RouteScheduleReceiver;
import ru.yandex.market.delivery.transport_manager.service.trip.TripReceiver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TripCapacityValidationServiceTest {
    public static final List<Transportation> TRANSPORTATIONS = List.of(
        transportation(
            TransportationType.INTERWAREHOUSE,
            TransportationSubtype.INTERWAREHOUSE_FIT,
            1L,
            10L,
            2L,
            30L
        ),
        transportation(
            TransportationType.XDOC_TRANSPORT,
            null,
            3L,
            10L,
            4L,
            30L
        ),
        transportation(
            TransportationType.XDOC_TRANSPORT,
            null,
            5L,
            10L,
            6L,
            20L
        ),
        transportation(
            TransportationType.INTERWAREHOUSE,
            TransportationSubtype.INTERWAREHOUSE_DEFECT,
            7L,
            10L,
            8L,
            20L
        ),
        transportation(
            TransportationType.INTERWAREHOUSE,
            TransportationSubtype.INTERWAREHOUSE_DEFECT,
            9L,
            20L,
            10L,
            30L
        )
    );
    private TripCapacityValidationService service;
    private TripReceiver tripReceiver;
    private RegisterUnitMapper registerUnitMapper;
    private RouteScheduleReceiver routeScheduleReceiver;

    @BeforeEach
    void setUp() {
        tripReceiver = mock(TripReceiver.class);
        registerUnitMapper = mock(RegisterUnitMapper.class);
        routeScheduleReceiver = mock(RouteScheduleReceiver.class);

        service = new TripCapacityValidationService(
            tripReceiver,
            registerUnitMapper,
            routeScheduleReceiver,
            new IdPrefixConverter()
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(tripReceiver, registerUnitMapper, routeScheduleReceiver);
    }

    @DisplayName("Рейс не найден")
    @Test
    void tripNotFound() {
        when(tripReceiver.getByIds(eq(List.of(1L)))).thenReturn(Collections.emptySet());
        assertThatThrownBy(() -> service.checkCapacity(1L, List.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Рейс 1 не найден!");

        verify(tripReceiver).getByIds(eq(List.of(1L)));
    }

    @DisplayName("В рейсе нет расписания")
    @Test
    void tripHasNoRouteScheduleFound() {
        when(tripReceiver.getByIds(eq(List.of(1L)))).thenReturn(Set.of(
            new Trip()
        ));
        assertThatThrownBy(() -> service.checkCapacity(1L, List.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Неизвестна вместимость машины: расписание null не существует "
                    + "или не задана максимальная паллетовместимость"
            );

        verify(tripReceiver).getByIds(eq(List.of(1L)));
    }

    @DisplayName("В расписании нет паллетовместимости машины")
    @Test
    void routeScheduleHasNoCapacityDefinedFound() {
        when(tripReceiver.getByIds(eq(List.of(1L)))).thenReturn(Set.of(
            new Trip().setRouteScheduleId(10L)
        ));
        when(routeScheduleReceiver.getById(eq(10L))).thenReturn(Optional.of(
            new RouteSchedule().setId(10L)
        ));
        assertThatThrownBy(() -> service.checkCapacity(1L, List.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Неизвестна вместимость машины: расписание 10 не существует "
                    + "или не задана максимальная паллетовместимость"
            );

        verify(tripReceiver).getByIds(eq(List.of(1L)));
        verify(routeScheduleReceiver).getById(eq(10L));
    }

    @DisplayName("Машина полностью заполнена, реестры корректные")
    @Test
    void capacityFullCarOk() {
        when(tripReceiver.getByIds(eq(List.of(1L)))).thenReturn(Set.of(
            new Trip()
                .setRouteScheduleId(10L)
                .setPoints(Set.of(
                    new TripPoint(1L, 1L, 0, 1L),
                    new TripPoint(2L, 1L, 1, 3L),
                    new TripPoint(3L, 1L, 2, 5L),
                    new TripPoint(4L, 1L, 3, 7L),
                    new TripPoint(5L, 1L, 4, 8L),
                    new TripPoint(6L, 1L, 5, 6L),
                    new TripPoint(7L, 1L, 6, 9L),
                    new TripPoint(8L, 1L, 7, 10L),
                    new TripPoint(9L, 1L, 8, 4L),
                    new TripPoint(10L, 1L, 9, 2L)
                ))
        ));
        when(routeScheduleReceiver.getById(eq(10L))).thenReturn(Optional.of(
            new RouteSchedule().setId(10L).setMaxPallet(33)
        ));
        when(registerUnitMapper.countsByTransportationUnitIdsRegisterTypeAndItemType(
            eq(List.of(1L, 3L, 5L, 7L, 9L)),
            eq(RegisterType.PLAN),
            eq(UnitType.PALLET)
        ))
            .thenReturn(List.of(
                new IdAndCount(1L, 20),
                new IdAndCount(7L, 13),
                new IdAndCount(9L, 13)
            ));

        service.checkCapacity(1L, TRANSPORTATIONS);

        verify(tripReceiver).getByIds(eq(List.of(1L)));
        verify(routeScheduleReceiver).getById(eq(10L));
        verify(registerUnitMapper).countsByTransportationUnitIdsRegisterTypeAndItemType(
            eq(List.of(1L, 3L, 5L, 7L, 9L)),
            eq(RegisterType.PLAN),
            eq(UnitType.PALLET)
        );
    }

    @DisplayName("Переполнение машины")
    @Test
    void capacityOverflow() {
        when(tripReceiver.getByIds(eq(List.of(1L)))).thenReturn(Set.of(
            new Trip()
                .setRouteScheduleId(10L)
                .setPoints(Set.of(
                    new TripPoint(1L, 1L, 0, 1L),
                    new TripPoint(2L, 1L, 1, 3L),
                    new TripPoint(3L, 1L, 2, 5L),
                    new TripPoint(4L, 1L, 3, 7L),
                    new TripPoint(5L, 1L, 4, 8L),
                    new TripPoint(6L, 1L, 5, 6L),
                    new TripPoint(7L, 1L, 6, 9L),
                    new TripPoint(8L, 1L, 7, 10L),
                    new TripPoint(9L, 1L, 8, 4L),
                    new TripPoint(10L, 1L, 9, 2L)
                ))
        ));
        when(routeScheduleReceiver.getById(eq(10L))).thenReturn(Optional.of(
            new RouteSchedule().setId(10L).setMaxPallet(33)
        ));
        when(registerUnitMapper.countsByTransportationUnitIdsRegisterTypeAndItemType(
            eq(List.of(1L, 3L, 5L, 7L, 9L)),
            eq(RegisterType.PLAN),
            eq(UnitType.PALLET)
        ))
            .thenReturn(List.of(
                new IdAndCount(1L, 20),
                new IdAndCount(3L, 13),
                new IdAndCount(9L, 13)
            ));

        assertThatThrownBy(() -> service.checkCapacity(1L, TRANSPORTATIONS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Не могу добавить поставку к рейсу 1: превышена паллетовместимость машины в точке 6 (TMU9)! "
                + "Максимум 33, пытаемся положить 46.");

        verify(tripReceiver).getByIds(eq(List.of(1L)));
        verify(routeScheduleReceiver).getById(eq(10L));
        verify(registerUnitMapper).countsByTransportationUnitIdsRegisterTypeAndItemType(
            eq(List.of(1L, 3L, 5L, 7L, 9L)),
            eq(RegisterType.PLAN),
            eq(UnitType.PALLET)
        );
    }

    @DisplayName("Перепутаны местами отгрузка и приёмка, но заполнение машины соблюдается")
    @Test
    void capacityFullCarWithBadUnitOrderOk() {
        when(tripReceiver.getByIds(eq(List.of(1L)))).thenReturn(Set.of(
            new Trip()
                .setRouteScheduleId(10L)
                .setPoints(Set.of(
                    new TripPoint(1L, 1L, 0, 1L),
                    new TripPoint(2L, 1L, 1, 3L),
                    new TripPoint(3L, 1L, 2, 5L),
                    new TripPoint(4L, 1L, 3, 7L),
                    new TripPoint(5L, 1L, 6, 8L),
                    new TripPoint(6L, 1L, 5, 6L),
                    new TripPoint(7L, 1L, 4, 9L),
                    new TripPoint(8L, 1L, 7, 10L),
                    new TripPoint(9L, 1L, 8, 4L),
                    new TripPoint(10L, 1L, 9, 2L)
                ))
        ));
        when(routeScheduleReceiver.getById(eq(10L))).thenReturn(Optional.of(
            new RouteSchedule().setId(10L).setMaxPallet(33)
        ));
        when(registerUnitMapper.countsByTransportationUnitIdsRegisterTypeAndItemType(
            eq(List.of(1L, 3L, 5L, 7L, 9L)),
            eq(RegisterType.PLAN),
            eq(UnitType.PALLET)
        ))
            .thenReturn(List.of(
                new IdAndCount(1L, 20),
                new IdAndCount(7L, 13),
                new IdAndCount(9L, 13)
            ));

        service.checkCapacity(1L, TRANSPORTATIONS);

        verify(tripReceiver).getByIds(eq(List.of(1L)));
        verify(routeScheduleReceiver).getById(eq(10L));
        verify(registerUnitMapper).countsByTransportationUnitIdsRegisterTypeAndItemType(
            eq(List.of(1L, 3L, 5L, 7L, 9L)),
            eq(RegisterType.PLAN),
            eq(UnitType.PALLET)
        );
    }

    private static Transportation transportation(
        TransportationType type,
        TransportationSubtype subtype,
        long outboundId,
        long outboundPointId,
        long inboundId,
        long inboundPointId
    ) {
        return new Transportation()
            .setTransportationType(type)
            .setSubtype(subtype)
            .setOutboundUnit(
                new TransportationUnit()
                    .setId(outboundId)
                    .setLogisticPointId(outboundPointId)
                    .setType(TransportationUnitType.OUTBOUND)
            )
            .setInboundUnit(
                new TransportationUnit()
                    .setId(inboundId)
                    .setLogisticPointId(inboundPointId)
                    .setType(TransportationUnitType.INBOUND)
            );
    }

}
