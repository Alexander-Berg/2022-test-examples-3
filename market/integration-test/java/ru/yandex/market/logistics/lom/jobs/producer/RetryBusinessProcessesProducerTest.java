package ru.yandex.market.logistics.lom.jobs.producer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.model.RetryBusinessProcessesPayload;

@ParametersAreNonnullByDefault
@DisplayName("Создание тасок на перевыставление бизнес-процессов")
class RetryBusinessProcessesProducerTest extends AbstractContextualTest {

    private static final String REQUEST_ID = "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd";

    @Autowired
    private RetryBusinessProcessesProducer retryBusinessProcessesProducer;
    @Autowired
    private QueueTaskChecker queueTaskChecker;

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Таски создаются батчами по 10 штук")
    void tasksProducedByPartitions(
        @SuppressWarnings("unused") String displayName,
        List<Long> processIds,
        @Nullable List<List<Long>> idsByPayloads
    ) {
        retryBusinessProcessesProducer.produceTasks(processIds);
        if (idsByPayloads == null) {
            queueTaskChecker.assertNoQueueTasksCreated();
            return;
        }

        for (int i = 0; i < idsByPayloads.size(); i++) {
            queueTaskChecker.assertQueueTaskCreated(
                QueueType.RETRY_BUSINESS_PROCESSES,
                new RetryBusinessProcessesPayload(REQUEST_ID, idsByPayloads.get(i)),
                i + 1
            );
        }

        queueTaskChecker.assertQueueTasksCreated(QueueType.RETRY_BUSINESS_PROCESSES, idsByPayloads.size());
    }

    @Nonnull
    private static Stream<Arguments> tasksProducedByPartitions() {
        return Stream.of(
            Arguments.of(
                "Пустой список идентификаторов - таски не создаются",
                List.of(),
                null
            ),
            Arguments.of(
                "Меньше 10 идентификаторов - 1 таска",
                generateIds(3L),
                List.of(generateIds(3L))
            ),
            Arguments.of(
                "10 идентификаторов - 1 таска",
                generateIds(10L),
                List.of(generateIds(10L))
            ),
            Arguments.of(
                "25 идентификаторов - 3 таски",
                generateIds(25L),
                List.of(generateIds(10L), generateIds(11L, 20L), generateIds(21L, 25L))
            )
        );
    }

    @Nonnull
    private static List<Long> generateIds(long minId, long maxId) {
        return LongStream.rangeClosed(minId, maxId)
            .boxed()
            .sorted()
            .collect(Collectors.toList());
    }

    @Nonnull
    private static List<Long> generateIds(long maxId) {
        return generateIds(1L, maxId);
    }
}
