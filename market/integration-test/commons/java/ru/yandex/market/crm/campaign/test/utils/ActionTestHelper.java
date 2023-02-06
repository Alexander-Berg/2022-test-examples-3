package ru.yandex.market.crm.campaign.test.utils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.domain.actions.ActionConfig;
import ru.yandex.market.crm.campaign.domain.actions.ActionStatus;
import ru.yandex.market.crm.campaign.domain.actions.ActionVariant;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.conditions.ActionSegmentPart;
import ru.yandex.market.crm.campaign.domain.actions.conditions.MobileUsersCondition;
import ru.yandex.market.crm.campaign.domain.actions.conditions.SegmentCondition;
import ru.yandex.market.crm.campaign.domain.actions.conditions.SubscribedCondition;
import ru.yandex.market.crm.campaign.domain.actions.status.BuildSegmentStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.StepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.FilterMobileUsersStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.FilterSubscribedStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.FoldByCryptaStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.IssueCashbackStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.IssueCoinsStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.ManualLocalControlStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.MultifilterStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.PrepareLoyaltyCoinsDataStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.SaveCoinsFromVarsStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.SendEmailsStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.SendPushesStep;
import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.actions.PlainActionsService;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;
import ru.yandex.market.crm.campaign.services.actions.steps.loyalty.PreparingCoinsDataType;
import ru.yandex.market.crm.campaign.services.grouping.campaign.CampaignDAO;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.mapreduce.domain.action.StepOutputRow;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ru.yandex.market.crm.campaign.services.actions.ActionConstants.SEGMENT_STEP_ID;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.ALL;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING;

/**
 * @author apershukov
 */
@Component
public class ActionTestHelper {

    public static StepOutputRow outputRow(UidType idType, String idValue) {
        StepOutputRow row = new StepOutputRow();
        row.setIdType(idType);
        row.setIdValue(idValue);
        row.setData(new StepOutputRow.Data());
        return row;
    }

    public static ActionStep foldByCrypta() {
        ActionStep step = new FoldByCryptaStep();
        generateId(step);
        return step;
    }

    public static IssueCoinsStep issueCoins(long promoId, Integer sizeLimit) {
        IssueCoinsStep step = new IssueCoinsStep();
        generateId(step);
        step.setPromoId(promoId);
        step.setSizeLimit(sizeLimit);
        return step;
    }

    public static IssueCoinsStep issueCoins(long promoId) {
        return issueCoins(promoId, null);
    }

    public static IssueCashbackStep issueCashback(long promoId, long cashbackAmount) {
        IssueCashbackStep step = new IssueCashbackStep();
        generateId(step);
        step.setPromoId(promoId);
        step.setCashbackCount(cashbackAmount);
        return step;
    }

    public static SendEmailsStep sendEmails(String templateId) {
        SendEmailsStep step = new SendEmailsStep();
        generateId(step);

        step.setUtmCampaign("campaign");
        step.setUtmMedium("medium");
        step.setUtmSource("source");

        step.setTemplateId(templateId);
        step.setSubscriptionType(ALL);
        return step;
    }

    public static SendPushesStep sendPushes(String templateId) {
        SendPushesStep step = new SendPushesStep();
        generateId(step);
        step.setTemplateId(templateId);
        step.setSubscriptionType(STORE_PUSH_GENERAL_ADVERTISING);
        return step;
    }

    public static FilterSubscribedStep filterSubscribed() {
        return filterSubscribed(ADVERTISING);
    }

    public static FilterSubscribedStep filterSubscribed(SubscriptionType subscriptionType) {
        FilterSubscribedStep step = new FilterSubscribedStep();
        generateId(step);
        step.setSubscriptionType(subscriptionType);
        return step;
    }

    public static FilterMobileUsersStep filterMobileUsers() {
        return filterMobileUsers(null);
    }

    public static FilterMobileUsersStep filterMobileUsers(SubscriptionType subscriptionType) {
        FilterMobileUsersStep step = new FilterMobileUsersStep();
        generateId(step);
        step.setApplication(MobileApplication.MARKET_APP);
        step.setSubscriptionType(subscriptionType);
        return step;
    }

