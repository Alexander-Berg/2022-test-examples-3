package ru.yandex.market.tpl.core.domain.routing.delivery;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.routing.delivery.courier.RoutingCourierCreationProducer;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RoutingCourierCreationProducerTest {

    private final RoutingCourierCreationProducer producer;

    private final JdbcTemplate jdbcTemplate;

    @Test
    void produce() {
        producer.produce(123L);
        jdbcTemplate.query("SELECT queue_name, task FROM queue_task", rs -> {
                    assertThat(rs.getString(1)).isEqualTo("ROUTING_COURIER_CREATION");
                    assertThat(rs.getString(2)).isEqualTo("{\"userId\":123,\"entityId\":\"123\"}");
                }
        );
    }

}
