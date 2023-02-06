package ru.yandex.market.delivery.transport_manager.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.yt.LogisticsPointChangeDto;
import ru.yandex.market.delivery.transport_manager.service.logistic_point.LogisticsPointChangeUpdateService;
import ru.yandex.market.delivery.transport_manager.service.yt.YtCommonReader;

@DatabaseSetup("/repository/order_route/orders.xml")
public class LogisticsPointChangeUpdateServiceTest extends AbstractContextualTest {
    @Autowired
    private LogisticsPointChangeUpdateService updateService;

    @Autowired
    private YtCommonReader<LogisticsPointChangeDto> reader;

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2022-03-13T00:00:00Z"), ZoneOffset.UTC);
        Mockito.doReturn(Set.of(
            new LogisticsPointChangeDto(1L, 2L, Instant.parse("2021-12-12T04:39:25Z")),
            new LogisticsPointChangeDto(3L, 4L, Instant.parse("2021-12-13T04:39:25Z")),
            new LogisticsPointChangeDto(16L, 20L, Instant.parse("2022-02-12T04:39:25Z"))
            )
        ).when(reader).getTableData(LogisticsPointChangeDto.class, "//path");
    }

    @Test
    @DatabaseSetup("/repository/logistic_point/change/changes.xml")
    @DatabaseSetup("/repository/order_route/routes.xml")
    @ExpectedDatabase(
        value = "/repository/logistic_point/change/after/after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/order_route/after/after_point_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdate() {
        updateService.update();
    }
}
