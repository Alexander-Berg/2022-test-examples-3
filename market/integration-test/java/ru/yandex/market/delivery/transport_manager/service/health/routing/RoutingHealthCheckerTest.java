package ru.yandex.market.delivery.transport_manager.service.health.routing;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.service.PropertyService;

class RoutingHealthCheckerTest extends AbstractContextualTest {
    public static final long SC_ID = 2234562L;
    @Autowired
    private RoutingHealthChecker routingHealthChecker;

    @Autowired
    private PropertyService<TmPropertyKey> propertyService;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2020-08-06T20:00:00.00Z"), ZoneOffset.UTC);

        Mockito
            .when(propertyService.getList(TmPropertyKey.ORDER_OPERATION_WITH_TRIP_OUTBOUND_PARTNER_IDS))
            .thenReturn(List.of(SC_ID));
    }

    @Test
    void checkOutdatedOk() {
        softly
            .assertThat(routingHealthChecker.checkOutdated())
            .isEqualTo("0;OK");
    }

    @DatabaseSetup(
        value = "/repository/routing/before/get_routing_request.xml"
    )
    @Test
    void checkOutdatedWarn() {
        // Error тестировать не будем, потому что это работает точно так же,
        // но с большим кол-вом перемещений
        softly
            .assertThat(routingHealthChecker.checkOutdated())
            .isEqualTo("1;Not routed transportations: TM1, TM2, TM3, TM4");
    }
}
