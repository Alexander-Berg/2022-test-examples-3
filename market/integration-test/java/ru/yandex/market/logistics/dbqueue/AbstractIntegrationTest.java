package ru.yandex.market.logistics.dbqueue;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.yoomoney.tech.dbqueue.config.QueueShard;
import ru.yoomoney.tech.dbqueue.spring.dao.SpringDatabaseAccessLayer;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.dbqueue.config.DbQueueTestConfig;
import ru.yandex.market.logistics.dbqueue.jobs.QueueTaskStatisticsExecutor;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;

@Slf4j
@CleanDatabase
@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(classes = DbQueueTestConfig.class)
@ActiveProfiles("integration-test")
@TestPropertySource("classpath:integration-test.properties")
@TestExecutionListeners(
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
    listeners = {
        DbUnitTestExecutionListener.class,
    }
)
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
public abstract class AbstractIntegrationTest {
    @InjectSoftAssertions
    protected SoftAssertions softly;

    @RegisterExtension
    protected final BackLogCaptor backLogCaptor = new BackLogCaptor("ru.yandex.market.logistics.dbqueue");

    @Autowired
    protected QueueShard<SpringDatabaseAccessLayer> queueShard;

    @Autowired
    protected List<? extends QueueTypeConsumer<?>> consumers;

    @Autowired
    protected DbQueueService dbQueueService;

    @Autowired
    protected QueueTaskStatisticsExecutor queueTaskStatisticsExecutor;

    @Autowired
    protected TestableClock clock;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-11-11T11:11:11.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }
}
