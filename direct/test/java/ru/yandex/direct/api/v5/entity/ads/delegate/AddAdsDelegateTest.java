package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.List;

import com.yandex.direct.api.v5.ads.AdAddItem;
import com.yandex.direct.api.v5.ads.AdBuilderAdAddItem;
import com.yandex.direct.api.v5.ads.AddRequest;
import com.yandex.direct.api.v5.ads.AddResponse;
import com.yandex.direct.api.v5.ads.CpmVideoAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.SmartAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.TextAdAdd;
import com.yandex.direct.api.v5.general.ExceptionNotification;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.ads.converter.AdsAddRequestConverter;
import ru.yandex.direct.api.v5.entity.ads.validation.AdsAddRequestValidator;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.regions.Region;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultPerformanceAdGroup;

@Api5Test
@RunWith(SpringRunner.class)
public class AddAdsDelegateTest {
    @Autowired
    private Steps steps;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private BannersAddOperationFactory bannersAddOperationFactory;
    @Autowired
    private AdsAddRequestValidator adsAddRequestValidator;
    @Autowired
    private AdsAddRequestConverter requestConverter;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private OrganizationsClientStub organizationsClient;

    private GenericApiService genericApiService;
    private AddAdsDelegate delegate;

    private ClientInfo clientInfo;
    private ClientId clientId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();

