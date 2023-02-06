package ru.yandex.market.logistics.tarifficator.util;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.SortedMap;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.logistics.tarifficator.jobs.model.QueueType;
import ru.yandex.market.logistics.tarifficator.jobs.model.TraceableExecutionQueuePayload;

@ParametersAreNonnullByDefault
public final class QueueTasksTestUtil {

    private static final QueueTaskMapper QUEUE_TASK_MAPPER = new QueueTaskMapper();

    private QueueTasksTestUtil() {
        throw new UnsupportedOperationException();
    }

    public static void assertQueueTasks(
        SoftAssertions softly,
        ObjectMapper objectMapper,
        JdbcTemplate jdbcTemplate,
        SortedMap<QueueType, ? extends TraceableExecutionQueuePayload> expectedTasks
    ) {
        List<QueueTask> tasks = jdbcTemplate.query(
            "SELECT queue_name, task FROM queue_tasks ORDER BY id",
            QUEUE_TASK_MAPPER
        );
        softly.assertThat(tasks).hasSize(expectedTasks.size());

        StreamEx.of(tasks)
            .zipWith(expectedTasks.entrySet().stream())
            .forKeyValue((task, expectedTask) -> {
                softly.assertThat(task.queueType).isEqualTo(expectedTask.getKey());
                try {
                    TraceableExecutionQueuePayload expectedPayload = expectedTask.getValue();
                    Object payload = objectMapper.readValue(task.task, expectedPayload.getClass());
                    softly.assertThat(payload)
                        .usingRecursiveComparison()
                        .isEqualTo(expectedPayload);
                } catch (IOException e) {
                    softly.fail(e.getMessage());
                }
            });
    }

    @AllArgsConstructor
    private static class QueueTask {
        private QueueType queueType;
        private String task;
    }

    @Nonnull
    private static class QueueTaskMapper implements RowMapper<QueueTask> {
        @Override
        public QueueTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new QueueTask(
                QueueType.valueOf(rs.getString("queue_name")),
                rs.getString("task")
            );
        }
    }
}
