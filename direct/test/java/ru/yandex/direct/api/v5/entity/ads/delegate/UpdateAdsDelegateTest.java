package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.List;

import com.yandex.direct.api.v5.ads.AdBuilderAdUpdateItem;
import com.yandex.direct.api.v5.ads.AdUpdateItem;
import com.yandex.direct.api.v5.ads.CpmBannerAdBuilderAdUpdate;
import com.yandex.direct.api.v5.ads.ObjectFactory;
import com.yandex.direct.api.v5.ads.SmartAdBuilderAdUpdate;
import com.yandex.direct.api.v5.ads.TextAdUpdate;
import com.yandex.direct.api.v5.ads.UpdateRequest;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.ads.AdsUpdateRequestItem;
import ru.yandex.direct.api.v5.entity.ads.converter.AdsUpdateRequestConverter;
import ru.yandex.direct.api.v5.entity.ads.validation.AdsUpdateRequestValidator;
import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.validTextAdUpdate;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activePerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;

@Api5Test
@RunWith(SpringRunner.class)
public class UpdateAdsDelegateTest {

    private static final ObjectFactory FACTORY = new ObjectFactory();

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupService adGroupService;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private AdsUpdateRequestConverter requestConverter;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private BannersUpdateOperationFactory bannersUpdateOperationFactory;

    @Autowired
    private ResultConverter resultConverter;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Mock
    private ApiAuthenticationSource auth;

