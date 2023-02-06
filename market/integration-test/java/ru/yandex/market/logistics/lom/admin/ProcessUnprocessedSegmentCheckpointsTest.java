package ru.yandex.market.logistics.lom.admin;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.jobs.model.ProcessSegmentCheckpointsPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateEntityIdTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateTableDescription;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DatabaseSetup("/controller/admin/process_checkpoints_task/before/prepare.xml")
@DisplayName("Создание таски на обработку необработанных чекпоинтов")
class ProcessUnprocessedSegmentCheckpointsTest extends AbstractContextualYdbTest {

    @Autowired
    private BusinessProcessStateTableDescription businessProcessStateTableDescription;

    @Autowired
    private BusinessProcessStateEntityIdTableDescription businessProcessStateEntityIdTableDescription;

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription stateStatusHistoryTableDescription;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(
            businessProcessStateTableDescription,
            businessProcessStateEntityIdTableDescription,
            stateStatusHistoryTableDescription
        );
    }

    @Test
    @SneakyThrows
    @ExpectedDatabase(
        value = "/controller/admin/process_checkpoints_task/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Сегмент удовлетворяет требованиям, таска создается")
    void taskCreated() {
        processUnprocessedCheckpoints(1L)
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_SEGMENT_CHECKPOINTS,
            new ProcessSegmentCheckpointsPayload(
                REQUEST_ID + "/1",
                1L,
                101L,
                new OrderHistoryEventAuthor()
            )
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Сегмент с заданным идентификатором не найден")
    @ExpectedDatabase(
        value = "/controller/admin/process_checkpoints_task/before/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void segmentNotFound() {
        processUnprocessedCheckpoints(123456789L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAYBILL_SEGMENT] with id [123456789]"));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @SneakyThrows
    @DisplayName("Пустое тело запроса")
    @ExpectedDatabase(
        value = "/controller/admin/process_checkpoints_task/before/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void invalidIdBody() {
        mockMvc.perform(
                post("/admin/routes/waybill/process-checkpoints")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andExpect(status().isBadRequest());

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @SneakyThrows
    @MethodSource
    @ExpectedDatabase(
        value = "/controller/admin/process_checkpoints_task/before/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Сегмент некорректный для создания таски обработки чекпоинтов")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void segmentIsIncorrectForProcessCheckpointsTask(
        @SuppressWarnings("unused") String displayName,
        long segmentId,
        String errorMessage
    ) {
        processUnprocessedCheckpoints(segmentId)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(errorMessage));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> segmentIsIncorrectForProcessCheckpointsTask() {
        return Stream.of(
            Arguments.of(
                "Сегмент неактивный",
                2L,
                "Невозможно обработать чекпоинты сегмента: сегмент неактивный."
            ),
            Arguments.of(
                "У сегмента нет необработанных статусов",
                3L,
                "Невозможно обработать чекпоинты сегмента: у сегмента нет необработанных статусов."
            ),
            Arguments.of(
                "У сегмента есть активная таска на обработку статусов",
                4L,
                "Невозможно обработать чекпоинты сегмента: у сегмента есть запущенный процесс обработки чекпоинтов."
            ),
            Arguments.of(
                "У сегмента не заполнено поле trackerId",
                5L,
                "Невозможно обработать чекпоинты сегмента: у сегмента нет ID в трекере."
            )
        );
    }

    @Nonnull
    @SneakyThrows
    private ResultActions processUnprocessedCheckpoints(long segmentId) {
        return mockMvc.perform(
            post("/admin/routes/waybill/process-checkpoints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + segmentId + "}")
        );
    }
}
