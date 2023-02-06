package ru.yandex.market.logistics.lom.jobs.executor;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.producer.AbstractLogbrokerHistoryEventsProducer;
import ru.yandex.market.logistics.lom.jobs.producer.PublishLogbrokerHistoryEventsProducer;
import ru.yandex.market.logistics.lom.jobs.producer.SetLogbrokerIdForHistoryEventsProducer;
import ru.yandex.market.logistics.lom.service.order.history.LogbrokerSourceService;

@DatabaseSetup("/jobs/executor/generatePublishLogbrokerHistoryEventsExecutor/before/setup.xml")
public class GeneratePublishLogbrokerHistoryEventsTasksExecutorTest extends AbstractContextualTest {
    @Autowired
    private PublishLogbrokerHistoryEventsProducer publishLogbrokerHistoryEventsProducer;
    @Autowired
    private SetLogbrokerIdForHistoryEventsProducer setLogbrokerIdForHistoryEventsProducer;
    @Autowired
    private LogbrokerSourceService logbrokerSourceService;

    @Test
    @DisplayName("Успешное создание задач для простановки logbrokerId ивентов")
    @ExpectedDatabase(
        value = "/jobs/executor/generatePublishLogbrokerHistoryEventsExecutor/after/set_logbrokerId_tasks_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void setLogbrokerIdEventsCreated() {
        createAndRunExecutorWithProducer(setLogbrokerIdForHistoryEventsProducer);
    }

    @Test
    @DisplayName("Успешное создание задач для отправки ивентов")
    @ExpectedDatabase(
        value = "/jobs/executor/generatePublishLogbrokerHistoryEventsExecutor/after/publish_events_tasks_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void publishLogbrokerEventsCreated() {
        createAndRunExecutorWithProducer(publishLogbrokerHistoryEventsProducer);
    }

    private void createAndRunExecutorWithProducer(AbstractLogbrokerHistoryEventsProducer producer) {
        new GenerateLogbrokerHistoryEventsTasksExecutor(logbrokerSourceService, producer)
            .doJob(null);
    }
}
