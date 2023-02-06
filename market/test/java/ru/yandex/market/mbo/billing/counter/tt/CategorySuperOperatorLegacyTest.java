package ru.yandex.market.mbo.billing.counter.tt;

import org.junit.Test;
import org.mockito.stubbing.Answer;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.BillingOperations;
import ru.yandex.market.mbo.billing.tarif.TarifProvider;
import ru.yandex.market.mbo.user.UserManager;
import ru.yandex.market.mbo.utils.BatchUpdater;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author sergtru
 * @since 28.03.2018
 */
public class CategorySuperOperatorLegacyTest {
    private static final long DUMMY_USER = 100;
    private static final int DUMMY_CATEGORY = 200;
    private static final int NOT_EXISTED_DAY = 72;

    @Test
    public void testPostPay() {
        UserManager userManager = mock(UserManager.class);
        when(userManager.getSuperUserPerCategory())
            .thenReturn(Collections.singletonList(new Pair<>(DUMMY_USER, (long) DUMMY_CATEGORY)));

        BatchUpdater dummyUpdater = mock(BatchUpdater.class);
        doNothing().when(dummyUpdater).addFailListener(any());
        doNothing().when(dummyUpdater).flush();
        doAnswer((Answer<Void>) invocation -> {
            throw new AssertionError("Suspended operations should not be payed");
        }).when(dummyUpdater).add(any());

        BillingOperations billingOperations = mock(BillingOperations.class);
        when(billingOperations.getOperationsUpdater()).thenReturn(dummyUpdater);
        when(billingOperations.getSuspendedOperationsUpdater()).thenReturn(dummyUpdater);

        TarifProvider tarifProvider = mock(TarifProvider.class);
        when(tarifProvider.getTarif(eq(PaidAction.IS_CATEGORY_SUPER_OPERATOR.getId()), eq(DUMMY_CATEGORY), any()))
            .thenReturn(BigDecimal.ONE);
        when(tarifProvider.getTarif(anyInt(), anyLong(), any()))
            .thenThrow(new AssertionError("This call not expected. Fix test"));
        when(tarifProvider.containsTarif(anyInt(), anyLong()))
            .thenReturn(true);

        CategorySuperOperatorCounter counter = new CategorySuperOperatorCounter();
        counter.setPayDay(NOT_EXISTED_DAY);
        counter.setUserManager(userManager);
        counter.setBillingOperations(billingOperations);

        Calendar calendar = Calendar.getInstance();
        counter.load(new Pair<>(calendar, calendar), tarifProvider);
        verify(dummyUpdater, never()).add(any());
    }
}
