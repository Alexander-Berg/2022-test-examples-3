package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.GenericGetRequest;
import ru.yandex.direct.api.v5.entity.ads.container.AdsGetContainer;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.security.utils.ApiAuthenticationSourceMockBuilder;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.container.AdsSelectionCriteria;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.repository.ApiUserRepository;
import ru.yandex.direct.core.testing.data.TestCreatives;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;

@Api5Test
@RunWith(SpringRunner.class)
public class AddAdsDelegateDbTest {
    private static final String CONTENT_HREF = "https://www.youtube.com";

    @Autowired
    Steps steps;
    @Autowired
    ApiUserRepository apiUserRepository;
    @Autowired
    ApiAuthenticationSource apiAuthenticationSourceMock;
    @Autowired
    AddAdsDelegate testedAddAdsDelegate;
    @Autowired
    GetAdsDelegate testedGetAdsDelegate;
    @Autowired
    ContentPromotionRepository contentPromotionRepository;
    @Autowired
    private TestContentPromotionBanners testNewContentPromotionBanners;

    private ClientInfo clientInfo;
    private Long collectionContentId;
    private Long videoContentId;
    private Long edaContentId;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();

        ApiUser operatorUser = apiUserRepository.fetchByUid(clientInfo.getShard(), clientInfo.getUid());
        new ApiAuthenticationSourceMockBuilder()
                .withOperator(operatorUser)
                .tuneAuthSourceMock(apiAuthenticationSourceMock);
        videoContentId = contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withUrl(CONTENT_HREF)
                        .withType(ContentPromotionContentType.VIDEO)
                        .withIsInaccessible(false)
                        .withExternalId("EXTERNALVIDEO"));
        collectionContentId = contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withUrl(CONTENT_HREF)
                        .withType(ContentPromotionContentType.COLLECTION)
                        .withIsInaccessible(false)
                        .withExternalId("EXTERNALCOLLECTIONS"));
        edaContentId = contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(),
                new ContentPromotionContent()
                        .withUrl(CONTENT_HREF)
                        .withType(ContentPromotionContentType.EDA)
                        .withIsInaccessible(false)
                        .withExternalId("EXTERNALEDA"));
    }

    @Test
    public void adCpmAd_Ok() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo);
        var creativeId = steps.creativeSteps()
                .createCreative(TestCreatives.defaultCanvas(clientInfo.getClientId(), null), clientInfo)
                .getCreativeId();
        var banner = fullCpmBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId)
                .withTnsId("someTnsId");

        var apiResult = testedAddAdsDelegate.processList(Collections.singletonList(banner));
        var errors = apiResult.getResult().get(0).getErrors();

        var bannerId = apiResult.getResult().get(0).getResult();
        MatcherAssert.assertThat(errors.isEmpty(), is(true));

        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(
                        ImmutableSet.of(AdAnyFieldEnum.AD_ID, AdAnyFieldEnum.CPM_BANNER_AD_BUILDER_AD_TNS_ID),
                        new AdsSelectionCriteria()
                                .withSelectCpmBanner(true)
                                .withSelectCpmVideo(true)
                                .withAdIds(bannerId),
                        LimitOffset.maxLimited()
                );

        List<AdsGetContainer> result = testedGetAdsDelegate.get(request);
        MatcherAssert.assertThat(result, hasSize(1));
        var actualBanner = (CpmBanner) result.get(0).getAd();
        MatcherAssert.assertThat(banner.getTnsId(), is(actualBanner.getTnsId()));
    }

    @Test
    public void addContentPromotionAdCollectionType_CollectionGroup_Success() {
        var collectionsAdGroup = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.COLLECTION);
        var collectionsBanner = testNewContentPromotionBanners.fullContentPromoCollectionBanner(collectionContentId,
                null)
                .withCampaignId(collectionsAdGroup.getCampaignId())
                .withAdGroupId(collectionsAdGroup.getAdGroupId())
                .withDomain(null);

        var apiResult = testedAddAdsDelegate.processList(Collections.singletonList(collectionsBanner));
        var errors = apiResult.getResult().get(0).getErrors();

        var bannerId = apiResult.getResult().get(0).getResult();

        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(
                        ImmutableSet.of(AdAnyFieldEnum.AD_ID, AdAnyFieldEnum.AD_TYPE),
                        new AdsSelectionCriteria()
                                .withSelectCpmBanner(true)
                                .withSelectCpmVideo(true)
                                .withAdIds(bannerId), LimitOffset.maxLimited());

        List<AdsGetContainer> result = testedGetAdsDelegate.get(request);
        MatcherAssert.assertThat(errors, hasSize(0));
        var actualBanner = result.get(0).getAd();
        MatcherAssert.assertThat(actualBanner, is(instanceOf((ContentPromotionBanner.class))));
    }

    @Test
    public void addContentPromotionAdCollectionType_VideoGroup_Error() {
        var videoAdGroup = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
        var collectionsBanner = testNewContentPromotionBanners.fullContentPromoBanner(collectionContentId, null)
                .withCampaignId(videoAdGroup.getCampaignId())
                .withAdGroupId(videoAdGroup.getAdGroupId());

        var apiResult = testedAddAdsDelegate.processList(Collections.singletonList(collectionsBanner));
        var errors = apiResult.getResult().get(0).getErrors();

        MatcherAssert.assertThat(errors, hasSize(1));
        MatcherAssert.assertThat(errors, contains(validationError(5005)));
    }

    @Test
    public void addContentPromotionAdVideoType_VideoGroup_Success() {
        var videoAdGroup = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
        var videoBanner = testNewContentPromotionBanners.fullContentPromoBanner(videoContentId, null)
                .withCampaignId(videoAdGroup.getCampaignId())
                .withAdGroupId(videoAdGroup.getAdGroupId());

        var apiResult = testedAddAdsDelegate.processList(Collections.singletonList(videoBanner));
        var errors = apiResult.getResult().get(0).getErrors();

        var bannerId = apiResult.getResult().get(0).getResult();

        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(
                        ImmutableSet.of(AdAnyFieldEnum.AD_ID, AdAnyFieldEnum.AD_TYPE),
                        new AdsSelectionCriteria()
                                .withSelectCpmBanner(true)
                                .withSelectCpmVideo(true)
                                .withAdIds(bannerId), LimitOffset.maxLimited());

        List<AdsGetContainer> result = testedGetAdsDelegate.get(request);
        MatcherAssert.assertThat(errors, hasSize(0));
        var actualBanner = result.get(0).getAd();
        MatcherAssert.assertThat(actualBanner, is(instanceOf(ContentPromotionBanner.class)));
    }

    @Test
    public void addContentPromotionAdVideoType_CollectionGroup_Error() {
        var collectionAdGroup = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.COLLECTION);
        var videoBanner = testNewContentPromotionBanners.fullContentPromoCollectionBanner(videoContentId, null)
                .withCampaignId(collectionAdGroup.getCampaignId())
                .withAdGroupId(collectionAdGroup.getAdGroupId());

        var apiResult = testedAddAdsDelegate.processList(Collections.singletonList(videoBanner));
        var errors = apiResult.getResult().get(0).getErrors();

        var bannerId = apiResult.getResult().get(0).getResult();

        MatcherAssert.assertThat(errors, contains(validationError(5005)));
    }

    @Test
    public void addContentPromotionAdEdaType_EdaGroup_Success() {
        var edaAdGroup = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.EDA);
        var edaBanner = testNewContentPromotionBanners.fullContentPromoBanner(edaContentId, null)
                .withVisitUrl(null)
                .withCampaignId(edaAdGroup.getCampaignId())
                .withAdGroupId(edaAdGroup.getAdGroupId());

        var apiResult = testedAddAdsDelegate.processList(Collections.singletonList(edaBanner));
        var errors = apiResult.getResult().get(0).getErrors();

        var bannerId = apiResult.getResult().get(0).getResult();

        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(
                        ImmutableSet.of(AdAnyFieldEnum.AD_ID, AdAnyFieldEnum.AD_TYPE),
                        new AdsSelectionCriteria()
                                .withSelectCpmBanner(true)
                                .withSelectCpmVideo(true)
                                .withAdIds(bannerId), LimitOffset.maxLimited());

        List<AdsGetContainer> result = testedGetAdsDelegate.get(request);
        MatcherAssert.assertThat(errors, hasSize(0));
        var actualBanner = result.get(0).getAd();
        MatcherAssert.assertThat(actualBanner, is(instanceOf(ContentPromotionBanner.class)));
    }

    @Test
    public void addContentPromotionAdEdaType_CollectionGroup_Error() {
        var collectionAdGroup = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.COLLECTION);
        var edaBanner = testNewContentPromotionBanners.fullContentPromoCollectionBanner(edaContentId, null)
                .withCampaignId(collectionAdGroup.getCampaignId())
                .withAdGroupId(collectionAdGroup.getAdGroupId());

        var apiResult = testedAddAdsDelegate.processList(Collections.singletonList(edaBanner));
        var errors = apiResult.getResult().get(0).getErrors();

        var bannerId = apiResult.getResult().get(0).getResult();

        MatcherAssert.assertThat(errors, contains(validationError(5005)));
    }
}
