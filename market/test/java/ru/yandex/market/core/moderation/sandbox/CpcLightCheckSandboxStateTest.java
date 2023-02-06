package ru.yandex.market.core.moderation.sandbox;

import java.util.Date;

import org.junit.Test;

import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;

import static ru.yandex.market.core.testing.TestingType.CPC_LITE_CHECK;

/**
 * @author zoom
 */
public class CpcLightCheckSandboxStateTest extends SandboxStateTest {

    @Test
    public void shouldWaitStartApprovingWhenRequestCpcLightCheck() {
        SandboxState state = factory.create(SHOP_ID, CPC_LITE_CHECK);
        state.enableQuickStart();
        state.requestModeration(CPC_LITE_CHECK);
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(1);
        expected.setTestingType(CPC_LITE_CHECK);
        expected.setStatus(TestingStatus.PENDING_CHECK_START);
        expected.setApproved(true);
        expected.setReady(true);
        expected.setQualityCheckRequired(true);
        expected.setStartDate(clock.get());
        expected.setUpdatedAt(clock.get());
        expected.setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldFeedLoadedWhenPassCheckAtCpcLightModeration() {
        SandboxState state = factory.create(SHOP_ID, CPC_LITE_CHECK);
        state.enableQuickStart();
        state.requestModeration(CPC_LITE_CHECK);
        state.startTesting();
        Date startDate = clock.get();
        tick();
        state.passFeedLoadCheck();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(1);
        expected.setTestingType(CPC_LITE_CHECK);
        expected.setStatus(TestingStatus.CHECKING);
        expected.setInProgress(true);
        expected.setQualityCheckRequired(true);
        expected.setStartDate(startDate);
        expected.setUpdatedAt(clock.get());
        expected.setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }

    /**
     * Магазин прошел cpc_light-проверку. Должен пройти проверку на загрузку фида
     */
    @Test
    public void shouldCheckFeedLoadWhenAboCpcLightChecksPassed() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, CPC_LITE_CHECK);
        state.enableQuickStart();
        state.requestModeration(CPC_LITE_CHECK);
        state.startTesting();
        tick();
        state.passFeedLoadCheck();
        tick();
        state.passAboChecks();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setStartDate(startDate);
        expected.setPushReadyButtonCount(1);
        expected.setTestingType(CPC_LITE_CHECK);
        expected.setStatus(TestingStatus.WAITING_FEED_LAST_LOAD);
        expected.setInProgress(true);
        expected.setUpdatedAt(clock.get());
        expected.setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @Test
    public void shouldCancelWhenWaitingFeedFirstLoad() {
        Date startDate = clock.get();
        SandboxState state = factory.create(SHOP_ID, CPC_LITE_CHECK);
        state.enableQuickStart();
        state.requestModeration(CPC_LITE_CHECK);
        state.startTesting();
        tick();
        state.cancel();
        TestingState expected = new TestingState();
        expected.setDatasourceId(SHOP_ID);
        expected.setStartDate(startDate);
        expected.setPushReadyButtonCount(0);
        expected.setTestingType(CPC_LITE_CHECK);
        expected.setStatus(TestingStatus.CANCELED);
        expected.setInProgress(false);
        expected.setApproved(true);
        expected.setCancelled(true);
        expected.setUpdatedAt(clock.get());
        expected.setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }


}