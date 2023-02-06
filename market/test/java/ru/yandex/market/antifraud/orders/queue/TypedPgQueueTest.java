package ru.yandex.market.antifraud.orders.queue;


import java.time.Duration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StopWatch;

import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;

import static org.assertj.core.api.Assertions.assertThat;

@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TypedPgQueueTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    @Test
    public void offerAndPeek() {
        var queue = createQueue("offerAndPeek", 5);
        queue.offer(5);
        assertThat(queue.peek()).isEqualTo(5);
        assertThat(queue.peek()).isEqualTo(5);
    }

    @Test
    public void offerAndPoll() {
        var queue = createQueue("offerAndPoll", 5);
        queue.offer(5);
        assertThat(queue.poll()).isEqualTo(5);
        assertThat(queue.poll()).isNull();
    }

    @Test
    public void size() {
        var queue = createQueue("size", 5);
        queue.offer(5);
        queue.offer(3);
        queue.offer(1);
        assertThat(queue.size()).isEqualTo(3);
    }

    @Test
    public void delay() {
        var delay = 500;
        var queue = createQueue("delay", delay);
        var stopWatch = new StopWatch();
        stopWatch.start();
        queue.offer(5);
        queue.peek();
        stopWatch.stop();
        assertThat(stopWatch.getTotalTimeMillis()).isGreaterThan(delay - 15);
    }

    private TypedPgQueue<Integer> createQueue(String offerAndPeek, int delay) {
        return new TypedPgQueue<>(offerAndPeek, Duration.ofMillis(delay), Integer.class, new PgQueueDao(jdbcTemplate));
    }
}