    public static MultifilterStep multifilter(ActionSegmentPart segmentPart) {
        MultifilterStep step = new MultifilterStep();
        generateId(step);
        step.setSegmentConfig(segmentPart);
        return step;
    }

    public static ManualLocalControlStep manualLocalControlStep(int controlGroupPercent) {
        var step = new ManualLocalControlStep();
        generateId(step);
        step.setControlGroupPercent(controlGroupPercent);
        return step;
    }

    public static PrepareLoyaltyCoinsDataStep prepareLoyaltyCoinsData(PreparingCoinsDataType preparingCoinsDataType) {
        PrepareLoyaltyCoinsDataStep step = new PrepareLoyaltyCoinsDataStep();
        generateId(step);
        step.setPreparingCoinsDataType(preparingCoinsDataType);
        return step;
    }

    public static SaveCoinsFromVarsStep saveNotifiedUsersCoinsStep() {
        SaveCoinsFromVarsStep step = new SaveCoinsFromVarsStep();
        generateId(step);
        return step;
    }

    public static MobileUsersCondition mobileUsersCondition() {
        return mobileUsersCondition(null);
    }

    public static MobileUsersCondition mobileUsersCondition(SubscriptionType subscriptionType) {
        MobileUsersCondition condition = new MobileUsersCondition();
        generateId(condition);
        condition.setApplication(MobileApplication.MARKET_APP);
        condition.setSubscriptionType(subscriptionType);
        return condition;
    }

    public static SubscribedCondition subscribedCondition() {
        SubscribedCondition condition = new SubscribedCondition();
        generateId(condition);
        condition.setSubscriptionType(ADVERTISING);
        return condition;
    }

    public static ActionVariant variant(String id, int percent, ActionStep... steps) {
        ActionVariant variant = new ActionVariant();
        variant.setId(id);
        variant.setPercent(percent);
        variant.setSteps(Arrays.asList(steps));
        return variant;
    }

    private static void generateId(ActionStep step) {
        step.setId(step.getType() + "_" + UUID.randomUUID());
    }

    private static void generateId(SegmentCondition condition) {
        condition.setId(condition.getConditionType() + "_" + UUID.randomUUID());
    }

    public static final String DEFAULT_VARIANT = "variant_a";

    private final PlainActionsService actionsService;
    private final CampaignDAO campaignDAO;
    private final StepsStatusDAO stepsStatusDAO;
    private final YtClient ytClient;
    private final YtFolders ytFolders;

    public ActionTestHelper(PlainActionsService actionsService,
                            CampaignDAO campaignDAO,
                            StepsStatusDAO stepsStatusDAO,
                            YtClient ytClient,
                            YtFolders ytFolders) {
        this.actionsService = actionsService;
        this.campaignDAO = campaignDAO;
        this.stepsStatusDAO = stepsStatusDAO;
        this.ytClient = ytClient;
        this.ytFolders = ytFolders;
    }

