package ru.yandex.market.logistics.lom.jobs.producer;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateStatusHistoryYdb;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.ydb.BusinessProcessStateStatusHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DisplayName("Тесты на корректность создания бизнес процессов для очередей DbQueue")
class BaseQueueProducerBusinessProcessTest extends AbstractContextualYdbTest {

    @Autowired
    private OrderExternalValidationProducer orderExternalValidationProducer;

    @Autowired
    private PublishLogbrokerHistoryEventsProducer noBusinessProcessProducer;

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription ydbHistoryTable;

    @Autowired
    private BusinessProcessStateStatusHistoryYdbRepository ydbHistoryRepository;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-08-30T11:12:13.00Z"), clock.getZone());
    }

    @Test
    @DisplayName("Бизнес процесс создается для очереди с businessProcessNeeded = true, факт создания сохраняется в ydb")
    @ExpectedDatabase(
        value = "/jobs/producer/after/business_process_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void businessProcessCreated() {
        orderExternalValidationProducer.produceTask(1L);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.VALIDATE_ORDER_EXTERNAL,
            PayloadFactory.createOrderIdPayload(1L, "1", 1L)
        );

        softly.assertThat(
                ydbHistoryRepository.getBusinessProcessStatusHistory(1L, Pageable.unpaged())
            )
            .usingRecursiveComparison()
            .isEqualTo(List.of(
                new BusinessProcessStateStatusHistoryYdb()
                    .setId(1L)
                    .setSequenceId(1L)
                    .setStatus(BusinessProcessStatus.ENQUEUED)
                    .setCreated(clock.instant())
                    .setRequestId("1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1")
            ));
    }

    @Test
    @DisplayName("Бизнес процесс не создается для очереди с businessProcessNeeded = false, в ydb ничего не пишется")
    @ExpectedDatabase(
        value = "/jobs/producer/after/business_process_not_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void noBusinessProcessCreated() {
        noBusinessProcessProducer.produceTask(1);

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PUBLISH_LOGBROKER_HISTORY_EVENTS,
            PayloadFactory.logbrokerSourceIdPayload(1, "1", 1L)
        );

        softly.assertThat(ydbHistoryRepository.getBusinessProcessStatusHistory(1L, Pageable.unpaged())).isEmpty();
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(ydbHistoryTable);
    }
}
