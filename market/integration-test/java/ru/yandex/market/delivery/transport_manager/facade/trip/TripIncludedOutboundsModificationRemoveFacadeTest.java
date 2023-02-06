package ru.yandex.market.delivery.transport_manager.facade.trip;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"}
)
class TripIncludedOutboundsModificationRemoveFacadeTest extends AbstractContextualTest {
    @Autowired
    private TripIncludedOutboundsModificationRemoveFacade facade;

    @DisplayName("Удалить часть грузомест из перемещения")
    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/insert_transportation_registry_example.xml",
        "/repository/trip/new_units_2_to_3_interwarehouse_fit_frozen.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/insert_transportation_full_example.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/trip/after/remove_register_units.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/route/before/empty_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    void remove() {
        facade.remove(List.of("TMT1"), List.of("TMU1001"));
    }

    @DisplayName("Удалить всё из перемещения. Само перемещение должно быть отменено")
    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example.xml",
        "/repository/trip/insert_transportation_registry_example.xml",
        "/repository/trip/new_units_2_to_3_interwarehouse_fit_frozen.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/after/cancel_transportation_and_remove_register_units.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/trip/after/cancel_transportation_and_remove_register_units_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    void removeAll() {
        facade.remove(List.of("TMT1"), List.of("TMU1001", "10003"));
    }

    @DisplayName("Удалить всё из перемещения. Само перемещение не отменяем, т.к. XDock")
    @DatabaseSetup({
        "/repository/trip/insert_transportation_full_example_xdock.xml",
        "/repository/trip/insert_transportation_registry_example.xml",
        "/repository/trip/new_units_2_to_3_xdock_frozen.xml",
    })
    @ExpectedDatabase(
        value = "/repository/trip/after/remove_all_register_units_xdock.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/route/before/empty_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    void removeAllXDock() {
        facade.remove(List.of("TMT1"), List.of("TMU1001", "10003"));
    }
}
