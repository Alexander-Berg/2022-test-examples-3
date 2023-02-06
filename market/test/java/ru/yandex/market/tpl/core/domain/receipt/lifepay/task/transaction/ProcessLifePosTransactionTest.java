package ru.yandex.market.tpl.core.domain.receipt.lifepay.task.transaction;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.routing.delivery.courier.RoutingCourierCreationProducer;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProcessLifePosTransactionTest {

    private final ProcessLifePosTransactionProducer producer;

    private final JdbcTemplate jdbcTemplate;

    @Test
    void produce() {
        producer.produce(123L);
        jdbcTemplate.query("SELECT queue_name, task FROM queue_task", rs -> {
                    assertThat(rs.getString(1)).isEqualTo("PROCESS_LIFE_POS_TRANSACTION");
                    assertThat(rs.getString(2)).isEqualTo("{\"transactionId\":123,\"entityId\":\"123\"}");
                }
        );
    }

}
