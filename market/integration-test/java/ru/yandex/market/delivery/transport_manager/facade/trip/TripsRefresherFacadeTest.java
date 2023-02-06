package ru.yandex.market.delivery.transport_manager.facade.trip;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DatabaseSetup({
    "/repository/route/full_routes.xml",
    "/repository/route_schedule/full_schedules.xml"
})
class TripsRefresherFacadeTest extends AbstractContextualTest {

    @Autowired
    private TripsRefresherFacade facade;

    @Test
    @DisplayName("Smoke test на работу всей интеграции: получение, обновление, сохранение рейсов")
    @ExpectedDatabase(
        value = "/repository/trip/after/trip.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void smokeTestUpdate() {
        clock.setFixed(Instant.parse("2021-11-04T15:00:00.0Z"), ZoneOffset.UTC);
        facade.update(List.of(100L));
    }
}
