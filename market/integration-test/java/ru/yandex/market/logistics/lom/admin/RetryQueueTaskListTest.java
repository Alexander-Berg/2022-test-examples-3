package ru.yandex.market.logistics.lom.admin;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.admin.filter.ActionDto;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.service.businessProcess.AbstractBusinessProcessStateYdbServiceTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Тесты на ручку массового перевыставления задач")
public class RetryQueueTaskListTest extends AbstractBusinessProcessStateYdbServiceTest {

    @Autowired
    private QueueTaskChecker queueTaskChecker;

    @Test
    @DisplayName("Перевыставить задачу по несуществующему идентификатору состояния бизнес-процесса")
    void bulkRetryTaskErrorBusinessProcessStateNotFound() throws Exception {
        retry(Set.of(1L, 2L))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Бизнес-процессы, которые не были перевыставлены: \n"
                    + "Бизнес-процесс c id=1 не найден\n"
                    + "Бизнес-процесс c id=2 не найден"
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Перевыставить задачу бизнес-процесса, для которого срок последнего изменения еще не истек")
    @DatabaseSetup("/service/business_process_state/delivery_service_create_order_external_1_async_request_sent.xml")
    void bulkRetryTaskErrorNonExpiredProcess() throws Exception {
        clock.setFixed(Instant.parse("2019-11-13T12:00:30.00Z"), ZoneOffset.UTC);
        retry(Set.of(1L))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Бизнес-процессы, которые не были перевыставлены: \n"
                    + "Перевыставление процесса 1 невозможно, т.к. ещё не истекло время (60с) "
                    + "с момента последнего изменения статуса процесса (2019-11-13T12:00:00Z)"
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(
        "/service/business_process_state/ds_create_order_external_1_success_response_processing_succeeded.xml"
    )
    @DisplayName("Перевыставить задачу бизнес-процесса, который находится в терминальном успешном статусе")
    void bulkRetryTaskErrorNonTerminalStatusProcess() throws Exception {
        clock.setFixed(Instant.parse("2019-11-13T12:30:00.00Z"), ZoneOffset.UTC);
        retry(Set.of(1L))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Бизнес-процессы, которые не были перевыставлены: \n"
                    + "Перевыставление процесса 1 невозможно, т.к. он находится в успешном терминальном статусе "
                    + "(SUCCESS_RESPONSE_PROCESSING_SUCCEEDED)"
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup("/service/business_process_state/delivery_service_create_order_external_1_async_request_sent.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/delivery_service_create_order_external_1_async_request_sent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешно перевыставить задачу бизнес-процесса")
    void bulkRetryTaskSuccess() throws Exception {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        retry(Set.of(1L))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        assertRetryTaskCreated(1L);
    }

    @Test
    @DatabaseSetup("/service/business_process_state/register_delivery_track_success_and_error.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/register_delivery_track_success_and_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Перевыставить задачу бизнес-процесса с частично невалидными идентификаторами")
    void bulkRetryTaskWithMultipleEntityIdsPartialNotFound() throws Exception {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        retry(Set.of(1L, 4L, 10L))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Бизнес-процессы, которые не были перевыставлены: \nБизнес-процесс c id=10 не найден"
            ));

        assertRetryTaskCreated(List.of(1L, 4L));
    }

    @Test
    @DatabaseSetup("/service/business_process_state/register_delivery_track_success_and_error.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/register_delivery_track_success_and_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Перевыставить задачу бизнес-процесса с незаэкспайренным безнес-процессом")
    void bulkRetryTaskWithMultipleEntityIdsPartialNotExpired() throws Exception {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        retry(Set.of(1L, 4L, 5L))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Бизнес-процессы, которые не были перевыставлены: \n"
                    + "Перевыставление процесса 5 невозможно, т.к. ещё не истекло время (60с) "
                    + "с момента последнего изменения статуса процесса (2019-11-13T13:30:00Z)"
            ));

        assertRetryTaskCreated(List.of(1L, 4L));
    }

    @Test
    @DatabaseSetup("/service/business_process_state/register_delivery_track_success_and_error.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/register_delivery_track_success_and_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Перевыставить задачу бизнес-процесса с несколькими идентификаторами сущностей")
    void bulkRetryTaskWithMultipleEntityIdsSuccess() throws Exception {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        retry(Set.of(1L, 4L))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        assertRetryTaskCreated(List.of(1L, 4L));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное перевыставление бизнес-процессов из YDB")
    @DatabaseSetup("/service/business_process_state/before/retry_task_from_ydb.xml")
    void bulkRetryTasksFromYdbSuccess() {
        insertProcessesToYdb(Map.of(
            111L,
            BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_FAILED,
            222L,
            BusinessProcessStatus.QUEUE_TASK_ERROR
        ));
        clock.setFixed(Instant.parse("2021-08-30T17:12:13.00Z"), ZoneOffset.UTC);

        retry(Set.of(111L, 222L))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        assertRetryTaskCreated(List.of(111L, 222L));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное перевыставление бизнес-процессов из YDB + Postgres")
    @DatabaseSetup({
        "/service/business_process_state/delivery_service_create_order_external_1_async_request_sent.xml",
        "/service/business_process_state/before/order_for_ydb_process.xml",
    })
    void bulkRetryTasksFromPostgresAndYdb() {
        insertProcessesToYdb(Map.of(
            111L,
            BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_FAILED
        ));
        clock.setFixed(Instant.parse("2021-08-30T17:12:13.00Z"), ZoneOffset.UTC);

        retry(Set.of(1L, 111L))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        assertRetryTaskCreated(List.of(1L, 111L));
    }

    @Nonnull
    private ResultActions retry(Set<Long> ids) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.POST, "/admin/business-processes/retry-list", new ActionDto().setIds(ids))
        );
    }
}

