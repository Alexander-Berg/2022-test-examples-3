package ru.yandex.market.core.moderation.sandbox;

import java.util.Date;

import org.junit.Test;

import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.testing.TestingType;

import static ru.yandex.market.core.testing.TestingType.SELF_CHECK;

/**
 * @author zoom
 */
public class SelfTestSandboxStateTest extends SandboxStateTest {

    @Test
    public void shouldFeedsLoadFirstCheckWhenRequestSelfTest() {
        SandboxState state = factory.create(SHOP_ID, SELF_CHECK);
        state.enableQuickStart();
        state.requestSelfLoadToSandbox();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(0);
        expected.setTestingType(TestingType.SELF_CHECK);
        expected.setStatus(TestingStatus.WAITING_FEED_FIRST_LOAD);
        expected.setStartDate(clock.get());
        expected.setUpdatedAt(clock.get());
        expected.setInProgress(true);
        expected.setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldSelfTestWhenPassFeedLoadCheck() {
        SandboxState state = factory.create(SHOP_ID, SELF_CHECK);
        state.enableQuickStart();
        Date startDate = clock.get();
        state.requestSelfLoadToSandbox();
        tick();
        state.passFeedLoadCheck();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(0);
        expected.setTestingType(TestingType.SELF_CHECK);
        expected.setStatus(TestingStatus.CHECKING);
        expected.setInProgress(true);
        expected.setStartDate(startDate);
        expected.setUpdatedAt(clock.get());
        expected.setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldExpiredWhenExpired() {
        SandboxState state = factory.create(SHOP_ID, SELF_CHECK);
        Date startDate = clock.get();
        state.enableQuickStart();
        state.requestSelfLoadToSandbox();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.expire();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(0);
        expected.setTestingType(TestingType.SELF_CHECK);
        expected.setStatus(TestingStatus.EXPIRED);
        expected.setInProgress(false);
        expected.setStartDate(startDate);
        expected.setUpdatedAt(clock.get());
        expected.setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldSelfTestWhenInited() {
        SandboxState state = factory.create(SHOP_ID, SELF_CHECK);
        Date startDate = clock.get();
        state.enableQuickStart();
        state.requestSelfLoadToSandbox();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(0);
        expected.setTestingType(TestingType.SELF_CHECK);
        expected.setStatus(TestingStatus.WAITING_FEED_FIRST_LOAD);
        expected.setInProgress(true);
        expected.setStartDate(startDate);
        expected.setUpdatedAt(clock.get());
        expected.setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }
}
