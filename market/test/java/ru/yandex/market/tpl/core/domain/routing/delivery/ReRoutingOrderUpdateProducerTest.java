package ru.yandex.market.tpl.core.domain.routing.delivery;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.routing.delivery.reroute.ReRoutingOrderUpdateProducer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_ROUTING_MONITORING_ENABLED;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReRoutingOrderUpdateProducerTest {

    private final ReRoutingOrderUpdateProducer producer;
    private final JdbcTemplate jdbcTemplate;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    void init() {
        when(configurationProviderAdapter.isBooleanEnabled(same(IS_ROUTING_MONITORING_ENABLED))).thenReturn(true);
    }

    @Test
    void produce() {
        producer.produce(123L, 1L);
        jdbcTemplate.query("SELECT queue_name, task FROM queue_task", rs -> {
                    assertThat(rs.getString(1)).isEqualTo("REROUTING_ORDER_UPDATE");
                    assertThat(rs.getString(2)).isEqualTo("{\"userShiftId\":123,\"routePointId\":1," +
                            "\"entityId\":\"1\"}");
                }
        );
    }

}
