package ru.yandex.direct.grid.processing.service.group;

import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
import ru.yandex.direct.core.testing.data.TestFeeds;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobile;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobileAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdatePerformanceAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdatePerformanceAdGroupItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServicePerformanceMutationTest {
    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    protected GridGraphQLProcessor processor;
    @Autowired
    Steps steps;
    @Autowired
    AdGroupRepository adGroupRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    BidModifierRepository bidModifierRepository;
    @Autowired
    BannerTypedRepository bannerTypedRepository;

    private static final String UPDATE_MUTATION_NAME = "updateSmartAdGroups";
    private static final String UPDATE_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    updatedAdGroupItems {\n"
            + "         adGroupId,\n"
            + "     }\n"
            + "  }\n"
            + "}";

    private PerformanceAdGroup changedAdGroup;
    private PerformanceAdGroupInfo adGroupInfo;
    private User user;
    private GdUpdatePerformanceAdGroupItem adGroupItem;
    private Long adGroupId;
    private Integer shard;
    private Long minusKeywordPackId;

    @Before
    public void setUp() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        adGroupId = adGroupInfo.getAdGroupId();
        Long uid = adGroupInfo.getClientInfo().getUid();
        shard = adGroupInfo.getShard();
        user = userRepository.fetchByUids(shard, singletonList(uid)).get(0);

        MinusKeywordsPackInfo minusKeywordsPackInfo =
                steps.minusKeywordsPackSteps().createAndLinkMinusKeywordsPack(adGroupInfo);
        minusKeywordPackId = minusKeywordsPackInfo.getMinusKeywordPackId();

        changedAdGroup = new PerformanceAdGroup()
                .withId(adGroupId)
                .withFieldToUseAsName("new feed field name #1")
                .withFieldToUseAsBody("new feed field name #2")
                .withTrackingParams("new_tracking_params")
                .withName("New test Name")
                .withMinusKeywords(asList("word1", "word2"))
                .withGeo(singletonList(Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID));

        List<Integer> regionIds = mapList(changedAdGroup.getGeo(), Long::intValue);

        adGroupItem = new GdUpdatePerformanceAdGroupItem()
                .withId(changedAdGroup.getId())
                .withFieldToUseAsName(changedAdGroup.getFieldToUseAsName())
                .withFieldToUseAsBody(changedAdGroup.getFieldToUseAsBody())
                .withName(changedAdGroup.getName())
                .withMinusKeywords(changedAdGroup.getMinusKeywords())
                .withRegionIds(regionIds)
                .withTrackingParams(changedAdGroup.getTrackingParams());
    }

    @Test
    public void updateSmartAdGroups_success() {
        //Ожидаемые результаты
        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(
                        singletonList(
                                new GdUpdateAdGroupPayloadItem().withAdGroupId(changedAdGroup.getId())));
        List<GdUpdatePerformanceAdGroupItem> updateItems = singletonList(adGroupItem);

        //Выполняем запрос
        Map<String, Object> data = executeMutation(updateItems);
        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(UPDATE_MUTATION_NAME), GdUpdateAdGroupPayload.class);

        //Сверяем ожидания и реальность
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        assertSoftly(soft -> {
            soft.assertThat(payload)
                    .is(matchedBy(beanDiffer(expectedPayload)));
            soft.assertThat(actualAdGroup)
                    .is(matchedBy(beanDiffer(changedAdGroup)
                            .useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void updateSmartAdGroups_withWrongStatus() {
        FeedInfo feedInfo =
                new FeedInfo()
                        .withClientInfo(adGroupInfo.getClientInfo())
                        .withFeed(TestFeeds.defaultFeed(adGroupInfo.getClientId()).withUpdateStatus(UpdateStatus.ERROR));
        Long feedId = steps.feedSteps().createFeed(feedInfo).getFeedId();
        AdGroupInfo activePerformanceAdGroup =
                steps.adGroupSteps().createActivePerformanceAdGroup(adGroupInfo.getClientInfo(), feedId);

        //Выполняем запрос
        adGroupItem.withId(activePerformanceAdGroup.getAdGroupId());
        List<GdUpdatePerformanceAdGroupItem> updateItems = singletonList(adGroupItem);
        Map<String, Object> data = executeMutation(updateItems);
        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(UPDATE_MUTATION_NAME), GdUpdateAdGroupPayload.class);

        //Сверяем ожидания и реальность
        assertSoftly(soft -> {
            soft.assertThat(payload.getUpdatedAdGroupItems())
                    .hasSize(1);
            soft.assertThat(payload.getValidationResult())
                    .isNull();
        });
    }

    @Test
    public void updateSmartAdGroups_withBidModifiers_success() {
        //Создаём исходные данные
        BidModifierMobile defaultBidModifierMobile = TestBidModifiers
                .createDefaultBidModifierMobile(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupId);
        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers()
                .withBidModifierMobile(new GdUpdateBidModifierMobile()
                        .withId(defaultBidModifierMobile.getId())
                        .withAdGroupId(defaultBidModifierMobile.getAdGroupId())
                        .withCampaignId(defaultBidModifierMobile.getCampaignId())
                        .withAdjustment(new GdUpdateBidModifierMobileAdjustmentItem()
                                .withPercent((defaultBidModifierMobile)
                                        .getMobileAdjustment().getPercent()))
                        .withEnabled(defaultBidModifierMobile.getEnabled())
                        .withType(GdBidModifierType.valueOf(defaultBidModifierMobile.getType().name())));
        adGroupItem.setBidModifiers(bidModifiers);
        List<GdUpdatePerformanceAdGroupItem> updateItems = singletonList(adGroupItem);

        //Выполняем запрос
        executeMutation(updateItems);

        //Сверяем ожидания и реальность
        List<BidModifier> byAdGroupIds = bidModifierRepository
                .getByAdGroupIds(shard,
                        singletonMap(adGroupId, adGroupInfo.getCampaignId()),
                        singleton(defaultBidModifierMobile.getType()),
                        singleton(BidModifierLevel.ADGROUP));
        assertSoftly(soft -> {
            soft.assertThat(byAdGroupIds).hasSize(1);
            soft.assertThat(byAdGroupIds.get(0).getType())
                    .isEqualTo(defaultBidModifierMobile.getType());
            soft.assertThat(byAdGroupIds.get(0).getCampaignId())
                    .isEqualTo(defaultBidModifierMobile.getCampaignId());
            soft.assertThat(byAdGroupIds.get(0).getAdGroupId())
                    .isEqualTo(defaultBidModifierMobile.getAdGroupId());
            soft.assertThat(byAdGroupIds.get(0).getEnabled())
                    .isEqualTo(defaultBidModifierMobile.getEnabled());

            soft.assertThat(((BidModifierMobile) (byAdGroupIds.get(0))).getMobileAdjustment().getPercent())
                    .isEqualTo((defaultBidModifierMobile).getMobileAdjustment().getPercent());
        });
    }

    @Test
    public void updateSmartAdGroups_keepMinusKeywordsPackWhenItemFieldIsNull_success() {
        //Подготавливаем данные и проверяем состояние системы
        AdGroup startAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        assumeThat(startAdGroup.getLibraryMinusKeywordsIds(), CoreMatchers.hasItem(minusKeywordPackId));
        assumeThat(adGroupItem.getLibraryMinusKeywordsIds(), CoreMatchers.nullValue());
        List<GdUpdatePerformanceAdGroupItem> updateItems = singletonList(adGroupItem);

        //Выполняем запрос
        executeMutation(updateItems);

        //Сверяем ожидания и реальность
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        assertThat(actualAdGroup.getLibraryMinusKeywordsIds()).containsExactly(minusKeywordPackId);
    }

    @Test
    public void updateSmartAdGroups_saveNewMinusKeywordsPack() {
        //Подготавливаем данные и проверяем состояние системы
        MinusKeywordsPackInfo newPackInfo = steps.minusKeywordsPackSteps().createAndLinkMinusKeywordsPack(adGroupInfo);
        Long newPackId = newPackInfo.getMinusKeywordPackId();
        adGroupItem.setLibraryMinusKeywordsIds(singletonList(newPackId));
        List<GdUpdatePerformanceAdGroupItem> updateItems = singletonList(adGroupItem);

        //Выполняем запрос
        executeMutation(updateItems);

        //Сверяем ожидания и реальность
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        assertThat(actualAdGroup.getLibraryMinusKeywordsIds()).containsExactly(newPackId);
    }

    @Test
    public void updateSmartAdGroups_createMainBanner() {
        steps.featureSteps().addClientFeature(user.getClientId(), FeatureName.SMART_NO_CREATIVES, true);
        steps.featureSteps().addClientFeature(user.getClientId(), FeatureName.CREATIVE_FREE_INTERFACE, true);

        List<GdUpdatePerformanceAdGroupItem> updateItems = singletonList(adGroupItem);

        executeMutation(updateItems);

        List<PerformanceBannerMain> banners = bannerTypedRepository
                .getBannersByGroupIds(shard, singletonList(adGroupId), PerformanceBannerMain.class);
        assertThat(banners).hasSize(1);
    }

    @Test
    public void updateSmartAdGroups_updateMainBanner() {
        steps.featureSteps().addClientFeature(user.getClientId(), FeatureName.SMART_NO_CREATIVES, true);
        steps.featureSteps().addClientFeature(user.getClientId(), FeatureName.CREATIVE_FREE_INTERFACE, true);

        String imageHash = steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo()).getImageHash();
        steps.performanceMainBannerSteps().createPerformanceMainBanner(adGroupInfo);
        adGroupItem.setLogoImageHash(imageHash);
        List<GdUpdatePerformanceAdGroupItem> updateItems = singletonList(adGroupItem);

        executeMutation(updateItems);

        List<PerformanceBannerMain> banners = bannerTypedRepository
                .getBannersByGroupIds(shard, singletonList(adGroupId), PerformanceBannerMain.class);
        assertSoftly(softly -> {
           softly.assertThat(banners).hasSize(1);
           softly.assertThat(banners.get(0)).satisfies(banner -> {
               assertThat(banner.getLogoImageHash()).isEqualTo(imageHash);
           });
        });
    }

    @Test
    public void updateSmartAdGroups_suspendOldBanners() {
        steps.featureSteps().addClientFeature(user.getClientId(), FeatureName.SMART_NO_CREATIVES, true);
        steps.featureSteps().addClientFeature(user.getClientId(), FeatureName.CREATIVE_FREE_INTERFACE, true);

        steps.performanceBannerSteps().createPerformanceBanner(adGroupInfo);
        List<PerformanceBanner> oldBanners = bannerTypedRepository
                .getBannersByGroupIds(shard, singletonList(adGroupId), PerformanceBanner.class);
        assumeThat(oldBanners, hasSize(1));
        assumeThat(oldBanners.get(0).getStatusShow(), is(true));

        executeMutation(singletonList(adGroupItem));

        List<PerformanceBanner> updatedOldBanners = bannerTypedRepository
                .getBannersByGroupIds(shard, singletonList(adGroupId), PerformanceBanner.class);
        List<PerformanceBannerMain> banners = bannerTypedRepository
                .getBannersByGroupIds(shard, singletonList(adGroupId), PerformanceBannerMain.class);
        assertSoftly(softly -> {
            softly.assertThat(updatedOldBanners).hasSize(1);
            softly.assertThat(updatedOldBanners.get(0)).satisfies(oldBanner -> {
                assertThat(oldBanner).isNotNull();
                assertThat(oldBanner.getStatusShow()).isFalse();
            });
            softly.assertThat(banners).hasSize(1);
            softly.assertThat(banners.get(0)).satisfies(banner -> {
                assertThat(banner).isNotNull();
                assertThat(banner.getStatusShow()).isTrue();
            });
        });
    }

    private Map<String, Object> executeMutation(List<GdUpdatePerformanceAdGroupItem> updateItems) {
        GdUpdatePerformanceAdGroup request = new GdUpdatePerformanceAdGroup()
                .withUpdateItems(updateItems);
        String query = String.format(UPDATE_MUTATION_TEMPLATE, UPDATE_MUTATION_NAME, graphQlSerialize(request));
        TestAuthHelper.setDirectAuthentication(user);
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(user));
        checkErrors(result.getErrors());
        return result.getData();
    }

}
