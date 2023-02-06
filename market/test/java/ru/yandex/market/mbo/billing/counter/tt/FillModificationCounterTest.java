package ru.yandex.market.mbo.billing.counter.tt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.BatchUpdateData;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.Status;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("checkstyle:magicnumber")
@RunWith(MockitoJUnitRunner.Silent.class)
public class FillModificationCounterTest extends SimpleTaskCounterBaseTest {
    @Spy
    private FillModificationCounter counter = new FillModificationCounter();

    @Override
    protected TTOperationCounter getCounter() {
        return counter;
    }

    /**
     * Allowed type is:
     * {@link TaskType#FILL_MODIFICATION}.
     * Allowed statuses are:
     * {@link Status#TASK_ACCEPTED},
     * {@link Status#TASK_ACCEPTED_WITHOUT_CHECK}.
     */
    @Test
    public void testTaskBillingActionsCreated() {
        counter.doLoad(INTERVAL, tarifProvider);

        verify(operationsUpdater, times(3)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
            createBilledEntityAction(PaidAction.FILL_MODIFICATION_CARD, TASK_OWN_ID1, CATEGORY_ID, UID1, CONTENT_ID_1,
                    AuditAction.EntityType.MODEL_GURU),
            createBilledEntityAction(PaidAction.FILL_MODIFICATION_CARD, TASK_OWN_ID2, CATEGORY_ID, UID2, CONTENT_ID_1,
                    AuditAction.EntityType.MODEL_GURU),
            createBilledEntityAction(PaidAction.FILL_MODIFICATION_CARD, TASK_OWN_ID3, CATEGORY_ID, UID2, CONTENT_ID_2,
                    AuditAction.EntityType.MODEL_GURU)
        );
    }
}
