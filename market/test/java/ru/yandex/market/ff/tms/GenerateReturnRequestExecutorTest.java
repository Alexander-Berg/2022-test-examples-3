package ru.yandex.market.ff.tms;

import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.service.CustomerReturnService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

/**
 * Интеграционные тесты для {@link GenerateReturnRequestExecutor}.
 *
 * @author avetokhin 02/02/18.
 */
class GenerateReturnRequestExecutorTest extends IntegrationTest {

    @Autowired
    private CustomerReturnService customerReturnService;

    @Autowired
    private HistoryAgent historyAgent;

    @Test
    @DatabaseSetup("classpath:tms/generate-returns/before.xml")
    @ExpectedDatabase(value = "classpath:tms/generate-returns/after.xml", assertionMode = NON_STRICT)
    void generate() {
        new GenerateReturnRequestExecutor(customerReturnService, Executors.newSingleThreadExecutor(), historyAgent)
                .doJob(null);
    }
}
