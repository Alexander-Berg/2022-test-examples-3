package ru.yandex.market.crm.campaign.services.actions;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.status.IssueBunchStepStatus;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.test.AbstractServiceMediumTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.misc.thread.ThreadUtils;

/**
 * @author apershukov
 */
public class StepStatusUpdaterTest extends AbstractServiceMediumTest {

    private static void waitForBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private static final String STEP_ID = "step_id";

    @Inject
    private ActionTestHelper actionTestHelper;

    @Inject
    private StepsStatusDAO stepsStatusDAO;

    @Inject
    private StepStatusUpdater updater;

    private PlainAction action;

    @BeforeEach
    public void setUp() {
        action = actionTestHelper.prepareActionWithVariants("segment_id", LinkingMode.NONE);
    }

    @Test
    public void testUpdateStatus() {
        IssueBunchStepStatus initialStatus = prepareInitialStatus();

        IssueBunchStepStatus newStatus = updater.update(
                action.getId(),
                STEP_ID,
                status -> status.setStageStatus(StageStatus.SUSPENDING)
        );

        Assertions.assertNotNull(newStatus);
        Assertions.assertEquals(StageStatus.SUSPENDING, newStatus.getStageStatus());
        Assertions.assertEquals(initialStatus.getIssuedCount(), newStatus.getIssuedCount());

        IssueBunchStepStatus saveStatus = (IssueBunchStepStatus) stepsStatusDAO.get(action.getId(), STEP_ID);
        Assertions.assertEquals(StageStatus.SUSPENDING, saveStatus.getStageStatus());
        Assertions.assertEquals(initialStatus.getIssuedCount(), saveStatus.getIssuedCount());
    }

    @Test
    @Disabled("Не понятно что тестиует и как чинить")
    public void testWhenStepStatusIsChangingImposibleToChangeItConcurrently() throws Exception {
        prepareInitialStatus();

        CyclicBarrier initBarrier = new CyclicBarrier(2);

        Thread thread1 = new Thread(() -> updater.<IssueBunchStepStatus>update(
                action.getId(),
                STEP_ID,
                status -> {
                    waitForBarrier(initBarrier);
                    status.setIssuedCount(200);
                    ThreadUtils.sleep(500);
                }
        ));

        Thread thread2 = new Thread(() -> {
            IssueBunchStepStatus newStatus = new IssueBunchStepStatus()
                    .setStepId(STEP_ID)
                    .setStageStatus(StageStatus.IN_PROGRESS)
                    .setIssuedCount(300);

            waitForBarrier(initBarrier);

            stepsStatusDAO.upsert(action.getId(), newStatus);
        });

        thread1.start();
        thread2.start();

        thread1.join(1000);
        thread2.join(1000);

        IssueBunchStepStatus savedStatus = (IssueBunchStepStatus) stepsStatusDAO.get(action.getId(), STEP_ID);
        Assertions.assertEquals(300, (long) savedStatus.getIssuedCount());
    }

    private IssueBunchStepStatus prepareInitialStatus() {
        IssueBunchStepStatus initialStatus = new IssueBunchStepStatus()
                .setStepId(STEP_ID)
                .setStageStatus(StageStatus.IN_PROGRESS)
                .setIssuedCount(100);

        stepsStatusDAO.upsert(action.getId(), initialStatus);

        return initialStatus;
    }
}
