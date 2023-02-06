package ru.yandex.market.delivery.transport_manager.facade.trip;

import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip_included_outbounds.CargoUnitIdWithDirection;

public class TripSearchFacadeTest extends AbstractContextualTest {

    @Autowired
    private TripSearchFacade facade;

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/insert_transportation_registry_example.xml",
    })
    @Test
    void getCargoUnitsTripIds() {
        softly.assertThat(facade.getCargoUnitsTripIds(List.of(
                new CargoUnitIdWithDirection("PALLET010", 20L, 30L),
                new CargoUnitIdWithDirection("PALLET030", 20L, 30L)
            )))
            .isEqualTo(
                Map.of(
                    new CargoUnitIdWithDirection("PALLET010", 20L, 30L), 1L,
                    new CargoUnitIdWithDirection("PALLET030", 20L, 30L), 1L
                )
            );
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/insert_transportation_registry_example.xml",
    })
    @Test
    void getCargoUnitsTripIdsBadDirection() {
        softly.assertThat(facade.getCargoUnitsTripIds(List.of(
                new CargoUnitIdWithDirection("PALLET010", 10L, 20L),
                new CargoUnitIdWithDirection("PALLET030", 10L, 20L)
            )))
            .isEmpty();
    }

    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/insert_transportation_registry_example.xml",
        "/repository/trip/second_trip.xml",
    })
    @Test
    void getCargoUnitsTripIdsMultipleDirections() {
        softly.assertThat(facade.getCargoUnitsTripIds(List.of(
                // Этот юнит по этому направлению присутствует в 2 рейсах, берём более поздний ID=2
                new CargoUnitIdWithDirection("PALLET010", 20L, 30L),
                // Этот юнит присутствует только в рейсе 1
                new CargoUnitIdWithDirection("PALLET030", 20L, 30L),
                // Этот юнит по этому направлению присутствует только в рейсе 2
                new CargoUnitIdWithDirection("PALLET010", 10L, 20L),
                // Этот юнит по этому направлению не существует
                new CargoUnitIdWithDirection("PALLET030", 10L, 20L)
            )))
            .isEqualTo(
                Map.of(
                    new CargoUnitIdWithDirection("PALLET010", 10L, 20L), 2L,
                    new CargoUnitIdWithDirection("PALLET030", 20L, 30L), 1L,
                    new CargoUnitIdWithDirection("PALLET010", 20L, 30L), 2L
                    )
            );
    }
}
