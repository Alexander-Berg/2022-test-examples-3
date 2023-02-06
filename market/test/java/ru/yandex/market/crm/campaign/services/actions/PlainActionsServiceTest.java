package ru.yandex.market.crm.campaign.services.actions;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.ListableBeanFactory;

import ru.yandex.market.crm.campaign.domain.actions.ActionConfig;
import ru.yandex.market.crm.campaign.domain.actions.ActionVariant;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.StepType;
import ru.yandex.market.crm.campaign.domain.actions.status.BuildSegmentStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.IssueBunchStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.StepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.BuildSegmentStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.IssueCoinsStep;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.actions.tasks.ExecutePlainActionStepTask;
import ru.yandex.market.crm.campaign.services.appmetrica.POJOFrequencyToggleDAO;
import ru.yandex.market.crm.campaign.services.sending.FrequencyToggleService;
import ru.yandex.market.crm.campaign.test.ActionTestHelper.StrategyMock;
import ru.yandex.market.crm.campaign.test.StepStatusDAOMock;
import ru.yandex.market.crm.campaign.test.StepStatusUpdaterMock;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.services.control.GlobalControlSaltProvider;
import ru.yandex.market.crm.tasks.services.ClusterTasksService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.campaign.domain.workflow.StageStatus.FINISHED;
import static ru.yandex.market.crm.campaign.domain.workflow.StageStatus.SUSPENDED;
import static ru.yandex.market.crm.campaign.services.actions.ActionConstants.SEGMENT_STEP_ID;
import static ru.yandex.market.crm.campaign.test.ActionTestHelper.coinStep;
import static ru.yandex.market.crm.campaign.test.ActionTestHelper.variant;

/**
 * @author apershukov
 */
@RunWith(MockitoJUnitRunner.class)
public class PlainActionsServiceTest {

    @Mock
    private PlainActionsDAO actionsDAO;

    @Mock
    private ActionWorkflow actionWorkflow;

    @Mock
    private ListableBeanFactory beanFactory;

    @Mock
    private ClusterTasksService clusterTasksService;

    @Mock
    private ExecutePlainActionStepTask executeStepTask;

    @Mock
    private ActionPreviewService actionPreviewService;

    private final FrequencyToggleService frequencyToggleService =
            new FrequencyToggleService(new POJOFrequencyToggleDAO());

    private StepsStatusDAO stepsStatusDAO;

    private PlainActionsService service;

    @Before
    public void setUp() {
        stepsStatusDAO = new StepStatusDAOMock();

        StepStrategyProvider strategyProvider = new StepStrategyProvider(
                ImmutableMap.of(
                        StepType.ISSUE_COINS,
                        new StrategyMock<IssueCoinsStep, IssueBunchStepStatus>(IssueBunchStepStatus::new),
                        StepType.BUILD_SEGMENT,
                        new StrategyMock<BuildSegmentStep, BuildSegmentStepStatus>(BuildSegmentStepStatus::new)
                )
        );

        service = new PlainActionsService(
                actionsDAO,
                stepsStatusDAO,
                actionWorkflow,
                beanFactory,
                new StepStatusUpdaterMock(stepsStatusDAO),
                strategyProvider,
                clusterTasksService,
                new ActionDefaultConfigGenerator(),
                executeStepTask,
                new GlobalControlSaltProvider(),
                actionPreviewService,
                frequencyToggleService);
    }

    /**
     * При рестарте шага сегметации сбрасываются статусы шагов во всех вариантах
     */
    @Test
    public void testRemoveAllActionStatesOnSegmentStepRelaunch() {
        ActionStep step1 = coinStep();
        ActionStep step2 = coinStep();

        PlainAction action = prepareAction(
                variant("variant_a", step1),
                variant("variant_b", step2)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new BuildSegmentStepStatus()
                        .setStageStatus(FINISHED)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new IssueBunchStepStatus()
                        .setStepId(step1.getId())
                        .setStageStatus(SUSPENDED)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new IssueBunchStepStatus()
                        .setStepId(step2.getId())
                        .setStageStatus(FINISHED)
        );

        service.launchStep(action.getId(), SEGMENT_STEP_ID);

        StepStatus<?> segmentStatus = stepsStatusDAO.get(action.getId(), SEGMENT_STEP_ID);
        assertNotNull(segmentStatus);
        assertEquals(StageStatus.IN_PROGRESS, segmentStatus.getStageStatus());
        assertNull(segmentStatus.getSubStepId());

        assertNull(stepsStatusDAO.get(action.getId(), step1.getId()));
        assertNull(stepsStatusDAO.get(action.getId(), step2.getId()));
    }

    /**
     * При рестарте шага сбрасываются статусы шагов, выполняемых после него.
     */
    @Test
    public void testRestartOfStepClearsStatusOfNextSteps() {
        ActionStep step1 = coinStep("step_1", 111);
        ActionStep step2 = coinStep("step_2", 222);
        ActionStep step3 = coinStep("step_3", 333);

        PlainAction action = prepareAction(
                variant("variant_a", step1, step2),
                variant("variant_b", step3)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new BuildSegmentStepStatus()
                        .setStageStatus(FINISHED)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new IssueBunchStepStatus()
                        .setStepId(step1.getId())
                        .setStageStatus(FINISHED)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new IssueBunchStepStatus()
                        .setStepId(step2.getId())
                        .setStageStatus(SUSPENDED)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new IssueBunchStepStatus()
                        .setStepId(step3.getId())
                        .setStageStatus(FINISHED)
        );

        service.launchStep(action.getId(), step1.getId());

        StepStatus<?> segmentStatus = stepsStatusDAO.get(action.getId(), SEGMENT_STEP_ID);
        assertEquals(StageStatus.FINISHED, segmentStatus.getStageStatus());

        StepStatus<?> step1Status = stepsStatusDAO.get(action.getId(), step1.getId());
        assertNotNull(step1Status);
        assertEquals(StageStatus.IN_PROGRESS, step1Status.getStageStatus());

        assertNull(stepsStatusDAO.get(action.getId(), step2.getId()));

        StepStatus<?> step3Status = stepsStatusDAO.get(action.getId(), step3.getId());
        assertEquals(StageStatus.FINISHED, step3Status.getStageStatus());
    }

