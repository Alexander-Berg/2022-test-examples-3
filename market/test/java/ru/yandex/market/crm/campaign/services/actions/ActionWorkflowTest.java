package ru.yandex.market.crm.campaign.services.actions;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.crm.campaign.domain.actions.ActionConfig;
import ru.yandex.market.crm.campaign.domain.actions.ActionStatus;
import ru.yandex.market.crm.campaign.domain.actions.ActionVariant;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.WfPermissions;
import ru.yandex.market.crm.campaign.domain.actions.WfPermissions.GlobalAction;
import ru.yandex.market.crm.campaign.domain.actions.WfPermissions.StepAction;
import ru.yandex.market.crm.campaign.domain.actions.status.BuildSegmentStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.FoldByCryptaStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.IssueBunchStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.SendEmailsStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.IssueCoinsStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.SendEmailsStep;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.exceptions.BadRequestException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.crm.campaign.domain.actions.WfPermissions.StepAction.CONTINUE;
import static ru.yandex.market.crm.campaign.domain.actions.WfPermissions.StepAction.DELETE;
import static ru.yandex.market.crm.campaign.domain.actions.WfPermissions.StepAction.EDIT;
import static ru.yandex.market.crm.campaign.domain.actions.WfPermissions.StepAction.MOVE;
import static ru.yandex.market.crm.campaign.domain.actions.WfPermissions.StepAction.START;
import static ru.yandex.market.crm.campaign.domain.actions.WfPermissions.StepAction.SUSPEND;
import static ru.yandex.market.crm.campaign.domain.workflow.StageStatus.ERROR;
import static ru.yandex.market.crm.campaign.domain.workflow.StageStatus.FINISHED;
import static ru.yandex.market.crm.campaign.domain.workflow.StageStatus.IN_PROGRESS;
import static ru.yandex.market.crm.campaign.domain.workflow.StageStatus.SUSPENDED;
import static ru.yandex.market.crm.campaign.domain.workflow.StageStatus.SUSPENDING;
import static ru.yandex.market.crm.campaign.services.actions.ActionConstants.SEGMENT_STEP_ID;
import static ru.yandex.market.crm.campaign.test.ActionTestHelper.coinStep;
import static ru.yandex.market.crm.campaign.test.ActionTestHelper.config;
import static ru.yandex.market.crm.campaign.test.ActionTestHelper.foldByCryptaStep;
import static ru.yandex.market.crm.campaign.test.ActionTestHelper.sendEmailsStep;
import static ru.yandex.market.crm.campaign.test.ActionTestHelper.variant;

/**
 * @author apershukov
 */
public class ActionWorkflowTest {

    private static class PermissionChecker {

        private final WfPermissions permissions;

        PermissionChecker(WfPermissions permissions) {
            this.permissions = permissions;
        }

        PermissionChecker assertPermitted(GlobalAction... actions) {
            Set<GlobalAction> actual = permissions.getGlobal();
            assertNotNull(actual);

            for (GlobalAction action : actions) {
                assertTrue("Action '" + action + "' is not permitted", actual.contains(action));
            }

            return this;
        }

        void assertNotPermitted(GlobalAction... actions) {
            Set<GlobalAction> actual = permissions.getGlobal();
            assertNotNull(actual);

            for (GlobalAction action : actions) {
                assertFalse("Action '" + action + "' is permitted", actual.contains(action));
            }
        }

        PermissionChecker assertPermittedForStep(String stepId, StepAction... actions) {
            Set<StepAction> actual = getStepPermissions(stepId);

            for (StepAction action : actions) {
                assertTrue("Action '" + action + "' is not permitted", actual.contains(action));
            }

            return this;
        }

        PermissionChecker assertNotPermittedForStep(String stepId, StepAction... actions) {
            Set<StepAction> actual = getStepPermissions(stepId);

            for (StepAction action : actions) {
                assertFalse("Action '" + action + "' is permitted", actual.contains(action));
            }

            return this;
        }

        PermissionChecker assertPermittedForStep(ActionStep step, StepAction... actions) {
            return assertPermittedForStep(step.getId(), actions);
        }

