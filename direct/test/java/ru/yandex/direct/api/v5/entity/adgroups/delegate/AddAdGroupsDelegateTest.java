package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.yandex.direct.api.v5.adgroups.AdGroupAddItem;
import com.yandex.direct.api.v5.adgroups.AddRequest;
import com.yandex.direct.api.v5.adgroups.ContentPromotionAdGroupAdd;
import com.yandex.direct.api.v5.adgroups.CpmVideoAdGroupAdd;
import com.yandex.direct.api.v5.adgroups.PromotedContentTypeEnum;
import com.yandex.direct.api.v5.adgroups.SmartAdGroupAdd;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.adgroups.container.AdGroupsContainer;
import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.result.ApiResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class AddAdGroupsDelegateTest {

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupService adGroupService;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private ApiAuthenticationSource auth;

    @Autowired
    private AddAdGroupsDelegate delegate;

    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();

        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
    }

    @After
    public void resetMocks() {
        Mockito.reset(auth);
    }

    @Test
    public void processList_performanceAdGroup() {
        var feed = steps.feedSteps().createDefaultFeed(clientInfo);
        var smartAdGroup = new SmartAdGroupAdd().withFeedId(feed.getFeedId());
        var campaign = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
        Long campaignId = campaign.getCampaignId();
        var adGroupAddItem = new AdGroupAddItem()
                .withName("name")
                .withRegionIds(225L)
                .withCampaignId(campaignId)
                .withSmartAdGroup(smartAdGroup);
        var request = new AddRequest().withAdGroups(adGroupAddItem);
        List<AdGroupsContainer> adGroups = delegate.convertRequest(request);
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(adGroups);
        ApiMassResult<Long> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getErrorCount()).isEqualTo(0);
        ApiResult<Long> adGroupIdResult = apiResult.get(0);
        var adGroup = (PerformanceAdGroup) adGroupService.getAdGroup(adGroupIdResult.getResult());
        assertThat(adGroup).isNotNull();
        assertThat(adGroup.getFeedId()).isEqualTo(feed.getFeedId());
    }

    @Test
    public void processList_performanceAdGroup_noCreatives() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.SMART_NO_CREATIVES, true);

        String logoImageHash = steps.bannerSteps().createLogoImageFormat(clientInfo).getImageHash();
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        CampaignInfo campaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);

        AddRequest request = new AddRequest().withAdGroups(
                new AdGroupAddItem()
                        .withName("name")
                        .withRegionIds(225L)
                        .withCampaignId(campaignInfo.getCampaignId())
                        .withSmartAdGroup(new SmartAdGroupAdd()
                                .withFeedId(feedInfo.getFeedId())
                                .withLogoExtensionHash(logoImageHash)
                                .withNoCreatives(true)));

        List<AdGroupsContainer> internalRequest = delegate.convertRequest(request);
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(internalRequest);
        ApiMassResult<Long> apiResult = delegate.processList(vr.getValue());

        assertThat(apiResult.getErrorCount()).isEqualTo(0);
        assertThat(apiResult.get(0)).satisfies(adGroupResult -> {
            assertThat(adGroupResult).isNotNull();
            assertThat(adGroupResult.isSuccessful()).isTrue();
        });
        Long adGroupId = apiResult.get(0).getResult();
        List<AdGroup> adGroups = adGroupService.getAdGroups(Objects.requireNonNull(clientInfo.getClientId()),
                List.of(adGroupId));
        List<PerformanceBannerMain> banners = bannerTypedRepository.getBannersByGroupIds(clientInfo.getShard(),
                List.of(adGroupId), PerformanceBannerMain.class);
        assertSoftly(softly -> {
            softly.assertThat(adGroups).hasSize(1);
            softly.assertThat(adGroups.get(0)).satisfies(adGroup -> {
                assertThat(adGroup).isNotNull();
                assertThat(adGroup).isInstanceOf(PerformanceAdGroup.class);
                PerformanceAdGroup performanceAdGroup = (PerformanceAdGroup) adGroup;
                assertThat(performanceAdGroup.getFeedId()).isEqualTo(feedInfo.getFeedId());
            });
            softly.assertThat(banners).hasSize(1);
            softly.assertThat(banners.get(0)).satisfies(banner -> {
                assertThat(banner).isNotNull();
                assertThat(banner.getLogoImageHash()).isEqualTo(logoImageHash);
            });
        });
    }

    @Test
    public void processList_cpmVideoAdGroup() {
        var campaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        Long campaignId = campaign.getCampaignId();
        CpmVideoAdGroupAdd cpmVideoAdGroup = new CpmVideoAdGroupAdd().withIsNonSkippable(YesNoEnum.YES);
        var adGroupAddItem = new AdGroupAddItem()
                .withName("name")
                .withRegionIds(225L)
                .withCampaignId(campaignId)
                .withCpmVideoAdGroup(cpmVideoAdGroup);
        var request = new AddRequest().withAdGroups(adGroupAddItem);
        List<AdGroupsContainer> adGroups = delegate.convertRequest(request);
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(adGroups);
        ApiMassResult<Long> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getErrorCount()).isEqualTo(0);
        ApiResult<Long> adGroupIdResult = apiResult.get(0);
        var adGroup = (CpmVideoAdGroup) adGroupService.getAdGroup(adGroupIdResult.getResult());
        assertThat(adGroup).extracting(CpmVideoAdGroup::getIsNonSkippable).isEqualTo(true);
    }

    @Test
    public void processList_ContentPromotionAdGroups() {
        var contentPromotionVideoAdGroup = new ContentPromotionAdGroupAdd()
                .withPromotedContentType(PromotedContentTypeEnum.VIDEO);
        var contentPromotionCollectionAdGroup = new ContentPromotionAdGroupAdd()
                .withPromotedContentType(PromotedContentTypeEnum.COLLECTION);
        var contentPromotionServiceAdGroup = new ContentPromotionAdGroupAdd()
                .withPromotedContentType(PromotedContentTypeEnum.SERVICE);
        var contentPromotionEdaAdGroup = new ContentPromotionAdGroupAdd()
                .withPromotedContentType(PromotedContentTypeEnum.EDA);
        var campaignForVideo = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        var campaignForCollections = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        var campaignForServices = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        var campaignForEda = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        var mixedCampaign = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        var emptyContentTypeCampaign = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        BiFunction<ContentPromotionAdGroupAdd, Long, AdGroupAddItem> addItemConstructor = (item, cid) ->
                new AdGroupAddItem()
                        .withName("name")
                        .withRegionIds(225L)
                        .withCampaignId(cid)
                        .withContentPromotionAdGroup(item);
        List<AdGroupAddItem> addItems = ImmutableList.<AdGroupAddItem>builder()
                .add(addItemConstructor.apply(contentPromotionVideoAdGroup, campaignForVideo.getCampaignId()))
                .add(addItemConstructor.apply(contentPromotionCollectionAdGroup,
                        campaignForCollections.getCampaignId()))
                .add(addItemConstructor.apply(contentPromotionServiceAdGroup, campaignForServices.getCampaignId()))
                .add(addItemConstructor.apply(contentPromotionEdaAdGroup, campaignForEda.getCampaignId()))
                .add(addItemConstructor.apply(contentPromotionCollectionAdGroup, mixedCampaign.getCampaignId()))
                .add(addItemConstructor.apply(contentPromotionVideoAdGroup, mixedCampaign.getCampaignId()))
                .add(addItemConstructor.apply(contentPromotionServiceAdGroup, mixedCampaign.getCampaignId()))
                .add(addItemConstructor.apply(new ContentPromotionAdGroupAdd(),
                        emptyContentTypeCampaign.getCampaignId()))
                .build();
        List<AdGroupsContainer> adGroups = delegate.convertRequest(new AddRequest().withAdGroups(addItems));
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(adGroups);
        ApiMassResult<Long> apiResult = delegate.processList(vr.getValue());
        assertSoftly(softly -> {
            softly.assertThat(apiResult.getErrorCount()).isEqualTo(4);
            softly.assertThat(apiResult.get(0).isSuccessful()).isTrue();
            softly.assertThat(apiResult.get(1).isSuccessful()).isTrue();
            softly.assertThat(apiResult.get(2).isSuccessful()).isTrue();
            softly.assertThat(apiResult.get(3).isSuccessful()).isTrue();
            softly.assertThat(apiResult.get(4).getErrors()).matches(t ->
                    contains(validationError(5005)).matches(t));
            softly.assertThat(apiResult.get(5).getErrors()).matches(t ->
                    contains(validationError(5005)).matches(t));
            softly.assertThat(apiResult.get(6).getErrors()).matches(t ->
                    contains(validationError(5005)).matches(t));
            softly.assertThat(apiResult.get(6).getErrors()).matches(t ->
                    contains(validationError(5005)).matches(t));
        });
    }
}
