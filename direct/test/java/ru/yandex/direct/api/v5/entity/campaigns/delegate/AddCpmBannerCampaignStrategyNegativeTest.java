package ru.yandex.direct.api.v5.entity.campaigns.delegate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.yandex.direct.api.v5.campaigns.AddRequest;
import com.yandex.direct.api.v5.campaigns.AddResponse;
import com.yandex.direct.api.v5.campaigns.CampaignAddItem;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignAddItem;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignStrategyAdd;
import com.yandex.direct.api.v5.campaigns.StrategyCpAverageCpvAdd;
import com.yandex.direct.api.v5.campaigns.StrategyWbMaximumImpressionsAdd;
import com.yandex.direct.api.v5.general.ExceptionNotification;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

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
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Api5Test
@RunWith(Parameterized.class)
public class AddCpmBannerCampaignStrategyNegativeTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String NAME = "Тестовая кампания";
    private static final Integer BAD_PARAMS_ERROR_CODE = 4000;

    @Autowired
    private Steps steps;
    @Autowired
    private CampaignOperationService campaignOperationService;
    @Autowired
    private CampaignsAddRequestConverter requestConverter;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FeatureService featureService;

    private GenericApiService genericApiService;
    private AddCampaignsDelegate delegate;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public CpmBannerCampaignStrategyAdd strategy;

    @Parameterized.Parameters(name = "RequestSource = {0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {
                        "NetworkSettings does not match strategyType",
                        new CpmBannerCampaignStrategyAdd()
                                .withSearch(new CpmBannerCampaignSearchStrategyAdd()
                                        .withBiddingStrategyType(CpmBannerCampaignSearchStrategyTypeEnum.SERVING_OFF))
                                .withNetwork(
                                new CpmBannerCampaignNetworkStrategyAdd()
                                        .withBiddingStrategyType(CpmBannerCampaignNetworkStrategyTypeEnum
                                                .WB_DECREASED_PRICE_FOR_REPEATED_IMPRESSIONS)
                                        .withCpAverageCpv(new StrategyCpAverageCpvAdd())
                        )
                },
                {
                        "NetworkSettings does not contain structure with settings",
                        new CpmBannerCampaignStrategyAdd()
                                .withSearch(new CpmBannerCampaignSearchStrategyAdd()
                                        .withBiddingStrategyType(CpmBannerCampaignSearchStrategyTypeEnum.SERVING_OFF))
                                .withNetwork(
                                new CpmBannerCampaignNetworkStrategyAdd()
                                        .withBiddingStrategyType(CpmBannerCampaignNetworkStrategyTypeEnum
                                                .WB_DECREASED_PRICE_FOR_REPEATED_IMPRESSIONS)
                        )
                },
                {
                        "Search type is unknown",
                        new CpmBannerCampaignStrategyAdd()
                                .withSearch(new CpmBannerCampaignSearchStrategyAdd()
                                        .withBiddingStrategyType(CpmBannerCampaignSearchStrategyTypeEnum.UNKNOWN))
                                .withNetwork(
                                new CpmBannerCampaignNetworkStrategyAdd()
                                        .withBiddingStrategyType(
                                                CpmBannerCampaignNetworkStrategyTypeEnum.WB_MAXIMUM_IMPRESSIONS)
                                        .withWbMaximumImpressions(new StrategyWbMaximumImpressionsAdd()
                                                .withAverageCpm(1000L)
                                                .withAverageCpm(1000L))
                        )
                },
                {
                        "Network type is unknown",
                        new CpmBannerCampaignStrategyAdd()
                                .withSearch(new CpmBannerCampaignSearchStrategyAdd()
                                        .withBiddingStrategyType(CpmBannerCampaignSearchStrategyTypeEnum.SERVING_OFF))
                                .withNetwork(
                                new CpmBannerCampaignNetworkStrategyAdd()
                                        .withBiddingStrategyType(CpmBannerCampaignNetworkStrategyTypeEnum.UNKNOWN)
                        )
                }
        });
    }

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        ApiUser user = new ApiUser()
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId());

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
    }

    @Test
    public void addCpmBannerCampaignWithInvalidStrategy() {
        CampaignAddItem campaignAddItem = campaignWithStrategy(strategy);
        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);
        checkFirstElementHasError(response, BAD_PARAMS_ERROR_CODE);
    }

    private static CampaignAddItem campaignWithStrategy(CpmBannerCampaignStrategyAdd strategy) {
        CpmBannerCampaignAddItem cpmBannerCampaignAddItem = new CpmBannerCampaignAddItem()
                .withBiddingStrategy(strategy);

        return new CampaignAddItem()
                .withName(NAME)
                .withStartDate(LocalDate.now().toString())
                .withCpmBannerCampaign(cpmBannerCampaignAddItem);
    }

    private void checkFirstElementHasError(AddResponse response, Integer expectedErrorCode) {
        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).hasSize(1);
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(expectedErrorCode);
    }

}
