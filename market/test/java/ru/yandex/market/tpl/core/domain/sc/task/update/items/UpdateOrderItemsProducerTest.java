package ru.yandex.market.tpl.core.domain.sc.task.update.items;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UpdateOrderItemsProducerTest {

    private final UpdateOrderItemsProducer producer;

    private final JdbcTemplate jdbcTemplate;

    @Test
    void produce() {
        producer.produce("123", 456L);
        jdbcTemplate.query("select queue_name, task from queue_task", rs -> {
                    assertThat(rs.getString(1)).isEqualTo("UPDATE_ORDER_ITEMS");
                    assertThat(rs.getString(2)).isEqualTo("{\"orderId\":\"123\",\"sortingCenterId\":456," +
                            "\"entityId\":\"123\"}");
                }
        );
    }

}
