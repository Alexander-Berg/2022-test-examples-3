package ru.yandex.market.mbo.billing.counter.tt;

import org.junit.Before;
import ru.yandex.market.mbo.billing.counter.AbstractBillingLoaderTest;
import ru.yandex.market.mbo.tt.status.Status;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Базовый тест для счётчиков, не имеющих уникальной логики, а просто считающих таски определённого типа и статуса.
 */
@SuppressWarnings("checkstyle:magicnumber")
public abstract class SimpleTaskCounterBaseTest extends AbstractBillingLoaderTest {

    protected static final long TASK_OWN_ID1 = 9000593L;
    protected static final long TASK_OWN_ID2 = 1044368L;
    protected static final long TASK_OWN_ID3 = 6611379L;
    protected static final long CATEGORY_ID = 9005L;
    protected static final long UID1 = 666L;
    protected static final long UID2 = 16662L;
    protected static final long CONTENT_ID_1 = 1;
    protected static final long CONTENT_ID_2 = 2;

    @Before
    public void setUp() {
        super.setUp();
        getCounter().setBillingOperations(billingOperations);
        doReturn(generateValidTasks()).when(getCounter()).getTasks(any());
    }

    protected abstract TTOperationCounter getCounter();

    protected List<TTOperationCounter.TaskToPay> generateValidTasks() {
        Timestamp now = Timestamp.from(ACTIONS_DATE.toInstant());
        return Arrays.asList(//as from the db
            new TTOperationCounter.TaskToPay(TASK_OWN_ID1, 1L, CONTENT_ID_1, CATEGORY_ID, UID1,
                    Status.TASK_ACCEPTED, now),
            new TTOperationCounter.TaskToPay(TASK_OWN_ID2, 2L, CONTENT_ID_1, CATEGORY_ID, UID2,
                    Status.TASK_ACCEPTED, now),
            new TTOperationCounter.TaskToPay(TASK_OWN_ID3, 3L, CONTENT_ID_2, CATEGORY_ID, UID2,
                    Status.TASK_ACCEPTED_WITHOUT_CHECK, now));
    }
}
