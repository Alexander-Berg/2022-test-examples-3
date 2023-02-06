package ru.yandex.market.core.moderation.sandbox;

import java.util.Date;

import org.junit.Test;

import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.validation.ConstraintViolationsException;

import static ru.yandex.market.core.testing.TestingType.FULL_PREMODERATION;

/**
 * @author zoom
 */
public class FullModerationFailuresSandboxStateTest extends SandboxStateTest {

    @Test
    public void shouldAbleToRollbackModerationWhenPendingCheckStart() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        tick();
        state.cancel();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(0)
                .setTestingType(FULL_PREMODERATION)
                .setStatus(TestingStatus.CANCELED)
                .setCancelled(true)
                .setReady(false)
                .setApproved(true)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldAbleToRollbackModerationWhenWaitingFeedFirstLoad() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        tick();
        state.startTesting();
        tick();
        state.cancel();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(0)
                .setTestingType(FULL_PREMODERATION)
                .setStatus(TestingStatus.CANCELED)
                .setCancelled(true)
                .setApproved(true)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldAbleToRollbackModerationWhenChecking() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        tick();
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.cancel();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(0)
                .setTestingType(FULL_PREMODERATION)
                .setStatus(TestingStatus.CANCELED)
                .setCancelled(true)
                .setApproved(true)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldAbleToRollbackModerationWhenWaitFeedLastLoad() {
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
        state.cancel();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(0)
                .setTestingType(FULL_PREMODERATION)
                .setStatus(TestingStatus.CANCELED)
                .setCancelled(true)
                .setApproved(true)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldNotBeAbleToRollbackModerationWhenReadyToFail() {
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        tick();
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.fail();
        tick();
        try {
            state.cancel();
            fail();
        } catch (ConstraintViolationsException e) {
            // OK
        }
    }

    @Test
    public void shouldNotBeAbleToRollbackModerationWhenFailed() {
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        tick();
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.fail();
        tick();
        state.approveFail();
        try {
            state.cancel();
            fail();
        } catch (ConstraintViolationsException e) {
            // OK
        }
    }

    @Test
    public void shouldBeAbleToRollbackModerationManyTimes() {
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        tick();
        state.cancel();
        state.cancel();
        state.cancel();
    }

    @Test
    public void shouldWaitForFailApproveWhenFailAboFullCheck() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        tick();
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.fail();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(1)
                .setTestingType(FULL_PREMODERATION)
                .setStatus(TestingStatus.READY_TO_FAIL)
                .setCancelled(true)
                .setCloneCheckRequired(true)
                .setQualityCheckRequired(true)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldWaitForFailApproveWhenFailAboFullCheck____________() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        state.enableQuickStart();
        state.requestFullModeration();
        tick();
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.fail();
        tick();
        state.approveFail();
        TestingState expected = new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(startDate)
                .setPushReadyButtonCount(1)
                .setTestingType(FULL_PREMODERATION)
                .setStatus(TestingStatus.FAILED)
                .setCancelled(true)
                .setApproved(true)
                .setCloneCheckRequired(true)
                .setQualityCheckRequired(true)
                .setUpdatedAt(clock.get())
                .setIterationNum(1)
                .setAttemptNum(1);
        assertEquals(expected, factory.getState(state));
    }
}