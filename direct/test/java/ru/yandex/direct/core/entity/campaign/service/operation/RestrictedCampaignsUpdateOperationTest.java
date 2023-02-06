package ru.yandex.direct.core.entity.campaign.service.operation;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegmentAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignAdditionalActionsService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.AUTO_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.THE_ONLY_VALID_CONTEXT_PRICE_COEF;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.illegalMinusKeywordChars;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategy;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultMetrikaGoals;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.isAfter;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignsUpdateOperationTest {

    private static final long TWO_SECONDS = 2000;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private UpdateRestrictedCampaignValidationService updateRestrictedCampaignValidationService;
    @Autowired
    private CampaignUpdateOperationSupportFacade campaignUpdateOperationSupportFacade;
    @Autowired
    private CampaignAdditionalActionsService campaignAdditionalActionsService;
    @Autowired
    private DslContextProvider ppcDslContextProvider;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private Steps steps;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaHelperStub metrikaHelperStub;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private NetAcl netAcl;

    private CampaignInfo textCampaignInfo;
    private CampaignInfo otherCampaignInfo;
    private CampaignInfo secondOtherCampaignInfo;
    private Integer counterId;
    private Goal firstGoal;
    private List<Goal> goals;
    private CampaignUpdateOperationSupportFacade campaignUpdateOperationSupportFacadeSpy;

    @Before
    public void before() {
        textCampaignInfo = steps.campaignSteps().createActiveTextCampaign();
        otherCampaignInfo = steps.campaignSteps().createActiveTextCampaign(textCampaignInfo.getClientInfo());
        secondOtherCampaignInfo = steps.campaignSteps().createActiveTextCampaign(textCampaignInfo.getClientInfo());

        goals = defaultMetrikaGoals();
        firstGoal = goals.get(0);
        counterId = 1;
        firstGoal.setCounterId(counterId);

        steps.retargetingGoalsSteps().createMetrikaGoalsInPpcDict(goals);
        metrikaClientStub.addUserCounter(textCampaignInfo.getUid(), counterId);
        goals.forEach(goal -> metrikaClientStub.addCounterGoal(counterId, goal.getId().intValue()));
        metrikaClientStub.addGoals(textCampaignInfo.getUid(), new HashSet<>(goals));
        metrikaHelperStub.addGoalIds(textCampaignInfo.getUid(), listToSet(goals, GoalBase::getId));

        doReturn(false).when(netAcl).isInternalIp(any(InetAddress.class));
    }

    @After
    public void after() {
        reset(netAcl);
    }

    @Test
    public void update() throws InterruptedException {
        // Задержка нужна для того, чтобы lastChange мог измениться на новое значение.
        Thread.sleep(TWO_SECONDS);

        String newName = "newName";
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.plusDays(1);
        LocalDate endDate = now.plusDays(2);

        Long dialogId = steps.dialogSteps()
                .createStandaloneDefaultDialog(textCampaignInfo.getClientInfo())
                .getDialog()
                .getId();

        List<Long> counterIds = List.of(counterId.longValue());
        List<MeaningfulGoal> meaningfulGoals =
                List.of(new MeaningfulGoal().withGoalId(firstGoal.getId()).withConversionValue(BigDecimal.TEN));

        BidModifierABSegment bidModifierABSegment = new BidModifierABSegment()
                .withType(BidModifierType.AB_SEGMENT_MULTIPLIER)
                .withAbSegmentAdjustments(List.of(new BidModifierABSegmentAdjustment()
                                .withPercent(11)
                                .withSectionId(goals.get(4).getSectionId())
                                .withSegmentId(goals.get(4).getId())
                        )
                );
        BigDecimal dayBudget = BigDecimal.valueOf(55555, 2);
        DayBudgetShowMode dayBudgetShowMode = DayBudgetShowMode.STRETCHED;
        Boolean enableCpcHold = true;

        ModelChanges<TextCampaign> textCampaignModelChanges =
                getTextCampaignModelChanges(newName, startDate, endDate, counterIds, meaningfulGoals, dialogId,
                        bidModifierABSegment, dayBudget, dayBudgetShowMode, MIN_CONTEXT_LIMIT, enableCpcHold);

        campaignUpdateOperationSupportFacadeSpy = spy(campaignUpdateOperationSupportFacade);
        var options = new CampaignOptions();
        RestrictedCampaignsUpdateOperation restrictedCampaignsUpdateOperation = new RestrictedCampaignsUpdateOperation(
                List.of(textCampaignModelChanges),
                textCampaignInfo.getUid(),
                UidClientIdShard.of(textCampaignInfo.getUid(), textCampaignInfo.getClientId(),
                        textCampaignInfo.getShard()),
                campaignModifyRepository,
                campaignTypedRepository,
                strategyTypedRepository,
                updateRestrictedCampaignValidationService, campaignUpdateOperationSupportFacadeSpy,
                campaignAdditionalActionsService, ppcDslContextProvider, rbacService, metrikaClientFactory,
                featureService, Applicability.PARTIAL, options);
        MassResult<Long> result = restrictedCampaignsUpdateOperation.apply();
        assertThat(result.getValidationResult().flattenErrors()).is(matchedBy(beanDiffer(emptyList())));


        TextCampaign expectedCampaign = new TextCampaign()
                .withId(textCampaignInfo.getCampaignId())
                .withClientId(textCampaignInfo.getClientId().asLong())
                .withName(newName)
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withType(CampaignType.TEXT)
                .withMetrikaCounters(counterIds)
                .withMeaningfulGoals(meaningfulGoals)
                .withHasTitleSubstitution(true)
                .withHasExtendedGeoTargeting(true)
                .withStrategy(defaultStrategy())
                .withClientDialogId(dialogId)
                .withDayBudget(dayBudget)
                .withDayBudgetShowMode(dayBudgetShowMode)
                .withDayBudgetDailyChangeCount(1)
                .withContextLimit(AUTO_CONTEXT_LIMIT)
                .withEnableCpcHold(enableCpcHold)
                .withContextPriceCoef(THE_ONLY_VALID_CONTEXT_PRICE_COEF)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withLastChange(textCampaignInfo.getCampaign().getLastChange());

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(textCampaignInfo.getShard(),
                        Collections.singletonList(textCampaignInfo.getCampaignId()));

        List<TextCampaign> textCampaigns = mapList(typedCampaigns, campaign -> (TextCampaign) campaign);
        TextCampaign actualCampaign = textCampaigns.get(0);

        DefaultCompareStrategy compareStrategy =
                DefaultCompareStrategies.onlyExpectedFields()
                        .forFields(newPath(TextCampaign.LAST_CHANGE.name()))
                        .useMatcher(isAfter(expectedCampaign.getLastChange()))
                        .forFields(
                                newPath(TextCampaign.BID_MODIFIERS.name(), ".*", BidModifier.LAST_CHANGE.name()),
                                newPath(TextCampaign.BID_MODIFIERS.name(), ".*",
                                        BidModifierABSegment.AB_SEGMENT_ADJUSTMENTS.name(), ".*",
                                        BidModifierABSegmentAdjustment.LAST_CHANGE.name()))
                        .useMatcher(approximatelyNow());

        assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign)
                .useCompareStrategy(compareStrategy)));
        verify(campaignUpdateOperationSupportFacadeSpy, times(1)).afterExecution(anyList(), any());
    }

    @Test
    public void updateCampaignsOneWithErrorOnPreValidationOneWithErrorOnValidateBeforeApplyOneValid_FirstCampaignHasPreValidationErrorSecondValidateBeforeApplyError() {
        var invalidMinusKeywords = List.of("--!@#$");
        var notExistMeaningfulGoal = 0L;

        ModelChanges<TextCampaign> mcFirst = new ModelChanges<>(textCampaignInfo.getCampaignId(), TextCampaign.class);
        mcFirst.process(invalidMinusKeywords, TextCampaign.MINUS_KEYWORDS);

        ModelChanges<TextCampaign> mcSecond = new ModelChanges<>(otherCampaignInfo.getCampaignId(), TextCampaign.class);
        mcSecond.process(List.of(new MeaningfulGoal().withGoalId(notExistMeaningfulGoal)),
                TextCampaign.MEANINGFUL_GOALS);

        ModelChanges<TextCampaign> mcThird = new ModelChanges<>(secondOtherCampaignInfo.getCampaignId(),
                TextCampaign.class);

        var options = new CampaignOptions();
        RestrictedCampaignsUpdateOperation restrictedCampaignsUpdateOperation = new RestrictedCampaignsUpdateOperation(
                List.of(mcFirst, mcSecond, mcThird),
                textCampaignInfo.getUid(),
                UidClientIdShard.of(textCampaignInfo.getUid(), textCampaignInfo.getClientId(),
                        textCampaignInfo.getShard()),
                campaignModifyRepository,
                campaignTypedRepository,
                strategyTypedRepository,
                updateRestrictedCampaignValidationService, campaignUpdateOperationSupportFacade,
                campaignAdditionalActionsService, ppcDslContextProvider, rbacService, metrikaClientFactory,
                featureService, Applicability.PARTIAL, options);

        MassResult<Long> result = restrictedCampaignsUpdateOperation.apply();

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0),
                                field(TextCampaign.MINUS_KEYWORDS),
                                index(0)),
                        illegalMinusKeywordChars(invalidMinusKeywords)))))
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(1),
                                field(TextCampaign.MEANINGFUL_GOALS),
                                index(0), field(MeaningfulGoal.GOAL_ID)),
                        inCollection()))))
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(1),
                                field(TextCampaign.MEANINGFUL_GOALS),
                                index(0), field(MeaningfulGoal.CONVERSION_VALUE)),
                        notNull()))));
    }

    @Test
    public void updateCampaignWithErrorOnPreValidationAndValidateBeforeApply_HasOnlyPreValidationErrors() {
        var invalidMinusKeywords = List.of("--!@#$");
        var notExistMeaningfulGoal = 0L;

        ModelChanges<TextCampaign> mc = new ModelChanges<>(textCampaignInfo.getCampaignId(), TextCampaign.class);

        mc.process(List.of(new MeaningfulGoal().withGoalId(notExistMeaningfulGoal)), TextCampaign.MEANINGFUL_GOALS);
        mc.process(invalidMinusKeywords, TextCampaign.MINUS_KEYWORDS);

        var options = new CampaignOptions();
        RestrictedCampaignsUpdateOperation restrictedCampaignsUpdateOperation = new RestrictedCampaignsUpdateOperation(
                List.of(mc),
                textCampaignInfo.getUid(),
                UidClientIdShard.of(textCampaignInfo.getUid(), textCampaignInfo.getClientId(),
                        textCampaignInfo.getShard()),
                campaignModifyRepository,
                campaignTypedRepository,
                strategyTypedRepository,
                updateRestrictedCampaignValidationService, campaignUpdateOperationSupportFacade,
                campaignAdditionalActionsService, ppcDslContextProvider, rbacService, metrikaClientFactory,
                featureService, Applicability.PARTIAL, options);

        MassResult<Long> result = restrictedCampaignsUpdateOperation.apply();

        assertThat(result.getValidationResult().flattenErrors()).hasSize(1);
    }

    private ModelChanges<TextCampaign> getTextCampaignModelChanges(String newName, LocalDate startDate,
                                                                   LocalDate endDate,
                                                                   List<Long> counters,
                                                                   List<MeaningfulGoal> meaningfulGoals,
                                                                   Long clientDialogId,
                                                                   BidModifier bidModifier,
                                                                   BigDecimal dayBudget,
                                                                   DayBudgetShowMode dayBudgetShowMode,
                                                                   Integer contextLimit,
                                                                   Boolean enableCpcHold) {
        TextCampaign newTextCampaign = new TextCampaign()
                .withId(textCampaignInfo.getCampaignId())
                .withName(newName);

        ModelChanges<TextCampaign> textCampaignModelChanges = ModelChanges.build(newTextCampaign, TextCampaign.NAME,
                newTextCampaign.getName());
        textCampaignModelChanges.process(startDate, TextCampaign.START_DATE);
        textCampaignModelChanges.process(endDate, TextCampaign.END_DATE);
        textCampaignModelChanges.process(counters, TextCampaign.METRIKA_COUNTERS);
        textCampaignModelChanges.process(meaningfulGoals, TextCampaign.MEANINGFUL_GOALS);
        textCampaignModelChanges.process(RandomUtils.nextBoolean(), TextCampaign.ENABLE_OFFLINE_STAT_NOTICE);
        textCampaignModelChanges.process(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru", TextCampaign.EMAIL);
        textCampaignModelChanges.process(defaultStrategy(), TextCampaign.STRATEGY);
        textCampaignModelChanges.process(clientDialogId, TextCampaign.CLIENT_DIALOG_ID);
        textCampaignModelChanges.process(List.of(bidModifier), TextCampaign.BID_MODIFIERS);
        textCampaignModelChanges.process(contextLimit, TextCampaign.CONTEXT_LIMIT);
        textCampaignModelChanges.process(enableCpcHold, TextCampaign.ENABLE_CPC_HOLD);
        textCampaignModelChanges.process(dayBudget, TextCampaign.DAY_BUDGET);
        textCampaignModelChanges.process(dayBudgetShowMode, TextCampaign.DAY_BUDGET_SHOW_MODE);

        return textCampaignModelChanges;
    }

}
