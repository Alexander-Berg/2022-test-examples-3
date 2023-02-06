package ru.yandex.direct.api.v5.entity.campaigns.delegate;

import java.time.LocalDate;
import java.util.List;

import com.yandex.direct.api.v5.campaigns.AddRequest;
import com.yandex.direct.api.v5.campaigns.AddResponse;
import com.yandex.direct.api.v5.campaigns.CampaignAddItem;
import com.yandex.direct.api.v5.campaigns.StrategyAverageCpcAdd;
import com.yandex.direct.api.v5.campaigns.StrategyMaximumClicksAdd;
import com.yandex.direct.api.v5.campaigns.StrategyNetworkDefaultAdd;
import com.yandex.direct.api.v5.campaigns.TextCampaignAddItem;
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.TextCampaignStrategyAdd;
import com.yandex.direct.api.v5.general.ActionResult;
import com.yandex.direct.api.v5.general.ExceptionNotification;
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
@RunWith(SpringRunner.class)
public class AddTextCampaignStrategyNegativeTest {

    private static final String NAME = "Тестовая кампания";
    private static final Integer BAD_PARAMS_ERROR_CODE = 4000;
    private static final Integer NOT_SUPPORTED_ERROR_CODE = 3500;

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
    public void addTextCampaign_failure_whenSearchStrategyHasTwoStructuresWithSettings() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS)
                        .withWbMaximumClicks(new StrategyMaximumClicksAdd()
                                .withWeeklySpendLimit(400_000_000L))
                        .withAverageCpc(new StrategyAverageCpcAdd()
                                .withAverageCpc(5_000_000L)))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        AddResponse response = addTextCampaignWithStrategy(strategy);
        checkFirstElementHasError(response, BAD_PARAMS_ERROR_CODE);
    }

    @Test
    public void addTextCampaign_failure_whenSearchSettingsStructureDoesNotMatchStrategyType() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS)
                        .withAverageCpc(new StrategyAverageCpcAdd()
                                .withAverageCpc(5_000_000L)))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        AddResponse response = addTextCampaignWithStrategy(strategy);
        checkFirstElementHasError(response, BAD_PARAMS_ERROR_CODE);
    }

    @Test
    public void addTextCampaign_failure_whenSearchStrategyDoesNotContainStructureWithSettings() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        AddResponse response = addTextCampaignWithStrategy(strategy);
        checkFirstElementHasError(response, BAD_PARAMS_ERROR_CODE);
    }

    @Test
    public void addTextCampaign_failure_whenNetworkSettingsStructureDoesNotMatchStrategyType() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.SERVING_OFF))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.WB_MAXIMUM_CLICKS)
                        .withAverageCpc(new StrategyAverageCpcAdd()
                                .withAverageCpc(5_000_000L)));

        AddResponse response = addTextCampaignWithStrategy(strategy);
        checkFirstElementHasError(response, BAD_PARAMS_ERROR_CODE);
    }

    @Test
    public void addTextCampaign_onlyOneFailure_whenSecondCampaignIsValid() {
        var invalidStrategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        var validStrategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        CampaignAddItem invalidCampaign = campaignWithStrategy(invalidStrategy);
        CampaignAddItem validCampaign = campaignWithStrategy(validStrategy);

        AddRequest request = new AddRequest().withCampaigns(invalidCampaign, validCampaign);
        List<ActionResult> results = genericApiService.doAction(delegate, request).getAddResults();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(results.get(0).getErrors()).isNotEmpty();
            softly.assertThat(results.get(1).getErrors()).isEmpty();
            softly.assertThat(results.get(1).getId()).isNotNull();
        });
    }

    @Test
    public void addTextCampaign_failure_whenSearchTypeIsImpressionsBelowSearch() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.IMPRESSIONS_BELOW_SEARCH))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        AddResponse response = addTextCampaignWithStrategy(strategy);
        checkFirstElementHasError(response, NOT_SUPPORTED_ERROR_CODE);
    }

    @Test
    public void addTextCampaign_failure_whenSearchServingOffAndNetworkDefault() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.SERVING_OFF))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT)
                        .withNetworkDefault(new StrategyNetworkDefaultAdd()));

        AddResponse response = addTextCampaignWithStrategy(strategy);
        checkFirstElementHasError(response, BAD_PARAMS_ERROR_CODE);
    }

    @Test
    public void addTextCampaign_failure_whenSearchTypeIsUnknown() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.UNKNOWN))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.AVERAGE_CPC)
                        .withAverageCpc(new StrategyAverageCpcAdd()
                                .withAverageCpc(5_000_000L)));

        AddResponse response = addTextCampaignWithStrategy(strategy);
        checkFirstElementHasError(response, BAD_PARAMS_ERROR_CODE);
    }

    @Test
    public void addTextCampaign_failure_whenNetworkTypeIsUnknown() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.AVERAGE_CPC)
                        .withAverageCpc(new StrategyAverageCpcAdd()
                                .withAverageCpc(5_000_000L)))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.UNKNOWN));

        AddResponse response = addTextCampaignWithStrategy(strategy);
        checkFirstElementHasError(response, BAD_PARAMS_ERROR_CODE);
    }

    private AddResponse addTextCampaignWithStrategy(TextCampaignStrategyAdd strategy) {
        CampaignAddItem campaignAddItem = campaignWithStrategy(strategy);
        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        return genericApiService.doAction(delegate, request);
    }

    private static CampaignAddItem campaignWithStrategy(TextCampaignStrategyAdd strategy) {
        TextCampaignAddItem textCampaignAddItem = new TextCampaignAddItem()
                .withBiddingStrategy(strategy);

        return new CampaignAddItem()
                .withName(NAME)
                .withStartDate(LocalDate.now().toString())
                .withTextCampaign(textCampaignAddItem);
    }

    private void checkFirstElementHasError(AddResponse response, Integer expectedErrorCode) {
        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).hasSize(1);
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(expectedErrorCode);
    }
}