        PermissionChecker assertNotPermittedForStep(ActionStep step, StepAction... actions) {
            return assertNotPermittedForStep(step.getId(), actions);
        }

        PermissionChecker assertPermittedForSegmentStep(StepAction... actions) {
            return assertPermittedForStep(SEGMENT_STEP_ID, actions);
        }

        PermissionChecker assertNotPermittedForSegmentStep(StepAction... actions) {
            return assertNotPermittedForStep(SEGMENT_STEP_ID, actions);
        }

        private Set<StepAction> getStepPermissions(String stepId) {
            Map<String, Set<StepAction>> steps = permissions.getSteps();
            assertNotNull(steps);

            Set<StepAction> actions = steps.get(stepId);
            assertNotNull("Permitted actions for step is empty", actions);

            return actions;
        }
    }

    private final ActionWorkflow workflow = new ActionWorkflow();

    /**
     * Для только что настроенной акции с установлеммым сегментом разрешен запуск только
     * шага построения сегмента
     */
    @Test
    public void testForNewActionsOnlySegmentStepIsPermitted() {
        IssueCoinsStep step1 = coinStep("step_1", 111);
        ActionVariant variant1 = variant("variant_a", step1);

        IssueCoinsStep step2 = coinStep("step_2", 222);
        ActionVariant variant2 = variant("variant_b", step2);

        ActionConfig config = new ActionConfig();
        config.setTarget(new TargetAudience(LinkingMode.ALL, "segment_id"));
        config.setVariants(Arrays.asList(variant1, variant2));

        PlainAction action = new PlainAction();
        action.setConfig(config);

        getPermissions(action)
                .assertPermittedForSegmentStep(START)
                .assertNotPermittedForStep(step1, START)
                .assertNotPermittedForStep(step2, START);
    }

    /**
     * Для акции с незаданным сегментом запуск шага построения сегмента недоступен
     */
    @Test
    public void testBuildSegmentStepLaunchIsForbiddenForActionWithoutSegment() {
        IssueCoinsStep step = coinStep("step", 111);
        ActionVariant variant = variant("variant_a", step);

        ActionConfig config = new ActionConfig();
        config.setVariants(Collections.singletonList(variant));

        PlainAction action = new PlainAction();
        action.setConfig(config);

        getPermissions(action)
                .assertNotPermittedForSegmentStep(START)
                .assertNotPermittedForStep(step, START);
    }

    /**
     * Для акции без конфига возвращаются доступные действия для шага сегментации
     */
    @Test
    public void testReturnEmptyForActionWithonConfig() {
        getPermissions(new PlainAction())
                .assertPermittedForSegmentStep(EDIT)
                .assertNotPermittedForSegmentStep(START);
    }

    /**
     * Для акции без настроенных вариантов, но с указанным сегментом разрешен запуск шага
     * построения сегмента
     */
    @Test
    public void testSegmentStageIsAllowedForLaunchWithoutVariants() {
        PlainAction action = new PlainAction();
        action.setConfig(config());

        getPermissions(action)
                .assertPermittedForSegmentStep(START);
    }

