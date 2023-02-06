package ru.yandex.market.core.moderation.sandbox;

import java.util.Date;

import org.junit.Test;

import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.testing.TestingType;

import static ru.yandex.market.core.testing.TestingType.CPA_CHECK;

/**
 * @author zoom
 */
public class CpaLightCheckSandboxStateTest extends SandboxStateTest {

    @Test
    public void shouldWaitStartApprovingWhenRequestCpaLightCheck() {
        SandboxState state = factory.create(SHOP_ID, CPA_CHECK);
        state.enableQuickStart();
        state.requestModeration(TestingType.CPA_CHECK);
        state.startTesting();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(1);
        expected.setTestingType(CPA_CHECK);
        expected.setStatus(TestingStatus.WAITING_FEED_FIRST_LOAD);
        expected.setInProgress(true);
        expected.setStartDate(clock.get());
        expected.setUpdatedAt(clock.get());
        expected.setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldAboCheckWhenPassFeedLoadCheck() {
        SandboxState state = factory.create(SHOP_ID, CPA_CHECK);
        state.enableQuickStart();
        state.requestModeration(TestingType.CPA_CHECK);
        Date startDate = clock.get();
        tick();
        state.startTesting();
        state.passFeedLoadCheck();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(1);
        expected.setTestingType(CPA_CHECK);
        expected.setStatus(TestingStatus.CHECKING);
        expected.setInProgress(true);
        expected.setStartDate(startDate);
        expected.setUpdatedAt(clock.get());
        expected.setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldPassedWhenPassAboChecks() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, CPA_CHECK);
        state.enableQuickStart();
        state.requestModeration(TestingType.CPA_CHECK);
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.passAboChecks();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(1);
        expected.setTestingType(CPA_CHECK);
        expected.setStatus(TestingStatus.WAITING_FEED_LAST_LOAD);
        expected.setInProgress(true);
        expected.setStartDate(startDate);
        expected.setUpdatedAt(clock.get());
        expected.setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }


}