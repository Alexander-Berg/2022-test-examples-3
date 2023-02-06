package ru.yandex.direct.grid.processing.service.banner.contentpromotion;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderBy;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderByField;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContainer;
import ru.yandex.direct.grid.processing.model.contentpromotion.GdContentPromotionType;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.COLLECTION;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.EDA;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.SERVICE;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerCollectionType;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerEdaType;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerServiceType;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerVideoType;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.fromSerpData;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCollections.realLifeCollection;
import static ru.yandex.direct.core.testing.data.TestContentPromotionVideos.fromVideoBannerAsContent;
import static ru.yandex.direct.core.testing.data.TestContentPromotionVideos.realLifeVideoBanner;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CONTENT_PROMOTION_COLLECTION;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CONTENT_PROMOTION_EDA;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CONTENT_PROMOTION_SERVICE;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.CONTENT_PROMOTION_VIDEO;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlServiceTest.convertToGroupsRowset;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.BANNERSTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceGetContentPromotionAdsTest {

    private static final GdAdOrderBy ORDER_BY_ID = new GdAdOrderBy()
            .withField(GdAdOrderByField.ID)
            .withOrder(Order.ASC);

    @Autowired
    private Steps steps;

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    ads(input: %s) {\n"
            + "      totalCount\n"
            + "      adIds\n"
            + "      rowset {\n"
            + "        index\n"
            + "        id\n"
            + "        type\n"
            + "        ... on GdContentPromotionAd {\n"
            + "          contentPromotionId\n"
            + "          contentPromotionType\n"
            + "          contentPromotionPreviewUrl\n"
            + "          contentPromotionUrl\n"
            + "          contentPromotionVisitUrl\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private GridGraphQLContext context;

    private UserInfo userInfo;
    private ClientInfo clientInfo;

    private CampaignInfo campaignInfo;
    private ContentPromotionBannerInfo contentPromotionVideoBannerInfo;
    private ContentPromotionBannerInfo contentPromotionVideoBannerWithoutPreviewUrlInfo;
    private ContentPromotionBannerInfo contentPromotionCollectionBannerWithUrlInfo;
    private ContentPromotionBannerInfo contentPromotionCollectionBannerWithoutUrlInfo;
    private ContentPromotionBannerInfo contentPromotionServiceBannerInfo;
    private ContentPromotionBannerInfo contentPromotionEdaBannerInfo;

    private Long videoContentPromotionId;
    private Long videoContentPromotionWithoutPreviewUrlId;
    private Long collectionContentPromotionId;
    private Long serviceContentPromotionId;
    private Long edaContentPromotionId;
    private ContentPromotionContent collection;

    @Before
    public void before() throws IOException {
        userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        ClientId clientId = clientInfo.getClientId();

        campaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        AdGroupInfo videoAdGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(campaignInfo, VIDEO);
        AdGroupInfo collectionAdGroupInfo =
                steps.adGroupSteps().createDefaultContentPromotionAdGroup(campaignInfo, COLLECTION);
        AdGroupInfo serviceAdGroupInfo =
                steps.adGroupSteps().createDefaultContentPromotionAdGroup(campaignInfo, SERVICE);
        AdGroupInfo edaAdGroupInfo =
                steps.adGroupSteps().createDefaultContentPromotionAdGroup(campaignInfo, EDA);

        Long campaignId = campaignInfo.getCampaignId();
        Long videoAdGroupId = videoAdGroupInfo.getAdGroupId();
        Long collectionAdGroupId = collectionAdGroupInfo.getAdGroupId();
        Long serviceAdGroupId = serviceAdGroupInfo.getAdGroupId();

        videoContentPromotionId = contentPromotionRepository.insertContentPromotion(clientId,
                fromVideoBannerAsContent(realLifeVideoBanner(), clientId));
        collection = fromSerpData(realLifeCollection(), clientId);
        collectionContentPromotionId = contentPromotionRepository.insertContentPromotion(clientId, collection);
        videoContentPromotionWithoutPreviewUrlId = contentPromotionRepository.insertContentPromotion(clientId,
                fromVideoBannerAsContent(realLifeVideoBanner().setUrl("www.youtube.com/watch?v=0hCBBnZI2AUkek"), clientId)
                        .withPreviewUrl(null));
        serviceContentPromotionId = contentPromotionRepository.insertContentPromotion(clientId,
                new ContentPromotionContent()
                        .withType(ContentPromotionContentType.SERVICE)
                        .withIsInaccessible(false)
                        .withUrl("https://some-url.ru")
                        .withExternalId("CONTENT_PROMOTION_SERVICE")
        );
        edaContentPromotionId = contentPromotionRepository.insertContentPromotion(clientId,
                new ContentPromotionContent()
                        .withType(ContentPromotionContentType.EDA)
                        .withIsInaccessible(false)
                        .withUrl("https://some-url.ru")
                        .withExternalId("CONTENT_PROMOTION_EDA")
        );

        contentPromotionVideoBannerInfo = steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerVideoType(campaignId, videoAdGroupId)
                        .withContentPromotionId(videoContentPromotionId), videoAdGroupInfo);
        contentPromotionVideoBannerWithoutPreviewUrlInfo = steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerVideoType(campaignId, videoAdGroupId)
                        .withContentPromotionId(videoContentPromotionWithoutPreviewUrlId), videoAdGroupInfo);
        contentPromotionCollectionBannerWithUrlInfo = steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerCollectionType(campaignId, collectionAdGroupId)
                        .withContentPromotionId(collectionContentPromotionId), collectionAdGroupInfo);
        contentPromotionCollectionBannerWithoutUrlInfo = steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerCollectionType(campaignId, collectionAdGroupId)
                        .withContentPromotionId(collectionContentPromotionId)
                        .withVisitUrl(null),
                collectionAdGroupInfo);
        contentPromotionServiceBannerInfo = steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerServiceType(campaignId, serviceAdGroupId)
                        .withContentPromotionId(serviceContentPromotionId), serviceAdGroupInfo);
        contentPromotionEdaBannerInfo = steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerEdaType(campaignId, edaAdGroupInfo.getAdGroupId())
                        .withContentPromotionId(edaContentPromotionId), edaAdGroupInfo);

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void ads_ContentPromotionVideo_WithVideoFeatureOn_AdReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID,
                true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID,
                false);

        doAnswer(getAnswer(singletonList(contentPromotionVideoBannerInfo.getAdGroupInfo()),
                singletonList(contentPromotionVideoBannerInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(ImmutableSet.of(contentPromotionVideoBannerInfo.getBannerId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                )
                .withOrderBy(singletonList(ORDER_BY_ID));

        ExecutionResult executionResult = processQuery(adsContainer);
        Map<String, Object> data = executionResult.getData();

        Map<String, Object> expected = map(
                "client", map(
                        "ads", map(
                                "totalCount", 1,
                                "adIds", list(contentPromotionVideoBannerInfo.getBannerId()),
                                "rowset", list(
                                        map(
                                                "index", 0,
                                                "id", contentPromotionVideoBannerInfo.getBannerId(),
                                                "type", CONTENT_PROMOTION_VIDEO.name(),
                                                "contentPromotionId", videoContentPromotionId,
                                                "contentPromotionType", GdContentPromotionType.VIDEO.name(),
                                                "contentPromotionPreviewUrl", "//avatars.mds.yandex.net/get-vthumb/892163/1fa3f319a9688257ef22f6c2a2c9da91",
                                                "contentPromotionUrl", "https://www.youtube.com/watch?v=0hCBBnZI2AU",
                                                "contentPromotionVisitUrl", "https://www.yandex.ru/"
                                        )
                                )
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void ads_ContentPromotionVideoWithoutPreviewUrl_WithVideoFeatureOn_AdReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID,
                true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID,
                false);

        doAnswer(getAnswer(singletonList(contentPromotionVideoBannerWithoutPreviewUrlInfo.getAdGroupInfo()),
                singletonList(contentPromotionVideoBannerWithoutPreviewUrlInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(ImmutableSet.of(contentPromotionVideoBannerWithoutPreviewUrlInfo.getBannerId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                )
                .withOrderBy(singletonList(ORDER_BY_ID));

        ExecutionResult executionResult = processQuery(adsContainer);
        Map<String, Object> data = executionResult.getData();

        Map<String, Object> expected = map(
                "client", map(
                        "ads", map(
                                "totalCount", 1,
                                "adIds", list(contentPromotionVideoBannerWithoutPreviewUrlInfo.getBannerId()),
                                "rowset", list(
                                        map(
                                                "index", 0,
                                                "id", contentPromotionVideoBannerWithoutPreviewUrlInfo.getBannerId(),
                                                "type", CONTENT_PROMOTION_VIDEO.name(),
                                                "contentPromotionId", videoContentPromotionWithoutPreviewUrlId,
                                                "contentPromotionType", GdContentPromotionType.VIDEO.name(),
                                                "contentPromotionPreviewUrl", null,
                                                "contentPromotionUrl", "https://www.youtube.com/watch?v=0hCBBnZI2AUkek",
                                                "contentPromotionVisitUrl", "https://www.yandex.ru/"
                                        )
                                )
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void ads_ContentPromotionCollectionWithUrl_WithCollectionsFeatureOn_AdReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID,
                false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID,
                true);

        doAnswer(getAnswer(singletonList(contentPromotionCollectionBannerWithUrlInfo.getAdGroupInfo()),
                singletonList(contentPromotionCollectionBannerWithUrlInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(ImmutableSet.of(contentPromotionCollectionBannerWithUrlInfo.getBannerId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                )
                .withOrderBy(singletonList(ORDER_BY_ID));

        ExecutionResult executionResult = processQuery(adsContainer);
        Map<String, Object> data = executionResult.getData();

        Map<String, Object> expected = map(
                "client", map(
                        "ads", map(
                                "totalCount", 1,
                                "adIds", list(contentPromotionCollectionBannerWithUrlInfo.getBannerId()),
                                "rowset", list(
                                        map(
                                                "index", 0,
                                                "id", contentPromotionCollectionBannerWithUrlInfo.getBannerId(),
                                                "type", CONTENT_PROMOTION_COLLECTION.name(),
                                                "contentPromotionId", collectionContentPromotionId,
                                                "contentPromotionType", GdContentPromotionType.COLLECTION.name(),
                                                "contentPromotionPreviewUrl", collection.getPreviewUrl(),
                                                "contentPromotionUrl", collection.getUrl(),
                                                "contentPromotionVisitUrl", "https://www.yandex.ru/"
                                        )
                                )
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void ads_ContentPromotionCollectionWithoutUrl_WithCollectionsFeatureOn_AdReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID,
                false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID,
                true);

        doAnswer(getAnswer(singletonList(contentPromotionCollectionBannerWithoutUrlInfo.getAdGroupInfo()),
                singletonList(contentPromotionCollectionBannerWithoutUrlInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(ImmutableSet.of(contentPromotionCollectionBannerWithoutUrlInfo.getBannerId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                )
                .withOrderBy(singletonList(ORDER_BY_ID));

        ExecutionResult executionResult = processQuery(adsContainer);
        Map<String, Object> data = executionResult.getData();

        Map<String, Object> expected = map(
                "client", map(
                        "ads", map(
                                "totalCount", 1,
                                "adIds", list(contentPromotionCollectionBannerWithoutUrlInfo.getBannerId()),
                                "rowset", list(
                                        map(
                                                "index", 0,
                                                "id", contentPromotionCollectionBannerWithoutUrlInfo.getBannerId(),
                                                "type", CONTENT_PROMOTION_COLLECTION.name(),
                                                "contentPromotionId", collectionContentPromotionId,
                                                "contentPromotionType", GdContentPromotionType.COLLECTION.name(),
                                                "contentPromotionPreviewUrl", collection.getPreviewUrl(),
                                                "contentPromotionUrl", collection.getUrl(),
                                                "contentPromotionVisitUrl", null
                                        )
                                )
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void ads_ContentPromotionVideo_WithVideoFeatureOff_AdNotReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID,
                false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID,
                true);

        doAnswer(getAnswer(singletonList(contentPromotionVideoBannerInfo.getAdGroupInfo()),
                singletonList(contentPromotionVideoBannerInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(ImmutableSet.of(contentPromotionVideoBannerInfo.getBannerId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                )
                .withOrderBy(singletonList(ORDER_BY_ID));

        ExecutionResult executionResult = processQuery(adsContainer);
        Map<String, Object> data = executionResult.getData();

        Map<String, Object> expected = map(
                "client", map(
                        "ads", map(
                                "totalCount", 0,
                                "adIds", list(),
                                "rowset", list()
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void ads_ContentPromotionCollection_WithCollectionsFeatureOff_AdNotReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID,
                true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID,
                false);

        doAnswer(getAnswer(singletonList(contentPromotionCollectionBannerWithUrlInfo.getAdGroupInfo()),
                singletonList(contentPromotionCollectionBannerWithUrlInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(ImmutableSet.of(contentPromotionCollectionBannerWithUrlInfo.getBannerId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                )
                .withOrderBy(singletonList(ORDER_BY_ID));

        ExecutionResult executionResult = processQuery(adsContainer);
        Map<String, Object> data = executionResult.getData();

        Map<String, Object> expected = map(
                "client", map(
                        "ads", map(
                                "totalCount", 0,
                                "adIds", list(),
                                "rowset", list()
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void ads_ContentPromotionService_WithServiceFeatureOn_AdReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID,
                false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID,
                false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_SERVICES_ON_GRID,
                true);

        doAnswer(getAnswer(singletonList(contentPromotionServiceBannerInfo.getAdGroupInfo()),
                singletonList(contentPromotionServiceBannerInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(ImmutableSet.of(contentPromotionServiceBannerInfo.getBannerId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                )
                .withOrderBy(singletonList(ORDER_BY_ID));

        ExecutionResult executionResult = processQuery(adsContainer);
        Map<String, Object> data = executionResult.getData();

        Map<String, Object> expected = map(
                "client", map(
                        "ads", map(
                                "totalCount", 1,
                                "adIds", list(contentPromotionServiceBannerInfo.getBannerId()),
                                "rowset", list(
                                        map(
                                                "index", 0,
                                                "id", contentPromotionServiceBannerInfo.getBannerId(),
                                                "type", CONTENT_PROMOTION_SERVICE.name(),
                                                "contentPromotionId", serviceContentPromotionId,
                                                "contentPromotionType", GdContentPromotionType.SERVICE.name(),
                                                "contentPromotionPreviewUrl", null,
                                                "contentPromotionUrl", "https://some-url.ru",
                                                "contentPromotionVisitUrl", null
                                        )
                                )
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void ads_ContentPromotionEda_WithEdaFeatureOff_AdNotReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID,
                true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID,
                true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_SERVICES_ON_GRID,
                true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_EDA_INTERFACE,
                false);

        doAnswer(getAnswer(singletonList(contentPromotionEdaBannerInfo.getAdGroupInfo()),
                singletonList(contentPromotionEdaBannerInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(ImmutableSet.of(contentPromotionEdaBannerInfo.getBannerId()))
                        .withCampaignIdIn(ImmutableSet.of(contentPromotionEdaBannerInfo.getCampaignId()))
                )
                .withOrderBy(singletonList(ORDER_BY_ID));

        ExecutionResult executionResult = processQuery(adsContainer);
        Map<String, Object> data = executionResult.getData();

        Map<String, Object> expected = map(
                "client", map(
                        "ads", map(
                                "totalCount", 0,
                                "adIds", list(),
                                "rowset", list()
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }


    @Test
    public void ads_ContentPromotionEda_WithEdaFeatureOn_AdReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID,
                false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID,
                false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_SERVICES_ON_GRID,
                false);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_EDA_INTERFACE,
                true);

        doAnswer(getAnswer(singletonList(contentPromotionEdaBannerInfo.getAdGroupInfo()),
                singletonList(contentPromotionEdaBannerInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(ImmutableSet.of(contentPromotionEdaBannerInfo.getBannerId()))
                        .withCampaignIdIn(ImmutableSet.of(contentPromotionEdaBannerInfo.getCampaignId()))
                )
                .withOrderBy(singletonList(ORDER_BY_ID));

        ExecutionResult executionResult = processQuery(adsContainer);
        Map<String, Object> data = executionResult.getData();

        Map<String, Object> expected = map(
                "client", map(
                        "ads", map(
                                "totalCount", 1,
                                "adIds", list(contentPromotionEdaBannerInfo.getBannerId()),
                                "rowset", list(
                                        map(
                                                "index", 0,
                                                "id", contentPromotionEdaBannerInfo.getBannerId(),
                                                "type", CONTENT_PROMOTION_EDA.name(),
                                                "contentPromotionId", edaContentPromotionId,
                                                "contentPromotionType", GdContentPromotionType.EDA.name(),
                                                "contentPromotionPreviewUrl", null,
                                                "contentPromotionUrl", "https://some-url.ru",
                                                "contentPromotionVisitUrl", null
                                        )
                                )
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void ads_ContentPromotionService_WithServiceFeatureOff_AdNotReturned() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_VIDEO_ON_GRID,
                true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_COLLECTIONS_ON_GRID,
                true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CONTENT_PROMOTION_SERVICES_ON_GRID,
                false);

        doAnswer(getAnswer(singletonList(contentPromotionServiceBannerInfo.getAdGroupInfo()),
                singletonList(contentPromotionServiceBannerInfo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(ImmutableSet.of(contentPromotionServiceBannerInfo.getBannerId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                )
                .withOrderBy(singletonList(ORDER_BY_ID));

        ExecutionResult executionResult = processQuery(adsContainer);
        Map<String, Object> data = executionResult.getData();

        Map<String, Object> expected = map(
                "client", map(
                        "ads", map(
                                "totalCount", 0,
                                "adIds", list(),
                                "rowset", list()
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    private ExecutionResult processQuery(GdAdsContainer adsContainer) {
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(adsContainer));
        return processor.processQuery(null, query, null, context);
    }

    private Answer<UnversionedRowset> getAnswer(List<AdGroupInfo> groups, List<ContentPromotionBannerInfo> banners) {
        return invocation -> {
            Select query = invocation.getArgument(1);
            if (query.toString().contains(BANNERSTABLE_DIRECT.getName())) {
                return convertToBannerRowset(banners);
            }
            return convertToGroupsRowset(groups);
        };
    }

    private static UnversionedRowset convertToBannerRowset(List<ContentPromotionBannerInfo> infos) {
        RowsetBuilder builder = rowsetBuilder();
        infos.forEach(info -> builder.add(
                rowBuilder()
                        .withColValue(BANNERSTABLE_DIRECT.BID.getName(), info.getBannerId())
                        .withColValue(BANNERSTABLE_DIRECT.PID.getName(), info.getAdGroupId())
                        .withColValue(BANNERSTABLE_DIRECT.CID.getName(), info.getCampaignId())
                        .withColValue(BANNERSTABLE_DIRECT.BANNER_TYPE.getName(),
                                info.getBanner().getBannerType().name())
                        .withColValue(BANNERSTABLE_DIRECT.STATUS_SHOW.getName(), "Yes")
                        .withColValue(BANNERSTABLE_DIRECT.STATUS_ACTIVE.getName(), "Yes")
                        .withColValue(BANNERSTABLE_DIRECT.STATUS_ARCH.getName(), "No")
                        .withColValue(BANNERSTABLE_DIRECT.STATUS_BS_SYNCED.getName(), "Yes")
        ));

        return builder.build();
    }
}
