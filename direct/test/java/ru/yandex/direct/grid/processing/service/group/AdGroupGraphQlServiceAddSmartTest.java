package ru.yandex.direct.grid.processing.service.group;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddSmartAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddSmartAdGroupItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.CAMPAIGN_NOT_FOUND_DEFECT;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.FEED_NOT_EXIST_DEFECT;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.INCONSISTENT_AD_GROUP_TYPE_TO_CAMPAIGN_TYPE_DEFECT;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.ListUtils.integerToLongList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceAddSmartTest {

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
    PerformanceFilterRepository performanceFilterRepository;

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;

    private static final String AD_GROUP_NAME = RandomStringUtils.randomAlphanumeric(16);
    private static final Long UNEXISTING_CAMPAIGN_ID = Long.MAX_VALUE;
    private static final Long NEGATIVE_CAMPAIGN_ID = -1L;
    private static final String ADD_MUTATION_NAME = "addSmartAdGroups";
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
            + "         adGroupId,\n"
            + "     }\n"
            + "  }\n"
            + "}";

    private static final GraphQlTestExecutor.TemplateMutation<GdAddSmartAdGroup, GdAddAdGroupPayload>
            ADD_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(ADD_MUTATION_NAME, ADD_MUTATION_TEMPLATE,
                    GdAddSmartAdGroup.class, GdAddAdGroupPayload.class);

    private Integer shard;
    private ClientInfo clientInfo;
    private User operator;
    private Long campaignIdWithGeo;
    private Long campaignIdWithoutGeo;
    private FeedInfo feedInfo;
    private Set<Integer> campaignGeo = StreamEx.of(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID)
            .map(Long::intValue)
            .toSet();

    @Before
    public void before() {
        //Создаём исходные данные
        clientInfo =
                steps.clientSteps().createClient(defaultClient().withCountryRegionId(Region.KAZAKHSTAN_REGION_ID));

        shard = clientInfo.getShard();
        operator = clientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator);

        Campaign campaignWithGeo =
                TestCampaigns.activePerformanceCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withGeo(campaignGeo);
        Campaign campaignWithoutGeo =
                TestCampaigns.activePerformanceCampaign(clientInfo.getClientId(), clientInfo.getUid());

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignWithGeo, clientInfo);
        checkState(isNotEmpty(campaignInfo.getCampaign().getGeo()));
        campaignIdWithGeo = campaignInfo.getCampaignId();
        campaignInfo = steps.campaignSteps().createCampaign(campaignWithoutGeo, clientInfo);
        checkState(isEmpty(campaignInfo.getCampaign().getGeo()));
        campaignIdWithoutGeo = campaignInfo.getCampaignId();

        feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
    }

    @Test
    public void addSmartAdGroups_success_withCampaignGeo() {
        GdAddSmartAdGroup gdAddSmartAdGroup = new GdAddSmartAdGroup()
                .withAddItems(singletonList(new GdAddSmartAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignIdWithGeo)
                        .withFeedId(feedInfo.getFeedId())));

        //Ожидаемые результаты
        PerformanceAdGroup expectedAdGroup = new PerformanceAdGroup()
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignIdWithGeo)
                .withFeedId(feedInfo.getFeedId())
                .withGeo(integerToLongList(campaignGeo));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddSmartAdGroup, operator);
        validateResponseSuccessful(payload);

        checkState(payload.getAddedAdGroupItems().size() == 1);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addSmartAdGroups_success_withoutCampaignGeo() {
        GdAddSmartAdGroup gdAddSmartAdGroup = new GdAddSmartAdGroup()
                .withAddItems(singletonList(new GdAddSmartAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignIdWithoutGeo)
                        .withFeedId(feedInfo.getFeedId())));

        //Ожидаемые результаты
        PerformanceAdGroup expectedAdGroup = new PerformanceAdGroup()
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignIdWithoutGeo)
                .withFeedId(feedInfo.getFeedId())
                .withGeo(singletonList(clientInfo.getClient().getCountryRegionId()));


        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddSmartAdGroup, operator);
        validateResponseSuccessful(payload);

        checkState(payload.getAddedAdGroupItems().size() == 1);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addSmartAdGroups_failure_incorrectAdGroupData() {
        GdAddSmartAdGroup gdAddSmartAdGroup = new GdAddSmartAdGroup()
                .withAddItems(singletonList(new GdAddSmartAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(NEGATIVE_CAMPAIGN_ID)
                        .withFeedId(feedInfo.getFeedId())));
        List<GraphQLError> graphQLErrors =
                graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddSmartAdGroup, operator).getErrors();
        assertThat(graphQLErrors).isNotEmpty();
    }

    @Test
    public void addSmartAdGroups_success_checkPayload() {
        GdAddSmartAdGroup gdAddSmartAdGroup = new GdAddSmartAdGroup()
                .withAddItems(singletonList(new GdAddSmartAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignIdWithGeo)
                        .withFeedId(feedInfo.getFeedId())));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddSmartAdGroup, operator);
        validateResponseSuccessful(payload);

        checkState(payload.getAddedAdGroupItems().size() == 1);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        GdAddAdGroupPayload expectedPayload = new GdAddAdGroupPayload()
                .withAddedAdGroupItems(singletonList(new GdAddAdGroupPayloadItem().withAdGroupId(adGroupId)));
        assertThat(payload).is(
                matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void addSmartAdGroups_success_newSiteFeed() {
        FeedInfo siteFeedInfo = steps.feedSteps().createDefaultSyncedSiteFeed(clientInfo);
        steps.feedSteps().setFeedProperty(siteFeedInfo, Feed.UPDATE_STATUS, UpdateStatus.NEW);

        GdAddSmartAdGroup gdAddSmartAdGroup = new GdAddSmartAdGroup()
                .withAddItems(singletonList(new GdAddSmartAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignIdWithGeo)
                        .withFeedId(siteFeedInfo.getFeedId())));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddSmartAdGroup, operator);
        validateResponseSuccessful(payload);

        checkState(payload.getAddedAdGroupItems().size() == 1);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        GdAddAdGroupPayload expectedPayload = new GdAddAdGroupPayload()
                .withAddedAdGroupItems(singletonList(new GdAddAdGroupPayloadItem().withAdGroupId(adGroupId)));
        assertThat(payload).is(
                matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void addSmartAdGroups_partialAdd() {
        GdAddSmartAdGroup gdAddSmartAdGroup = new GdAddSmartAdGroup()
                .withAddItems(ImmutableList.of(new GdAddSmartAdGroupItem()
                                .withName(AD_GROUP_NAME)
                                .withCampaignId(campaignIdWithGeo)
                                .withFeedId(feedInfo.getFeedId()),
                        new GdAddSmartAdGroupItem()
                                .withName(AD_GROUP_NAME)
                                .withCampaignId(campaignIdWithGeo)
                                .withFeedId(UNEXISTING_CAMPAIGN_ID),
                        new GdAddSmartAdGroupItem()
                                .withName(AD_GROUP_NAME)
                                .withCampaignId(UNEXISTING_CAMPAIGN_ID)
                                .withFeedId(feedInfo.getFeedId())));

        ExecutionResult result = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddSmartAdGroup, operator);
        Map<String, Object> data = result.getData();
        GdAddAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(ADD_MUTATION_NAME), GdAddAdGroupPayload.class);

        Long adGroupId = StreamEx.of(payload.getAddedAdGroupItems())
                .nonNull()
                .findFirst()
                .map(GdAddAdGroupPayloadItem::getAdGroupId)
                .orElseThrow(() -> new IllegalStateException("Got no GdAddAdGroupPayloadItem to get adGroupId"));

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(payload.getAddedAdGroupItems()).hasSize(3);
        softAssertions.assertThat(payload.getAddedAdGroupItems()).containsAll(
                Arrays.asList(null, new GdAddAdGroupPayloadItem().withAdGroupId(adGroupId)));
        softAssertions.assertThat(payload.getValidationResult().getErrors()).hasSize(2);
        List<String> defectCodes =
                mapList(payload.getValidationResult().getErrors(), GdDefect::getCode);
        softAssertions.assertThat(defectCodes)
                .containsAll(Arrays.asList(CAMPAIGN_NOT_FOUND_DEFECT, FEED_NOT_EXIST_DEFECT));
        softAssertions.assertAll();
    }

    @Test
    public void addSmartAdGroups_failure_campaignCreatedByAnotherClient() {
        //Создаём исходные данные
        ClientInfo clientInfoAnother = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfoOfAnotherClient =
                steps.campaignSteps().createActivePerformanceCampaign(clientInfoAnother);

        GdAddSmartAdGroup gdAddSmartAdGroup = new GdAddSmartAdGroup()
                .withAddItems(singletonList(new GdAddSmartAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignInfoOfAnotherClient.getCampaignId())
                        .withFeedId(feedInfo.getFeedId())));

        ExecutionResult result = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddSmartAdGroup, operator);
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
    public void addSmartAdGroups_failure_campaignIsAnotherType() {
        //Создаём исходные данные
        CampaignInfo campaignInfoOfAnotherType =
                steps.campaignSteps().createActiveTextCampaign(clientInfo);

        GdAddSmartAdGroup gdAddSmartAdGroup = new GdAddSmartAdGroup()
                .withAddItems(singletonList(new GdAddSmartAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignInfoOfAnotherType.getCampaignId())
                        .withFeedId(feedInfo.getFeedId())));

        ExecutionResult result = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddSmartAdGroup, operator);
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
    public void addSmartAdGroups_success_withDefaultSmartFilter() {
        GdAddSmartAdGroup gdAddSmartAdGroup = new GdAddSmartAdGroup()
                .withAddItems(singletonList(new GdAddSmartAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignIdWithGeo)
                        .withFeedId(feedInfo.getFeedId())));

        //Ожидаемые результаты
        PerformanceAdGroup expectedAdGroup = new PerformanceAdGroup()
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignIdWithGeo)
                .withFeedId(feedInfo.getFeedId());
        PerformanceFilter expectedSmartFilter = new PerformanceFilter()
                .withConditions(emptyList())
                .withTargetFunnel(TargetFunnel.SAME_PRODUCTS)
                .withTab(PerformanceFilterTab.ALL_PRODUCTS)
                .withIsSuspended(false);

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddSmartAdGroup, operator);
        validateResponseSuccessful(payload);

        checkState(payload.getAddedAdGroupItems().size() == 1);

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        Map<Long, List<PerformanceFilter>> addedSmartFilters = performanceFilterRepository
                .getFiltersByAdGroupIds(shard, payload.getAddedAdGroupItems().stream()
                        .map(GdAddAdGroupPayloadItem::getAdGroupId).collect(Collectors.toList()));

        checkState(addedSmartFilters.get(adGroupId).size() == 1);

        PerformanceFilter actualSmartFilter = addedSmartFilters.get(adGroupId).get(0);

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
        assertThat(actualSmartFilter).is(
                matchedBy(beanDiffer(expectedSmartFilter).useCompareStrategy(onlyExpectedFields())));
    }
}
