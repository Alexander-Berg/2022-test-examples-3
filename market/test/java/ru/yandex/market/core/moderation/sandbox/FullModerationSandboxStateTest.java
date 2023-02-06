package ru.yandex.market.core.moderation.sandbox;

import java.util.Date;

import org.junit.Test;

import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.market.core.testing.TestingType.FULL_PREMODERATION;

/**
 * @author zoom
 */
public class FullModerationSandboxStateTest extends SandboxStateTest {

    @Test
    public void shouldWaitStartApprovingWhenRequestModeration() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setTestingType(FULL_PREMODERATION)
                .setStatus(TestingStatus.PENDING_CHECK_START)
                .setReady(true)
                .setApproved(true)
                .setCloneCheckRequired(true)
                .setQualityCheckRequired(true)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setPushReadyButtonCount(1)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldWaitFeedFirstLoadWhenStartTestFirstTime() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        tick();
        state.startTesting();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(1)
                .setTestingType(FULL_PREMODERATION)
                .setStatus(TestingStatus.WAITING_FEED_FIRST_LOAD)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setInProgress(true)
                .setCloneCheckRequired(true)
                .setQualityCheckRequired(true)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldAboCheckWhenPassFeedLoadCheck() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        tick();
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(1)
                .setTestingType(FULL_PREMODERATION)
                .setStatus(TestingStatus.CHECKING)
                .setInProgress(true)
                .setCloneCheckRequired(true)
                .setQualityCheckRequired(true)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldFeedsLoadLastCheckWhenPassAboChecks() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.passAboChecks();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(1)
                .setTestingType(FULL_PREMODERATION)
                .setStatus(TestingStatus.WAITING_FEED_LAST_LOAD)
                .setInProgress(true)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldPassedWhenPassFeedLoadLastCheck() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        tick();
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.passAboChecks();
        tick();
        state.passFeedLoadCheck();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(1)
                .setTestingType(FULL_PREMODERATION)
                .setStatus(TestingStatus.PASSED)
                .setInProgress(false)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldNotDelayBeforeStartCheckWhenNotTheFirstAttempt() {
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        tick();
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.cancel();
        tick();
        state.requestFullModeration();
        tick();
        state.startTesting();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(clock.get())
                .setTestingType(FULL_PREMODERATION)
                .setStatus(TestingStatus.WAITING_FEED_FIRST_LOAD)
                .setReady(false)
                .setApproved(false)
                .setInProgress(true)
                .setRecommendations(null)
                .setCancelled(false)
                .setCloneCheckRequired(true)
                .setQualityCheckRequired(true)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setPushReadyButtonCount(1)
                .setAttemptNum(2);
        assertThat(expected, equalTo(factory.getState(state)));
    }
}