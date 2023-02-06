package ru.yandex.market.delivery.transport_manager.queue;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.put.PutMovementProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.put.PutMovementQueueDto;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;

@TestPropertySource("classpath:/dbqueue/enable_dbqueue.properties")
class DbQueueMessagePersistenceTest extends AbstractContextualTest {

    @Autowired
    PutMovementProducer producer;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void testMessagePersistence() {
        producer.enqueue(EnqueueParams.create(
            new PutMovementQueueDto(100500L)).withExecutionDelay(java.time.Duration.ofSeconds(60))
        );
        var task = getSingleTask();
        softly.assertThat(task.get("attempt")).isEqualTo(0);
        softly.assertThat(task.get("message")).isEqualTo(null);
        softly.assertThat(task.get("request_id")).isEqualTo(null);

        AtomicReference<String> requestId1Ref = new AtomicReference<>();
        AtomicReference<String> requestId2Ref = new AtomicReference<>();

        jdbcTemplate.update("UPDATE dbqueue.task SET next_process_at = now()");

        // Задача неуспешно выполняется один раз с сохранением сообщения об ошибке и трассировки
        Awaitility.waitAtMost(Duration.TEN_SECONDS).until(() -> {
            var processedTask = getSingleTask();
            String requestId = (String) processedTask.get("request_id");
            Object message = processedTask.get("failure_message");
            requestId1Ref.set(requestId);
            return ((int) processedTask.get("attempt")) == 1 &&
                requestId != null &&
                message != null &&
                processedTask.get("failure_message").toString().startsWith("Transportation with id=100500");
        });

        jdbcTemplate.update("UPDATE dbqueue.task SET next_process_at = now()");

        // Задача неуспешно выполняется второй раз. Проверяем, что сохранилась уже другая трассировка
        Awaitility.waitAtMost(Duration.TEN_SECONDS).until(() -> {
            var processedTask = getSingleTask();
            String requestId = (String) processedTask.get("request_id");
            Object message = processedTask.get("failure_message");
            requestId2Ref.set(requestId);
            return ((int) processedTask.get("attempt")) == 2 &&
                requestId != null &&
                message != null &&
                processedTask.get("failure_message").toString().startsWith("Transportation with id=100500");
        });
        softly.assertThat(requestId1Ref.get())
            .isNotEqualTo(requestId2Ref.get());

        // Таска успешно удаляется
        jdbcTemplate.update("DELETE FROM dbqueue.task WHERE id = 1");
        softly.assertThat(jdbcTemplate.queryForList("SELECT 1 FROM dbqueue.task")).isEmpty();
    }

    private Map<String, Object> getSingleTask() {
        var tasks = jdbcTemplate
            .queryForList("SELECT * FROM dbqueue.task t LEFT JOIN dbqueue.task_log tl ON t.id = tl.task_id");
        softly.assertThat(tasks).hasSize(1);
        return tasks.get(0);
    }
}
