package ru.yandex.direct.core.entity.campaign.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMinusKeywords;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.steps.Steps;
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
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.TestCampaigns.getCampaignClassByCampaignType;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(Parameterized.class)
public class RestrictedCampaignsUpdateOperationMinusPhrasesTest {

    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies
            .onlyFields(newPath("type"), newPath("minusKeywords"));
    private static final String STORE_URL = "https://itunes.apple.com/ru/app/meduza/id555";

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
    @Autowired
    public DslContextProvider dslContextProvider;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public MetrikaClientStub metrikaClient;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private FeatureService featureService;

    private ClientInfo clientInfo;
    private Long campaignId;
    private Long mobileAppId;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        Campaign campaign =
                activeCampaignByCampaignType(campaignType, clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(manualStrategy()
                                .withPlatform(CampaignsPlatform.SEARCH));
        campaign.withStartTime(LocalDate.now())
                .withMinusKeywords(List.of("bla"))
                .withEmail("example@example.com");
        campaignId = steps.campaignSteps().createCampaign(campaign, clientInfo).getCampaignId();

        MobileAppInfo mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL);
        mobileAppId = mobileAppInfo.getMobileAppId();
    }

    @Test
    public void testValidationWorksFineAfterMinusPhrasesPreparation() {
        var minusPhrases = new ArrayList<>(List.of(" мИнУс фРаЗа ", "серо-белый [Москва]", " мИнУс фРаЗа ", "ааааа"));
        var preparedMinusPhrases = List.of("ааааа", "мИнУс фРаЗа", "серо белый [Москва]");

        ModelChanges mc;
        if (campaignType == CampaignType.MOBILE_CONTENT) {
            mc = getMobileContentCampaignModelChanges(minusPhrases);
        } else {
            mc = getCommonModelChanges(minusPhrases);
        }
        var options = new CampaignOptions();
        MassResult<Long> result = apply(mc, options);
        assertThat(result).matches(isFullySuccessful()::matches);

        CommonCampaign actualCampaign = getActualCampaign();
        CommonCampaign expectedCampaign = getExpectedCampaign(preparedMinusPhrases);

        MatcherAssert.assertThat(actualCampaign, beanDiffer(expectedCampaign).useCompareStrategy(COMPARE_STRATEGY));
    }

    public ModelChanges getMobileContentCampaignModelChanges(List<String> minusPhrases) {
        ModelChanges<? extends MobileContentCampaign> mc = ModelChanges.build(
                campaignId,
                (Class<MobileContentCampaign>) getCampaignClassByCampaignType(campaignType),
                CampaignWithMinusKeywords.MINUS_KEYWORDS,
                minusPhrases);
        mc.process(EnumSet.of(MobileAppDeviceTypeTargeting.PHONE), MobileContentCampaign.DEVICE_TYPE_TARGETING);
        mc.process(EnumSet.of(MobileAppNetworkTargeting.WI_FI), MobileContentCampaign.NETWORK_TARGETING);
        mc.process(mobileAppId, MobileContentCampaign.MOBILE_APP_ID);
        return mc;
    }

    public ModelChanges getCommonModelChanges(List<String> minusPhrases) {
        return ModelChanges.build(
                campaignId,
                (Class<CampaignWithMinusKeywords>) getCampaignClassByCampaignType(campaignType),
                CampaignWithMinusKeywords.MINUS_KEYWORDS,
                minusPhrases);
    }

    private MassResult<Long> apply(ModelChanges<? extends BaseCampaign> modelChanges, CampaignOptions options) {
        RestrictedCampaignsUpdateOperation restrictedCampaignsUpdateOperation = new RestrictedCampaignsUpdateOperation(
                Collections.singletonList(modelChanges),
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

    private CampaignWithMinusKeywords getExpectedCampaign(List<String> preparedMinusPhrases) {
        CampaignWithMinusKeywords campaign =
                (CampaignWithMinusKeywords) TestCampaigns.newCampaignByCampaignType(campaignType);
        return campaign.withMinusKeywords(preparedMinusPhrases);
    }

    private CommonCampaign getActualCampaign() {
        List<? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                Collections.singletonList(campaignId));
        List<CommonCampaign> campaigns = mapList(typedCampaigns, campaign -> (CommonCampaign) campaign);
        return campaigns.get(0);
    }
}