    private UpdateAdsDelegate delegate;
    private ClientInfo clientInfo;
    private ClientId clientId;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));

        var requestValidator = new AdsUpdateRequestValidator(auth, adGroupService);
        delegate = new UpdateAdsDelegate(
                auth,
                requestValidator,
                requestConverter,
                resultConverter,
                ppcPropertiesSupport,
                featureService,
                bannersUpdateOperationFactory);
    }

    @Test
    public void updateCmpAd_success() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var creative = defaultCanvas(clientId, null);
        var creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();
        var campaignId = adGroupInfo.getCampaignId();
        var banner = activeCpmBanner(campaignId, adGroupId, creativeId).withStatusShow(true);
        var bannerId = steps.bannerSteps().createActiveCpmBanner(banner, adGroupInfo).getBannerId();
        var creativeIdForUpdate = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();

        var updateItem = new AdBuilderAdUpdateItem().withCreativeId(creativeIdForUpdate);
        var updateAd = new CpmBannerAdBuilderAdUpdate().withCreative(updateItem);
        var adUpdateItem = new AdUpdateItem().withId(bannerId).withCpmBannerAdBuilderAd(updateAd);
        var updateRequest = new UpdateRequest().withAds(adUpdateItem);
        var vr = validate(updateRequest);
        ApiMassResult<Long> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getResult().get(0).getErrors()).isEmpty();
        var banners = bannerTypedRepository.getStrictly(adGroupInfo.getShard(), List.of(bannerId), CpmBanner.class);
        var actualBanner = banners.get(0);
        assertThat(actualBanner.getCreativeId()).isEqualTo(creativeIdForUpdate);
    }

    @Test
    public void updatePerformanceAd_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var creative = defaultPerformanceCreative(clientId, null);
        var creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();
        var campaignId = adGroupInfo.getCampaignId();
        var banner = activePerformanceBanner(campaignId, adGroupId, creativeId).withStatusShow(true);
        var bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();
        var creativeIdForUpdate = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();

        var updateItem = new AdBuilderAdUpdateItem().withCreativeId(creativeIdForUpdate);
        var updateAd = new SmartAdBuilderAdUpdate().withCreative(updateItem);
        var adUpdateItem = new AdUpdateItem().withId(bannerId).withSmartAdBuilderAd(updateAd);
        var updateRequest = new UpdateRequest().withAds(adUpdateItem);
        var vr = validate(updateRequest);
        ApiMassResult<Long> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getResult().get(0).getErrors()).isEmpty();
        var banners = bannerTypedRepository.getStrictly(adGroupInfo.getShard(), List.of(bannerId),
                PerformanceBanner.class);
        var actualBanner = banners.get(0);
        assertThat(actualBanner.getCreativeId()).isEqualTo(creativeIdForUpdate);
    }

    @Test
    public void updateTextAd_deletePermalinkId_success() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var campaignId = adGroupInfo.getCampaignId();
        long permalinkId = RandomUtils.nextLong();
        steps.organizationSteps().createClientOrganization(clientId, permalinkId);
        Long phoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId, permalinkId).getId();

        var banner = activeTextBanner(campaignId, adGroupId).withPermalinkId(permalinkId).withPhoneId(phoneId);
        var bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        TextAdUpdate textAdUpdate = validTextAdUpdate().withBusinessId(FACTORY.createTextAdUpdateBusinessId(null));
        var adUpdateItem = new AdUpdateItem().withId(bannerId).withTextAd(textAdUpdate);
        var updateRequest = new UpdateRequest().withAds(adUpdateItem);
        var vr = validate(updateRequest);
        ApiMassResult<Long> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getResult().get(0).getErrors()).isEmpty();
        var banners = bannerTypedRepository.getStrictly(adGroupInfo.getShard(), List.of(bannerId), TextBanner.class);
        var actualBanner = banners.get(0);
        SoftAssertions.assertSoftly(softly -> {
            assertThat(actualBanner.getPermalinkId()).isNull();
            assertThat(actualBanner.getPhoneId()).isNull();
        });
    }

    @Test
    public void updateTextAd_addTrackingPhone_success() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var campaignId = adGroupInfo.getCampaignId();
        long permalinkId = RandomUtils.nextLong();
        steps.organizationSteps().createClientOrganization(clientId, permalinkId);

        var banner = activeTextBanner(campaignId, adGroupId).withPermalinkId(permalinkId);
        var bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        Long phoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientInfo, permalinkId).getId();
        TextAdUpdate textAdUpdate = validTextAdUpdate()
                .withTrackingPhoneId(FACTORY.createTextAdUpdateTrackingPhoneId(phoneId));
        var adUpdateItem = new AdUpdateItem().withId(bannerId).withTextAd(textAdUpdate);
        var updateRequest = new UpdateRequest().withAds(adUpdateItem);
        var vr = validate(updateRequest);
        ApiMassResult<Long> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getResult().get(0).getErrors()).isEmpty();
        var banners = bannerTypedRepository.getStrictly(adGroupInfo.getShard(), List.of(bannerId), TextBanner.class);
        var actualBanner = banners.get(0);
        SoftAssertions.assertSoftly(softly -> {
            assertThat(actualBanner.getPermalinkId()).isEqualTo(permalinkId);
            assertThat(actualBanner.getPhoneId()).isEqualTo(phoneId);
        });
    }

    @Test
    public void updateTextAd_addTrackingPhone_failure() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var campaignId = adGroupInfo.getCampaignId();
        long permalinkId = RandomUtils.nextLong();
        steps.organizationSteps().createClientOrganization(clientId, permalinkId);

        var banner = activeTextBanner(campaignId, adGroupId).withPermalinkId(permalinkId);
        var bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        organizationsClient.addUidsByPermalinkId(permalinkId, List.of(clientInfo.getUid()));
        TextAdUpdate textAdUpdate = validTextAdUpdate()
                .withTrackingPhoneId(FACTORY.createTextAdUpdateTrackingPhoneId(9999999L));
        var adUpdateItem = new AdUpdateItem().withId(bannerId).withTextAd(textAdUpdate);
        var updateRequest = new UpdateRequest().withAds(adUpdateItem);
        var vr = validate(updateRequest);
        ApiMassResult<Long> apiResult = delegate.processList(vr.getValue());
        List<DefectInfo<DefectType>> errors = apiResult.getResult().get(0).getErrors();
        assertThat(errors).isNotEmpty();
        Assert.assertThat(errors.get(0).getDefect().getCode(), is(8800));
    }

    private ValidationResult<List<AdsUpdateRequestItem<BannerWithSystemFields>>, DefectType> validate(
            UpdateRequest updateRequest) {
        var request = delegate.convertRequest(updateRequest);
        return delegate.validateInternalRequest(request);
    }
}
