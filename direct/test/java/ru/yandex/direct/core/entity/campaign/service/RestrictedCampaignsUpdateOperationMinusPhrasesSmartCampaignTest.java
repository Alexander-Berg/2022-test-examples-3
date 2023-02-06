package ru.yandex.direct.core.entity.campaign.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMetrikaCounters;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMinusKeywords;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignsUpdateOperationMinusPhrasesSmartCampaignTest {

    private static final CompareStrategy COMPARE_MINUS_PHRASES = DefaultCompareStrategies
            .onlyFields(newPath("type"), newPath("minusKeywords"));
    private static final Long COUNTER_ID = 5L;

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
    private Steps steps;
    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    public RbacService rbacService;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected MetrikaClientStub metrikaClientStub;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private FeatureService featureService;

    private ClientInfo clientInfo;
    private Long campaignId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        metrikaClientStub.addUserCounter(clientInfo.getUid(), COUNTER_ID.intValue());
        createSmartCampaign();
    }

    @Test
    public void testValidationWorksFineAfterMinusPhrasesPreparation() {
        var minusPhrases = new ArrayList<>(List.of(" мИнУс фРаЗа ", "серо-белый [Москва]", " мИнУс фРаЗа ", "ааааа"));
        var preparedMinusPhrases = List.of("ааааа", "мИнУс фРаЗа", "серо белый [Москва]");

        ModelChanges<SmartCampaign> campaignModelChanges = new ModelChanges<>(campaignId, SmartCampaign.class);
        campaignModelChanges.process(minusPhrases, CampaignWithMinusKeywords.MINUS_KEYWORDS);
        campaignModelChanges.process(List.of(COUNTER_ID),
                CampaignWithMetrikaCounters.METRIKA_COUNTERS);

        var options = new CampaignOptions();
        MassResult<Long> result = apply(campaignModelChanges, options);
        assertThat(result).matches(isFullySuccessful()::matches);

        CommonCampaign actualCampaign = getActualCampaign();
        CommonCampaign expectedCampaign = new SmartCampaign()
                .withType(CampaignType.PERFORMANCE)
                .withMinusKeywords(preparedMinusPhrases);

        MatcherAssert.assertThat(actualCampaign, beanDiffer(expectedCampaign)
                .useCompareStrategy(COMPARE_MINUS_PHRASES));
    }

    private MassResult<Long> apply(ModelChanges<? extends BaseCampaign> modelChanges, CampaignOptions options) {
        RestrictedCampaignsUpdateOperation restrictedCampaignsUpdateOperation = new RestrictedCampaignsUpdateOperation(
                singletonList(modelChanges),
                clientInfo.getUid(),
                UidClientIdShard.of(clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getShard()),
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

    private CommonCampaign getActualCampaign() {
        List<? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                Collections.singletonList(campaignId));
        List<CommonCampaign> campaigns = mapList(typedCampaigns, campaign -> (CommonCampaign) campaign);
        return campaigns.get(0);
    }

    private void createSmartCampaign() {
        SmartCampaign campaign = TestCampaigns.defaultSmartCampaignWithSystemFields(clientInfo);

        var addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(clientInfo.getShard(),
                clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid());
        List<Long> campaignIds =
                campaignModifyRepository.addCampaigns(dslContextProvider.ppc(clientInfo.getShard()),
                        addCampaignParametersContainer, Collections.singletonList(campaign));
        campaignId = campaignIds.get(0);
    }
}