    /**
     * Редактирование шага приводит к сбрасыванию его статуса и статусов всех шагов,
     * следующих за ним
     */
    @Test
    public void testStepEditingResetsNextStepsStatuses() {
        ActionStep step1 = coinStep();
        ActionStep step2 = coinStep();
        ActionStep step3 = coinStep();

        PlainAction action = prepareAction(
                variant("variant_a", step1, step2, step3)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new BuildSegmentStepStatus()
                        .setStageStatus(FINISHED)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new IssueBunchStepStatus()
                        .setStepId(step1.getId())
                        .setStageStatus(FINISHED)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new IssueBunchStepStatus()
                        .setStepId(step2.getId())
                        .setStageStatus(FINISHED)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new IssueBunchStepStatus()
                        .setStepId(step3.getId())
                        .setStageStatus(FINISHED)
        );

        IssueCoinsStep newStep2 = coinStep();
        newStep2.setId(step2.getId());
        newStep2.setPromoId(45L);

        ActionConfig newConfig = new ActionConfig();
        newConfig.setTarget(action.getConfig().getTarget());
        newConfig.setVariants(Collections.singletonList(
                variant("variant_a", step1, newStep2, step3)
        ));

        PlainAction newAction = new PlainAction();
        newAction.setId(action.getId());
        newAction.setConfig(newConfig);

        service.editAction(newAction);

        StepStatus<?> segmentStatus = stepsStatusDAO.get(action.getId(), SEGMENT_STEP_ID);
        assertNotNull(segmentStatus);
        assertEquals(StageStatus.FINISHED, segmentStatus.getStageStatus());

        StepStatus<?> step1Status = stepsStatusDAO.get(action.getId(), step1.getId());
        assertNotNull(step1Status);
        assertEquals(StageStatus.FINISHED, step1Status.getStageStatus());

        assertNull(stepsStatusDAO.get(action.getId(), step2.getId()));
        assertNull(stepsStatusDAO.get(action.getId(), step3.getId()));
    }

    /**
     * Изменение настроек сегментации приводит к сбросу статуса шагов во всех вариантов
     */
    @Test
    public void testSegmentSettingsChangeResetsAllStepStatuses() {
        ActionStep step1 = coinStep();
        ActionStep step2 = coinStep();

        PlainAction action = prepareAction(
                variant("variant_a", step1),
                variant("variant_b", step2)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new BuildSegmentStepStatus()
                        .setStageStatus(FINISHED)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new IssueBunchStepStatus()
                        .setStepId(step1.getId())
                        .setStageStatus(FINISHED)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new IssueBunchStepStatus()
                        .setStepId(step2.getId())
                        .setStageStatus(FINISHED)
        );

        ActionConfig newConfig = new ActionConfig();
        newConfig.setTarget(new TargetAudience(LinkingMode.DIRECT_ONLY, "new_segment_id"));
        newConfig.setVariants(action.getConfig().getVariants());

        PlainAction newAction = new PlainAction();
        newAction.setId(action.getId());
        newAction.setConfig(newConfig);

        service.editAction(newAction);

        assertNull(stepsStatusDAO.get(action.getId(), SEGMENT_STEP_ID));
        assertNull(stepsStatusDAO.get(action.getId(), step1.getId()));
        assertNull(stepsStatusDAO.get(action.getId(), step2.getId()));
    }

    @Test
    public void testVariantRemovingClearsStatusesOfAllItsSteps() {
        ActionStep step1 = coinStep();
        ActionStep step2 = coinStep();

        PlainAction action = prepareAction(
                variant("variant_a", step1, step2)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new BuildSegmentStepStatus()
                        .setStageStatus(FINISHED)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new IssueBunchStepStatus()
                        .setStepId(step1.getId())
                        .setStageStatus(FINISHED)
        );

        stepsStatusDAO.upsert(
                action.getId(),
                new IssueBunchStepStatus()
                        .setStepId(step2.getId())
                        .setStageStatus(FINISHED)
        );

        ActionConfig newConfig = new ActionConfig();
        newConfig.setTarget(action.getConfig().getTarget());

        PlainAction newAction = new PlainAction();
        newAction.setId(action.getId());
        newAction.setConfig(newConfig);

        service.editAction(newAction);

        StepStatus<?> segmentStatus = stepsStatusDAO.get(action.getId(), SEGMENT_STEP_ID);
        assertNotNull(segmentStatus);
        assertEquals(StageStatus.FINISHED, segmentStatus.getStageStatus());

        assertNull(stepsStatusDAO.get(action.getId(), step1.getId()));
        assertNull(stepsStatusDAO.get(action.getId(), step2.getId()));
    }

    private PlainAction prepareAction(ActionVariant... variants) {
        ActionConfig config = new ActionConfig();
        config.setTarget(new TargetAudience(LinkingMode.ALL, "segment_id"));
        config.setVariants(Arrays.asList(variants));

        PlainAction action = new PlainAction();
        action.setId(UUID.randomUUID().toString());
        action.setConfig(config);

        when(actionsDAO.getAction(action.getId()))
                .thenReturn(Optional.of(action));

        return action;
    }
}
