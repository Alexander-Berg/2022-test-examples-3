package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.Collections;

import com.yandex.direct.api.v5.ads.AdUpdateItem;
import com.yandex.direct.api.v5.ads.ContentPromotionCollectionAdUpdate;
import com.yandex.direct.api.v5.ads.ContentPromotionEdaAdUpdate;
import com.yandex.direct.api.v5.ads.ObjectFactory;
import com.yandex.direct.api.v5.ads.UpdateRequest;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.security.utils.ApiAuthenticationSourceMockBuilder;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.repository.ApiUserRepository;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.banner.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCommonData.defaultContentPromotion;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@Api5Test
@RunWith(SpringRunner.class)
public class UpdateAdsDelegateDbTest {
    private static final ObjectFactory FACTORY = new ObjectFactory();
    private static final String CONTENT_HREF = "https://www.youtube.com";

    @Autowired
    Steps steps;
    @Autowired
    ApiUserRepository apiUserRepository;
    @Autowired
    ApiAuthenticationSource apiAuthenticationSourceMock;
    @Autowired
    UpdateAdsDelegate testedUpdateAdsDelegate;
    @Autowired
    ContentPromotionRepository contentPromotionRepository;
    @Autowired
    BannerTypedRepository bannerTypedRepository;
    @Autowired
    TestContentPromotionBanners testContentPromotionBanners;

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
    public void updateContentPromotionAdCollectionType_CollectionGroup_Success() {
        var adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.COLLECTION);

