package ru.yandex.market.delivery.transport_manager.facade.routing;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.dto.routing.RouteItemDto;
import ru.yandex.market.delivery.transport_manager.dto.routing.RoutePointDto;
import ru.yandex.market.delivery.transport_manager.dto.routing.RoutingResultDto;
import ru.yandex.market.delivery.transport_manager.dto.routing.UserRoutingResultDto;

@DbUnitConfiguration(
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"},
    dataSetLoader = ReplacementDataSetLoader.class
)
class ApplyRoutingResultFacadeIntegrationTest extends AbstractContextualTest {
    @Autowired
    private ApplyRoutingResultFacade facade;
    @Autowired
    protected TestableClock clock;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Успешное примененеие результатов маршрутизации")
    @DatabaseSetup(
        value = "/repository/routing/before/get_routing_request.xml"
    )
    @ExpectedDatabase(
        value = "/repository/routing/after/put_routing_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        // Проверим, что не испортили существующие перемещения и поменяли partner_id
        value = "/repository/routing/after/get_routing_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/routing/after/resend_movement.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void apply() {
        facade.apply(2234562L, LocalDate.now(clock), new RoutingResultDto(
                Map.of(
                    0L, new UserRoutingResultDto(List.of(
                        new RoutePointDto(List.of(
                            new RouteItemDto(List.of(11L, 21L)),
                            new RouteItemDto(List.of(31L))
                        ))
                    )),
                    1L, new UserRoutingResultDto(List.of(
                        new RoutePointDto(List.of(
                            new RouteItemDto(List.of(41L))
                        ))
                    ))
                ),
                Map.of(
                    0L, 1000L,
                    1L, 1001L
                )
            )
        );
    }

    @Test
    @DisplayName("Частично успешное примененеие результатов маршрутизации")
    @DatabaseSetup(
        value = "/repository/routing/before/get_routing_request_partial.xml"
    )
    @ExpectedDatabase(
        value = "/repository/routing/after/put_routing_result_partial.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        // Проверим, что не испортили существующие перемещения и поменяли partner_id
        value = "/repository/routing/after/get_routing_request_partial.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/routing/after/resend_movement_partial.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void applyPartial() {
        facade.apply(2234562L, LocalDate.now(clock), new RoutingResultDto(
                Map.of(
                    0L, new UserRoutingResultDto(List.of(
                        new RoutePointDto(List.of(
                            new RouteItemDto(List.of(11L, 21L)),
                            new RouteItemDto(List.of(31L))
                        ))
                    )),
                    1L, new UserRoutingResultDto(List.of(
                        new RoutePointDto(List.of(
                            new RouteItemDto(List.of(41L))
                        ))
                    ))
                ),
                new TreeMap<>(
                    Map.of(
                        0L, 1000L,
                        1L, 1001L
                    )
                )
            )
        );
    }

    @Test
    @DisplayName("Ошибка примененеия результатов маршрутизации - часть мувментов уже отправлена")
    @DatabaseSetup(
        value = "/repository/routing/before/get_routing_request_in_progress.xml"
    )
    @ExpectedDatabase(
        value = "/repository/routing/after/trips_empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        // Проверим, что не испортили существующие перемещения
        value = "/repository/routing/before/get_routing_request_in_progress.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/health/dbqueue/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void applyInProgress() {
        softly.assertThatThrownBy(() ->
                facade.apply(2234562L, LocalDate.now(clock), new RoutingResultDto(
                    Map.of(
                        0L, new UserRoutingResultDto(List.of(
                            new RoutePointDto(List.of(
                                new RouteItemDto(List.of(11L, 21L)),
                                new RouteItemDto(List.of(31L))
                            ))
                        )),
                        1L, new UserRoutingResultDto(List.of(
                            new RoutePointDto(List.of(
                                new RouteItemDto(List.of(41L))
                            ))
                        ))
                    ),
                    Map.of()
                ))
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No routing result can be applied. Movements TMM1,TMM4 are in unapplicable status");
    }
}
