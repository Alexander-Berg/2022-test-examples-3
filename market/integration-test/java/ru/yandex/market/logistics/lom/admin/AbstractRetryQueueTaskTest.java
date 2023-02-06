package ru.yandex.market.logistics.lom.admin;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateStatusHistoryYdb;
import ru.yandex.market.logistics.lom.repository.ydb.BusinessProcessStateStatusHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateEntityIdTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateTableDescription;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Перевыставление задач из админки")
public abstract class AbstractRetryQueueTaskTest extends AbstractContextualYdbTest {

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription businessProcessStateStatusHistoryTableDescription;

    @Autowired
    private BusinessProcessStateTableDescription businessProcessStateTableDescription;

    @Autowired
    private BusinessProcessStateEntityIdTableDescription businessProcessStateEntityIdTableDescription;

    @Autowired
    private BusinessProcessStateStatusHistoryYdbRepository businessProcessStateStatusHistoryYdbRepository;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(
            businessProcessStateStatusHistoryTableDescription,
            businessProcessStateTableDescription,
            businessProcessStateEntityIdTableDescription
        );
    }

    @Nonnull
    abstract ResultActions retry();

    @Test
    @SneakyThrows
    @DisplayName("Перевыставить задачу по несуществующему идентификатору состояния бизнес-процесса")
    final void retryTaskErrorBusinessProcessStateNotFound() {
        retry()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_PROCESS] with id [1]"));
    }

    @Test
    @SneakyThrows
    @DatabaseSetup("/service/business_process_state/delivery_service_create_order_external_1_async_request_sent.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/retried_no_author.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешно перевыставить задачу бизнес-процесса")
    final void retryTaskSuccess() {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        retry()
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        assertProcessEnqueued(1L, 0L);
    }

    @Test
    @SneakyThrows
    @DatabaseSetup("/service/business_process_state/register_delivery_track_success_and_error.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/retry_task_with_multiple_entity_ids_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Перевыставить задачу бизнес-процесса с несколькими идентификаторами сущностей")
    final void retryTaskWithMultipleEntityIdsSuccess() {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        retry()
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        assertProcessEnqueued(1L, 1001L);
    }

    @Test
    @SneakyThrows
    @DatabaseSetup("/service/business_process_state/retry_order_business_process_with_active_change_request.xml")
    @DisplayName("Перевыставить задачу по заказу, у которого есть активный запрос на изменение опций доставки")
    final void retryOrderTaskWithActiveChangeDeliveryOptionRequest() {
        retry()
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Перевыставление процесса 1 невозможно, т.к. у связанного заказа "
                    + "есть активная заявка на изменение опции доставки"
            ));
    }

    @Test
    @SneakyThrows
    @DatabaseSetup("/service/business_process_state/retry_order_business_process_with_fail_change_request.xml")
    @ExpectedDatabase(
        value = "/service/business_process_state/retried_order_business_process_with_fail_change_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешно перевыставить задачу по заказу, у которого есть упавший запрос на изменение опций доставки")
    final void retryOrderTaskWithFailChangeDeliveryOptionRequest() {
        clock.setFixed(Instant.parse("2019-11-13T13:30:00.00Z"), ZoneOffset.UTC);
        retry()
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        assertProcessEnqueued(1L, 0L);
    }

    @Test
    @DisplayName("Перевыставить задачу бизнес-процесса, для которого срок последнего изменения еще не истек")
    abstract void retryTaskErrorNonExpiredProcess();

    @Test
    @DisplayName("Перевыставить задачу бизнес-процесса, который находится в терминальном успешном статусе")
    abstract void retryTaskErrorNonTerminalStatusProcess();

    protected void assertProcessEnqueued(Long processId, Long sequenceId) {
        softly.assertThat(businessProcessStateStatusHistoryYdbRepository.getBusinessProcessStatusHistory(
                sequenceId,
                Pageable.unpaged()
            ))
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setId(processId)
                    .setSequenceId(sequenceId)
                    .setStatus(BusinessProcessStatus.ENQUEUED)
                    .setRequestId("1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd")
                    .setCreated(clock.instant())
            ));
    }
}
