package ru.yandex.market.logistics.utilizer.repo;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.DbqueueTaskType;
import ru.yandex.market.logistics.utilizer.domain.entity.DbQueueLog;
import ru.yandex.market.logistics.utilizer.domain.enums.DbQueueLogEvent;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class DbQueueLogRepositoryTest extends DbqueueContextualTest {

    @Autowired
    private DbQueueLogRepository repository;

    @Test
    @ExpectedDatabase(value = "classpath:fixtures/repo/db-queue-log-repository/after.xml", assertionMode = NON_STRICT,
            connection = "dbqueueDatabaseConnection")
    public void saveWorksCorrect() {
        DbQueueLog dbQueueLog = new DbQueueLog();
        dbQueueLog.setQueueName(DbqueueTaskType.SKU_STOCKS_EVENT);
        dbQueueLog.setEvent(DbQueueLogEvent.NEW);
        dbQueueLog.setPayload("payload");
        dbQueueLog.setTaskId(3L);
        dbQueueLog.setHostName("mockHost");
        repository.save(dbQueueLog);
    }


}
