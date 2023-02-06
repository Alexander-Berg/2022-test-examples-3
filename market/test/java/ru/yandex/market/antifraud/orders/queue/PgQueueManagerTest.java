package ru.yandex.market.antifraud.orders.queue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.logbroker.entities.CancelOrderRequest;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;

import static org.assertj.core.api.Assertions.assertThat;

@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PgQueueManagerTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    @Autowired
    private Environment environment;

    @Test
    public void getQueueShouldReturnSingletons() {
        var pgQueueManager = new PgQueueManager(jdbcTemplate, environment);
        var queue = pgQueueManager.getQueue("cancelRequests", CancelOrderRequest.class);
        assertThat(pgQueueManager.getQueue("cancelRequests", CancelOrderRequest.class))
                .isSameAs(queue);
    }
}
