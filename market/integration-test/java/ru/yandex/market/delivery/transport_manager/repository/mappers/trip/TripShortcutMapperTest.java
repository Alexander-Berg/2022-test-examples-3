package ru.yandex.market.delivery.transport_manager.repository.mappers.trip;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip_included_outbounds.CargoUnitIdWithDirection;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip_included_outbounds.CargoUnitIdWithDirectionAndTrip;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleType;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.TripShortcut;
import ru.yandex.market.delivery.transport_manager.facade.trip.TripSearchFacade;

public class TripShortcutMapperTest extends AbstractContextualTest {
    @Autowired
    private TripShortcutMapper mapper;

    @DatabaseSetup({
        "/repository/route/full_routes.xml",
        "/repository/route_schedule/full_schedules.xml",
        "/repository/trip/before/trips_and_transportations.xml"
    })
    @Test
    void getByIds() {
        softly.assertThat(mapper.getByIds(
                List.of(
                    1L,
                    2L
                )
            ))
            .containsExactlyInAnyOrder(
                new TripShortcut(2,
                    102L,
                    null,
                    Set.of(2L, 3L),
                    RouteScheduleType.LINEHAUL, LocalDate.of(2021, 11, 27),
                    LocalDateTime.of(2021, 11, 27, 10, 30)
                ),
                new TripShortcut(1,
                    100L,
                    "testname",
                    Set.of(1L),
                    RouteScheduleType.LINEHAUL, LocalDate.of(2021, 11, 26),
                    LocalDateTime.of(2021, 11, 26, 10, 30)
                )
            );
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/insert_transportation_registry_example.xml",
    })
    @Test
    void getByCargoUnits() {
        softly.assertThat(mapper.getByCargoUnits(
                List.of(
                    new CargoUnitIdWithDirection("PALLET010", 20L, 30L),
                    new CargoUnitIdWithDirection("PALLET030", 20L, 30L)
                ),
                TripSearchFacade.EXCEPT_TRANSPORTATION_STATUSES,
                TripSearchFacade.EXCEPT_OUTBOUND_STATUSES
            ))
            .containsExactlyInAnyOrder(
                new CargoUnitIdWithDirectionAndTrip(20L, 30L, "PALLET010", 1L),
                new CargoUnitIdWithDirectionAndTrip(20L, 30L, "PALLET030", 1L)
            );
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/insert_transportation_registry_example.xml",
    })
    @Test
    void getByCargoUnitsBadDirection() {
        softly.assertThat(mapper.getByCargoUnits(
                List.of(
                    new CargoUnitIdWithDirection("PALLET010", 10L, 20L),
                    new CargoUnitIdWithDirection("PALLET030", 10L, 20L)
                ),
                TripSearchFacade.EXCEPT_TRANSPORTATION_STATUSES,
                TripSearchFacade.EXCEPT_OUTBOUND_STATUSES
            ))
            .isEmpty();
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/insert_transportation_registry_example.xml",
        "/repository/trip/second_trip.xml",
    })
    @Test
    void getByCargoUnitsMultipleDirections() {
        softly.assertThat(mapper.getByCargoUnits(
                List.of(
                    // Этот юнит по этому направлению присутствует в 2 рейсах, берём более поздний ID=2
                    new CargoUnitIdWithDirection("PALLET010", 20L, 30L),
                    // Этот юнит присутствует только в рейсе 1
                    new CargoUnitIdWithDirection("PALLET030", 20L, 30L),
                    // Этот юнит по этому направлению присутствует только в рейсе 2
                    new CargoUnitIdWithDirection("PALLET010", 10L, 20L),
                    // Этот юнит по этому направлению не существует
                    new CargoUnitIdWithDirection("PALLET030", 10L, 20L)
                ),
                TripSearchFacade.EXCEPT_TRANSPORTATION_STATUSES,
                TripSearchFacade.EXCEPT_OUTBOUND_STATUSES
            ))
            .containsExactlyInAnyOrder(
                new CargoUnitIdWithDirectionAndTrip(20L, 30L, "PALLET010", 2L),
                new CargoUnitIdWithDirectionAndTrip(20L, 30L, "PALLET030", 1L),
                new CargoUnitIdWithDirectionAndTrip(10L, 20L, "PALLET010", 2L)
            );
    }
}
