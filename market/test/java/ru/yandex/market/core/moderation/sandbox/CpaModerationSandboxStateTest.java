package ru.yandex.market.core.moderation.sandbox;

import java.util.Date;

import org.junit.Test;

import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;

import static ru.yandex.market.core.testing.TestingType.CPA_PREMODERATION;

/**
 * @author zoom
 */
public class CpaModerationSandboxStateTest extends SandboxStateTest {

    @Test
    public void shouldFeedsLoadFirstCheckWhenRequestCpaModeration() {
        SandboxState state = factory.create(SHOP_ID, CPA_PREMODERATION);
        state.enableQuickStart();
        state.requestModeration(CPA_PREMODERATION);
        state.startTesting();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(1);
        expected.setTestingType(CPA_PREMODERATION);
        expected.setStatus(TestingStatus.WAITING_FEED_FIRST_LOAD);
        expected.setInProgress(true);
        expected.setStartDate(clock.get());
        expected.setUpdatedAt(clock.get());
        expected.setIterationNum(1);
        expected.setAttemptNum(1);
        expected.setCloneCheckRequired(true);
        expected.setQualityCheckRequired(true);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldAboCheckWhenPassFeedLoadCheck() {
        SandboxState state = factory.create(SHOP_ID, CPA_PREMODERATION);
        state.enableQuickStart();
        state.requestModeration(CPA_PREMODERATION);
        Date startDate = clock.get();
        // No need to `state.approveCheckStart()` since it's the first attempt
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(1);
        expected.setTestingType(CPA_PREMODERATION);
        expected.setStatus(TestingStatus.CHECKING);
        expected.setInProgress(true);
        expected.setStartDate(startDate);
        expected.setUpdatedAt(clock.get());
        expected.setAttemptNum(1);
        expected.setIterationNum(1);
        expected.setCloneCheckRequired(true);
        expected.setQualityCheckRequired(true);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldPassedWhenPassAboChecks() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, CPA_PREMODERATION);
        state.enableQuickStart();
        state.requestModeration(CPA_PREMODERATION);
        // No need to `state.approveCheckStart()` since it's the first attempt
        tick();
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.passAboChecks();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(1);
        expected.setTestingType(CPA_PREMODERATION);
        expected.setStatus(TestingStatus.WAITING_FEED_LAST_LOAD);
        expected.setInProgress(true);
        expected.setStartDate(startDate);
        expected.setUpdatedAt(clock.get());
        expected.setIterationNum(1);
        expected.setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }
}