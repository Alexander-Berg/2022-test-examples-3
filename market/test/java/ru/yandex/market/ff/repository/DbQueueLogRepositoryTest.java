package ru.yandex.market.ff.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.DbQueueLog;
import ru.yandex.market.ff.model.enums.DbQueueLogEvent;
import ru.yandex.market.ff.model.enums.DbQueueType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class DbQueueLogRepositoryTest extends IntegrationTest {

    @Autowired
    private DbQueueLogRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/db-queue-log-repository/before.xml")
    @ExpectedDatabase(value = "classpath:repository/db-queue-log-repository/after.xml", assertionMode = NON_STRICT)
    public void saveWorksCorrect() {
        DbQueueLog dbQueueLog = new DbQueueLog();
        dbQueueLog.setQueueName(DbQueueType.VALIDATE_CIS);
        dbQueueLog.setEvent(DbQueueLogEvent.NEW);
        dbQueueLog.setEntityId(2L);
        dbQueueLog.setTaskId(3L);
        dbQueueLog.setHostName("hostName");
        repository.save(dbQueueLog);
    }

    @Test
    @DatabaseSetup("classpath:repository/db-queue-log-repository/before-select.xml")
    @ExpectedDatabase(value = "classpath:repository/db-queue-log-repository/before-select.xml",
            assertionMode = NON_STRICT)
    public void findMaxIdAfterOffsetWhenExists() {
        Long maxIdAfterOffset = repository.findMaxIdAfterOffset(3);
        assertions.assertThat(maxIdAfterOffset).isEqualTo(3);
    }

    @Test
    @DatabaseSetup("classpath:repository/db-queue-log-repository/before-select.xml")
    @ExpectedDatabase(value = "classpath:repository/db-queue-log-repository/before-select.xml",
            assertionMode = NON_STRICT)
    public void findMaxIdAfterOffsetWhenNotExists() {
        Long maxIdAfterOffset = repository.findMaxIdAfterOffset(7);
        assertions.assertThat(maxIdAfterOffset).isNull();
    }

    @Test
    @DatabaseSetup("classpath:repository/db-queue-log-repository/before-select.xml")
    @ExpectedDatabase(value = "classpath:repository/db-queue-log-repository/after-delete.xml",
            assertionMode = NON_STRICT)
    public void deleteWorksCorrect() {
        repository.deleteByIdLessThanEqual(5, 500);
    }
}
