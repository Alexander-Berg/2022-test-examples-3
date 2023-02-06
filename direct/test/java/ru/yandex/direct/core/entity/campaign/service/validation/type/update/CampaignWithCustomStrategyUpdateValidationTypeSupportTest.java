package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.TextCampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.CampaignStrategyService;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCounterByDomainRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.metrika.client.MetrikaClientException;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.unableToUseCurrentMeaningfulGoalsForOptimization;
import static ru.yandex.direct.core.entity.metrika.utils.MetrikaGoalsUtils.ecommerceGoalId;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetRoiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategyForSimpleView;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.core.validation.defects.Defects.metrikaReturnsResultWithErrors;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithCustomStrategyUpdateValidationTypeSupportTest {
    private final static long UNAVAILABLE_COUNTER_ID = 123,
            UNAVAILABLE_COUNTER_ID_2 = 124,
            NOT_ALLOWED_BY_METRIKA_COUNTER_ID = 125,
            UNAVAILABLE_ECOMMERCE_COUNTER_ID = 126;
    private final static long UNAVAILABLE_AUTO_GOAL_ID = 1234,
            UNAVAILABLE_USER_GOAL_ID = 1235,
            NOT_ALLOWED_BY_METRIKA_GOAL_ID = 1236;

    @Autowired
    private CampaignWithCustomStrategyUpdateValidationTypeSupport typeSupport;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private CampaignStrategyService campaignStrategyService;
    @Autowired
    protected DslContextProvider dslContextProvider;
    @Autowired
    protected MetrikaCounterByDomainRepository metrikaCounterByDomainRepository;
    @Autowired
    private NetAcl netAcl;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    private CampaignInfo roiCampaignInfo;

    @Before
    public void before() {
        doReturn(false).when(netAcl).isInternalIp(any(InetAddress.class));

        roiCampaignInfo = steps.campaignSteps().createActiveTextCampaign();
        setRoiCampaignInfoMeaningfulGoals("[{\"value\": 1, \"goal_id\": \"34554921\"}]");
        steps.featureSteps().addClientFeature(roiCampaignInfo.getClientId(),
                FeatureName.METRIKA_COUNTERS_ACCESS_VALIDATION_ON_SAVE_CAMPAIGN_ENABLED, true);

        campaignStrategyService.updateTextCampaignStrategy(roiCampaignInfo.getCampaignId(),
                defaultAutobudgetRoiStrategy(MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID),
                roiCampaignInfo.getUid(),
                UidAndClientId.of(roiCampaignInfo.getUid(), roiCampaignInfo.getClientId()), false);

        metrikaClientStub.addUnavailableCounter(UNAVAILABLE_COUNTER_ID);
        metrikaClientStub.addUnavailableCounter(UNAVAILABLE_COUNTER_ID_2);
        metrikaClientStub.addUnavailableCounter(NOT_ALLOWED_BY_METRIKA_COUNTER_ID, false);
        metrikaClientStub.addUnavailableEcommerceCounter(UNAVAILABLE_ECOMMERCE_COUNTER_ID);
        metrikaClientStub.addCounterGoal((int) UNAVAILABLE_COUNTER_ID, new CounterGoal()
                .withId((int) UNAVAILABLE_AUTO_GOAL_ID)
                .withType(CounterGoal.Type.URL)
                .withSource(CounterGoal.Source.AUTO));
        metrikaClientStub.addCounterGoal((int) UNAVAILABLE_COUNTER_ID, new CounterGoal()
                .withId((int) UNAVAILABLE_USER_GOAL_ID)
                .withType(CounterGoal.Type.URL)
                .withSource(CounterGoal.Source.USER));
        metrikaClientStub.addCounterGoal((int) NOT_ALLOWED_BY_METRIKA_COUNTER_ID,
                (int) NOT_ALLOWED_BY_METRIKA_GOAL_ID);
    }

    @After
    public void after() {
        reset(netAcl, metrikaCounterByDomainRepository);
    }

    private void setRoiCampaignInfoMeaningfulGoals(String meaningfulGoals) {
        dslContextProvider.ppc(roiCampaignInfo.getShard())
                .update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.MEANINGFUL_GOALS, meaningfulGoals)
                .where(CAMP_OPTIONS.CID.eq(roiCampaignInfo.getCampaignId()))
                .execute();
    }

    @Test
    public void validate_Successfully() {
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(TestCampaigns.activeTextCampaign(null, null)
                .withStrategy(manualStrategy())
                .withMetrikaCounters(List.of(123L))
        );
        var vr = validate(campaignInfo, false);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_StrategyWithGoalIdAndCampaignWithoutCounters_Error() {
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(TestCampaigns.activeTextCampaign(null, null)
                .withStrategy(TestCampaigns.averageCpaStrategy()));
        var vr = validate(campaignInfo, false);
        assertGoalNotFound(vr);
    }

    @Test
    public void validate_StrategyWithUnavailableGoalId_AllowedByMetrikaFlagCounter_Success() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultTextCampaignWithSystemFields()
                .withStrategy(defaultStrategyForSimpleView(UNAVAILABLE_USER_GOAL_ID))
                .withMetrikaCounters(List.of(UNAVAILABLE_COUNTER_ID))
        );
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true);
        var vr = validate(campaignInfo, true);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    /**
     * Валидириуем кампанию с недоступным счетчиком и ее автоцелью.
     * Фича на автоцели включена, счетчик с автоцелью в связке с доменом из блэклиста,
     * поэтому не даем использовать цели с этого счетчика.
     */
    @Test
    public void validate_StrategyWithUnavailableGoalId_NotAllowedByMetrikaFlagCounter_Error() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultTextCampaignWithSystemFields()
                .withStrategy(defaultStrategyForSimpleView(NOT_ALLOWED_BY_METRIKA_GOAL_ID))
                .withMetrikaCounters(List.of(NOT_ALLOWED_BY_METRIKA_COUNTER_ID))
        );
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true);
        var vr = validate(campaignInfo, true);
        assertGoalNotFound(vr);
    }

    @Test
    public void validate_StrategyWithUnavailableAutoGoalId_Error() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultTextCampaignWithSystemFields()
                .withStrategy(defaultStrategyForSimpleView(UNAVAILABLE_AUTO_GOAL_ID))
                .withMetrikaCounters(List.of(UNAVAILABLE_COUNTER_ID))
        );
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_AUTO_GOALS_ALLOWED, false);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, false);
        var vr = validate(campaignInfo, true);
        assertGoalNotFound(vr);
    }

    @Test
    public void validate_UacStrategyWithUnavailableGoalId_Success() {
        CampaignInfo campaignInfo = createCampaignWithUnavailableGoal(CampaignSource.UAC);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.UAC_UNAVAILABLE_AUTO_GOALS_ALLOWED, false);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.UAC_UNAVAILABLE_GOALS_ALLOWED, true);
        var vr = validate(campaignInfo, true);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_UacStrategyWithUnavailableGoalId_Error() {
        CampaignInfo campaignInfo = createCampaignWithUnavailableGoal(CampaignSource.UAC);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.UAC_UNAVAILABLE_AUTO_GOALS_ALLOWED, true);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.UAC_UNAVAILABLE_GOALS_ALLOWED, false);
        var vr = validate(campaignInfo, true);
        assertGoalNotFound(vr);
    }

    @Test
    public void validate_StrategyWithUnavailableGoalId_Success() {
        CampaignInfo campaignInfo = createCampaignWithUnavailableGoal(CampaignSource.DIRECT);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_AUTO_GOALS_ALLOWED, false);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true);
        var vr = validate(campaignInfo, true);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_StrategyWithUnavailableGoalId_Error() {
        CampaignInfo campaignInfo = createCampaignWithUnavailableGoal(CampaignSource.DIRECT);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_AUTO_GOALS_ALLOWED, true);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, false);
        var vr = validate(campaignInfo, true);
        assertGoalNotFound(vr);
    }

    @Test
    public void validate_ApiStrategyWithUnavailableGoalId_Success() {
        CampaignInfo campaignInfo = createCampaignWithUnavailableGoal(CampaignSource.API);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true);
        var vr = validate(campaignInfo, true);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_ApiStrategyWithUnavailableGoalId_Error() {
        CampaignInfo campaignInfo = createCampaignWithUnavailableGoal(CampaignSource.API);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, false);
        var vr = validate(campaignInfo, true);
        assertGoalNotFound(vr);
    }

    @Test
    public void validate_GeoStrategyWithUnavailableGoalId_Success() {
        CampaignInfo campaignInfo = createCampaignWithUnavailableGoal(CampaignSource.GEO);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true);
        var vr = validate(campaignInfo, true);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_GeoStrategyWithUnavailableGoalId_Error() {
        CampaignInfo campaignInfo = createCampaignWithUnavailableGoal(CampaignSource.GEO);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, false);
        var vr = validate(campaignInfo, true);
        assertGoalNotFound(vr);
    }

    @Test
    public void validate_MetrikaUnavailable_Error() {
        CampaignInfo campaignInfo1 = steps.campaignSteps().createCampaign(TestCampaigns.activeTextCampaign(null, null)
                .withStrategy(TestCampaigns.averageCpaStrategy())
                .withMetrikaCounters(singletonList(456L))
        );

        MetrikaClient metrikaClient = mock(MetrikaClient.class);
        when(metrikaClient.getUsersCountersNumExtended2(any(), any())).thenThrow(MetrikaClientException.class);
        var vr = validate(campaignInfo1, metrikaClient,
                Set.of(FeatureName.GOALS_FROM_ALL_ORGS_ALLOWED.getName()), false);
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)), metrikaReturnsResultWithErrors())));
    }

    @Test
    public void testAutobudgetRoi_goal13Success() {
        //в стратегиях ROI начинаем принимать goal_id=13 для оптимизации по ключевым целям
        setRoiCampaignInfoMeaningfulGoals("[{\"value\": 1, \"goal_id\": \"12\"}, {\"value\": 1, \"goal_id\": " +
                "\"34554921\"}]");
        var vr = validate(roiCampaignInfo, false);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void testAutobudgetRoi_goal12Error() {
        //возвращаем ошибку валидации , если в кампании заданы только вовлеченные сессии
        // одна цель с goal_id == '12' это "вовлеченные сессии", их за цель не считаем
        setRoiCampaignInfoMeaningfulGoals("[{\"value\": 412, \"goal_id\": \"12\"}]");
        var vr = validate(roiCampaignInfo, false);
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                field(TextCampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.GOAL_ID)),
                unableToUseCurrentMeaningfulGoalsForOptimization())));
    }

    @Test
    public void testAutobudgetRoi_meaningfulGoalsEmptyError() {
        //возвращаем ошибку валидации , если в кампании в camp_options.meaningful_goals лежит null
        setRoiCampaignInfoMeaningfulGoals(null);
        var vr = validate(roiCampaignInfo, false);
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                field(TextCampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.GOAL_ID)),
                unableToUseCurrentMeaningfulGoalsForOptimization())));
    }

    @Test
    public void validate_StrategyWithUnavailableEcommerceGoalId_Success() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultTextCampaignWithSystemFields()
                .withStrategy(defaultStrategyForSimpleView(ecommerceGoalId(UNAVAILABLE_ECOMMERCE_COUNTER_ID)))
                .withMetrikaCounters(List.of(UNAVAILABLE_ECOMMERCE_COUNTER_ID))
        );
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.COLD_START_FOR_ECOMMERCE_GOALS, true);
        var vr = validate(campaignInfo, true);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_StrategyWithUnavailableEcommerceGoalId_Error() {
        CampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(defaultTextCampaignWithSystemFields()
                .withStrategy(defaultStrategyForSimpleView(ecommerceGoalId(UNAVAILABLE_COUNTER_ID)))
                .withMetrikaCounters(List.of(UNAVAILABLE_COUNTER_ID))
        );
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true);
        steps.featureSteps().addClientFeature(campaignInfo.getClientId(),
                FeatureName.COLD_START_FOR_ECOMMERCE_GOALS, true);
        var vr = validate(campaignInfo, true);
        assertGoalNotFound(vr);
    }

    private CampaignInfo createCampaignWithUnavailableGoal(CampaignSource source) {
        return steps.textCampaignSteps().createCampaign(defaultTextCampaignWithSystemFields()
                .withStrategy(defaultStrategyForSimpleView(UNAVAILABLE_USER_GOAL_ID))
                .withMetrikaCounters(List.of(UNAVAILABLE_COUNTER_ID))
                .withSource(source));
    }

    private ValidationResult<List<CampaignWithCustomStrategy>, Defect> validate(CampaignInfo campaignInfo,
                                                                                boolean shouldFetchUnavailableGoals) {
        return validate(campaignInfo, metrikaClientStub, Set.of(), shouldFetchUnavailableGoals);
    }

    private ValidationResult<List<CampaignWithCustomStrategy>, Defect> validate(CampaignInfo campaignInfo,
                                                                                MetrikaClient metrikaClient,
                                                                                Set<String> enabledFeatures,
                                                                                boolean shouldFetchUnavailableGoals) {
        var campaign = (TextCampaignWithCustomStrategy) campaignTypedRepository.getTypedCampaigns(
                campaignInfo.getShard(), singletonList(campaignInfo.getCampaignId())).get(0);
        var metrikaAdapter = new RequestBasedMetrikaClientAdapter(metrikaClient,
                List.of(campaignInfo.getClientId().asLong()), enabledFeatures,
                List.of(campaign), shouldFetchUnavailableGoals
        );
        var container = createContainer(campaignInfo, metrikaAdapter);
        AppliedChanges<CampaignWithCustomStrategy> appliedChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithCustomStrategy.class)
                        .applyTo(campaign);
        return typeSupport.validate(container, new ValidationResult<>(singletonList(campaign)),
                Map.of(0, appliedChanges));
    }

    private void assertGoalNotFound(ValidationResult<List<CampaignWithCustomStrategy>, Defect> vr) {
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                field(TextCampaignWithCustomStrategy.STRATEGY),
                field(DbStrategy.STRATEGY_DATA), field(StrategyData.GOAL_ID)), objectNotFound())));
    }

    private RestrictedCampaignsUpdateOperationContainerImpl createContainer(
            CampaignInfo campaignInfo,
            RequestBasedMetrikaClientAdapter metrikaAdapter) {
        return new RestrictedCampaignsUpdateOperationContainerImpl(
                campaignInfo.getShard(),
                campaignInfo.getUid(),
                campaignInfo.getClientId(),
                campaignInfo.getClientInfo().getUid(),
                campaignInfo.getClientInfo().getChiefUserInfo().getUid(),
                metrikaAdapter,
                new CampaignOptions(),
                null,
                emptyMap()
        );
    }
}
