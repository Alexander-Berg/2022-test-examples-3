package ru.yandex.market.ff.tms;

import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.dbqueue.producer.FinishUpdatingRequestQueueProducer;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.service.ShopRequestFetchingService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

/**
 * Функциональный тест для {@link UpdatingRequestFinalizerExecutor}.
 */
class UpdatingRequestFinalizerExecutorTest extends IntegrationTest {

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private FinishUpdatingRequestQueueProducer queueProducer;

    private UpdatingRequestFinalizerExecutor executor;

    @Autowired
    private HistoryAgent historyAgent;

    @BeforeEach
    void init() {
        executor = new UpdatingRequestFinalizerExecutor(Executors.newSingleThreadExecutor(),
                shopRequestFetchingService, queueProducer, historyAgent);
    }

    @Test
    @DatabaseSetup("classpath:tms/finalize-updating-request/before.xml")
    @ExpectedDatabase(value = "classpath:tms/finalize-updating-request/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void executeSuccessfully() {
        executor.doJob(null);
    }
}