        ApiUser user = new ApiUser()
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId());
        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(user);
        when(auth.getChiefSubclient()).thenReturn(user);
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));
        delegate = new AddAdsDelegate(
                auth,
                adsAddRequestValidator,
                requestConverter,
                resultConverter,
                ppcPropertiesSupport,
                featureService,
                bannersAddOperationFactory);
    }

    @Test
    public void addPerformanceAd_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var creative = defaultPerformanceCreative(clientInfo.getClientId(), null);
        var creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();

        AddRequest request = getAddPerfomanceAdRequest(adGroupId, creativeId);
        AddResponse response = genericApiService.doAction(delegate, request);
        checkState(response.getAddResults().get(0).getErrors().isEmpty(), "Unexpected error");

        var bannerId = response.getAddResults().get(0).getId();
        var banners = bannerTypedRepository.getStrictlyFullyFilled(adGroupInfo.getShard(), List.of(bannerId),
                PerformanceBanner.class);
        var actualBanner = (PerformanceBanner) banners.get(0);
        assertThat(actualBanner.getCreativeId()).isEqualTo(creativeId);
    }

    @Test
    public void addPerformanceAd_whenCreativeHasMatchedGeo_success() {
        var campaignInfo = steps.campaignSteps().createActivePerformanceCampaignWithStrategy(clientInfo);
        var feedInfo = steps.feedSteps().createDefaultFeed(campaignInfo.getClientInfo());
        PerformanceAdGroup adGroup = defaultPerformanceAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId())
                .withGeo(singletonList(Region.KYIV_REGION_ID));
        PerformanceAdGroupInfo adGroupInfo = new PerformanceAdGroupInfo()
                .withClientInfo(clientInfo)
                .withFeedInfo(feedInfo)
                .withAdGroup(adGroup)
                .withCampaignInfo(campaignInfo);
        var adGroupId = steps.adGroupSteps().addPerformanceAdGroup(adGroupInfo).getAdGroupId();
        var creative = defaultPerformanceCreative(clientInfo.getClientId(), null)
                .withStatusModerate(StatusModerate.YES)
                .withSumGeo(singletonList(Region.UKRAINE_REGION_ID));
        var creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();

        AddRequest request = getAddPerfomanceAdRequest(adGroupId, creativeId);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isEmpty();
    }

    @Test
    public void addPerformanceAd_whenCreativeHasUnmatchedGeo_failure() {
        var campaignInfo = steps.campaignSteps().createActivePerformanceCampaignWithStrategy(clientInfo);
        var feedInfo = steps.feedSteps().createDefaultFeed(campaignInfo.getClientInfo());
        PerformanceAdGroup adGroup = defaultPerformanceAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId())
                .withGeo(singletonList(Region.MOSCOW_REGION_ID));
        PerformanceAdGroupInfo adGroupInfo = new PerformanceAdGroupInfo()
                .withClientInfo(clientInfo)
                .withFeedInfo(feedInfo)
                .withAdGroup(adGroup)
                .withCampaignInfo(campaignInfo);
        var adGroupId = steps.adGroupSteps().addPerformanceAdGroup(adGroupInfo).getAdGroupId();
        var creative = defaultPerformanceCreative(clientInfo.getClientId(), null)
                .withStatusModerate(StatusModerate.YES)
                .withSumGeo(singletonList(Region.UKRAINE_REGION_ID));
        var creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();

        AddRequest request = getAddPerfomanceAdRequest(adGroupId, creativeId);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
    }

    @Test
    public void addPerformanceAd_toNoCreativesAdGroup_failure() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        steps.performanceMainBannerSteps().createPerformanceMainBanner(adGroupInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var creative = defaultPerformanceCreative(clientInfo.getClientId(), null);
        var creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();

        AddRequest request = getAddPerfomanceAdRequest(adGroupId, creativeId);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
    }

    @Test
    public void addTextAd_withTrackingPhone_success() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        long permalinkId = RandomUtils.nextLong();
        steps.organizationSteps().createClientOrganization(clientId, permalinkId);

        Long phoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientInfo, permalinkId).getId();

        AddRequest request = getAddTextAdRequest(adGroupId, permalinkId, phoneId);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isEmpty();

        Long bannerId = response.getAddResults().get(0).getId();

        var banners = bannerTypedRepository.getStrictly(adGroupInfo.getShard(), List.of(bannerId), TextBanner.class);
        var actualBanner = banners.get(0);
        SoftAssertions.assertSoftly(softly -> {
            assertThat(actualBanner.getPermalinkId()).isEqualTo(permalinkId);
            assertThat(actualBanner.getPhoneId()).isEqualTo(phoneId);
        });
    }

    @Test
    public void addTextAd_withTrackingPhone_failure() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        long permalinkId = RandomUtils.nextLong();
        steps.organizationSteps().createClientOrganization(clientId, permalinkId);
        organizationsClient.addUidsByPermalinkId(permalinkId, List.of(clientInfo.getUid()));

        AddRequest request = getAddTextAdRequest(adGroupId, permalinkId, 99999999999L);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        Assert.assertThat(returnedErrors.get(0).getCode(), is(8800));
    }

    @Test
    public void addNonSkippableCpmVideoAdIntoSkippableAdGroup_failure() {
        var campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroup(campaignInfo);
        var adGroupId = adGroupInfo.getAdGroupId();

        var creativeId = steps.creativeSteps().getNextCreativeId();
        var creative = steps.creativeSteps().addDefaultNonSkippableCreative(clientInfo, creativeId);

        AddRequest request = getAddCpmVideoAdRequest(adGroupId, creativeId);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        Assert.assertThat(returnedErrors.get(0).getCode(), is(5005));
    }

    @Test
    public void addSkippableCpmVideoAdIntoNonSkippableAdGroup_failure() {
        var campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveNonSkippableCpmVideoAdGroup(campaignInfo);
        var adGroupId = adGroupInfo.getAdGroupId();

        var creativeId = steps.creativeSteps().getNextCreativeId();
        var creative = steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, creativeId);

        AddRequest request = getAddCpmVideoAdRequest(adGroupId, creativeId);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        Assert.assertThat(returnedErrors.get(0).getCode(), is(5005));
    }

    @Test
    public void addNonSkippableCpmVideoAd_success() {
        var campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveNonSkippableCpmVideoAdGroup(campaignInfo);
        var adGroupId = adGroupInfo.getAdGroupId();

        var creativeId = steps.creativeSteps().getNextCreativeId();
        var creative = steps.creativeSteps().addDefaultNonSkippableCreative(clientInfo, creativeId);

        AddRequest request = getAddCpmVideoAdRequest(adGroupId, creativeId);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isEmpty();
    }

    private AddRequest getAddTextAdRequest(Long adGroupId, Long permalinkId, Long trackingPhoneId) {
        var addAd = new TextAdAdd()
                .withTitle("Title")
                .withText("Text")
                .withMobile(YesNoEnum.NO)
                .withBusinessId(permalinkId)
                .withTrackingPhoneId(trackingPhoneId);
        var adAddItem = new AdAddItem()
                .withAdGroupId(adGroupId)
                .withTextAd(addAd);
        return new AddRequest().withAds(adAddItem);
    }

    private AddRequest getAddCpmVideoAdRequest(Long adGroupId, long creativeId) {
        var addAd = new CpmVideoAdBuilderAdAdd()
                .withHref("https://www.ya.ru")
                .withCreative(new AdBuilderAdAddItem().withCreativeId(creativeId));
        var adAddItem = new AdAddItem()
                .withAdGroupId(adGroupId)
                .withCpmVideoAdBuilderAd(addAd);
        return new AddRequest().withAds(adAddItem);
    }

    private AddRequest getAddPerfomanceAdRequest(Long adGroupId, Long creativeId) {
        var addItem = new AdBuilderAdAddItem().withCreativeId(creativeId);
        var addAd = new SmartAdBuilderAdAdd().withCreative(addItem);
        var adAddItem = new AdAddItem()
                .withAdGroupId(adGroupId)
                .withSmartAdBuilderAd(addAd);
        return new AddRequest().withAds(adAddItem);
    }

}
