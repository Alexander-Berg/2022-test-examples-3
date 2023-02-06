package ru.yandex.direct.grid.processing.service.group;

import java.util.List;
import java.util.Set;

import graphql.GraphQLError;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroupItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroupItem;
import ru.yandex.direct.grid.processing.model.retargeting.GdGoalMinimal;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleItemReq;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleType;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupNameCantBeEmpty;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoEmptyRegions;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRule;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
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
public class AdGroupGraphQlServiceAddTextTest {

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
    private GraphQlTestExecutor graphQlTestExecutor;

    private static final String AD_GROUP_NAME = RandomStringUtils.randomAlphanumeric(16);
    private static final Long NEGATIVE_CAMPAIGN_ID = -1L;
    private static final String ADD_MUTATION_NAME = "addTextAdGroups";
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

    private static final GraphQlTestExecutor.TemplateMutation<GdAddTextAdGroup, GdAddAdGroupPayload>
            ADD_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(ADD_MUTATION_NAME, ADD_MUTATION_TEMPLATE,
                    GdAddTextAdGroup.class, GdAddAdGroupPayload.class);

    private Integer shard;
    private User operator;
    private Long campaignIdWithGeo;
    private final Set<Integer> campaignGeo = StreamEx.of(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID)
            .map(Long::intValue)
            .toSet();
    private List<String> minusKeywords;

    @Before
    public void before() {
        ClientInfo clientInfo =
                steps.clientSteps().createClient(defaultClient().withCountryRegionId(Region.KAZAKHSTAN_REGION_ID));

        shard = clientInfo.getShard();
        operator = clientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator);

        Campaign campaignWithGeo = TestCampaigns.activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                .withGeo(campaignGeo);
        Campaign campaignWithoutGeo = TestCampaigns.activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid());

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignWithGeo, clientInfo);
        checkState(isNotEmpty(campaignInfo.getCampaign().getGeo()));
        campaignIdWithGeo = campaignInfo.getCampaignId();
        campaignInfo = steps.campaignSteps().createCampaign(campaignWithoutGeo, clientInfo);
        checkState(isEmpty(campaignInfo.getCampaign().getGeo()));
        minusKeywords = List.of(RandomStringUtils.randomAlphabetic(10));
    }

    @Test
    public void addTextAdGroups_failure_incorrectAdGroupData() {
        GdAddTextAdGroup gdAddTextAdGroup = new GdAddTextAdGroup()
                .withAddItems(singletonList(new GdAddTextAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(NEGATIVE_CAMPAIGN_ID)));
        List<GraphQLError> graphQLErrors =
                graphQlTestExecutor.doMutation(ADD_MUTATION, gdAddTextAdGroup, operator).getErrors();
        assertThat(graphQLErrors).isNotEmpty();
    }

    @Test
    public void addTextAdGroups_success() {
        GdAddTextAdGroup gdAddTextAdGroup = new GdAddTextAdGroup()
                .withAddItems(singletonList(new GdAddTextAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withAdGroupMinusKeywords(minusKeywords)
                        .withRegionIds(List.of((int) SAINT_PETERSBURG_REGION_ID, (int) MOSCOW_REGION_ID))
                        .withBidModifiers(new GdUpdateBidModifiers())
                        .withLibraryMinusKeywordsIds(emptyList())
                        .withCampaignId(campaignIdWithGeo)));
        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddTextAdGroup, operator);
        validateResponseSuccessful(payload);

        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        TextAdGroup expectedAdGroup = new TextAdGroup()
                .withType(AdGroupType.BASE)
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignIdWithGeo)
                .withGeo(integerToLongList(campaignGeo))
                .withMinusKeywords(minusKeywords);

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addTextAdGroups_WithNonExistentCampaignId_ReturnsValidationError() {
        GdAddTextAdGroup gdAddTextAdGroup = new GdAddTextAdGroup()
                .withAddItems(singletonList(new GdAddTextAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withAdGroupMinusKeywords(minusKeywords)
                        .withRegionIds(List.of((int) SAINT_PETERSBURG_REGION_ID, (int) MOSCOW_REGION_ID))
                        .withBidModifiers(new GdUpdateBidModifiers())
                        .withLibraryMinusKeywordsIds(emptyList())
                        .withCampaignId(Long.MAX_VALUE)));
        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddTextAdGroup, operator);
        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddTextAdGroup.ADD_ITEMS.name()), index(0)),
                CampaignDefects.campaignNotFound())
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addTextAdGroups_WithoutRegionIds_ReturnsValidationError() {
        GdAddTextAdGroup gdAddTextAdGroup = new GdAddTextAdGroup()
                .withAddItems(singletonList(new GdAddTextAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withAdGroupMinusKeywords(minusKeywords)
                        .withRegionIds(emptyList())
                        .withBidModifiers(new GdUpdateBidModifiers())
                        .withLibraryMinusKeywordsIds(emptyList())
                        .withCampaignId(campaignIdWithGeo)));
        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddTextAdGroup, operator);
        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddTextAdGroup.ADD_ITEMS.name()), index(0),
                        field(GdAddTextAdGroupItem.REGION_IDS.name())), geoEmptyRegions())
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addTextAdGroups_WithEmptyName_ReturnsValidationError() {
        GdAddTextAdGroup gdAddTextAdGroup = new GdAddTextAdGroup()
                .withAddItems(singletonList(new GdAddTextAdGroupItem()
                        .withName("")
                        .withAdGroupMinusKeywords(minusKeywords)
                        .withRegionIds(List.of((int) SAINT_PETERSBURG_REGION_ID, (int) MOSCOW_REGION_ID))
                        .withBidModifiers(new GdUpdateBidModifiers())
                        .withLibraryMinusKeywordsIds(emptyList())
                        .withCampaignId(campaignIdWithGeo)));
        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddTextAdGroup, operator);
        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddTextAdGroup.ADD_ITEMS.name()), index(0),
                        field(GdUpdateTextAdGroupItem.AD_GROUP_NAME.name())), adGroupNameCantBeEmpty())
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addTextAdGroups_success_withContentCategories() {
        GdRetargetingConditionRuleItemReq rule = new GdRetargetingConditionRuleItemReq()
                .withGoals(singletonList(new GdGoalMinimal().withId(4_294_968_296L))) // Авто
                .withType(GdRetargetingConditionRuleType.OR);

        var regionIds = List.of(225);
        GdAddTextAdGroup gdAddTextAdGroup = new GdAddTextAdGroup()
                .withAddItems(singletonList(new GdAddTextAdGroupItem()
                        .withName(AD_GROUP_NAME)
                        .withCampaignId(campaignIdWithGeo)
                        .withAdGroupMinusKeywords(emptyList())
                        .withLibraryMinusKeywordsIds(emptyList())
                        .withRegionIds(regionIds)
                        .withBidModifiers(new GdUpdateBidModifiers())
                        .withContentCategoriesRetargetingConditionRules(singletonList(rule))));

        GdAddAdGroupPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddTextAdGroup, operator);
        validateResponseSuccessful(payload);

        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        List<Goal> contentCategories = singletonList((Goal) new Goal().withId(4_294_968_296L));
        TextAdGroup expectedAdGroup = new TextAdGroup()
                .withType(AdGroupType.BASE)
                .withName(AD_GROUP_NAME)
                .withCampaignId(campaignIdWithGeo)
                .withMinusKeywords(emptyList())
                .withLibraryMinusKeywordsIds(emptyList())
                .withContentCategoriesRetargetingConditionRules(singletonList(defaultRule(contentCategories)));

        assertThat(actualAdGroup).is(
                matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
    }
}
