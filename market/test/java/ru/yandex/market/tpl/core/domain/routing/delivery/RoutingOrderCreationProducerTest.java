package ru.yandex.market.tpl.core.domain.routing.delivery;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.routing.delivery.order.RoutingOrderCreationProducer;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RoutingOrderCreationProducerTest {

    private final RoutingOrderCreationProducer producer;

    private final JdbcTemplate jdbcTemplate;

    @Test
    void produce() {
        producer.produce(123L);
        jdbcTemplate.query("SELECT queue_name, task FROM queue_task", rs -> {
                    assertThat(rs.getString(1)).isEqualTo("ROUTING_ORDER_CREATION");
                    assertThat(rs.getString(2)).isEqualTo("{\"userShiftId\":123,\"entityId\":\"123\"}");
                }
        );
    }

}
