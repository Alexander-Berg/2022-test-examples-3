package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdUpdateItem;
import com.yandex.direct.api.v5.ads.ContentPromotionServiceAdUpdate;
import com.yandex.direct.api.v5.ads.UpdateRequest;
import com.yandex.direct.api.v5.ads.UpdateResponse;
import org.assertj.core.api.SoftAssertions;
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
import ru.yandex.direct.api.v5.entity.ads.converter.AdsUpdateRequestConverter;
import ru.yandex.direct.api.v5.entity.ads.validation.AdsUpdateRequestValidator;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.banner.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.validation.DefectTypes.invalidValue;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCommonData.defaultContentPromotion;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class UpdateAdsDelegateContentPromotionServiceTest {
    private static final String TITLE = "Services ad";

    @Autowired
    private AdsUpdateRequestConverter adsUpdateRequestConverter;
    @Autowired
    private AdGroupService adGroupService;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private Steps steps;
    @Autowired
    private BannersUpdateOperationFactory bannersUpdateOperationFactory;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private TestContentPromotionBanners testContentPromotionBanners;

    private GenericApiService genericApiService;
    private UpdateAdsDelegate delegate;
    private ApiAuthenticationSource auth;

    private int shard;
    private long campaignId;
    private long adGroupId;
    private long contentId;
    private long otherContentId;
    private ContentPromotionBannerInfo bannerInfo;
    private long bannerId;

    @Before
    public void before() {
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(
                apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));

        AdsUpdateRequestValidator adsUpdateRequestValidator = new AdsUpdateRequestValidator(auth, adGroupService);
        delegate = new UpdateAdsDelegate(auth, adsUpdateRequestValidator,
                adsUpdateRequestConverter, resultConverter, mock(PpcPropertiesSupport.class),
                mock(FeatureService.class), bannersUpdateOperationFactory);

        var campaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
        var adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(campaignInfo, ContentPromotionAdgroupType.SERVICE);
        adGroupId = adGroupInfo.getAdGroupId();
        var content = steps.contentPromotionSteps()
                .createContentPromotionContent(clientId,
                        defaultContentPromotion(clientId, ContentPromotionContentType.SERVICE)
                                .withExternalId("INITIAL_EXTERNAL_ID"));
        bannerInfo = steps.contentPromotionBannerSteps()
                .createBanner(adGroupInfo, content, testContentPromotionBanners.fullContentPromoServiceBanner(null, null));
        bannerId = bannerInfo.getBannerId();

        contentId = steps.contentPromotionSteps()
                .createContentPromotionContent(clientId, ContentPromotionContentType.SERVICE)
                .getId();
        otherContentId = steps.contentPromotionSteps().createContentPromotionContent(clientId,
                ContentPromotionContentType.COLLECTION)
                .getId();
    }

    @Test
    public void doAction_ServicesApp_AdUpdated() {
        servicesApplication();
        UpdateRequest request = new UpdateRequest()
                .withAds(new AdUpdateItem()
                        .withId(bannerId)
                        .withContentPromotionServiceAd(new ContentPromotionServiceAdUpdate()
                                .withTitle(TITLE)
                                .withPromotedContentId(contentId)));
        UpdateResponse response = genericApiService.doAction(delegate, request);

        assumeThat(response.getUpdateResults(), hasSize(1));

        long bannerId = response.getUpdateResults().get(0).getId();
        var banners = bannerTypedRepository
                .getStrictlyFullyFilled(shard, List.of(bannerId), ContentPromotionBanner.class);
        var expectedBanner = new ContentPromotionBanner()
                .withId(bannerId)
                .withAdGroupId(adGroupId)
                .withCampaignId(campaignId)
                .withTitle(TITLE)
                .withContentPromotionId(contentId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(banners).hasSize(1);
            softly.assertThat(banners.get(0)).isEqualToIgnoringNullFields(expectedBanner);
        });
    }

    @Test(expected = ApiValidationException.class)
    public void doAction_NotServicesApp_ExceptionIsThrown() {
        UpdateRequest request = new UpdateRequest()
                .withAds(new AdUpdateItem()
                        .withId(bannerId)
                        .withContentPromotionServiceAd(new ContentPromotionServiceAdUpdate()
                                .withTitle(TITLE)
                                .withPromotedContentId(contentId)));
        UpdateResponse response = genericApiService.doAction(delegate, request);
    }

    @Test
    public void doAction_ServicesApp_NoTitle_AdUpdated() {
        servicesApplication();
        UpdateRequest request = new UpdateRequest()
                .withAds(new AdUpdateItem()
                        .withId(bannerId)
                        .withContentPromotionServiceAd(new ContentPromotionServiceAdUpdate()
                                .withPromotedContentId(contentId)));
        UpdateResponse response = genericApiService.doAction(delegate, request);

        assumeThat(response.getUpdateResults(), hasSize(1));

        long bannerId = response.getUpdateResults().get(0).getId();
        var banners = bannerTypedRepository.getStrictlyFullyFilled(shard, List.of(bannerId),
                ContentPromotionBanner.class);
        var expectedBanner = new ContentPromotionBanner()
                .withId(bannerId)
                .withAdGroupId(adGroupId)
                .withCampaignId(campaignId)
                .withTitle(bannerInfo.getBanner().getTitle())
                .withContentPromotionId(contentId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(banners).hasSize(1);
            softly.assertThat(banners.get(0)).isEqualToIgnoringNullFields(expectedBanner);
        });
    }

    @Test
    public void doAction_ServicesApp_NoPromotedContent_AdUpdated() {
        servicesApplication();
        UpdateRequest request = new UpdateRequest()
                .withAds(new AdUpdateItem()
                        .withId(bannerId)
                        .withContentPromotionServiceAd(new ContentPromotionServiceAdUpdate()
                                .withTitle(TITLE)));
        UpdateResponse response = genericApiService.doAction(delegate, request);

        assumeThat(response.getUpdateResults(), hasSize(1));

        long bannerId = response.getUpdateResults().get(0).getId();
        var banners = bannerTypedRepository.getStrictlyFullyFilled(shard, List.of(bannerId),
                ContentPromotionBanner.class);
        var expectedBanner = new ContentPromotionBanner()
                .withId(bannerId)
                .withAdGroupId(adGroupId)
                .withCampaignId(campaignId)
                .withTitle(TITLE)
                .withContentPromotionId(bannerInfo.getBanner().getContentPromotionId());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(banners).hasSize(1);
            softly.assertThat(banners.get(0)).isEqualToIgnoringNullFields(expectedBanner);
        });
    }

    @Test
    public void doAction_ServicesApp_NotServicePromotedContent_AdNotUpdated() {
        servicesApplication();
        UpdateRequest request = new UpdateRequest()
                .withAds(new AdUpdateItem()
                        .withId(bannerId)
                        .withContentPromotionServiceAd(new ContentPromotionServiceAdUpdate()
                                .withTitle(TITLE)
                                .withPromotedContentId(otherContentId)));
        UpdateResponse response = genericApiService.doAction(delegate, request);

        assumeThat(response.getUpdateResults(), hasSize(1));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getUpdateResults()).hasSize(1);
            softly.assertThat(response.getUpdateResults().get(0).getId()).isNull();
            softly.assertThat(response.getUpdateResults().get(0).getErrors()).hasSize(1);
            softly.assertThat(response.getUpdateResults().get(0).getErrors().get(0).getCode())
                    .isEqualTo(invalidValue().getCode());
        });
    }

    private void servicesApplication() {
        when(auth.isServicesApplication()).thenReturn(true);
    }
}
