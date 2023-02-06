package ru.yandex.direct.api.v5.entity.ads.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdFieldEnum;
import com.yandex.direct.api.v5.ads.AdTypeEnum;
import com.yandex.direct.api.v5.ads.AdsSelectionCriteria;
import com.yandex.direct.api.v5.ads.GetRequest;
import com.yandex.direct.api.v5.ads.GetResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.entity.ApiValidationException;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.ads.converter.GetResponseConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.banner.type.creative.BannerCreativeRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository;
import ru.yandex.direct.core.entity.moderationreason.service.ModerationReasonService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class GetAdsDelegateContentPromotionServiceTest {

    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private BannerService bannerService;
    @Autowired
    private BannerCommonRepository bannerRepository;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private GetResponseConverter getResponseConverter;
    @Autowired
    private AdGroupService adGroupService;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private Steps steps;

    private GenericApiService genericApiService;
    private GetAdsDelegate delegate;
    private ApiAuthenticationSource auth;

    private long campaignId;
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

        auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));

        delegate = new GetAdsDelegate(auth, shardHelper, bannerService, bannerRepository, campaignService,
                mock(CalloutRepository.class), mock(BannerCreativeRepository.class), mock(CreativeRepository.class),
                mock(ModerationReasonService.class), mock(MobileContentRepository.class), getResponseConverter,
                adGroupService, adGroupRepository);

        ContentPromotionCampaignInfo campaignInfo =
                steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
        var adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(campaignInfo, ContentPromotionAdgroupType.SERVICE);
        var bannerInfo = steps.contentPromotionBannerSteps()
                .createDefaultBanner(adGroupInfo, ContentPromotionContentType.SERVICE);
        bannerId = bannerInfo.getBannerId();
    }

    @Test
    public void doAction_ServicesApp_SelectById_ContentReturned() {
        servicesApplication();
        GetRequest request = new GetRequest()
                .withFieldNames(AdFieldEnum.ID, AdFieldEnum.TYPE)
                .withSelectionCriteria(new AdsSelectionCriteria().withIds(bannerId));
        GetResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAds()).hasSize(1);
            softly.assertThat(response.getAds().get(0).getId()).isEqualTo(bannerId);
            softly.assertThat(response.getAds().get(0).getType()).isEqualTo(AdTypeEnum.CONTENT_PROMOTION_SERVICE_AD);
        });
    }

    @Test
    public void doAction_NotServicesApp_SelectById_ContentNotReturned() {
        GetRequest request = new GetRequest()
                .withFieldNames(AdFieldEnum.ID, AdFieldEnum.TYPE)
                .withSelectionCriteria(new AdsSelectionCriteria().withIds(bannerId));
        GetResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAds()).isEmpty();
    }

    @Test
    public void doAction_ServicesApp_SelectByCampaignId_ContentReturned() {
        servicesApplication();
        GetRequest request = new GetRequest()
                .withFieldNames(AdFieldEnum.ID, AdFieldEnum.TYPE)
                .withSelectionCriteria(new AdsSelectionCriteria()
                        .withCampaignIds(campaignId));
        GetResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAds()).hasSize(1);
            softly.assertThat(response.getAds().get(0).getId()).isEqualTo(bannerId);
            softly.assertThat(response.getAds().get(0).getType()).isEqualTo(AdTypeEnum.CONTENT_PROMOTION_SERVICE_AD);
        });
    }

    @Test
    public void doAction_NotServicesApp_SelectByCampaignId_ContentNotReturned() {
        GetRequest request = new GetRequest()
                .withFieldNames(AdFieldEnum.ID, AdFieldEnum.TYPE)
                .withSelectionCriteria(new AdsSelectionCriteria()
                        .withCampaignIds(campaignId));
        GetResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAds()).isEmpty();
    }

    @Test
    public void doAction_ServicesApp_SelectByContentPromotionType_ContentReturned() {
        servicesApplication();
        GetRequest request = new GetRequest()
                .withFieldNames(AdFieldEnum.ID, AdFieldEnum.TYPE)
                .withSelectionCriteria(new AdsSelectionCriteria()
                        .withCampaignIds(campaignId)
                        .withTypes(AdTypeEnum.CONTENT_PROMOTION_SERVICE_AD));
        GetResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getAds()).hasSize(1);
            softly.assertThat(response.getAds().get(0).getId()).isEqualTo(bannerId);
            softly.assertThat(response.getAds().get(0).getType()).isEqualTo(AdTypeEnum.CONTENT_PROMOTION_SERVICE_AD);
        });
    }

    @Test(expected = ApiValidationException.class)
    public void doAction_NotServicesApp_SelectByContentPromotionType_ExceptionIsThrown() {
        GetRequest request = new GetRequest()
                .withFieldNames(AdFieldEnum.ID, AdFieldEnum.TYPE)
                .withSelectionCriteria(new AdsSelectionCriteria()
                        .withCampaignIds(campaignId)
                        .withTypes(AdTypeEnum.CONTENT_PROMOTION_SERVICE_AD));
        GetResponse response = genericApiService.doAction(delegate, request);
    }

    private void servicesApplication() {
        when(auth.isServicesApplication()).thenReturn(true);
    }
}
