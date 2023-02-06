package ru.yandex.direct.api.v5.entity.campaigns.delegate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.yandex.direct.api.v5.campaigns.AddRequest;
import com.yandex.direct.api.v5.campaigns.AddResponse;
import com.yandex.direct.api.v5.campaigns.CampaignAddItem;
import com.yandex.direct.api.v5.campaigns.SmartCampaignAddItem;
import com.yandex.direct.api.v5.campaigns.SmartCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaigns.SmartCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.SmartCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaigns.SmartCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.SmartCampaignStrategyAdd;
import com.yandex.direct.api.v5.campaigns.StrategyMaximumConversionRateAdd;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.campaigns.converter.CampaignsAddRequestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.feature.FeatureName.AUTOBUDGET_STRATEGY_FOR_SMART_ALLOWED;

@Api5Test
@RunWith(SpringRunner.class)
public class AddSmartCampaignStrategyTest {
    private static final String NAME = "Тестовая кампания";
    private static final Integer COUNTER_ID = RandomNumberUtils.nextPositiveInteger();
    private static final Long GOAL_ID = RandomNumberUtils.nextPositiveLong();

    @Autowired
    private Steps steps;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private CampaignOperationService campaignOperationService;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private CampaignsAddRequestConverter requestConverter;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    private GenericApiService genericApiService;
    private AddCampaignsDelegate delegate;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();

        ApiUser user = new ApiUser()
                .withUid(clientInfo.getUid())
                .withClientId(clientId);

        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(user);
        when(auth.getChiefSubclient()).thenReturn(user);

        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());

        genericApiService = new GenericApiService(
                apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));

        delegate = new AddCampaignsDelegate(
                auth,
                campaignOperationService,
                requestConverter,
                resultConverter,
                ppcPropertiesSupport,
                featureService);

        metrikaClientStub.addUserCounter(clientInfo.getUid(), COUNTER_ID);
        metrikaClientStub.addCounterGoal(COUNTER_ID, GOAL_ID.intValue());

        steps.featureSteps().addClientFeature(clientId, AUTOBUDGET_STRATEGY_FOR_SMART_ALLOWED, true);
    }

    @Test
    public void addSmartCampaign_whenNetworkTypeIs_WbMaximumConversionRate() {
        var strategy = new SmartCampaignStrategyAdd()
                .withSearch(new SmartCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(SmartCampaignSearchStrategyTypeEnum.SERVING_OFF))
                .withNetwork(new SmartCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(SmartCampaignNetworkStrategyTypeEnum.WB_MAXIMUM_CONVERSION_RATE)
                        .withWbMaximumConversionRate(new StrategyMaximumConversionRateAdd()
                                .withWeeklySpendLimit(456_789_000L)
                                .withGoalId(GOAL_ID)));

        Long campaignId = addSmartCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.YES);
        expectedStrategy.withPlatform(CampaignsPlatform.CONTEXT);
        expectedStrategy.withStrategyName(StrategyName.AUTOBUDGET);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("autobudget")
                .withSum(new BigDecimal("456.79"))
                .withGoalId(GOAL_ID));

        checkStrategy(campaignId, expectedStrategy);
    }

    private Long addSmartCampaignWithStrategy(SmartCampaignStrategyAdd strategy) {
        SmartCampaignAddItem smartCampaignAddItem = new SmartCampaignAddItem()
                .withCounterId(COUNTER_ID)
                .withBiddingStrategy(strategy);

        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(LocalDate.now().toString())
                .withSmartCampaign(smartCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        return response.getAddResults().get(0).getId();
    }

    private void checkStrategy(Long campaignId, DbStrategy expected) {
        SmartCampaign campaign = getSmartCampaign(campaignId);
        DbStrategy actual = campaign.getStrategy();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getAutobudget()).as("autobudget").isEqualTo(expected.getAutobudget());
            softly.assertThat(actual.getPlatform()).as("platform").isEqualTo(expected.getPlatform());
            softly.assertThat(actual.getStrategy()).as("strategy").isEqualTo(expected.getStrategy());
            softly.assertThat(actual.getStrategyName()).as("strategyName").isEqualTo(expected.getStrategyName());
            softly.assertThat(actual.getStrategyData()).as("strategyData").isEqualTo(expected.getStrategyData());
        });
    }

    private SmartCampaign getSmartCampaign(Long campaignId) {
        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        return (SmartCampaign) campaigns.get(campaignId);
    }
}
