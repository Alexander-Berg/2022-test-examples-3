package ru.yandex.direct.grid.processing.service.group.contentpromotion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
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
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobile;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobileAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddContentPromotionAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddContentPromotionAdGroupItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdContentPromotionGroupType;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupKeywordItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRelevanceMatchItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.BigDecimalComparator.BIG_DECIMAL_COMPARATOR;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroupType.CONTENT_PROMOTION;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.COLLECTION;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeContentPromotionCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.CAMPAIGN_NOT_FOUND_DEFECT;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.CONTENT_PROMOTION_DISTINCT_TYPE_FROM_EXISTING;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.CONTENT_PROMOTION_SEVERAL_TYPES_NOT_ALLOWED;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.INCONSISTENT_AD_GROUP_TYPE_TO_CAMPAIGN_TYPE_DEFECT;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.ListUtils.integerToLongList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceAddContentPromotionTest {

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    protected GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;

    @Autowired
    private BidModifierRepository bidModifierRepository;

    @Autowired
    private KeywordService keywordService;

    @Autowired
    private RelevanceMatchRepository relevanceMatchRepository;

    private static final String AD_GROUP_NAME = RandomStringUtils.randomAlphanumeric(16);
    private static final Long UNEXISTING_CAMPAIGN_ID = Long.MAX_VALUE;
    private static final Long NEGATIVE_CAMPAIGN_ID = -1L;
    private static final String ADD_MUTATION_NAME = "addContentPromotionAdGroups";
    private static final String ADD_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    addedAdGroupItems {\n"
            + "         adGroupId\n"
            + "     }\n"
            + "  }\n"
            + "}";

    private static final GraphQlTestExecutor.TemplateMutation<GdAddContentPromotionAdGroup, GdAddAdGroupPayload>
            ADD_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(ADD_MUTATION_NAME, ADD_MUTATION_TEMPLATE,
                    GdAddContentPromotionAdGroup.class, GdAddAdGroupPayload.class);

    private Integer shard;
    private ClientInfo clientInfo;
    private User operator;
    private Long campaignIdWithGeo;
    private Long campaignIdWithoutGeo;
    private Set<Integer> campaignGeo = StreamEx.of(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID)
            .map(Long::intValue)
            .toSet();

    private AdGroup defaultContentPromotionVideoAdGroup;
    private AdGroup defaultContentPromotionCollectionAdGroup;

    @Before
    public void before() {
        clientInfo =
                steps.clientSteps().createClient(defaultClient().withCountryRegionId(Region.KAZAKHSTAN_REGION_ID));

        shard = clientInfo.getShard();
        operator = clientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator);

        Campaign campaignWithGeo = activeContentPromotionCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withGeo(campaignGeo);
        Campaign campaignWithoutGeo = activeContentPromotionCampaign(clientInfo.getClientId(), clientInfo.getUid());

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignWithGeo, clientInfo);
        checkState(isNotEmpty(campaignInfo.getCampaign().getGeo()));
        campaignIdWithGeo = campaignInfo.getCampaignId();
        campaignInfo = steps.campaignSteps().createCampaign(campaignWithoutGeo, clientInfo);
        checkState(isEmpty(campaignInfo.getCampaign().getGeo()));
        campaignIdWithoutGeo = campaignInfo.getCampaignId();

        defaultContentPromotionVideoAdGroup = steps.adGroupSteps().createDefaultContentPromotionAdGroup(VIDEO)
                .getAdGroup();
        defaultContentPromotionCollectionAdGroup = steps.adGroupSteps().createDefaultContentPromotionAdGroup(COLLECTION)
                .getAdGroup();
    }

    @Test
    public void addContentPromotionAdGroups_ContentPromotionVideo_CampaignWithGeo_Success() {
        GdAddContentPromotionAdGroup gdAddContentPromotionAdGroup = new GdAddContentPromotionAdGroup()
                .withAddItems(singletonList(buildDefaultAddItem(defaultContentPromotionVideoAdGroup)
                        .withName(AD_GROUP_NAME)
                        .withContentPromotionGroupType(GdContentPromotionGroupType.VIDEO)
                        .withCampaignId(campaignIdWithGeo)));
        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddContentPromotionAdGroup, operator);
        validateResponseSuccessful(payload);

        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        ContentPromotionAdGroup expectedAdGroup = new ContentPromotionAdGroup()
                .withType(CONTENT_PROMOTION)
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignIdWithGeo)
                .withGeo(asList(225L, 977L))
                .withContentPromotionType(VIDEO);

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addContentPromotionAdGroups_ContentPromotionCollection_CampaignWithoutGeo_Success() {
        GdAddContentPromotionAdGroup gdAddContentPromotionAdGroup = new GdAddContentPromotionAdGroup()
                .withAddItems(singletonList(buildDefaultAddItem(defaultContentPromotionCollectionAdGroup)
                        .withName(AD_GROUP_NAME)
                        .withContentPromotionGroupType(GdContentPromotionGroupType.COLLECTION)
                        .withCampaignId(campaignIdWithoutGeo)));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddContentPromotionAdGroup, operator);
        validateResponseSuccessful(payload);

        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        ContentPromotionAdGroup expectedAdGroup = new ContentPromotionAdGroup()
                .withType(CONTENT_PROMOTION)
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignIdWithoutGeo)
                .withContentPromotionType(ContentPromotionAdgroupType.COLLECTION)
                .withGeo(asList(225L, 977L));

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addContentPromotionAdGroups_CheckSuccessPayload() {
        GdAddContentPromotionAdGroup gdAddContentPromotionAdGroup = new GdAddContentPromotionAdGroup()
                .withAddItems(singletonList(buildDefaultAddItem(defaultContentPromotionVideoAdGroup)
                        .withName(AD_GROUP_NAME)
                        .withContentPromotionGroupType(GdContentPromotionGroupType.VIDEO)
                        .withCampaignId(campaignIdWithoutGeo)));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddContentPromotionAdGroup, operator);
        validateResponseSuccessful(payload);

        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        GdAddAdGroupPayload expectedPayload = new GdAddAdGroupPayload()
                .withAddedAdGroupItems(singletonList(new GdAddAdGroupPayloadItem().withAdGroupId(adGroupId)));
        assertThat(payload).is(
                matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void addContentPromotionAdGroups_PartialAdd() {
        GdAddContentPromotionAdGroup gdAddContentPromotionAdGroup = new GdAddContentPromotionAdGroup()
                .withAddItems(List.of(
                        buildDefaultAddItem(defaultContentPromotionCollectionAdGroup)
                                .withName(AD_GROUP_NAME)
                                .withContentPromotionGroupType(GdContentPromotionGroupType.COLLECTION)
                                .withCampaignId(campaignIdWithGeo),
                        buildDefaultAddItem(defaultContentPromotionCollectionAdGroup)
                                .withName(AD_GROUP_NAME)
                                .withContentPromotionGroupType(GdContentPromotionGroupType.COLLECTION)
                                .withCampaignId(UNEXISTING_CAMPAIGN_ID)));

        ExecutionResult result = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddContentPromotionAdGroup, operator);
        Map<String, Object> data = result.getData();
        GdAddAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(ADD_MUTATION_NAME), GdAddAdGroupPayload.class);

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(payload.getAddedAdGroupItems()).hasSize(2);
        softAssertions.assertThat(payload.getAddedAdGroupItems()).containsAll(asList(null, null));
        softAssertions.assertThat(payload.getValidationResult().getErrors()).hasSize(1);
        softAssertions.assertThat(payload.getValidationResult().getErrors().get(0).getCode())
                .isEqualTo(CAMPAIGN_NOT_FOUND_DEFECT);
        softAssertions.assertAll();
    }

    @Test
    public void addContentPromotionAdGroups_InvalidCampaignId_ValidationError() {
        GdAddContentPromotionAdGroup gdAddContentPromotionAdGroup = new GdAddContentPromotionAdGroup()
                .withAddItems(singletonList(buildDefaultAddItem(defaultContentPromotionCollectionAdGroup)
                        .withName(AD_GROUP_NAME)
                        .withContentPromotionGroupType(GdContentPromotionGroupType.COLLECTION)
                        .withCampaignId(NEGATIVE_CAMPAIGN_ID)));

        ExecutionResult executionResult =
                graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddContentPromotionAdGroup, operator);
        assertThat(executionResult.getErrors()).isNotEmpty();
    }

    @Test
    public void addContentPromotionAdGroups_NoContentPromotionType_ValidationError() {
        GdAddContentPromotionAdGroup gdAddContentPromotionAdGroup = new GdAddContentPromotionAdGroup()
                .withAddItems(singletonList(buildDefaultAddItem(defaultContentPromotionVideoAdGroup)
                        .withName(AD_GROUP_NAME)
                        .withContentPromotionGroupType(null)
                        .withCampaignId(campaignIdWithGeo)));

        ExecutionResult executionResult = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddContentPromotionAdGroup,
                operator);
        assertThat(executionResult.getErrors()).isNotEmpty();
    }

    @Test
    public void addContentPromotionAdGroups_CampaignCreatedByAnotherClient_ValidationError() {
        ClientInfo clientInfoAnother = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfoOfAnotherClient =
                steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfoAnother);

        GdAddContentPromotionAdGroup gdAddContentPromotionAdGroup = new GdAddContentPromotionAdGroup()
                .withAddItems(singletonList(buildDefaultAddItem(defaultContentPromotionCollectionAdGroup)
                        .withName(AD_GROUP_NAME)
                        .withContentPromotionGroupType(GdContentPromotionGroupType.COLLECTION)
                        .withCampaignId(campaignInfoOfAnotherClient.getCampaignId())));

        ExecutionResult result = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddContentPromotionAdGroup, operator);
        Map<String, Object> data = result.getData();
        GdAddAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(ADD_MUTATION_NAME), GdAddAdGroupPayload.class);

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(payload.getAddedAdGroupItems()).hasSize(1);
        softAssertions.assertThat(payload.getAddedAdGroupItems()).containsExactlyElementsOf(singletonList(null));
        softAssertions.assertThat(payload.getValidationResult().getErrors()).hasSize(1);
        softAssertions.assertThat(payload.getValidationResult().getErrors().get(0).getCode())
                .isEqualTo(CAMPAIGN_NOT_FOUND_DEFECT);
        softAssertions.assertAll();
    }

    @Test
    public void addContentPromotionAdGroups_CampaignWithAnotherType_ValidationError() {
        CampaignInfo campaignInfoOfAnotherType =
                steps.campaignSteps().createActivePerformanceCampaign(clientInfo);

        GdAddContentPromotionAdGroup gdAddContentPromotionAdGroup = new GdAddContentPromotionAdGroup()
                .withAddItems(singletonList(buildDefaultAddItem(defaultContentPromotionVideoAdGroup)
                        .withName(AD_GROUP_NAME)
                        .withContentPromotionGroupType(GdContentPromotionGroupType.VIDEO)
                        .withCampaignId(campaignInfoOfAnotherType.getCampaignId())));

        ExecutionResult result = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddContentPromotionAdGroup, operator);
        Map<String, Object> data = result.getData();
        GdAddAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(ADD_MUTATION_NAME), GdAddAdGroupPayload.class);

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(payload.getAddedAdGroupItems()).hasSize(1);
        softAssertions.assertThat(payload.getAddedAdGroupItems()).containsExactlyElementsOf(singletonList(null));
        softAssertions.assertThat(payload.getValidationResult().getErrors()).hasSize(1);
        softAssertions.assertThat(payload.getValidationResult().getErrors().get(0).getCode())
                .isEqualTo(INCONSISTENT_AD_GROUP_TYPE_TO_CAMPAIGN_TYPE_DEFECT);
        softAssertions.assertAll();
    }

    @Test
    public void addContentPromotionAdGroups_SeveralContentPromotionTypesInOneRequest_ValidationError() {
        GdAddContentPromotionAdGroup gdAddContentPromotionAdGroup = new GdAddContentPromotionAdGroup()
                .withAddItems(List.of(
                        buildDefaultAddItem(defaultContentPromotionCollectionAdGroup)
                                .withName(AD_GROUP_NAME)
                                .withContentPromotionGroupType(GdContentPromotionGroupType.COLLECTION)
                                .withCampaignId(campaignIdWithGeo),
                        buildDefaultAddItem(defaultContentPromotionVideoAdGroup)
                                .withName(AD_GROUP_NAME)
                                .withContentPromotionGroupType(GdContentPromotionGroupType.VIDEO)
                                .withCampaignId(campaignIdWithGeo)));

        ExecutionResult result = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddContentPromotionAdGroup, operator);
        Map<String, Object> data = result.getData();
        GdAddAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(ADD_MUTATION_NAME), GdAddAdGroupPayload.class);

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(payload.getAddedAdGroupItems()).hasSize(2);
        softAssertions.assertThat(payload.getAddedAdGroupItems()).containsExactlyElementsOf(asList(null, null));
        softAssertions.assertThat(payload.getValidationResult().getErrors()).hasSize(2);
        softAssertions.assertThat(payload.getValidationResult().getErrors().get(0).getCode())
                .isEqualTo(CONTENT_PROMOTION_SEVERAL_TYPES_NOT_ALLOWED);
        softAssertions.assertThat(payload.getValidationResult().getErrors().get(1).getCode())
                .isEqualTo(CONTENT_PROMOTION_SEVERAL_TYPES_NOT_ALLOWED);
        softAssertions.assertAll();
    }

    @Test
    public void addContentPromotionAdGroups_SeveralContentPromotionTypesInOneCampaign_ValidationError() {
        GdAddContentPromotionAdGroup gdAddContentPromotionAdGroup = new GdAddContentPromotionAdGroup()
                .withAddItems(singletonList(buildDefaultAddItem(defaultContentPromotionVideoAdGroup)
                        .withName(AD_GROUP_NAME)
                        .withContentPromotionGroupType(GdContentPromotionGroupType.VIDEO)
                        .withCampaignId(campaignIdWithGeo)));
        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddContentPromotionAdGroup, operator);
        validateResponseSuccessful(payload);

        gdAddContentPromotionAdGroup = new GdAddContentPromotionAdGroup()
                .withAddItems(singletonList(buildDefaultAddItem(defaultContentPromotionCollectionAdGroup)
                        .withName(AD_GROUP_NAME)
                        .withContentPromotionGroupType(GdContentPromotionGroupType.COLLECTION)
                        .withCampaignId(campaignIdWithGeo)));
        ExecutionResult result = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddContentPromotionAdGroup, operator);
        Map<String, Object> data = result.getData();
        payload = GraphQlJsonUtils.convertValue(data.get(ADD_MUTATION_NAME), GdAddAdGroupPayload.class);

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(payload.getAddedAdGroupItems()).hasSize(1);
        softAssertions.assertThat(payload.getAddedAdGroupItems()).containsExactlyElementsOf(singletonList(null));
        softAssertions.assertThat(payload.getValidationResult().getErrors()).hasSize(1);
        softAssertions.assertThat(payload.getValidationResult().getErrors().get(0).getCode())
                .isEqualTo(CONTENT_PROMOTION_DISTINCT_TYPE_FROM_EXISTING);
        softAssertions.assertAll();
    }

    @Test
    public void addContentPromotionAdGroups_FullVideoAdGroup_AdGroupSavedCorrectly() {
        checkAddFullAdGroup(defaultContentPromotionVideoAdGroup, GdContentPromotionGroupType.VIDEO,
                "content-promotion-video");
    }

    @Test
    public void addContentPromotionAdGroups_FullCollectionAdGroup_AdGroupSavedCorrectly() {
        checkAddFullAdGroup(defaultContentPromotionCollectionAdGroup, GdContentPromotionGroupType.COLLECTION,
                "content-promotion-collection");
    }

    private void checkAddFullAdGroup(AdGroup baseAdGroup, GdContentPromotionGroupType contentPromotionGroupType,
                                     String additionalTag) {
        List<String> minusWords = asList("минус", "слово");

        MinusKeywordsPackInfo packInfo = steps.minusKeywordsPackSteps().createMinusKeywordsPack(clientInfo);
        List<Long> packIds = singletonList(packInfo.getMinusKeywordPackId());

        List<Integer> geo = singletonList((int) SAINT_PETERSBURG_REGION_ID);

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

        BidModifierMobile defaultBidModifierMobile = createDefaultBidModifierMobile(campaignIdWithGeo);
        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers()
                .withBidModifierMobile(new GdUpdateBidModifierMobile()
                        .withAdGroupId(defaultBidModifierMobile.getAdGroupId())
                        .withCampaignId(defaultBidModifierMobile.getCampaignId())
                        .withAdjustment(new GdUpdateBidModifierMobileAdjustmentItem()
                                .withPercent((defaultBidModifierMobile)
                                        .getMobileAdjustment().getPercent()))
                        .withEnabled(defaultBidModifierMobile.getEnabled())
                        .withType(GdBidModifierType.valueOf(defaultBidModifierMobile.getType().name())));

        String keyword = "новая ключевая фраза";
        String adGroupName = "название";

        GdAddContentPromotionAdGroup gdAddContentPromotionAdGroup = new GdAddContentPromotionAdGroup()
                .withAddItems(singletonList(buildDefaultAddItem(baseAdGroup)
                        .withName(adGroupName)
                        .withContentPromotionGroupType(contentPromotionGroupType)
                        .withCampaignId(campaignIdWithGeo)
                        .withAdGroupMinusKeywords(minusWords)
                        .withLibraryMinusKeywordsIds(packIds)
                        .withRegionIds(geo)
                        .withPageGroupTags(singletonList("page_group_tag"))
                        .withTargetTags(singletonList("target_tag"))
                        .withBidModifiers(bidModifiers)
                        .withKeywords(singletonList(new GdUpdateAdGroupKeywordItem().withPhrase(keyword)))
                        .withGeneralPrice(BigDecimal.ONE)
                        .withRelevanceMatch(new GdUpdateAdGroupRelevanceMatchItem().withIsActive(true))));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddContentPromotionAdGroup, operator);
        validateResponseSuccessful(payload);

        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        ContentPromotionAdGroup expectedAdGroup = new ContentPromotionAdGroup()
                .withType(CONTENT_PROMOTION)
                .withName(adGroupName)
                .withCampaignId(campaignIdWithGeo)
                .withGeo(integerToLongList(geo))
                .withContentPromotionType(ContentPromotionAdgroupType.valueOf(contentPromotionGroupType.name()))
                .withPageGroupTags(asList("page_group_tag", additionalTag))
                .withTargetTags(asList("target_tag", additionalTag))
                .withMinusKeywords(minusWords)
                .withLibraryMinusKeywordsIds(packIds);

        // проверка полей на группе
        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));

        // проверка корректировок
        List<BidModifier> byAdGroupIds = bidModifierRepository
                .getByAdGroupIds(shard,
                        singletonMap(adGroupId, campaignIdWithGeo),
                        singleton(defaultBidModifierMobile.getType()),
                        singleton(BidModifierLevel.ADGROUP));

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(byAdGroupIds).hasSize(1);
            softAssertions.assertThat(byAdGroupIds.get(0).getType()).isEqualTo(defaultBidModifierMobile.getType());
            softAssertions.assertThat(byAdGroupIds.get(0).getCampaignId()).isEqualTo(campaignIdWithGeo);
            softAssertions.assertThat(byAdGroupIds.get(0).getAdGroupId()).isEqualTo(adGroupId);
            softAssertions.assertThat(byAdGroupIds.get(0).getEnabled()).isEqualTo(defaultBidModifierMobile.getEnabled());
            softAssertions.assertThat(((BidModifierMobile) (byAdGroupIds.get(0))).getMobileAdjustment().getPercent())
                    .isEqualTo((defaultBidModifierMobile).getMobileAdjustment().getPercent());
        });

        // проверка ключевых фраз
        Map<Long, List<Keyword>> allKeywords = keywordService
                .getKeywordsByAdGroupIds(clientInfo.getClientId(), singleton(adGroupId));

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(allKeywords)
                    .hasSize(1);
            softAssertions.assertThat(allKeywords.get(adGroupId))
                    .hasSize(1);
            softAssertions.assertThat(allKeywords.get(adGroupId).get(0).getPhrase())
                    .isEqualTo(keyword);
        });

        // проверка автотаргетингов
        Map<Long, RelevanceMatch> relevanceMatchByAdGroupId = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(clientInfo.getShard(), clientInfo.getClientId(),
                        singletonList(adGroupId));

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(relevanceMatchByAdGroupId)
                    .hasSize(1);

            RelevanceMatch expectedNewRelevanceMatch = new RelevanceMatch()
                    .withAdGroupId(adGroupId)
                    .withCampaignId(campaignIdWithGeo)
                    .withIsDeleted(false)
                    .withIsSuspended(false)
                    .withStatusBsSynced(StatusBsSynced.NO);

            softAssertions.assertThat(relevanceMatchByAdGroupId.get(adGroupId))
                    .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                    .isEqualToIgnoringNullFields(expectedNewRelevanceMatch);
        });
    }

    private GdAddContentPromotionAdGroupItem buildDefaultAddItem(AdGroup adGroup) {
        return new GdAddContentPromotionAdGroupItem()
                .withContentPromotionGroupType(GdContentPromotionGroupType.VIDEO)
                .withName(adGroup.getName())
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
