package ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTrackerTrackingSystem;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.inconsistentStrategyToMobileCampaignWithoutTrackingSystem;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageClickStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaStrategy;
import static ru.yandex.direct.feature.FeatureName.RMP_CPI_UNDER_KNOWN_TRACKING_SYSTEM_ONLY;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты на использование стратегий в РМП в зваисимости от трекинговой системы приложения и параметров кампании
 */
@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithCustomStrategyValidatorMobileContentCampaignWithoutTrackingSystemTest {
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.test";
    private static final String INVALID_TRACKING_URL = "https://trusted1.com/?id=ru.com";
    private static final String VALID_TRACKING_URL = "https://adjust.com/q1w2e3?clickid={logid}";
    private static final String IMPRESSION_URL = "https://view.adjust.com/impression/q1w2e3?click_id={logid}";
    private static final long GOAL_ID = 38403191L;

    @Autowired
    private ClientService clientService;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;

    private MobileContentCampaign mobileCampaign;
    private CampaignWithCustomStrategyValidator validator;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public boolean knownTrackingSystem;

    @Parameterized.Parameter(2)
    public boolean isUacCampaign;

    @Parameterized.Parameter(3)
    public DbStrategy strategy;

    @Parameterized.Parameter(4)
    public Set<String> features;

    @Parameterized.Parameter(5)
    public Defect<String> expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                    "неизвестная трекинговая система, не uac-кампания, CPC стратегия",
                        false,
                        false,
                        averageClickStrategy(new BigDecimal("123"), null),
                        featureIsOn(),
                        null
                },
                {
                        "неизвестная трекинговая система, не uac-кампания, CPI стратегия",
                        false,
                        false,
                        averageCpiStrategy(GOAL_ID),
                        featureIsOn(),
                        inconsistentStrategyToMobileCampaignWithoutTrackingSystem()
                },
                {
                        "неизвестная трекинговая система, uac-кампания, CPI стратегия",
                        false,
                        true,
                        averageCpiStrategy(GOAL_ID),
                        featureIsOn(),
                        null
                },
                {
                        "неизвестная трекинговая система, не uac-кампания, CPI стратегия, фича отключена",
                        false,
                        false,
                        averageCpiStrategy(GOAL_ID),
                        featureIsOff(),
                        null
                },
                {
                        "неизвестная трекинговая система, не uac-кампания, CPA стратегия",
                        false,
                        false,
                        defaultAverageCpaStrategy(GOAL_ID),
                        featureIsOn(),
                        inconsistentStrategyToMobileCampaignWithoutTrackingSystem()
                },
                {
                        "известная трекинговая система, не uac-кампания, CPI стратегия",
                        true,
                        false,
                        averageCpiStrategy(GOAL_ID),
                        featureIsOn(),
                        null
                }
        });
    }

    private static Set<String> featureIsOff() {
        return Set.of();
    }

    private static Set<String> featureIsOn() {
        return Set.of(RMP_CPI_UNDER_KNOWN_TRACKING_SYSTEM_ONLY.getName());
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        steps.trustedRedirectSteps().addValidMobileCounter();
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        MobileAppInfo mobileAppInfo;
        if (knownTrackingSystem) {
            mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL, VALID_TRACKING_URL, IMPRESSION_URL);
            mobileAppInfo.getMobileApp().getTrackers().get(0).withTrackingSystem(MobileAppTrackerTrackingSystem.APPSFLYER);
        } else {
            mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL, INVALID_TRACKING_URL, "");
        }
        CampaignInfo campaignInfo = steps.campaignSteps()
                .createActiveMobileAppCampaign(clientInfo, mobileAppInfo);
        Currency currency = clientService.getWorkCurrency(campaignInfo.getClientId());
        mobileCampaign = (MobileContentCampaign) campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                singletonList(campaignInfo.getCampaignId())).get(0);
        mobileCampaign.withStrategy(strategy);
        mobileCampaign.withSource(isUacCampaign ? CampaignSource.UAC : CampaignSource.DIRECT);

        CommonStrategyValidatorConstants constants = new CommonStrategyValidatorConstants(currency);

        CampaignValidationContainer validationContainer = CampaignValidationContainer
                .create(campaignInfo.getShard(), campaignInfo.getUid(), campaignInfo.getClientId());
        validator = new CampaignWithCustomStrategyValidator(currency,
                Set.of((Goal) new Goal().withId(GOAL_ID).withIsMobileGoal(true)),
                Collections::emptyList, Collections::emptyList,
                banners -> Collections.emptyList(), mobileCampaign,
                Set.of(StrategyName.values()), Set.of(CampOptionsStrategy.values()),
                Set.of(CampaignsPlatform.values()),
                constants, features,
                validationContainer, null, false, mobileAppIds -> List.of(), appId -> mobileAppInfo.getMobileApp(),
                false, null);
    }

    @Test
    public void validateStrategy() {
        ValidationResult<CampaignWithCustomStrategy, Defect> vr = validator.apply(mobileCampaign);
        if (expectedDefect != null) {
            assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CampaignWithCustomStrategy.STRATEGY), field(DbStrategy.STRATEGY_NAME)),
                    expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }
}