        Long bannerId = steps.contentPromotionBannerSteps().createBanner(
                new ContentPromotionBannerInfo()
                        .withBanner(testContentPromotionBanners
                                .fullContentPromoCollectionBanner(collectionContentId, null))
                        .withContent(defaultContentPromotion(null, ContentPromotionContentType.COLLECTION)
                                .withId(collectionContentId))
                        .withAdGroupInfo(adGroupInfo))
                .getBannerId();
        var banner = bannerTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                Collections.singletonList(bannerId), ContentPromotionBanner.class).get(0);
        assumeThat(banner.getVisitUrl(), is("https://www.yandex.ru"));

        AdUpdateItem item = new AdUpdateItem()
                .withId(bannerId)
                .withContentPromotionCollectionAd(new ContentPromotionCollectionAdUpdate()
                        .withPromotedContentId(collectionContentId)
                        .withVisitHref(FACTORY
                                .createContentPromotionCollectionAdUpdateVisitHref("https://ya.ru")));
        UpdateRequest updateRequest = new UpdateRequest().withAds(Collections.singletonList(item));
        var apiResult = testedUpdateAdsDelegate.processList(testedUpdateAdsDelegate.convertRequest(updateRequest));

        banner = bannerTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                Collections.singletonList(bannerId), ContentPromotionBanner.class).get(0);
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(0));
        MatcherAssert.assertThat(banner.getVisitUrl(), is("https://ya.ru"));
    }

    @Test
    public void updateContentPromotionAdCollectionType_VideoGroup_Error() {
        var adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
        Long bannerId = steps.contentPromotionBannerSteps().createBanner(
                new ContentPromotionBannerInfo()
                        .withBanner(testContentPromotionBanners
                                .fullContentPromoBanner(videoContentId, null))
                        .withContent(defaultContentPromotion(null, ContentPromotionContentType.VIDEO)
                                .withId(videoContentId))
                        .withAdGroupInfo(adGroupInfo))
                .getBannerId();
        var banner = bannerTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                Collections.singletonList(bannerId), ContentPromotionBanner.class).get(0);
        assumeThat(banner.getVisitUrl(), is("https://www.yandex.ru"));

        AdUpdateItem item = new AdUpdateItem()
                .withId(bannerId)
                .withContentPromotionCollectionAd(new ContentPromotionCollectionAdUpdate()
                        .withPromotedContentId(collectionContentId)
                        .withVisitHref(FACTORY
                                .createContentPromotionCollectionAdUpdateVisitHref("https://ya.ru")));
        UpdateRequest updateRequest = new UpdateRequest().withAds(Collections.singletonList(item));
        var apiResult = testedUpdateAdsDelegate.processList(testedUpdateAdsDelegate.convertRequest(updateRequest));

        banner = bannerTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                Collections.singletonList(bannerId), ContentPromotionBanner.class).get(0);
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(1));
        MatcherAssert.assertThat(errors, contains(validationError(5005)));
        MatcherAssert.assertThat(banner.getVisitUrl(), is("https://www.yandex.ru"));
    }

    @Test
    public void updateContentPromotionAdVideoType_VideoGroup_Success() {
        var adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
        Long bannerId = steps.contentPromotionBannerSteps().createBanner(
                new ContentPromotionBannerInfo()
                        .withBanner(testContentPromotionBanners
                                .fullContentPromoBanner(videoContentId, null))
                        .withContent(defaultContentPromotion(null, ContentPromotionContentType.VIDEO)
                                .withId(videoContentId))
                        .withAdGroupInfo(adGroupInfo))
                .getBannerId();
        var banner = bannerTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                Collections.singletonList(bannerId), ContentPromotionBanner.class).get(0);
        assumeThat(banner.getVisitUrl(), is("https://www.yandex.ru"));

        AdUpdateItem item = new AdUpdateItem()
                .withId(bannerId)
                .withContentPromotionCollectionAd(new ContentPromotionCollectionAdUpdate()
                        .withPromotedContentId(videoContentId)
                        .withVisitHref(FACTORY
                                .createContentPromotionCollectionAdUpdateVisitHref("https://ya.ru")));
        UpdateRequest updateRequest = new UpdateRequest().withAds(Collections.singletonList(item));
        var apiResult = testedUpdateAdsDelegate.processList(testedUpdateAdsDelegate.convertRequest(updateRequest));

        banner = bannerTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                Collections.singletonList(bannerId), ContentPromotionBanner.class).get(0);
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(0));
        MatcherAssert.assertThat(banner.getVisitUrl(), is("https://ya.ru"));
    }

    @Test
    public void updateContentPromotionAdVideoType_CollectionGroup_Error() {
        var adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.COLLECTION);
        Long bannerId = steps.contentPromotionBannerSteps().createBanner(
                new ContentPromotionBannerInfo()
                        .withBanner(testContentPromotionBanners
                                .fullContentPromoCollectionBanner(collectionContentId, null))
                        .withContent(defaultContentPromotion(null, ContentPromotionContentType.COLLECTION)
                                .withId(collectionContentId))
                        .withAdGroupInfo(adGroupInfo))
                .getBannerId();
        var banner = bannerTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                Collections.singletonList(bannerId), ContentPromotionBanner.class).get(0);
        assumeThat(banner.getVisitUrl(), is("https://www.yandex.ru"));

        AdUpdateItem item = new AdUpdateItem()
                .withId(bannerId)
                .withContentPromotionCollectionAd(new ContentPromotionCollectionAdUpdate()
                        .withPromotedContentId(videoContentId)
                        .withVisitHref(FACTORY
                                .createContentPromotionCollectionAdUpdateVisitHref("https://ya.ru")));
        UpdateRequest updateRequest = new UpdateRequest().withAds(Collections.singletonList(item));
        var apiResult = testedUpdateAdsDelegate.processList(testedUpdateAdsDelegate.convertRequest(updateRequest));

        banner = bannerTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                Collections.singletonList(bannerId), ContentPromotionBanner.class).get(0);
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(1));
        MatcherAssert.assertThat(errors, contains(validationError(5005)));
        MatcherAssert.assertThat(banner.getVisitUrl(), is("https://www.yandex.ru"));
    }

    @Test
    public void updateContentPromotionAdEdaType_EdaGroup_Success() {
        var adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.EDA);
        Long bannerId = steps.contentPromotionBannerSteps().createBanner(
                new ContentPromotionBannerInfo()
                        .withBanner(testContentPromotionBanners
                                .fullContentPromoEdaBanner(collectionContentId, null)
                                .withBody("oldtext"))
                        .withContent(defaultContentPromotion(null, ContentPromotionContentType.EDA)
                                .withId(collectionContentId))
                        .withAdGroupInfo(adGroupInfo))
                .getBannerId();
        var banner = bannerTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                Collections.singletonList(bannerId), ContentPromotionBanner.class).get(0);
        assumeThat(banner.getBody(), is("oldtext"));

        AdUpdateItem item = new AdUpdateItem()
                .withId(bannerId)
                .withContentPromotionEdaAd(new ContentPromotionEdaAdUpdate()
                        .withPromotedContentId(edaContentId)
                        .withText(FACTORY.createContentPromotionEdaAdUpdateText("newtext")));
        UpdateRequest updateRequest = new UpdateRequest().withAds(Collections.singletonList(item));
        var apiResult = testedUpdateAdsDelegate.processList(testedUpdateAdsDelegate.convertRequest(updateRequest));

        banner = bannerTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                Collections.singletonList(bannerId), ContentPromotionBanner.class).get(0);
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(0));
        MatcherAssert.assertThat(banner.getBody(), is("newtext"));
    }

    @Test
    public void updateContentPromotionAdEdaType_VideoGroup_Error() {
        var adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
        Long bannerId = steps.contentPromotionBannerSteps().createBanner(
                new ContentPromotionBannerInfo()
                        .withBanner(testContentPromotionBanners
                                .fullContentPromoBanner(videoContentId, null)
                                .withBody("oldtext"))
                        .withContent(defaultContentPromotion(null, ContentPromotionContentType.VIDEO)
                                .withId(videoContentId))
                        .withAdGroupInfo(adGroupInfo))
                .getBannerId();
        var banner = bannerTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                Collections.singletonList(bannerId), ContentPromotionBanner.class).get(0);
        assumeThat(banner.getBody(), is("oldtext"));

        AdUpdateItem item = new AdUpdateItem()
                .withId(bannerId)
                .withContentPromotionEdaAd(new ContentPromotionEdaAdUpdate()
                        .withPromotedContentId(edaContentId)
                        .withText(FACTORY.createContentPromotionEdaAdUpdateText("newtext")));
        UpdateRequest updateRequest = new UpdateRequest().withAds(Collections.singletonList(item));
        var apiResult = testedUpdateAdsDelegate.processList(testedUpdateAdsDelegate.convertRequest(updateRequest));

        banner = bannerTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                Collections.singletonList(bannerId), ContentPromotionBanner.class).get(0);
        var errors = apiResult.getResult().get(0).getErrors();
        MatcherAssert.assertThat(errors, hasSize(1));
        MatcherAssert.assertThat(errors, contains(validationError(5005)));
        MatcherAssert.assertThat(banner.getBody(), is("oldtext"));
    }
}
