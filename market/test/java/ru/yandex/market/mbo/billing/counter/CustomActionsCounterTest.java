package ru.yandex.market.mbo.billing.counter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.info.BillingOperationInfoBase;
import ru.yandex.market.mbo.billing.custom.BillingCustomActionsServiceMock;
import ru.yandex.market.mbo.gwt.models.billing.CustomAction;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbo.billing.tarif.TarifManagerImpl.fromRubles;

@SuppressWarnings("checkstyle:magicnumber")
@RunWith(MockitoJUnitRunner.Silent.class)
public class CustomActionsCounterTest extends AbstractBillingLoaderTest {

    private static final long CATEGORY_ID = 195923L;
    private static final long UID1 = 3321L;
    private static final long UID2 = 4352L;

    private CustomActionsCounter counter;
    private BillingCustomActionsServiceMock customActions;

    @Before
    public void setup() {
        super.setUp();
        customActions = new BillingCustomActionsServiceMock();
        counter = new CustomActionsCounter();
        counter.setBillingOperations(billingOperations);
        counter.setCustomActionsService(customActions);
    }
    @Test
    public void testCustomActionsBilled() {
        customActions.saveAction(action(111L, UID1, 1L, 30.0), UID1);
        customActions.saveAction(action(222L, UID1, 2L, 60.0), UID1);
        customActions.saveAction(action(333L, UID2, 3L, 15.0), UID2);

        counter.doLoad(INTERVAL, null);
        verify(operationsUpdater, times(3)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
            billedCustomAction(111L, UID1, 1L, 30.0),
            billedCustomAction(222L, UID1, 2L, 60.0),
            billedCustomAction(333L, UID2, 3L, 15.0)
        );
    }

    private CustomAction action(long actionId, long userId, long count, double price) {
        CustomAction customAction = new CustomAction();
        customAction.setId(actionId);
        customAction.setUserId(userId);
        customAction.setCount(BigDecimal.valueOf(count));
        customAction.setPrice(BigDecimal.valueOf(price));
        customAction.setCategoryId(CATEGORY_ID);
        customAction.setCreatedDate(INTERVAL.first.getTime());
        return customAction;
    }

    private BatchUpdateData billedCustomAction(long sourceId, long uid, long count, double price) {
        return new BatchUpdateData(
                uid,
                CATEGORY_ID,
                new Timestamp(INTERVAL.first.getTimeInMillis()),
                PaidAction.CUSTOM_ACTION.getId(),
                fromRubles(BigDecimal.valueOf(price)),
                null,
                sourceId,
                DEFAULT_ACTION_ID,
                count,
                BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE,
                BillingOperationInfoBase.DEFAULT_EXTERNAL_SOURCE_ID,
                null,
                null,
                null,
                null
        );
    }
}