    public void waitFor(String actionId, Duration timeout, Predicate<PlainAction> condition) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        while (true) {
            if (System.currentTimeMillis() - startTime > timeout.toMillis()) {
                fail("Action condition wait timeout");
            }

            Thread.sleep(1000);

            PlainAction action = actionsService.getAction(actionId);

            if (condition.test(action)) {
                return;
            }
        }
    }

    public PlainAction prepareAction(String segmentId, LinkingMode linkingMode, ActionStep... steps) {
        ActionVariant variant = variant(DEFAULT_VARIANT, 100, steps);
        return prepareActionWithVariants(segmentId, linkingMode, variant);
    }

    public PlainAction prepareActionWithVariants(String segmentId, LinkingMode linkingMode, ActionVariant... variants) {
        ActionConfig config = new ActionConfig();
        config.setVariants(Arrays.asList(variants));
        config.setTarget(new TargetAudience(linkingMode, segmentId));

        Campaign campaign = prepareCampaign();

        PlainAction action = new PlainAction();
        action.setId(IdGenerationUtils.dateTimeId());
        action.setName("Test Action");
        action.setConfig(config);

        actionsService.addAction(campaign.getId(), action);

        action.setConfig(config);
        actionsService.editAction(action);
        return action;
    }

    public void enableGlobalControl(PlainAction action) {
        action.getConfig().setGlobalControlEnabled(true);
        actionsService.editAction(action);
    }

    public void enableFoldByCrypta(PlainAction action) {
        action.getConfig().setFoldByCryptaEnabled(true);
        actionsService.editAction(action);
    }

    public void updateAction(PlainAction action) {
        actionsService.editAction(action);
    }

    public void finishStep(String actionId, String stepId, Supplier<? extends StepStatus<?>> statusSupplier) {
        StepStatus<?> status = statusSupplier.get()
                .setStepId(stepId)
                .setStageStatus(StageStatus.FINISHED);

        stepsStatusDAO.upsert(actionId, status);
    }

    public void finishSegmentation(String actionId) {
        finishStep(actionId, SEGMENT_STEP_ID, BuildSegmentStepStatus::new);
    }

    public void prepareStepOutput(PlainAction action, ActionStep step, StepOutputRow... rows) {
        YPath output = getStepOutputPath(action.getId(), step.getId());
        ytClient.write(output, StepOutputRow.class, List.of(rows));
    }

    public YPath getStepOutputPath(String actionId, String stepId) {
        return getStepDirectory(actionId, stepId).child("output");
    }

    public void prepareSegmentationResult(String actionId, List<StepOutputRow> rows) {
        finishSegmentation(actionId);

        var prevPath = createSegmentationTable(actionId);
        ytClient.write(prevPath, StepOutputRow.class, rows);
    }

    private YPath createSegmentationTable(String actionId) {
        YPath prevPath = ytFolders.getActionPath(actionId)
                .child(SEGMENT_STEP_ID)
                .child(DEFAULT_VARIANT);
        ytClient.createTable(prevPath, "actions/step_output.yson");
        return prevPath;
    }

    public YPath getStepDirectory(String actionId, String stepId) {
        return ytFolders.getActionPath(actionId).child(stepId);
    }

    public List<StepOutputRow> execute(PlainAction action, ActionStep startStep, ActionStep endStep)
            throws InterruptedException {
        actionsService.launchStep(action.getId(), startStep.getId());
        waitExecuted(action.getId(), Set.of(startStep.getId(), endStep.getId()));

        return readResults(endStep, action);
    }

    public List<StepOutputRow> execute(PlainAction action, ActionStep step) throws Exception {
        actionsService.launchStep(action.getId(), step.getId());
        waitExecuted(action.getId(), Collections.singleton(step.getId()));

        return readResults(step, action);
    }

    private List<StepOutputRow> readResults(ActionStep step, PlainAction action) {
        return ytClient.read(
                getStepOutputPath(action.getId(), step.getId()),
                StepOutputRow.class
        );
    }

    private Campaign prepareCampaign() {
        Campaign campaign = new Campaign();
        campaign.setName("Campaign");
        campaign = campaignDAO.insert(campaign);
        return campaign;
    }

    private void waitExecuted(String actionId, Set<String> lastStepIds) throws InterruptedException {
        waitFor(actionId, Duration.ofMinutes(25), action -> {
            ActionStatus status = action.getStatus();
            assertNotNull(status);

            return lastStepIds.stream()
                    .allMatch(stepId -> {
                        StepStatus<?> stepStatus = status.getSteps().get(stepId);

                        if (stepStatus == null) {
                            return false;
                        }

                        StageStatus stageStatus = stepStatus.getStageStatus();

                        assertTrue(
                                String.format("Step %s: status is %s", stepId, stageStatus),
                                stageStatus == StageStatus.IN_PROGRESS || stageStatus == StageStatus.FINISHED
                        );

                        return stageStatus == StageStatus.FINISHED;
                    });
        });
    }
}
