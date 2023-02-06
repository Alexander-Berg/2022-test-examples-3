package ru.yandex.market.core.moderation.sandbox;

import java.util.Date;

import org.junit.Test;

import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.validation.ConstraintViolationsException;

import static ru.yandex.market.core.testing.TestingStatus.PENDING_CHECK_START;
import static ru.yandex.market.core.testing.TestingType.CPC_PREMODERATION;

/**
 * @author zoom
 */
public class CpcModerationSandboxStateTest extends SandboxStateTest {

    @Test
    public void shouldWaitStartApprovingWhenNoState() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, CPC_PREMODERATION);
        state.enableQuickStart();
        state.requestCpcModeration();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(1)
                .setTestingType(CPC_PREMODERATION)
                .setStatus(PENDING_CHECK_START)
                .setReady(true)
                .setApproved(true)
                .setCloneCheckRequired(true)
                .setQualityCheckRequired(true)
                .setUpdatedAt(clock.get())
                .setAttemptNum(1)
                .setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test(expected = ConstraintViolationsException.class)
    public void shouldRaiseExceptionWhenRequestCpcModerationWhileStartApproved() {
        SandboxState state = factory.create(SHOP_ID, CPC_PREMODERATION);
        state.enableQuickStart();
        state.requestCpcModeration();
        state.requestCpcModeration();
        fail();
    }

    @Test
    public void shouldAboCheckWhenPassFirstFeedLoadCheck() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, CPC_PREMODERATION);
        state.enableQuickStart();
        state.requestCpcModeration();
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(1)
                .setTestingType(CPC_PREMODERATION)
                .setStatus(TestingStatus.CHECKING)
                .setInProgress(true)
                .setCloneCheckRequired(true)
                .setQualityCheckRequired(true)
                .setUpdatedAt(clock.get())
                .setAttemptNum(1)
                .setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test(expected = ConstraintViolationsException.class)
    public void shouldRaiseExceptionWhenRequestCpcModerationWhileWaitingFeedFirstLoad() {
        SandboxState state = factory.create(SHOP_ID, CPC_PREMODERATION);
        state.enableQuickStart();
        state.requestCpcModeration();
        state.startTesting();
        state.requestCpcModeration();
        fail();
    }

    @Test
    public void shouldFeedsLoadLastCheckWhenPassAboChecks() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, CPC_PREMODERATION);
        state.enableQuickStart();
        state.requestCpcModeration();
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.passAboChecks();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(1)
                .setTestingType(CPC_PREMODERATION)
                .setStatus(TestingStatus.WAITING_FEED_LAST_LOAD)
                .setInProgress(true)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldPassedWhenPassLastFeedsLoadCheck() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, CPC_PREMODERATION);
        state.enableQuickStart();
        state.requestCpcModeration();
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
                .setTestingType(CPC_PREMODERATION)
                .setStatus(TestingStatus.PASSED)
                .setInProgress(false)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }
}