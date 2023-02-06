package ru.yandex.direct.grid.processing.service.group.contentpromotion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobile;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobileAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupKeywordItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRelevanceMatchItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateContentPromotionAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateContentPromotionAdGroupItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.BigDecimalComparator.BIG_DECIMAL_COMPARATOR;
import static ru.yandex.direct.core.entity.StatusBsSynced.NO;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.COLLECTION;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast.NEW;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestGroups.DEFAULT_GEO;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceUpdateContentPromotionTest {

    @Autowired
    private Steps steps;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdGroupService adGroupService;

    @Autowired
    private BidModifierRepository bidModifierRepository;

    @Autowired
    private KeywordService keywordService;

    @Autowired
    private RelevanceMatchRepository relevanceMatchRepository;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    protected GridGraphQLProcessor processor;

    private static final String UPDATE_MUTATION_NAME = "updateContentPromotionAdGroups";
    private static final String UPDATE_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    updatedAdGroupItems {\n"
            + "         adGroupId,\n"
            + "    }\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "      warnings {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private int shard;
    private ClientInfo clientInfo;
    private User operator;

    private AdGroupInfo videoAdGroupInfo;
    private AdGroupInfo collectionAdGroupInfo;
    private Long videoAdGroupId;
    private Long collectionAdGroupId;

    private SoftAssertions softAssertions = new SoftAssertions();

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        videoAdGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo, VIDEO);
        collectionAdGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo, COLLECTION);

        steps.adGroupSteps().setAdGroupProperty(videoAdGroupInfo, AdGroup.GEO, DEFAULT_GEO);
        steps.adGroupSteps().setAdGroupProperty(collectionAdGroupInfo, AdGroup.GEO, DEFAULT_GEO);

        videoAdGroupId = videoAdGroupInfo.getAdGroupId();
        collectionAdGroupId = collectionAdGroupInfo.getAdGroupId();
    }

    @Test
    public void updateAdGroups_ContentPromotionVideo_UpdateName() {
        String newAdGroupName = "New AdGroup Name";

        GdUpdateContentPromotionAdGroup request = new GdUpdateContentPromotionAdGroup()
                .withUpdateItems(singletonList(buildUpdateItem(videoAdGroupInfo)
                        .withAdGroupName(newAdGroupName)));

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(videoAdGroupId)));

        processQueryAndCheck(request, expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(videoAdGroupId));

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .isEqualToIgnoringGivenFields(videoAdGroupInfo.getAdGroup().withName(newAdGroupName), "lastChange");

        softAssertions.assertAll();
    }

    @Test
    public void updateAdGroups_ContentPromotionCollection_UpdateMinusWords() {
        List<String> newMinusWords = asList("минус", "слово");

        GdUpdateContentPromotionAdGroup request = new GdUpdateContentPromotionAdGroup()
                .withUpdateItems(singletonList(
                        buildUpdateItem(videoAdGroupInfo).withAdGroupMinusKeywords(newMinusWords)));

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(videoAdGroupId)));

        processQueryAndCheck(request, expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(videoAdGroupId));

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .isEqualToIgnoringGivenFields(videoAdGroupInfo.getAdGroup()
                        .withMinusKeywords(newMinusWords)
                        .withStatusBsSynced(NO)
                        .withStatusShowsForecast(NEW), "lastChange", "minusKeywordsId");

        softAssertions.assertAll();
    }

    @Test
    public void updateAdGroups_ContentPromotionVideo_UpdateMinusKeywordsPack() {
        MinusKeywordsPackInfo newPackInfo = steps.minusKeywordsPackSteps().createAndLinkMinusKeywordsPack(videoAdGroupInfo);
        List<Long> newPackIds = singletonList(newPackInfo.getMinusKeywordPackId());

        GdUpdateContentPromotionAdGroup request = new GdUpdateContentPromotionAdGroup()
                .withUpdateItems(
                        singletonList(buildUpdateItem(videoAdGroupInfo).withLibraryMinusKeywordsIds(newPackIds)));

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(videoAdGroupId)));

        processQueryAndCheck(request, expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(videoAdGroupId));

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .isEqualToIgnoringGivenFields(videoAdGroupInfo.getAdGroup()
                        .withLibraryMinusKeywordsIds(newPackIds), "lastChange");

        softAssertions.assertAll();
    }

    @Test
    public void updateAdGroups_ContentPromotionCollection_UpdateGeo() {
        List<Integer> newGeo = singletonList((int) SAINT_PETERSBURG_REGION_ID);

        GdUpdateContentPromotionAdGroup request = new GdUpdateContentPromotionAdGroup()
                .withUpdateItems(
                        singletonList(buildUpdateItem(collectionAdGroupInfo).withRegionIds(newGeo)));

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(collectionAdGroupId)));

        processQueryAndCheck(request, expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(collectionAdGroupId));

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .isEqualToIgnoringGivenFields(collectionAdGroupInfo.getAdGroup()
                        .withStatusBsSynced(NO)
                        .withStatusShowsForecast(NEW)
                        .withGeo(mapList(newGeo, regionId -> (long) regionId)), "lastChange");

        softAssertions.assertAll();
    }

    @Test
    public void updateAdGroups_ContentPromotionVideo_UpdateAdGroupTags() {
        steps.featureSteps().addFeature(FeatureName.TARGET_TAGS_ALLOWED);
        Long featureId = steps.featureSteps().getFeatures().stream()
                .filter(f -> f.getFeatureTextId().equals(FeatureName.TARGET_TAGS_ALLOWED.getName()))
                .map(Feature::getId)
                .findFirst()
                .get();
        ClientFeature featureIdToClientId =
                new ClientFeature()
                        .withClientId(clientInfo.getClientId())
                        .withId(featureId)
                        .withState(FeatureState.ENABLED);
        steps.featureSteps().addClientFeature(featureIdToClientId);

        GdUpdateContentPromotionAdGroup request = new GdUpdateContentPromotionAdGroup()
                .withUpdateItems(
                        singletonList(buildUpdateItem(videoAdGroupInfo)
                                .withPageGroupTags(singletonList("page_group_tag"))
                                .withTargetTags(singletonList("target_tag"))));

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(videoAdGroupId)));

        processQueryAndCheck(request, expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(videoAdGroupId));

        AdGroup expectedGroup = videoAdGroupInfo.getAdGroup()
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPageGroupTags(asList("page_group_tag", "content-promotion-video"))
                .withTargetTags(asList("target_tag", "content-promotion-video"));

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .isEqualToIgnoringGivenFields(expectedGroup, "lastChange");

        softAssertions.assertAll();
    }

    @Test
    public void updateAdGroups_ContentPromotionCollection_UpdateBidModifiers() {
        BidModifierMobile defaultBidModifierMobile = createDefaultBidModifierMobile(collectionAdGroupInfo.getCampaignId())
                .withAdGroupId(collectionAdGroupId);
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

        GdUpdateContentPromotionAdGroup request = new GdUpdateContentPromotionAdGroup()
                .withUpdateItems(singletonList(buildUpdateItem(collectionAdGroupInfo)
                        .withBidModifiers(bidModifiers)));
        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(collectionAdGroupId)));

        processQueryAndCheck(request, expectedPayload);

        List<BidModifier> byAdGroupIds = bidModifierRepository
                .getByAdGroupIds(shard,
                        singletonMap(collectionAdGroupId, collectionAdGroupInfo.getCampaignId()),
                        singleton(defaultBidModifierMobile.getType()),
                        singleton(BidModifierLevel.ADGROUP));

        SoftAssertions.assertSoftly(soft -> {
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
    public void updateAdGroups_ContentPromotionVideo_UpdateKeywords() {
        String newKeyword = "новая ключевая фраза";

        GdUpdateContentPromotionAdGroup request = new GdUpdateContentPromotionAdGroup()
                .withUpdateItems(
                        singletonList(buildUpdateItem(videoAdGroupInfo)
                                .withKeywords(singletonList(new GdUpdateAdGroupKeywordItem().withPhrase(newKeyword)))));

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(videoAdGroupId)));

        processQueryAndCheck(request, expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(videoAdGroupId));

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .isEqualToIgnoringGivenFields(videoAdGroupInfo.getAdGroup()
                        .withStatusModerate(StatusModerate.READY)
                        .withStatusPostModerate(StatusPostModerate.NO)
                        .withStatusBsSynced(StatusBsSynced.NO)
                        .withStatusShowsForecast(StatusShowsForecast.NEW), "lastChange");

        Map<Long, List<Keyword>> allKeywords = keywordService
                .getKeywordsByAdGroupIds(clientInfo.getClientId(), singleton(videoAdGroupId));

        softAssertions.assertThat(allKeywords)
                .hasSize(1);
        softAssertions.assertThat(allKeywords.get(videoAdGroupId))
                .hasSize(1);
        softAssertions.assertThat(allKeywords.get(videoAdGroupId).get(0).getPhrase())
                .isEqualTo(newKeyword);

        softAssertions.assertAll();
    }

    @Test
    public void updateAdGroups_ContentPromotionCollection_UpdateRelevanceMatch() {
        GdUpdateContentPromotionAdGroup request = new GdUpdateContentPromotionAdGroup()
                .withUpdateItems(
                        singletonList(buildUpdateItem(collectionAdGroupInfo)
                                .withGeneralPrice(BigDecimal.ONE)
                                .withRelevanceMatch(new GdUpdateAdGroupRelevanceMatchItem().withIsActive(true))));

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(collectionAdGroupId)));

        processQueryAndCheck(request, expectedPayload);

        Map<Long, RelevanceMatch> relevanceMatchByAdGroupId = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(clientInfo.getShard(), clientInfo.getClientId(),
                        singletonList(collectionAdGroupId));

        softAssertions.assertThat(relevanceMatchByAdGroupId)
                .hasSize(1);

        RelevanceMatch expectedNewRelevanceMatch = new RelevanceMatch()
                .withAdGroupId(collectionAdGroupId)
                .withCampaignId(collectionAdGroupInfo.getCampaignId())
                .withIsDeleted(false)
                .withIsSuspended(false)
                .withStatusBsSynced(StatusBsSynced.NO);

        softAssertions.assertThat(relevanceMatchByAdGroupId.get(collectionAdGroupId))
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(expectedNewRelevanceMatch);

        softAssertions.assertAll();
    }

    @Test
    public void updateAdGroups_CollectionAndVideoInOneRequest() {
        GdUpdateContentPromotionAdGroup request = new GdUpdateContentPromotionAdGroup()
                .withUpdateItems(asList(buildUpdateItem(videoAdGroupInfo), buildUpdateItem(collectionAdGroupInfo)));

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(asList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(videoAdGroupId),
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(collectionAdGroupId)));

        processQueryAndCheck(request, expectedPayload);
    }

    @Test
    public void updateAdGroups_RequestWithUnsupportedType_ValidationError() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);

        GdUpdateContentPromotionAdGroup request = new GdUpdateContentPromotionAdGroup()
                .withUpdateItems(singletonList(buildUpdateItem(adGroupInfo)));

        ExecutionResult executionResult = processor.processQuery(null, getQuery(request), null, buildContext(operator));

        assertThat(executionResult.getErrors()).isNotEmpty();
    }

    private void processQueryAndCheck(GdUpdateContentPromotionAdGroup request, GdUpdateAdGroupPayload expectedPayload) {
        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(UPDATE_MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(UPDATE_MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);
    }

    private String getQuery(GdUpdateContentPromotionAdGroup request) {
        return String.format(UPDATE_MUTATION_TEMPLATE, UPDATE_MUTATION_NAME, GraphQlJsonUtils.graphQlSerialize(request));
    }

    private GdUpdateContentPromotionAdGroupItem buildUpdateItem(AdGroupInfo adGroupInfo) {
        AdGroup adGroup = adGroupInfo.getAdGroup();

        return new GdUpdateContentPromotionAdGroupItem()
                .withAdGroupId(adGroup.getId())
                .withAdGroupName(adGroup.getName())
                .withAdGroupMinusKeywords(adGroup.getMinusKeywords())
                .withLibraryMinusKeywordsIds(adGroup.getLibraryMinusKeywordsIds())
                .withRegionIds(mapList(adGroup.getGeo(), Long::intValue))
                .withRelevanceMatch(new GdUpdateAdGroupRelevanceMatchItem().withIsActive(false))
                .withBidModifiers(new GdUpdateBidModifiers())
                .withUseBidModifiers(true)
                .withKeywords(emptyList())
                .withPageGroupTags(adGroup.getPageGroupTags())
                .withTargetTags(adGroup.getTargetTags());
    }
}
