package ru.yandex.market.delivery.transport_manager.facade.lgw;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.model.dto.MovementCourierDto;
import ru.yandex.market.delivery.transport_manager.service.AxaptaStatusEventService;

@DbUnitConfiguration(
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"},
    dataSetLoader = ReplacementDataSetLoader.class
)
class LgwMovementFacadeTest extends AbstractContextualTest {
    @Autowired
    private LgwMovementFacade lgwMovementFacade;

    @Autowired
    private AxaptaStatusEventService axaptaStatusEventService;

    @BeforeEach
    void setUp() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 14, 20, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );
    }

    @DatabaseSetup({
        "/repository/route/full_routes.xml",
        "/repository/route_schedule/full_schedules.xml",
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml",
        "/repository/movement_courier/multiple_couriers_without_id.xml",
        "/repository/trip/multiple_transportations_trips.xml",
    })
    @ExpectedDatabase(
        value = "/repository/movement_courier/excepted/multiple_couriers_new_courier_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/movement_courier/excepted/multiple_couriers_new_courier_sending_state_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/movement_courier/excepted/multiple_couriers_new_courier_axapta_event_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/movement_courier/excepted/new_courier_saved_dbqueue.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    @SneakyThrows
    void getSuccess() {
        lgwMovementFacade.getSuccess("4", movementCourierDto());
        Mockito.verify(axaptaStatusEventService, Mockito.times(3)).createCourierFoundEvent(Mockito.any());
    }

    private static MovementCourierDto movementCourierDto() {
        return MovementCourierDto.builder()
            .name("Джордж")
            .build();
    }
}
