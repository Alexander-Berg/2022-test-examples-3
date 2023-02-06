package ru.yandex.market.logistics.utilizer.solomon.repository;

import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.DbqueueTaskType;
import ru.yandex.market.logistics.utilizer.dbqueue.enums.NumberOfRetriesInterval;
import ru.yandex.market.logistics.utilizer.dbqueue.state.DbqueueState;

public class DbqueueRepositoryTest extends AbstractContextualTest {

    @Autowired
    private DbqueueRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/empty.xml")
    public void getStateByQueueWorksCorrect() {
        insert(DbqueueTaskType.CREATE_TRANSFER, 0);
        insert(DbqueueTaskType.CREATE_TRANSFER, 16);
        insert(DbqueueTaskType.SKU_STOCKS_EVENT, 11);
        Map<DbqueueTaskType, DbqueueState> stateByQueue = repository.getStateByQueue();

        DbqueueState createTransferState = stateByQueue.get(DbqueueTaskType.CREATE_TRANSFER);
        assertNotNullStateIsCorrect(createTransferState, 2, 1, 0, 1);

        DbqueueState skuStockEventState = stateByQueue.get(DbqueueTaskType.SKU_STOCKS_EVENT);
        assertNotNullStateIsCorrect(skuStockEventState, 1, 0, 1, 0);

        DbqueueState sendUtilizationCycleFinalizationEmailState =
                stateByQueue.get(DbqueueTaskType.SEND_UTILIZATION_CYCLE_FINALIZATION_EMAIL);
        assertNotNullStateIsCorrect(sendUtilizationCycleFinalizationEmailState, 0, 0, 0, 0);
    }

    private void insert(DbqueueTaskType queue, int attempt) {
        jdbcTemplate.update("insert into dbqueue.task " +
                "(queue_name, payload, created_at, next_process_at, attempt, total_attempt) " +
                "values (?, '{\"task\":" + attempt + "}', now(), now(), 0, ?)", queue.name(), attempt);
    }

    private void assertNotNullStateIsCorrect(DbqueueState state,
                                             int elementsInQueue,
                                             int elementsWithoutRetries,
                                             int elementsWithFewRetries,
                                             int elementsWithManyRetries) {
        softly.assertThat(state).isNotNull();
        softly.assertThat(state.getElementsInQueue()).isEqualTo(elementsInQueue);
        Map<NumberOfRetriesInterval, Long> elementsWithRetries =
                state.getNumberOfElementsWithRetriesInterval();
        softly.assertThat(elementsWithRetries.get(NumberOfRetriesInterval.NO_RETRIES))
                .isEqualTo(elementsWithoutRetries);
        softly.assertThat(elementsWithRetries.get(NumberOfRetriesInterval.FEW_RETRIES))
                .isEqualTo(elementsWithFewRetries);
        softly.assertThat(elementsWithRetries.get(NumberOfRetriesInterval.MANY_RETRIES))
                .isEqualTo(elementsWithManyRetries);
    }
}
