package ru.yandex.direct.api.v5.entity.campaigns.delegate;

import java.time.LocalDate;
import java.util.List;

import com.yandex.direct.api.v5.campaigns.AddRequest;
import com.yandex.direct.api.v5.campaigns.AddResponse;
import com.yandex.direct.api.v5.campaigns.CampaignAddItem;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignAddItem;
import com.yandex.direct.api.v5.campaigns.TextCampaignAddItem;
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.TextCampaignSetting;
import com.yandex.direct.api.v5.campaigns.TextCampaignSettingsEnum;
import com.yandex.direct.api.v5.campaigns.TextCampaignStrategyAdd;
import com.yandex.direct.api.v5.general.ArrayOfString;
import com.yandex.direct.api.v5.general.ExceptionNotification;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.ApiValidationException;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.campaigns.converter.CampaignsAddRequestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
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
public class AddCampaignsDelegateTest {

    private static final String NAME = "Тестовая кампания";

    @Autowired
    private Steps steps;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
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

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().setCurrentClient(clientInfo.getClientId());

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
    public void addTextCampaign_success() {
        AddRequest request = getAddTextCampaignRequest(NAME);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        TextCampaign actualCampaign = (TextCampaign) campaigns.get(campaignId);
        assertThat(actualCampaign).isNotNull();
        assertThat(actualCampaign.getName()).isEqualTo(NAME);
    }

    @Test
    public void addTextCampaign_failure() {
        String invalidName = "";

        AddRequest request = getAddTextCampaignRequest(invalidName);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(5003);
    }

    @Test
    public void addCampaign_campaignAddItemWithNoCampaigns_ValidationError() {
        var campaignAddItem = newCampaignAddItem();

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(5008);
    }

    @Test
    public void addCampaign_campaignAddItemWithTwoCampaigns_ValidationError() {
        var campaignAddItem = newCampaignAddItem()
                .withTextCampaign(defaultTextCampaignAddItem())
                .withCpmBannerCampaign(new CpmBannerCampaignAddItem());

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(5009);
    }

    @Test
    public void addCampaign_BlockedIpsContainsBlankLine_ValidationError() {
        var campaignAddItem = newCampaignAddItem()
                .withTextCampaign(defaultTextCampaignAddItem())
                .withBlockedIps(new ArrayOfString().withItems(" "));

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(5004);
    }

    @Test
    public void addCampaign_ExcludedSitesContainsBlankLine_ValidationError() {
        CampaignAddItem campaignAddItem = newCampaignAddItem()
                .withTextCampaign(defaultTextCampaignAddItem())
                .withExcludedSites(new ArrayOfString().withItems(" "));

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(5006);
    }

    private static CampaignAddItem newCampaignAddItem() {
        return new CampaignAddItem()
                .withName(NAME)
                .withStartDate(LocalDate.now().toString());
    }

    @Test(expected = ApiValidationException.class)
    public void addCampaign_TooManyCampaigns_Exception() {
        var campaignAddItem = newCampaignAddItem();

        AddRequest request = new AddRequest().withCampaigns(
                campaignAddItem,
                campaignAddItem,
                campaignAddItem,
                campaignAddItem,
                campaignAddItem,
                campaignAddItem,
                campaignAddItem,
                campaignAddItem,
                campaignAddItem,
                campaignAddItem,
                campaignAddItem);
        genericApiService.doAction(delegate, request);
    }

    private static AddRequest getAddTextCampaignRequest(String name) {
        TextCampaignAddItem textCampaignAddItem = defaultTextCampaignAddItem();
        return getAddRequest(name, textCampaignAddItem);
    }

    private static AddRequest getAddRequest(String name, TextCampaignAddItem textCampaignAddItem) {
        var campaignAddItem = new CampaignAddItem()
                .withName(name)
                .withStartDate(LocalDate.now().toString())
                .withTextCampaign(textCampaignAddItem);

        return new AddRequest().withCampaigns(campaignAddItem);
    }

    private static TextCampaignAddItem defaultTextCampaignAddItem() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.MAXIMUM_COVERAGE));

        return new TextCampaignAddItem()
                .withBiddingStrategy(strategy);
    }

    private static TextCampaignSetting textCampaignSetting(TextCampaignSettingsEnum option, YesNoEnum value) {
        return new TextCampaignSetting().withOption(option).withValue(value);
    }
}
