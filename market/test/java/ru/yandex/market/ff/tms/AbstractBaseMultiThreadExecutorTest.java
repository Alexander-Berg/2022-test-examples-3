package ru.yandex.market.ff.tms;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.exception.FulfillmentWorkflowException;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.request.trace.RequestContextHolder;

public class AbstractBaseMultiThreadExecutorTest extends IntegrationTest {

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Set<Long> failedTaskIds = Set.of(2L, 3L);
    public static final int TOTAL_SUB_TASKS = 3;

    @Autowired
    private HistoryAgent historyAgent;


    @Test
    public void multipleSubTaskExceptionAndAggregatedExceptionHasRequestIds() {
        TestMultiThreadExecutor testMultiThreadExecutor = new TestMultiThreadExecutor(executorService);

        RequestContextHolder.createNewContext();

        try {
            testMultiThreadExecutor.doJob(null);
        } catch (Exception e) {
            String message = e.getMessage();
            System.out.println(message);
            for (Long failedTaskId : failedTaskIds) {
                assertions.assertThat(message).contains(String.format("/%s", failedTaskId));
            }
        } finally {
            RequestContextHolder.clearContext();
        }
    }

    public class TestMultiThreadExecutor extends AbstractBaseMultiThreadExecutor<Long> {

        public TestMultiThreadExecutor(@NotNull ExecutorService executorService) {
            super(executorService, historyAgent);
        }

        @NotNull
        @Override
        protected Collection<Long> getRows() {
            return LongStream.range(1, TOTAL_SUB_TASKS + 1)
                    .boxed()
                    .collect(Collectors.toList());
        }

        @NotNull
        @Override
        protected void processRow(Long row) {
            if (failedTaskIds.contains(row)) {
                throw new FulfillmentWorkflowException("test exception");
            }
        }

        @NotNull
        @Override
        protected ShopRequestRelation<Long> getShopRequestRelation() {
            return ShopRequestRelation.ROW_ENTITY_ITSELF_IS_ID;
        }

    }
}

