package ru.yandex.market.logistics.lom.jobs.processor;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.LogbrokerProperties;
import ru.yandex.market.logistics.lom.entity.enums.LogbrokerSourceLockType;
import ru.yandex.market.logistics.lom.jobs.consumer.SetLogbrokerIdForHistoryEventsConsumer;
import ru.yandex.market.logistics.lom.jobs.model.LogbrokerSourceIdPayload;
import ru.yandex.market.logistics.lom.service.order.history.LogbrokerSourceService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SetLogbrokerIdForHistoryEventsProcessorTest extends AbstractContextualTest {
    @Autowired
    private DataFieldMaxValueIncrementer logbrokerIdSequence;
    @Autowired
    private SetLogbrokerIdForHistoryEventsConsumer setLogbrokerIdForHistoryEventsConsumer;
    @Autowired
    private LogbrokerSourceService logbrokerSourceService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private LogbrokerProperties lomLogbrokerProperties;

    private static final LogbrokerSourceIdPayload PAYLOAD = PayloadFactory.logbrokerSourceIdPayload(1, "1", 1L);
    private static final Task<LogbrokerSourceIdPayload> TASK = TaskFactory.createTask(PAYLOAD);

    private int propagation;

    @BeforeEach
    void setup() {
        propagation = transactionTemplate.getPropagationBehavior();
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.setPropagationBehavior(propagation);
        lomLogbrokerProperties.getExport().setBatchSize(200);
    }

    @Test
    @DisplayName("Успешное проставление logbrokerId за несколько итераций")
    @DatabaseSetup("/jobs/processor/set_logbroker_id_for_history_events/before/setup.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/set_logbroker_id_for_history_events/after/updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @JpaQueriesCount(9)
    void testSuccess() {
        lomLogbrokerProperties.getExport().setBatchSize(2);
        when(logbrokerIdSequence.nextLongValue()).thenReturn(3L, 4L, 5L);
        setLogbrokerIdForHistoryEventsConsumer.execute(TASK);
    }

    @Test
    @DisplayName("Лок занят другой транзакцией")
    @DatabaseSetup("/jobs/processor/set_logbroker_id_for_history_events/before/setup.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/set_logbroker_id_for_history_events/after/not_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void lockNotAvailable() {
        transactionTemplate.execute(tc -> {
            logbrokerSourceService.getRowLock(1, LogbrokerSourceLockType.SET_LOGBROKER_ID);
            setLogbrokerIdForHistoryEventsConsumer.execute(TASK);
            return null;
        });
        verifyZeroInteractions(logbrokerIdSequence);
    }
}
