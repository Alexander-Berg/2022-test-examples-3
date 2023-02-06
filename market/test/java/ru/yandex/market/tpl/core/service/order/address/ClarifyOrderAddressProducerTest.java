package ru.yandex.market.tpl.core.service.order.address;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class ClarifyOrderAddressProducerTest extends TplAbstractTest {

    private final ClarifyOrderAddressProducer producer;
    private final JdbcTemplate jdbcTemplate;

    @Test
    void produce() {
        producer.produce(123L);
        jdbcTemplate.query("SELECT queue_name, task FROM queue_task", rs -> {
                    assertThat(rs.getString(1)).isEqualTo("CLARIFY_ORDER_ADDRESS");
                    assertThat(rs.getString(2)).isEqualTo("{\"orderId\":123,\"entityId\":\"123\"}");
                }
        );
    }
}
