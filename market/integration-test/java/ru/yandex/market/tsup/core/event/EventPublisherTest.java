package ru.yandex.market.tsup.core.event;

import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.event.impl.demo.DemoEventPayload;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnectionDbQueue"})
class EventPublisherTest extends AbstractContextualTest {
    @Autowired
    private EventPublisher eventPublisher;

    @Test
    @ExpectedDatabase(
        value = "/repository/dbqueue/event/after_publishing_event.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void publish() {
        eventPublisher.publish(EventType.DEMO, new DemoEventPayload("i am striiiing"));
    }
}
