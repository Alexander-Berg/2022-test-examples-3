package ru.yandex.market.delivery.transport_manager.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.TaskType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.DbQueueMapper;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnectionDbQueue"})
public class DbQueueMapperTest extends AbstractContextualTest {
    @Autowired
    private DbQueueMapper dbQueueMapper;

    @Test
    @DatabaseSetup(
        value = "/repository/task/xdoc_create.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void testExists() {
        softly.assertThat(dbQueueMapper.exists(1L)).isTrue();
        softly.assertThat(dbQueueMapper.exists(2L)).isFalse();
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/task/xdoc_create.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testInsert() {
        softly.assertThat(dbQueueMapper.insert(
            TaskType.X_DOC_CREATE_FF.name(),
            "{\"requestId\":1024,\"status\":\"VALIDATED\"}"
        )).isEqualTo(1L);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/task/xdoc_create.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void processImmediatelyAndFlush() {
        softly.assertThat(dbQueueMapper.processImmediatelyAndFlush(1L)).isEqualTo(1);
    }
}
