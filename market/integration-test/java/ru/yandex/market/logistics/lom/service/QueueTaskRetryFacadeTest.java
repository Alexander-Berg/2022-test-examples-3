package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistics.lom.dto.queue.RetryBusinessProcessStateError;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateStatusHistoryYdb;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.exception.http.base.BadRequestException;
import ru.yandex.market.logistics.lom.facade.queuetask.QueueTaskRetryFacade;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.ydb.BusinessProcessStateStatusHistoryYdbRepository;
import ru.yandex.market.logistics.lom.service.businessProcess.AbstractBusinessProcessStateYdbServiceTest;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DisplayName("Перевыставление бизнес-процессов")
public class QueueTaskRetryFacadeTest extends AbstractBusinessProcessStateYdbServiceTest {
    private static final OrderHistoryEventAuthor AUTHOR = new OrderHistoryEventAuthor()
        .setTvmServiceId(222L)
        .setYandexUid(BigDecimal.ONE);

    @Autowired
    private BusinessProcessStateStatusHistoryYdbRepository businessProcessStateStatusHistoryYdbRepository;

    @Autowired
    private QueueTaskRetryFacade queueTaskRetryFacade;

    @Test
    @DisplayName("Перевыставить задачу по несуществующему идентификатору состояния бизнес-процесса")
    void retryTaskErrorBusinessProcessStateNotFound() {
        softly.assertThatThrownBy(() -> queueTaskRetryFacade.retryTaskForBusinessProcessState(1L, AUTHOR))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [BUSINESS_PROCESS] with id [1]");

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Перевыставить задачу бизнес-процесса, для которого срок последнего изменения еще не истек")
    @DatabaseSetup("/service/business_process_state/delivery_service_create_order_external_1_async_request_sent.xml")
    void retryTaskErrorNonExpiredProcess() {
        clock.setFixed(Instant.parse("2019-11-13T12:00:30.00Z"), ZoneOffset.UTC);
        softly.assertThatThrownBy(() -> queueTaskRetryFacade.retryTaskForBusinessProcessState(1L, AUTHOR))
            .isInstanceOf(BadRequestException.class)
            .hasMessage(
                "Перевыставление процесса 1 невозможно, т.к. ещё не истекло время (60с) "
                    + "с момента последнего изменения статуса процесса (2019-11-13T12:00:00Z)"
            );

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(
        "/service/business_process_state/ds_create_order_external_1_success_response_processing_succeeded.xml"
    )
    @DisplayName("Перевыставить задачу бизнес-процесса, который находится в терминальном успешном статусе")
    void retryTaskErrorNonTerminalStatusProcess() {
        clock.setFixed(Instant.parse("2019-11-13T12:30:00.00Z"), ZoneOffset.UTC);
        softly.assertThatThrownBy(() -> queueTaskRetryFacade.retryTaskForBusinessProcessState(1L, AUTHOR))
            .isInstanceOf(BadRequestException.class)
            .hasMessage(
                "Перевыставление процесса 1 невозможно, т.к. он находится в успешном терминальном статусе "
                    + "(SUCCESS_RESPONSE_PROCESSING_SUCCEEDED)"
            );

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup("/service/business_process_state/delivery_service_create_order_external_1_async_request_sent.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/retried.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешно перевыставить задачу бизнес-процесса")
    void retryTaskSuccess() {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        queueTaskRetryFacade.retryTaskForBusinessProcessState(1L, AUTHOR);
        softly.assertThat(businessProcessStateStatusHistoryYdbRepository.getBusinessProcessStatusHistory(
                0L,
                Pageable.unpaged()
            ))
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setId(1L)
                    .setSequenceId(0L)
                    .setStatus(BusinessProcessStatus.ENQUEUED)
                    .setRequestId("1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd")
                    .setCreated(clock.instant())
            ));
    }

    @Test
    @DatabaseSetup("/service/business_process_state/delivery_service_create_order_external_1_async_error.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/retried_async.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешно перевыставить задачу бизнес-процесса с sequence id")
    void retryTaskAsync() {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        queueTaskRetryFacade.retryTaskForBusinessProcessState(1L, AUTHOR);
        softly.assertThat(businessProcessStateStatusHistoryYdbRepository.getBusinessProcessStatusHistory(
                2L,
                Pageable.unpaged()
            ))
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setId(1L)
                    .setSequenceId(2L)
                    .setStatus(BusinessProcessStatus.ENQUEUED)
                    .setRequestId("1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd")
                    .setCreated(clock.instant())
            ));
    }

    @Test
    @DisplayName("Перевыставить задачу по несуществующему идентификатору состояния бизнес-процесса")
    void bulkRetryTaskErrorBusinessProcessStateNotFound() {
        softly.assertThat(queueTaskRetryFacade.retryTaskForBusinessProcessStateListFailFree(Set.of(1L, 2L), null))
            .containsAllEntriesOf(Map.of(
                1L,
                RetryBusinessProcessStateError.builder()
                    .errorType(RetryBusinessProcessStateError.ErrorType.NOT_FOUND)
                    .message("Бизнес-процесс c id=1 не найден")
                    .build(),
                2L,
                RetryBusinessProcessStateError.builder()
                    .errorType(RetryBusinessProcessStateError.ErrorType.NOT_FOUND)
                    .message("Бизнес-процесс c id=2 не найден")
                    .build()
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Перевыставить задачу бизнес-процесса, для которого срок последнего изменения еще не истек")
    @DatabaseSetup("/service/business_process_state/delivery_service_create_order_external_1_async_request_sent.xml")
    void bulkRetryTaskErrorNonExpiredProcess() {
        clock.setFixed(Instant.parse("2019-11-13T12:00:30.00Z"), ZoneOffset.UTC);
        softly.assertThat(queueTaskRetryFacade.retryTaskForBusinessProcessStateListFailFree(Set.of(1L), AUTHOR))
            .containsAllEntriesOf(Map.of(
                1L,
                RetryBusinessProcessStateError.builder()
                    .errorType(RetryBusinessProcessStateError.ErrorType.LAST_MODIFY_TIME_NOT_EXPIRED)
                    .message(
                        "Перевыставление процесса 1 невозможно, т.к. ещё не истекло время (60с) "
                            + "с момента последнего изменения статуса процесса (2019-11-13T12:00:00Z)"
                    )
                    .build()
            ));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup(
        "/service/business_process_state/ds_create_order_external_1_success_response_processing_succeeded.xml"
    )
    @DisplayName("Перевыставить задачу бизнес-процесса, который находится в терминальном успешном статусе")
    void bulkRetryTaskErrorNonTerminalStatusProcess() {
        clock.setFixed(Instant.parse("2019-11-13T12:30:00.00Z"), ZoneOffset.UTC);
        softly.assertThat(queueTaskRetryFacade.retryTaskForBusinessProcessStateListFailFree(Set.of(1L), AUTHOR))
            .containsAllEntriesOf(Map.of(
                1L,
                RetryBusinessProcessStateError.builder()
                    .errorType(RetryBusinessProcessStateError.ErrorType.ALREADY_IN_SUCCESS_STATUS)
                    .message(
                        "Перевыставление процесса 1 невозможно, т.к. он находится в успешном терминальном статусе "
                            + "(SUCCESS_RESPONSE_PROCESSING_SUCCEEDED)"
                    )
                    .build()
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
    void bulkRetryTaskSuccess() {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        queueTaskRetryFacade.retryTaskForBusinessProcessStateListFailFree(Set.of(1L), new OrderHistoryEventAuthor());

        assertRetryTaskCreated(1L);
    }

    @Test
    @DatabaseSetup("/service/business_process_state/business_process_state_from_unsupported_queue.xml")
    @DisplayName("Ошибка обработки таски из удаленной очереди")
    void taskFromUnsupportedQueue() {
        softly.assertThatThrownBy(() -> queueTaskRetryFacade.retryTaskForBusinessProcessState(1L, AUTHOR))
            .isInstanceOf(BadRequestException.class)
            .hasMessage(
                "Перевыставление процесса 1 невозможно, т.к. очереди PROCESS_UPDATE_TRANSFER_CODES больше не существует"
            );

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup("/service/business_process_state/register_delivery_track_success_and_error.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/register_delivery_track_success_and_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Перевыставить задачу бизнес-процесса с частично невалидными идентификаторами")
    void bulkRetryTaskWithMultipleEntityIdsPartialNotFound() {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        softly.assertThat(queueTaskRetryFacade.retryTaskForBusinessProcessStateListFailFree(Set.of(1L, 4L, 10L), null))
            .containsAllEntriesOf(Map.of(
                10L,
                RetryBusinessProcessStateError.builder()
                    .errorType(RetryBusinessProcessStateError.ErrorType.NOT_FOUND)
                    .message("Бизнес-процесс c id=10 не найден")
                    .build()
            ));

        assertRetryTaskCreated(List.of(1L, 4L));
    }

    @Test
    @DatabaseSetup("/service/business_process_state/register_delivery_track_success_and_error.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/register_delivery_track_success_and_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Перевыставить задачу бизнес-процесса с незаэкспайренным бизнес-процессом")
    void bulkRetryTaskWithMultipleEntityIdsPartialNotExpired() {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        softly.assertThat(queueTaskRetryFacade
                .retryTaskForBusinessProcessStateListFailFree(Set.of(1L, 4L, 5L), AUTHOR))
            .containsAllEntriesOf(Map.of(
                5L,
                RetryBusinessProcessStateError.builder()
                    .errorType(RetryBusinessProcessStateError.ErrorType.LAST_MODIFY_TIME_NOT_EXPIRED)
                    .message(
                        "Перевыставление процесса 5 невозможно, т.к. ещё не истекло время (60с) "
                            + "с момента последнего изменения статуса процесса (2019-11-13T13:30:00Z)"
                    )
                    .build()
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
    void bulkRetryTaskWithMultipleEntityIdsSuccess() {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        queueTaskRetryFacade.retryTaskForBusinessProcessStateListFailFree(Set.of(1L, 4L), AUTHOR);

        assertRetryTaskCreated(List.of(1L, 4L));
    }

    @Test
    @DisplayName("Перевыставление из YDB бизнес-процесса: родительского процесса нет в Postgres")
    @DatabaseSetup("/service/business_process_state/before/retry_task_from_ydb.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/after/process_from_ydb_retried_no_parent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void retryTaskFromYdb() {
        retryFromYdb();
    }

    @Test
    @DatabaseSetup("/service/business_process_state/before/retry_from_ydb_parent.xml")
    @DisplayName("Перевыставление из YDB бизнес-процесса: родительский процесс есть в Postgres")
    @ExpectedDatabase(
        value = "/service/business_process_state/after/process_from_ydb_retried_has_parent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void retryTaskFromYdbParentIsInPostgres() {
        retryFromYdb();
    }

    private void retryFromYdb() {
        insertProcessToYdb(123L, BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_FAILED);

        clock.setFixed(Instant.parse("2021-08-30T17:12:13.00Z"), ZoneOffset.UTC);
        queueTaskRetryFacade.retryTaskForBusinessProcessState(123L, AUTHOR);
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
            PayloadFactory.createWaybillSegmentPayload(
                2,
                3,
                SEQUENCE_ID_FUNC.apply(123L).toString()
            )
        );
    }
}
