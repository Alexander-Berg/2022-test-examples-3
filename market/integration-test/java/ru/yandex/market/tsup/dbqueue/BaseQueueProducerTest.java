package ru.yandex.market.tsup.dbqueue;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.common.db.queue.base.BaseQueueProducer;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.dbqueue.pipeline.cube_runner.PipelineCubeRunnerDto;

@DatabaseSetup("/repository/dbqueue/empty.xml")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnectionDbQueue"})
public class BaseQueueProducerTest extends AbstractContextualTest {
    @Autowired
    private BaseQueueProducer baseQueueProducer;

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2021-09-15T18:01:00.00Z"), ZoneId.systemDefault());
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_enqueued.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test() {
        baseQueueProducer.enqueue(new PipelineCubeRunnerDto(1L));
    }
}