    /**
     * Запущенный шаг построения сегмента можно остановить
     */
    @Test
    public void testRunningSegmentStepIsAllowedToBeStopped() {
        ActionStep coinStep = coinStep("coin_step", 111);
        ActionConfig config = config(coinStep);

        ActionStatus status = new ActionStatus();
        status.setSteps(
                Collections.singletonMap(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(StageStatus.IN_PROGRESS)
                )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertPermittedForSegmentStep(SUSPEND);
    }

    /**
     * В случае если есть запущенный шаг, действия с остальными шагами блокируются
     */
    @Test
    public void testRunningStepBlocksOthers() {
        ActionStep coinStep = coinStep("coin_step", 111);
        ActionStep foldByCrypta = foldByCryptaStep();
        ActionConfig config = config(
            foldByCrypta,
            coinStep,
            sendEmailsStep()
        );

        ActionStatus status = new ActionStatus();
        status.setSteps(
            ImmutableMap.of(
                    SEGMENT_STEP_ID,
                    new BuildSegmentStepStatus().setStageStatus(FINISHED),
                    foldByCrypta.getId(), new FoldByCryptaStepStatus().setStageStatus(FINISHED),
                    coinStep.getId(), new IssueBunchStepStatus().setStageStatus(IN_PROGRESS)
            )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertNotPermittedForSegmentStep(START)
                .assertNotPermittedForStep(foldByCrypta, START, CONTINUE)
                .assertNotPermittedForStep(coinStep.getId(), START, CONTINUE)
                .assertPermittedForStep(coinStep, SUSPEND);
    }

    /**
     * Останавливающийся шаг нельзя перезапустить или продолжить
     */
    @Test
    public void testSuspendingStepCannotBeStarted() {
        ActionStep foldByCrypta = foldByCryptaStep();
        ActionConfig config = config(foldByCrypta);

        ActionStatus status = new ActionStatus();
        status.setSteps(
                Collections.singletonMap(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(SUSPENDING)
                )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertNotPermittedForStep(foldByCrypta, CONTINUE, START);
    }

    /**
     * Останавливающийся шаг блокирует действия со всеми предыдущими шагами
     */
    @Test
    public void testSuspendingStepBlocksRestartOfPreviousSteps() {
        ActionStep step1 = coinStep("step_1", 111);
        ActionVariant variant1 = variant("variant_a", step1);

        ActionStep step2 = coinStep("step_2", 222);
        ActionVariant variant2 = variant("variant_b", step2);

        ActionConfig config = new ActionConfig();
        config.setTarget(new TargetAudience(LinkingMode.ALL, "segment_id"));
        config.setVariants(Arrays.asList(variant1, variant2));

        ActionStatus status = new ActionStatus();
        status.setSteps(
            ImmutableMap.of(
                    SEGMENT_STEP_ID,
                    new BuildSegmentStepStatus()
                            .setStageStatus(FINISHED),
                    step1.getId(),
                    new IssueBunchStepStatus()
                            .setStageStatus(SUSPENDING)
            )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertNotPermittedForSegmentStep(START)
                .assertNotPermittedForStep(step1, START, SUSPEND, CONTINUE)
                .assertPermittedForStep(step2, START);
    }

    /**
     * Выполненный шаг, результаты работы которого не выходят за границы CRM, может быть
     * перезапущен вместе с предыдущими шагами
     */
    @Test
    public void testStepWithoutExternalImpactCanBeRestartedAlongWithPreviousSteps() {
        ActionStep foldByCrypta = foldByCryptaStep();
        ActionConfig config = config(foldByCrypta);

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        foldByCrypta.getId(),
                        new FoldByCryptaStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertPermittedForSegmentStep(START)
                .assertPermittedForStep(foldByCrypta, START);
    }

    /**
     * В случае если в шаг отправки email-сообщений завершен его
     * и предшествующие ему шаги нельзя перезапустить
     */
    @Test
    public void testSendEmailsStepCannotBeRestarted() {
        ActionStep coinStep = coinStep();
        ActionStep sendEmails = sendEmailsStep();
        ActionConfig config = config(coinStep, sendEmails);

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        coinStep.getId(),
                        new IssueBunchStepStatus()
                                .setStageStatus(FINISHED),
                        sendEmails.getId(),
                        new SendEmailsStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertNotPermittedForSegmentStep(START)
                .assertNotPermittedForStep(coinStep, START)
                .assertNotPermittedForStep(sendEmails, START);
    }

    /**
     * Выполненный шаг отправки email-сообщений не запрещает перезапускаться
     * последующим шагам и шагам из других вариантов
     */
    @Test
    public void testStepsAfterFinishedSendEmailStepAndInAnotherVariantsCanBeStarted() {
        ActionStep sendEmails = sendEmailsStep();
        ActionStep foldByCrypta1 = foldByCryptaStep();
        ActionVariant variant1 = variant("variant_a", sendEmails, foldByCrypta1);

        ActionStep foldByCrypta2 = foldByCryptaStep();
        ActionVariant variant2 = variant("variant_b", foldByCrypta2);

        ActionConfig config = new ActionConfig();
        config.setTarget(new TargetAudience(LinkingMode.ALL, "segment_id"));
        config.setVariants(Arrays.asList(variant1, variant2));

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        sendEmails.getId(),
                        new SendEmailsStepStatus()
                                .setStageStatus(FINISHED),
                        foldByCrypta1.getId(),
                        new FoldByCryptaStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertPermittedForStep(foldByCrypta1, START)
                .assertPermittedForStep(foldByCrypta2, START);
    }

    /**
     * Завершенный шаг выдачи монеток после которого нет завершенного шага отправки писем
     * <b>не</b> может быть перезапущен (LILUCRM-2090)
     */
    @Test
    public void testFinishedCoinIssuanceStepCanBeRestarted() {
        ActionStep foldByCrypta = foldByCryptaStep();
        ActionStep coinStep = coinStep();
        ActionConfig config = config(foldByCrypta, coinStep);

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        foldByCrypta.getId(),
                        new FoldByCryptaStepStatus()
                                .setStageStatus(FINISHED),
                        coinStep.getId(),
                        new IssueBunchStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertNotPermittedForSegmentStep(START)
                .assertNotPermittedForStep(foldByCrypta, START)
                .assertNotPermittedForStep(coinStep, START);
    }

    /**
     * Приостановленная отправка email-сообщений не может быть перезапущена.
     * Она может быть только продолжена
     */
    @Test
    public void testSuspendedEmailSendingStatusCannotBeRestarted() {
        ActionStep coinStep = coinStep();
        ActionStep sendEmails = sendEmailsStep();
        ActionConfig config = config(coinStep, sendEmails);

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        coinStep.getId(),
                        new IssueBunchStepStatus()
                                .setStageStatus(FINISHED),
                        sendEmails.getId(),
                        new SendEmailsStepStatus()
                                .setStageStatus(SUSPENDED)
                )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertNotPermittedForSegmentStep(START)
                .assertNotPermittedForStep(coinStep, START)
                .assertNotPermittedForStep(sendEmails, START)
                .assertPermittedForStep(sendEmails, CONTINUE);
    }

    /**
     * Приостановленный шаг выдачи монеток нельзя ни перезапустить,
     * ни продолжить его выполенние
     */
    @Test
    public void testSuspendedCoinIssuanceStepCanBeRestarted() {
        ActionStep foldByCrypta = foldByCryptaStep();
        ActionStep coinStep = coinStep();
        ActionConfig config = config(foldByCrypta, coinStep);

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        foldByCrypta.getId(),
                        new FoldByCryptaStepStatus()
                                .setStageStatus(FINISHED),
                        coinStep.getId(),
                        new IssueBunchStepStatus()
                                .setStageStatus(SUSPENDED)
                )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertNotPermittedForSegmentStep(START)
                .assertNotPermittedForStep(foldByCrypta, START)
                .assertNotPermittedForStep(coinStep, START, CONTINUE);
    }

    /**
     * Шаг, не выполнявшийся ранее, может быть отредактирован, удален или перемещен
     */
    @Test
    public void testEditOfNewBlockIsPermitted() {
        ActionStep foldByCrypta = foldByCryptaStep();
        ActionConfig config = config(foldByCrypta);

        PlainAction action = new PlainAction();
        action.setConfig(config);

        getPermissions(action)
                .assertPermittedForStep(foldByCrypta, EDIT, DELETE, MOVE);
    }

    /**
     * Выполненный шаг, не имеющий внешнего эффекта, может быть перемещен отредактирован
     * или удален
     */
    @Test
    public void testFinishedStepWithoutExternalImpactCanBeEdited() {
        ActionStep foldByCrypta = foldByCryptaStep();
        ActionConfig config = config(foldByCrypta);

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        foldByCrypta.getId(),
                        new FoldByCryptaStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertPermittedForSegmentStep(EDIT, DELETE, MOVE)
                .assertPermittedForStep(foldByCrypta, EDIT, DELETE, MOVE);
    }

    /**
     * Выполненный шаг с внешними результатами нельзя редактировать.
     * Так же нельзя редактировать последовательность шагов выполненных до него
     */
    @Test
    public void testFinishedEmailSendingStepCannotBeEdited() {
        ActionStep foldByCrypta = foldByCryptaStep();
        ActionStep sendEmail = sendEmailsStep();
        ActionStep coinStep = coinStep();
        ActionConfig config = config(foldByCrypta, sendEmail, coinStep);

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        foldByCrypta.getId(),
                        new FoldByCryptaStepStatus()
                                .setStageStatus(FINISHED),
                        sendEmail.getId(),
                        new SendEmailsStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertNotPermittedForSegmentStep(EDIT, DELETE, MOVE)
                .assertNotPermittedForStep(foldByCrypta, EDIT, DELETE, MOVE)
                .assertNotPermittedForStep(sendEmail, EDIT, DELETE, MOVE)
                .assertPermittedForStep(coinStep, EDIT, DELETE, MOVE);
    }

    /**
     * Только что созданную акцию можно удалить или отредактировать
     */
    @Test
    public void testActionWithoutConfigCanBeEditedOrRemoved() {
        PlainAction action = new PlainAction();

        getPermissions(action)
                .assertPermitted(GlobalAction.EDIT, GlobalAction.DELETE);
    }

    /**
     * Акцию с запущенным шагом сегментации нельзя отредактировать или удалить
     */
    @Test
    public void testActionWithRunningSegmentStepCannotBeEditedOrRemoved() {
        ActionStatus status = new ActionStatus()
                .setSteps(
                        Collections.singletonMap(
                                SEGMENT_STEP_ID,
                                new BuildSegmentStepStatus()
                                        .setStageStatus(IN_PROGRESS)
                        )
                );

        PlainAction action = new PlainAction()
                .setStatus(status);

        getPermissions(action)
                .assertNotPermitted(GlobalAction.EDIT, GlobalAction.DELETE);
    }

    /**
     * Акцию с выполненным шагом сегментации можно отредактировать или удалить
     */
    @Test
    public void testActionWithFinishedSegmentationStepCanBeEditedOrRemoved() {
        ActionStatus status = new ActionStatus();
        status.setSteps(
                Collections.singletonMap(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction action = new PlainAction()
                .setStatus(status);

        getPermissions(action)
                .assertPermitted(GlobalAction.EDIT, GlobalAction.DELETE);
    }

    /**
     * Акцию у которой есть выполняемые шаги нельзя отредактировать или удалить
     */
    @Test
    public void testActionWithWorkingStepCannotBeStoppedOrRemoved() {
        ActionStep foldByCrypta = foldByCryptaStep();
        ActionConfig config = config(foldByCrypta);

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        foldByCrypta.getId(),
                        new FoldByCryptaStepStatus()
                                .setStageStatus(IN_PROGRESS)
                )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertNotPermitted(GlobalAction.EDIT, GlobalAction.DELETE);
    }

    /**
     * Акцию, имеющую выполненный шаг результаты работы которого вышли за пределы CRM,
     * можно редактировать но уже нельзя удалить
     */
    @Test
    public void testActionWithFinishedStepWithExternalImpactCannotBeRemoved() {
        ActionStep sendEmails = sendEmailsStep();
        ActionConfig config = config(sendEmails);

        ActionStatus status = new ActionStatus()
                .setSteps(
                        ImmutableMap.of(
                                SEGMENT_STEP_ID,
                                new BuildSegmentStepStatus()
                                        .setStageStatus(FINISHED),
                                sendEmails.getId(),
                                new SendEmailsStepStatus()
                                        .setStageStatus(FINISHED)
                        )
                );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertPermitted(GlobalAction.EDIT)
                .assertNotPermitted(GlobalAction.DELETE);
    }

    /**
     * При редактировании, в случае если запрещенный для редактирования шаг
     * не был изменен, исключение не выбрасывается
     */
    @Test
    public void testNoExceptionIfSendEmailStepConfigHaveNotBeenChanged() {
        ActionStep oldSendEmails = sendEmailsStep();
        ActionConfig config = config(oldSendEmails);

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        oldSendEmails.getId(),
                        new SendEmailsStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction oldAction = new PlainAction();
        oldAction.setConfig(config);
        oldAction.setStatus(status);

        SendEmailsStep newSendEmails = sendEmailsStep(oldSendEmails.getId());

        PlainAction newAction = new PlainAction();
        newAction.setConfig(config(newSendEmails));

        workflow.checkEditingIsPermitted(oldAction, newAction);
    }

    /**
     * При попытке редактирования завершенного шага отправки email-оповещения
     * выбрасывается исключение
     */
    @Test(expected = BadRequestException.class)
    public void testExceptionEditFinishedStepWithExternalImpact() {
        ActionStep oldSendEmails = sendEmailsStep();
        ActionConfig config = config(oldSendEmails);

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        oldSendEmails.getId(),
                        new SendEmailsStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction oldAction = new PlainAction();
        oldAction.setConfig(config);
        oldAction.setStatus(status);

        SendEmailsStep newSendEmails = sendEmailsStep(oldSendEmails.getId());
        newSendEmails.setTemplateId("new_template");

        PlainAction newAction = new PlainAction();
        oldAction.setConfig(config(newSendEmails));

        workflow.checkEditingIsPermitted(oldAction, newAction);
    }

    /**
     * При изменении конфогурации шага редактирования которого разрешено
     * исключение не выбрасывается
     */
    @Test
    public void testIfStepEditIsPermittedExceptionIsNotThrown() {
        ActionStep oldIssueCoins = coinStep();

        PlainAction oldAction = new PlainAction();
        oldAction.setConfig(config(oldIssueCoins));

        IssueCoinsStep newIssueCoins = coinStep(oldIssueCoins.getId());
        newIssueCoins.setPromoId(222L);

        PlainAction newAction = new PlainAction();
        oldAction.setConfig(config(newIssueCoins));

        workflow.checkEditingIsPermitted(oldAction, newAction);
    }

    /**
     * При удалении шага, удаление которого запрещено выбрасывается
     */
    @Test(expected = BadRequestException.class)
    public void testIfStepWhichRemovingIsNotPermittedIsBeingRemovedExceptionIsThrown() {
        ActionStep issueCoins = coinStep();

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        issueCoins.getId(),
                        new IssueBunchStepStatus()
                                .setStageStatus(SUSPENDED)
                )
        );

        PlainAction oldAction = new PlainAction();
        oldAction.setConfig(config(issueCoins));
        oldAction.setStatus(status);

        PlainAction newAction = new PlainAction();
        newAction.setConfig(config());

        workflow.checkEditingIsPermitted(oldAction, newAction);
    }

    /**
     * При удалении шага удаление которого разрешено исключение не выбрасыватся
     */
    @Test
    public void testIfStepWhichRemovingIsPermittedIsBeingRemovedExceptionIsNotThrown() {
        ActionStep issueCoins = coinStep();

        PlainAction oldAction = new PlainAction();
        oldAction.setConfig(config(issueCoins));

        PlainAction newAction = new PlainAction();
        newAction.setConfig(config());

        workflow.checkEditingIsPermitted(oldAction, newAction);
    }

    /**
     * При перемещении шагов перемещение которых разрешено исключение не выбрасывается
     */
    @Test
    public void testIfStepWhichMovingIsPermittedIsBeingMovedExceptionIsNotThrown() {
        ActionStep foldByCrypta = foldByCryptaStep();
        ActionStep issueCouns = coinStep();

        PlainAction oldACtion = new PlainAction();
        oldACtion.setConfig(config(foldByCrypta, issueCouns));

        PlainAction newAction = new PlainAction();
        newAction.setConfig(config(issueCouns, foldByCrypta));

        workflow.checkEditingIsPermitted(oldACtion, newAction);
    }

    /**
     * При перемещении шага перемещение которого запрещено выбрасывается исключение
     */
    @Test(expected = BadRequestException.class)
    public void testIfStepWhichMovingIsNotPermittedIsBeingMovedExceptionIsThrown() {
        ActionStep foldByCrypta = foldByCryptaStep();
        ActionStep issueCouns = coinStep();

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        foldByCrypta.getId(),
                        new FoldByCryptaStepStatus()
                                .setStageStatus(FINISHED),
                        issueCouns.getId(),
                        new IssueBunchStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction oldAction = new PlainAction();
        oldAction.setConfig(config(foldByCrypta, issueCouns));
        oldAction.setStatus(status);

        PlainAction newAction = new PlainAction();
        newAction.setConfig(config(issueCouns, foldByCrypta));

        workflow.checkEditingIsPermitted(oldAction, newAction);
    }

    /**
     * При добавлении нового варианта исключение не выбрасывается
     */
    @Test
    public void testAddNewVarinatExceptionIsNotThrown() {
        ActionStep sendEmails = sendEmailsStep();
        ActionVariant variant1 = variant("variant_a", sendEmails);

        ActionConfig oldConfig = new ActionConfig();
        oldConfig.setTarget(new TargetAudience(LinkingMode.ALL, "segment_id"));
        oldConfig.setVariants(Collections.singletonList(variant1));

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        sendEmails.getId(),
                        new SendEmailsStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction oldAction = new PlainAction();
        oldAction.setConfig(oldConfig);
        oldAction.setStatus(status);

        ActionStep foldByCrypta = foldByCryptaStep();
        ActionVariant variant2 = variant("variant_b", foldByCrypta);

        ActionConfig newConfig = new ActionConfig();
        newConfig.setTarget(oldConfig.getTarget());
        newConfig.setVariants(Arrays.asList(variant1, variant2));

        PlainAction newAction = new PlainAction();
        newAction.setConfig(newConfig);

        workflow.checkEditingIsPermitted(oldAction, newAction);
    }

    /**
     * При удалении варианта удаление всех блоков которого разрешено
     * исключение не выбрасывается
     */
    @Test
    public void testOnRemoveVariantWhichBlocksHavePermissionToBeRemoved() {
        ActionStep sendEmails = sendEmailsStep();
        ActionStep foldByCrypta1 = foldByCryptaStep();
        ActionVariant variant1 = variant("variant_a", sendEmails, foldByCrypta1);

        ActionStep foldByCrypta2 = foldByCryptaStep();
        ActionVariant variant2 = variant("variant_b", foldByCrypta2);

        ActionConfig oldConfig = new ActionConfig();
        oldConfig.setTarget(new TargetAudience(LinkingMode.ALL, "segment_id"));
        oldConfig.setVariants(Arrays.asList(variant1, variant2));

        PlainAction oldAction = new PlainAction();
        oldAction.setConfig(oldConfig);

        ActionConfig newConfig = new ActionConfig();
        newConfig.setTarget(oldConfig.getTarget());
        newConfig.setVariants(Collections.singletonList(variant1));

        PlainAction newAction = new PlainAction();
        newAction.setConfig(newConfig);

        workflow.checkEditingIsPermitted(oldAction, newAction);
    }

    /**
     * При удалении варианта, содержащего блоки удаление которых запрещено
     * выбрасывается исключение
     */
    @Test(expected = BadRequestException.class)
    public void testOnRemoveVariantWithUnremovableBlocksExceptionIsThrown() {
        ActionStep sendEmails = sendEmailsStep();
        ActionVariant variant1 = variant("variant_a", sendEmails);

        ActionStep foldByCrypta = foldByCryptaStep();
        ActionVariant variant2 = variant("variant_b", foldByCrypta);

        ActionConfig oldConfig = new ActionConfig();
        oldConfig.setTarget(new TargetAudience(LinkingMode.ALL, "segment_id"));
        oldConfig.setVariants(Arrays.asList(variant1, variant2));

        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        sendEmails.getId(),
                        new SendEmailsStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction oldAction = new PlainAction();
        oldAction.setConfig(oldConfig);
        oldAction.setStatus(status);

        ActionConfig newConfig = new ActionConfig();
        newConfig.setTarget(oldConfig.getTarget());
        newConfig.setVariants(Collections.singletonList(variant2));

        PlainAction newAction = new PlainAction();
        newAction.setConfig(newConfig);

        workflow.checkEditingIsPermitted(oldAction, newAction);
    }

    /**
     * В случае если у акции есть завершенные блоки с внешними результатами
     * при изменении настроек сегментации выбрасывается исключение
     */
    @Test(expected = BadRequestException.class)
    public void testIfTargetChangeIsNotPermittedExceptionIsThrown() {
        ActionStep issueCouns = coinStep();
        ActionStatus status = new ActionStatus();
        status.setSteps(
                ImmutableMap.of(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(FINISHED),
                        issueCouns.getId(),
                        new IssueBunchStepStatus()
                                .setStageStatus(FINISHED)
                )
        );

        PlainAction oldAction = new PlainAction();
        oldAction.setConfig(config(issueCouns));
        oldAction.setStatus(status);

        ActionConfig newConfig = config(issueCouns);
        newConfig.setTarget(new TargetAudience(LinkingMode.DIRECT_ONLY, "new_segment"));

        PlainAction newAction = new PlainAction();
        newAction.setConfig(newConfig);

        workflow.checkEditingIsPermitted(oldAction, newAction);
    }

    /**
     * В случае если изменение настроек сегментации не запрещено при
     * их изменении исключение не выбрасывается
     */
    @Test
    public void testIfTargetChangeIsPermittedExceptionIsNotThrowed() {
        ActionStep issueCouns = coinStep();

        PlainAction oldAction = new PlainAction();
        oldAction.setConfig(config(issueCouns));

        ActionConfig newConfig = config(issueCouns);
        newConfig.setTarget(new TargetAudience(LinkingMode.DIRECT_ONLY, "new_segment"));

        PlainAction newAction = new PlainAction();
        newAction.setConfig(newConfig);

        workflow.checkEditingIsPermitted(oldAction, newAction);
    }

    /**
     * Выполнающийся шаг сегментации не может быть перезапущен
     */
    @Test
    public void testRunningBuildSegmentStepCannotBeRestarted() {
        ActionStatus status = new ActionStatus();
        status.setSteps(
                Collections.singletonMap(
                        SEGMENT_STEP_ID,
                        new BuildSegmentStepStatus()
                                .setStageStatus(StageStatus.IN_PROGRESS)
                )
        );

        PlainAction action = new PlainAction();
        action.setConfig(config());
        action.setStatus(status);

        getPermissions(action)
                .assertNotPermittedForSegmentStep(START);
    }

    /**
     * Нельзя перезапустить выполненный шаг, идущий перед шагом результат
     * выполнения которого имеет внешний эффект
     */
    @Test
    public void testCoinStepAfterFailedSentEmailStepCannotBeRestarted() {
        ActionStep coinStep = coinStep();
        ActionStep sendEmailsStep = sendEmailsStep();

        ActionConfig config = config(coinStep, sendEmailsStep);

        ActionStatus status = new ActionStatus()
                .setSteps(
                        ImmutableMap.of(
                                SEGMENT_STEP_ID,
                                new BuildSegmentStepStatus()
                                        .setStageStatus(StageStatus.FINISHED),
                                coinStep.getId(),
                                new IssueBunchStepStatus()
                                        .setStepId(coinStep.getId())
                                        .setStageStatus(FINISHED),
                                sendEmailsStep.getId(),
                                new SendEmailsStepStatus()
                                        .setStepId(sendEmailsStep.getId())
                                        .setStageStatus(ERROR)

                        )
                );

        PlainAction action = new PlainAction();
        action.setConfig(config);
        action.setStatus(status);

        getPermissions(action)
                .assertNotPermittedForStep(coinStep.getId(), START, CONTINUE);
    }

    /**
     * Для акции, не имеющей настроенных вариантов, доступен запуск шага сегментации
     */
    @Test
    public void testActionWithoutVariantsCanLaunchSegmentation() {
        ActionConfig config = new ActionConfig();
        config.setTarget(new TargetAudience(LinkingMode.ALL, "segment_id"));

        PlainAction action = new PlainAction();
        action.setConfig(config);

        getPermissions(action)
                .assertPermittedForSegmentStep(START);
    }

    private PermissionChecker getPermissions(PlainAction action) {
        WfPermissions permissions = workflow.getPermissions(action);
        return new PermissionChecker(permissions);
    }
}
