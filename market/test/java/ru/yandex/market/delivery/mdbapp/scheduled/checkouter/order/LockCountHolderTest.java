package ru.yandex.market.delivery.mdbapp.scheduled.checkouter.order;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.mdbapp.configuration.OrderEventsFailoverConfiguration;

public class LockCountHolderTest {
    private OrderEventsFailoverConfiguration orderEventsFailoverConfiguration;
    private HeldLockCountMeter heldLockCountMeter;

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Before
    public void setUp() throws Exception {
        heldLockCountMeter = Mockito.mock(HeldLockCountMeter.class);
        orderEventsFailoverConfiguration = Mockito.mock(OrderEventsFailoverConfiguration.class);
        Mockito.when(orderEventsFailoverConfiguration.getMinNodeLocks()).thenReturn(2);
    }

    @Test
    public void test() {
        LockCountHolder lockCountHolder = new LockCountHolder(orderEventsFailoverConfiguration, heldLockCountMeter);

        lockCountHolder.onLock(1);
        softly.assertThat(lockCountHolder.getLockCount()).isEqualTo(1);
        softly.assertThat(lockCountHolder.needsUnlock()).isFalse();

        lockCountHolder.onLock(1);
        softly.assertThat(lockCountHolder.getLockCount()).isEqualTo(1);
        softly.assertThat(lockCountHolder.needsUnlock()).isFalse();

        lockCountHolder.onLock(22);
        softly.assertThat(lockCountHolder.getLockCount()).isEqualTo(2);
        softly.assertThat(lockCountHolder.needsUnlock()).isFalse();

        lockCountHolder.onLock(22);
        softly.assertThat(lockCountHolder.getLockCount()).isEqualTo(2);
        softly.assertThat(lockCountHolder.needsUnlock()).isFalse();

        lockCountHolder.onLock(56);
        softly.assertThat(lockCountHolder.getLockCount()).isEqualTo(3);
        softly.assertThat(lockCountHolder.needsUnlock()).isTrue();

        lockCountHolder.onUnlock(1);
        softly.assertThat(lockCountHolder.getLockCount()).isEqualTo(2);
        softly.assertThat(lockCountHolder.needsUnlock()).isFalse();

        lockCountHolder.onUnlock(22);
        softly.assertThat(lockCountHolder.getLockCount()).isEqualTo(1);
        softly.assertThat(lockCountHolder.needsUnlock()).isFalse();

        lockCountHolder.onUnlock(56);
        softly.assertThat(lockCountHolder.getLockCount()).isEqualTo(0);
        softly.assertThat(lockCountHolder.needsUnlock()).isFalse();
    }
}
