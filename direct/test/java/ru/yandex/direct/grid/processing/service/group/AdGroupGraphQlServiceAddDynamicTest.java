package ru.yandex.direct.grid.processing.service.group;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.DynamicAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.dynamictextadtarget.DynamicTextAdTargetTranslations;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetTab;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddDynamicAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddDynamicAdGroupItem;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.service.validation.GridDefectDefinitions.mutuallyExclusive;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.ListUtils.integerToLongList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceAddDynamicTest {

    @Autowired
    private Steps steps;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private TranslationService translationService;
    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;

    private static final String AD_GROUP_NAME = RandomStringUtils.randomAlphanumeric(16);
    private static final String DOMAIN_URL = "yandex.ru";
    private static final Set<Integer> CAMPAIGN_GEO = StreamEx.of(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID)
            .map(Long::intValue)
            .toSet();

    private static final String ADD_MUTATION_NAME = "addDynamicAdGroups";
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

    private static final GraphQlTestExecutor.TemplateMutation<GdAddDynamicAdGroup, GdAddAdGroupPayload> ADD_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(ADD_MUTATION_NAME, ADD_MUTATION_TEMPLATE,
                    GdAddDynamicAdGroup.class, GdAddAdGroupPayload.class);

    private Integer shard;
    private ClientInfo clientInfo;
    private User operator;
    private Long campaignId;


    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        Campaign campaignWithGeo = TestCampaigns.activeDynamicCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withGeo(CAMPAIGN_GEO);
        campaignId = steps.campaignSteps().createCampaign(campaignWithGeo, clientInfo).getCampaignId();
    }

    @Test
    public void addDynamicAdGroups_success_withDomainUrl() {
        GdAddDynamicAdGroup gdAddDynamicAdGroup = new GdAddDynamicAdGroup()
                .withAddItems(singletonList(new GdAddDynamicAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignId)
                        .withDomainUrl(DOMAIN_URL)));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddDynamicAdGroup, operator);
        validateResponseSuccessful(payload);
        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        DynamicAdGroup expectedAdGroup = new DynamicTextAdGroup()
                .withType(AdGroupType.DYNAMIC)
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignId)
                .withGeo(integerToLongList(CAMPAIGN_GEO))
                .withDomainUrl(DOMAIN_URL);

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addDynamicAdGroups_success_withFeedId() {
        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();

        GdAddDynamicAdGroup gdAddDynamicAdGroup = new GdAddDynamicAdGroup()
                .withAddItems(singletonList(new GdAddDynamicAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignId)
                        .withFeedId(feedId)));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddDynamicAdGroup, operator);
        validateResponseSuccessful(payload);
        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        DynamicAdGroup expectedAdGroup = new DynamicFeedAdGroup()
                .withType(AdGroupType.DYNAMIC)
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignId)
                .withGeo(integerToLongList(CAMPAIGN_GEO))
                .withFeedId(feedId);

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addDynamicAdGroups_failure_withoutDomainUrlAndFeedId() {
        GdAddDynamicAdGroup gdAddDynamicAdGroup = new GdAddDynamicAdGroup()
                .withAddItems(singletonList(new GdAddDynamicAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignId)));

        ExecutionResult executionResult = graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddDynamicAdGroup, operator);
        assertThat(executionResult.getErrors()).hasSize(1);
        List<GdValidationResult> gdValidationResults = GraphQLUtils.getGdValidationResults(executionResult.getErrors());
        assertThat(gdValidationResults).hasSize(1);

        assertThat(gdValidationResults.get(0)).is(matchedBy(hasErrorsWith(gridDefect(
                path(field(GdAddDynamicAdGroup.ADD_ITEMS), index(0)), mutuallyExclusive()))));
    }

    @Test
    public void addDynamicAdGroups_success_withFeedIdAndDefaultFilter() {
        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();

        GdAddDynamicAdGroup gdAddDynamicAdGroup = new GdAddDynamicAdGroup()
                .withAddItems(singletonList(new GdAddDynamicAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignId)
                        .withFeedId(feedId)));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddDynamicAdGroup, operator);
        validateResponseSuccessful(payload);
        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        List<DynamicAdTarget> addedDynamicFilters = dynamicTextAdTargetRepository
                .getDynamicAdTargetsByAdGroupIds(dslContextProvider.ppc(shard), clientInfo.getClientId(),
                        payload.getAddedAdGroupItems().stream()
                                .map(GdAddAdGroupPayloadItem::getAdGroupId)
                                .collect(Collectors.toList()));

        checkState(addedDynamicFilters.size() == 1);

        DynamicAdTarget actualDynamicFilter = addedDynamicFilters.get(0);

        DynamicAdGroup expectedAdGroup = new DynamicFeedAdGroup()
                .withType(AdGroupType.DYNAMIC)
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignId)
                .withGeo(integerToLongList(CAMPAIGN_GEO))
                .withFeedId(feedId);
        DynamicAdTarget expectedDynamicFilter = new DynamicFeedAdTarget()
                .withAdGroupId(actualAdGroup.getId())
                .withConditionName(translationService.translate(
                        DynamicTextAdTargetTranslations.INSTANCE.defaultFeedDynamicConditionName()))
                .withCondition(emptyList())
                .withTab(DynamicAdTargetTab.ALL_PRODUCTS);

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
        assertThat(actualDynamicFilter).is(
                matchedBy(beanDiffer(expectedDynamicFilter).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addDynamicAdGroups_success_withDomainUrlAndDefaultFilter() {
        GdAddDynamicAdGroup gdAddDynamicAdGroup = new GdAddDynamicAdGroup()
                .withAddItems(singletonList(new GdAddDynamicAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignId)
                        .withDomainUrl(DOMAIN_URL)));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddDynamicAdGroup, operator);
        validateResponseSuccessful(payload);
        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        List<DynamicAdTarget> addedDynamicFilters = dynamicTextAdTargetRepository
                .getDynamicAdTargetsByAdGroupIds(dslContextProvider.ppc(shard), clientInfo.getClientId(),
                        payload.getAddedAdGroupItems().stream()
                                .map(GdAddAdGroupPayloadItem::getAdGroupId)
                                .collect(Collectors.toList()));

        checkState(addedDynamicFilters.size() == 1);

        DynamicAdTarget actualDynamicFilter = addedDynamicFilters.get(0);

        DynamicAdGroup expectedAdGroup = new DynamicTextAdGroup()
                .withType(AdGroupType.DYNAMIC)
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignId)
                .withGeo(integerToLongList(CAMPAIGN_GEO))
                .withDomainUrl(DOMAIN_URL);
        DynamicAdTarget expectedDynamicFilter = new DynamicTextAdTarget()
                .withAdGroupId(actualAdGroup.getId())
                .withConditionName(translationService.translate(
                        DynamicTextAdTargetTranslations.INSTANCE.defaultWebpageDynamicConditionName()));

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
        assertThat(actualDynamicFilter).is(
                matchedBy(beanDiffer(expectedDynamicFilter).useCompareStrategy(onlyExpectedFields())));
    }
}
