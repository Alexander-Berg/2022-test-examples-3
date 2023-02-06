package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdAddItem;
import com.yandex.direct.api.v5.ads.AddRequest;
import com.yandex.direct.api.v5.ads.AddResponse;
import com.yandex.direct.api.v5.ads.ContentPromotionServiceAdAdd;
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
import ru.yandex.direct.api.v5.entity.ads.converter.AdsAddRequestConverter;
import ru.yandex.direct.api.v5.entity.ads.validation.AdsAddRequestValidator;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.validation.DefectTypes.invalidValue;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class AddAdsDelegateContentPromotionServiceTest {
    private static final String TITLE = "Services ad";

    @Autowired
    private BannersAddOperationFactory bannersAddOperationFactory;
    @Autowired
    private AdsAddRequestConverter adsAddRequestConverter;
    @Autowired
    private AdsAddRequestValidator adsAddRequestValidator;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private Steps steps;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    private GenericApiService genericApiService;
    private AddAdsDelegate delegate;
    private ApiAuthenticationSource auth;

    private int shard;
    private long campaignId;
    private long adGroupId;
    private long contentId;
    private long otherContentId;

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

        delegate = new AddAdsDelegate(auth, adsAddRequestValidator, adsAddRequestConverter,
                resultConverter, mock(PpcPropertiesSupport.class), mock(FeatureService.class),
                bannersAddOperationFactory);

        var campaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
        var adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(campaignInfo, ContentPromotionAdgroupType.SERVICE);
        adGroupId = adGroupInfo.getAdGroupId();

        contentId = steps.contentPromotionSteps()
                .createContentPromotionContent(clientId, ContentPromotionContentType.SERVICE)
                .getId();
        otherContentId = steps.contentPromotionSteps().createContentPromotionContent(clientId,
                ContentPromotionContentType.COLLECTION)
                .getId();
    }

    @Test
    public void doAction_ServicesApp_AdCreated() {
        servicesApplication();
        AddRequest request = new AddRequest()
                .withAds(new AdAddItem()
                        .withAdGroupId(adGroupId)
                        .withContentPromotionServiceAd(new ContentPromotionServiceAdAdd()
                                .withTitle(TITLE)
                                .withPromotedContentId(contentId)));
        AddResponse response = genericApiService.doAction(delegate, request);

        assumeThat(response.getAddResults(), hasSize(1));

        long bannerId = response.getAddResults().get(0).getId();
        var banners = bannerTypedRepository.getStrictlyFullyFilled(shard, List.of(bannerId),
                ContentPromotionBanner.class);
        ContentPromotionBanner expectedBanner = new ContentPromotionBanner()
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
        AddRequest request = new AddRequest()
                .withAds(new AdAddItem()
                        .withAdGroupId(adGroupId)
                        .withContentPromotionServiceAd(new ContentPromotionServiceAdAdd()
                                .withTitle(TITLE)
                                .withPromotedContentId(contentId)));
        AddResponse response = genericApiService.doAction(delegate, request);
    }

    @Test
    public void doAction_ServicesApp_NoTitle_AdNotCreated() {
        servicesApplication();
        AddRequest request = new AddRequest()
                .withAds(new AdAddItem()
                        .withAdGroupId(adGroupId)
                        .withContentPromotionServiceAd(new ContentPromotionServiceAdAdd()
                                .withPromotedContentId(contentId)));
        AddResponse response = genericApiService.doAction(delegate, request);

        assumeThat(response.getAddResults(), hasSize(1));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAddResults()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getId()).isNull();
            softly.assertThat(response.getAddResults().get(0).getErrors()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getErrors().get(0).getCode())
                    .isEqualTo(invalidValue().getCode());
        });
    }

    @Test
    public void doAction_ServicesApp_NoPromotedContent_AdNotCreated() {
        servicesApplication();
        AddRequest request = new AddRequest()
                .withAds(new AdAddItem()
                        .withAdGroupId(adGroupId)
                        .withContentPromotionServiceAd(new ContentPromotionServiceAdAdd()
                                .withTitle(TITLE)));
        AddResponse response = genericApiService.doAction(delegate, request);

        assumeThat(response.getAddResults(), hasSize(1));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAddResults()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getId()).isNull();
            softly.assertThat(response.getAddResults().get(0).getErrors()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getErrors().get(0).getCode())
                    .isEqualTo(invalidValue().getCode());
        });
    }

    @Test
    public void doAction_ServicesApp_NotServicePromotedContent_AdNotCreated() {
        servicesApplication();
        AddRequest request = new AddRequest()
                .withAds(new AdAddItem()
                        .withAdGroupId(adGroupId)
                        .withContentPromotionServiceAd(new ContentPromotionServiceAdAdd()
                                .withTitle(TITLE)
                                .withPromotedContentId(otherContentId)));
        AddResponse response = genericApiService.doAction(delegate, request);

        assumeThat(response.getAddResults(), hasSize(1));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAddResults()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getId()).isNull();
            softly.assertThat(response.getAddResults().get(0).getErrors()).hasSize(1);
            softly.assertThat(response.getAddResults().get(0).getErrors().get(0).getCode())
                    .isEqualTo(invalidValue().getCode());
        });
    }

    private void servicesApplication() {
        when(auth.isServicesApplication()).thenReturn(true);
    }
}
