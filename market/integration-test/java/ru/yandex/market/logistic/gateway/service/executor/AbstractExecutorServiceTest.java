package ru.yandex.market.logistic.gateway.service.executor;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.response.CancelOrderResponse;
import ru.yandex.market.logistic.gateway.exceptions.ExecutorException;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.service.executor.delivery.async.CancelOrderExecutor;
import ru.yandex.market.logistic.gateway.service.executor.delivery.sync.CancelOrderRequestExecutor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

class AbstractExecutorServiceTest extends AbstractIntegrationTest {

    private static final Long TASK_ID = 1L;

    private static final ExecutorTaskWrapper EXECUTOR_TASK_WRAPPER =
        new ExecutorTaskWrapper(TASK_ID, System.currentTimeMillis());

    @Autowired
    private CancelOrderExecutor cancelOrderExecutor;

    @MockBean
    private CancelOrderRequestExecutor requestExecutor;

    @Test
    @DatabaseSetup(
        value = "classpath:repository/state/client_task_for_save_partner_test.xml",
        connection = "dbUnitDatabaseConnection")
    @ExpectedDatabase(
        value = "classpath:repository/expected/client_task_after_exec_for_save_partner_test.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void saveTaskEntityIdsTest() {
        when(requestExecutor.tryExecute(any(), anySet())).thenReturn(new CancelOrderResponse());
        cancelOrderExecutor.execute(EXECUTOR_TASK_WRAPPER);
    }

    @Test
    @DatabaseSetup(
        value = "classpath:repository/state/client_task_invalid_json.xml",
        connection = "dbUnitDatabaseConnection"
    )
    void executeRethrowsExecutorExceptionOnIOException() {
        when(requestExecutor.tryExecute(any(), anySet())).thenReturn(new CancelOrderResponse());
        assertThatThrownBy(() -> cancelOrderExecutor.execute(EXECUTOR_TASK_WRAPPER))
            .isInstanceOf(ExecutorException.class)
            .hasMessageContaining("Unrecognized token");
    }
}
