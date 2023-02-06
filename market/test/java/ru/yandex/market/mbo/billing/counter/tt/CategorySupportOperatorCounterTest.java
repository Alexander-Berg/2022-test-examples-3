package ru.yandex.market.mbo.billing.counter.tt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.AbstractBillingLoaderTest;
import ru.yandex.market.mbo.billing.counter.BatchUpdateData;
import ru.yandex.market.mbo.user.UserManagerMock;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Каждому оператору поддержки категории оплачивают "ответственность за категорию" N-го числа каждого месяца. Это число
 * (на данный момент 15-е число каждого месяца) называется PayDay. Если оператор надзирает за несколькими категориями,
 * то оплату он получает за все.
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class CategorySupportOperatorCounterTest extends AbstractBillingLoaderTest {
    private static final long CATEGORY_ID1 = 9005L;
    private static final long CATEGORY_ID2 = 9885L;
    private static final long UID1 = 666L;
    private static final long UID2 = 16662L;
    private static final long UID3 = 5213L;

    private UserManagerMock userManagerMock;
    private CategorySupportOperatorCounter counter;

    @Before
    public void setup() {
        super.setUp();
        userManagerMock = new UserManagerMock();
        counter = new CategorySupportOperatorCounter();
        counter.setUserManager(userManagerMock);
        counter.setBillingOperations(billingOperations);
    }

    /**
     * Нет супероператоров, и сегодня не пэй-дэй. Платить нечего и некому.
     */
    @Test
    public void testNoUsersWrongDay() {
        counter.setPayDay(getWrongPayDay());
        counter.doLoad(INTERVAL, tarifProvider);

        verify(operationsUpdater, times(0)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).isEmpty();
    }

    /**
     * Нет супероператоров, но сегодня пэй-дэй. Платить некому.
     */
    @Test
    public void testNoUsersPayDay() {
        counter.setPayDay(getPayDayToday());
        counter.doLoad(INTERVAL, tarifProvider);

        verify(operationsUpdater, times(0)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).isEmpty();
    }

    /**
     * Есть супероператоры, но не пэй-дэй. Платить тоже не будем.
     */
    @Test
    public void testUsersWrongDay() {
        counter.setPayDay(getWrongPayDay());
        userManagerMock.setCategorySupportOperators(Arrays.asList(UID1, UID2), CATEGORY_ID1);
        assertThat(userManagerMock.getSupportUserPerCategory()).isNotEmpty(); // Супероператоры реально есть.

        counter.doLoad(INTERVAL, tarifProvider);

        verify(operationsUpdater, times(0)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).isEmpty();
    }

    /**
     * Наконец, есть и супероператоры, и день подходящий. Платим.
     */
    @Test
    public void testUsersPayDay() {
        counter.setPayDay(getPayDayToday());
        userManagerMock.setCategorySupportOperators(Arrays.asList(UID1, UID2), CATEGORY_ID1);
        userManagerMock.setCategorySupportOperators(Arrays.asList(UID2, UID3), CATEGORY_ID2);
        assertThat(userManagerMock.getSupportUserPerCategory()).isNotEmpty(); // Супероператоры реально есть.

        counter.doLoad(INTERVAL, tarifProvider);

        verify(operationsUpdater, times(4)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
            createBilledActionWithTime(PaidAction.IS_CATEGORY_SUPPORT_OPERATOR, 0,
                CATEGORY_ID1, UID1, INTERVAL.first.getTimeInMillis()),
            createBilledActionWithTime(PaidAction.IS_CATEGORY_SUPPORT_OPERATOR, 0,
                CATEGORY_ID1, UID2, INTERVAL.first.getTimeInMillis()),
            createBilledActionWithTime(PaidAction.IS_CATEGORY_SUPPORT_OPERATOR, 0,
                CATEGORY_ID2, UID2, INTERVAL.first.getTimeInMillis()),
            createBilledActionWithTime(PaidAction.IS_CATEGORY_SUPPORT_OPERATOR, 0,
                CATEGORY_ID2, UID3, INTERVAL.first.getTimeInMillis())
        );
    }

    private int getPayDayToday() {
        return FROM_DATE.get(Calendar.DAY_OF_MONTH);
    }

    private int getWrongPayDay() {
        return getPayDayToday() + 1;
    }
}
