package ru.yandex.direct.core.entity.campaign.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.BroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.BroadmatchFlag;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.getCampaignClassByCampaignType;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(Parameterized.class)
public class RestrictedCampaignsUpdateOperationBroadMatchTest {
    private static final DefaultCompareStrategy COMPARE_STRATEGY =
            DefaultCompareStrategies.onlyExpectedFields();

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
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
    private MetrikaClientStub metrikaClient;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private FeatureService featureService;

    private CampaignInfo campaignWithEnabledBroadMatchInfo;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT}
        });
    }

    @Before
    public void before() {
        Campaign campaign = activeCampaignByCampaignType(campaignType);
        campaign.withStartTime(LocalDate.now())
                .withEmail("example@example.com")
                .withBroadmatchFlag(BroadmatchFlag.YES);
        campaignWithEnabledBroadMatchInfo = steps.campaignSteps().createCampaign(campaign);
    }

    @Test
    public void campaignWithEnabledBroadMatch_SetContextStrategy_BroadMatchOff() {
        DbStrategy strategy = defaultStrategy();
        strategy.setPlatform(CampaignsPlatform.CONTEXT);

        ModelChanges<? extends BaseCampaign> campaignModelChanges =
                ModelChanges.build(campaignWithEnabledBroadMatchInfo.getCampaignId(),
                        (Class<CampaignWithBroadMatch>) getCampaignClassByCampaignType(campaignType),
                        CampaignWithStrategy.STRATEGY,
                        strategy);

        var options = new CampaignOptions();
        MassResult<Long> result = apply(campaignWithEnabledBroadMatchInfo, campaignModelChanges, options);
        assertThat(result).matches(isFullySuccessful()::matches);

        CommonCampaign actualCampaign = getActualCampaign(campaignWithEnabledBroadMatchInfo);

        CommonCampaign expectedCampaign = getExpectedCampaign();

        assertThat(actualCampaign)
                .is(matchedBy(beanDiffer(expectedCampaign)
                        .useCompareStrategy(COMPARE_STRATEGY)));
    }

    @Test
    public void campaignWithContextStrategy_EnableBroadMatch_BroadMatchOff() {
        DbStrategy strategy = defaultStrategy();
        strategy.setPlatform(CampaignsPlatform.CONTEXT);

        ModelChanges<? extends CampaignWithBroadMatch> campaignModelChanges =
                ModelChanges.build(campaignWithEnabledBroadMatchInfo.getCampaignId(),
                        (Class<CampaignWithBroadMatch>) getCampaignClassByCampaignType(campaignType),
                        CampaignWithStrategy.STRATEGY,
                        strategy);

        campaignModelChanges.process(new BroadMatch().withBroadMatchFlag(true).withBroadMatchLimit(5),
                CampaignWithBroadMatch.BROAD_MATCH);

        var options = new CampaignOptions();
        MassResult<Long> result = apply(campaignWithEnabledBroadMatchInfo, campaignModelChanges, options);
        assertThat(result).matches(isFullySuccessful()::matches);

        CommonCampaign actualCampaign = getActualCampaign(campaignWithEnabledBroadMatchInfo);

        CommonCampaign expectedCampaign = getExpectedCampaign();

        assertThat(actualCampaign)
                .is(matchedBy(beanDiffer(expectedCampaign)
                        .useCompareStrategy(COMPARE_STRATEGY)));
    }

    private MassResult<Long> apply(
            CampaignInfo campaignInfo, ModelChanges<? extends BaseCampaign> modelChanges,
            CampaignOptions options
    ) {
        RestrictedCampaignsUpdateOperation restrictedCampaignsUpdateOperation = new RestrictedCampaignsUpdateOperation(
                Collections.singletonList(modelChanges),
                campaignInfo.getUid(),
                UidClientIdShard.of(campaignInfo.getUid(), campaignInfo.getClientId(), campaignInfo.getShard()),
                campaignModifyRepository,
                campaignTypedRepository,
                strategyTypedRepository,
                updateRestrictedCampaignValidationService,
                campaignUpdateOperationSupportFacade,
                campaignAdditionalActionsService,
                ppcDslContextProvider, rbacService, metrikaClientFactory, featureService,
                Applicability.PARTIAL, options);
        return restrictedCampaignsUpdateOperation.apply();
    }

    private CommonCampaign getActualCampaign(CampaignInfo campaignInfo) {
        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                        Collections.singletonList(campaignInfo.getCampaignId()));
        List<CommonCampaign> campaigns = mapList(typedCampaigns, campaign -> (CommonCampaign) campaign);
        return campaigns.get(0);
    }

    private CampaignWithBroadMatch getExpectedCampaign() {
        return ((CampaignWithBroadMatch) TestCampaigns.newCampaignByCampaignType(campaignType))
                .withBroadMatch(new BroadMatch()
                        .withBroadMatchFlag(false)
                        .withBroadMatchLimit(CampaignConstants.BROAD_MATCH_LIMIT_DEFAULT));
    }
}
