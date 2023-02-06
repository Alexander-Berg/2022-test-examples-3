package ru.yandex.direct.grid.processing.service.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupMinusKeywords;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupMinusKeywordsAction;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupMinusKeywordsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.MAX_LINKED_PACKS_TO_ONE_AD_GROUP;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupMinusKeywordsAction.ADD;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupMinusKeywordsAction.REMOVE;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.ListUtils.uniqueList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceUpdateAdGroupMinusKeywordsTest {

    private static final String MUTATION_NAME = "updateAdGroupMinusKeywords";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    updatedAdGroupIds"
            + "  }\n"
            + "}";

    private static final CompareStrategy WITHOUT_FIELDS = allFieldsExcept(newPath(AdGroup.LAST_CHANGE.name()));

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private Steps steps;

    private User operator;
    private ClientInfo clientInfo;
    private AdGroup adGroup1;
    private Long adGroupId1;
    private Long packId1;
    private Long packId2;
    private int shard;
    private List<Long> libPackIds;


    @Before
    public void initTestData() {

        clientInfo = steps.clientSteps().createDefaultClient();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        shard = clientInfo.getShard();
        adGroup1 = adGroupInfo.getAdGroup();
        adGroupId1 = adGroupInfo.getAdGroupId();
        packId1 = createLibraryPack(clientInfo);
        packId2 = createLibraryPack(clientInfo);

        libPackIds = new ArrayList<>();
        for (int i = 0; i < MAX_LINKED_PACKS_TO_ONE_AD_GROUP; i++) {
            libPackIds.add(createLibraryPack(clientInfo));
        }

        operator = UserHelper.getUser(adGroupInfo);
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void updateAdGroupMinusKeywords_AddLibraryPack_PackAdded() {
        List<Long> packsToAdd = singletonList(packId1);
        List<Long> adGroupIds = singletonList(adGroupId1);

        GdUpdateAdGroupMinusKeywordsPayload payload = updateAdGroupMinusKeywords(ADD, adGroupIds, packsToAdd);
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, adGroupIds).get(0);
        AdGroup expectedAdGroup = toExpectedAdGroup(adGroup1, packsToAdd, true);

        assertAdGroupsUpdated(actualAdGroup, expectedAdGroup, payload, adGroupIds);
    }

    @Test
    public void updateAdGroupMinusKeywords_RemoveLibraryPack_PackRemoved() {
        List<Long> packsToRemove = asList(packId1, packId2);
        List<Long> adGroupIds = singletonList(adGroupId1);
        linkToAdGroup(packId1, adGroup1);
        linkToAdGroup(packId2, adGroup1);

        GdUpdateAdGroupMinusKeywordsPayload payload = updateAdGroupMinusKeywords(REMOVE, adGroupIds, packsToRemove);
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, adGroupIds).get(0);
        AdGroup expectedAdGroup = toExpectedAdGroup(adGroup1, packsToRemove, false);

        assertAdGroupsUpdated(actualAdGroup, expectedAdGroup, payload, adGroupIds);
    }

    @Test
    public void updateAdGroupMinusKeywords_AddAlreadyAddedPack_AdGroupNotChangedAndContainsInSuccessList() {
        List<Long> packsToAdd = singletonList(packId1);
        List<Long> adGroupIds = singletonList(adGroupId1);
        linkToAdGroup(packId1, adGroup1);

        GdUpdateAdGroupMinusKeywordsPayload payload = updateAdGroupMinusKeywords(ADD, adGroupIds, packsToAdd);
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, adGroupIds).get(0);

        assertAdGroupsUpdated(actualAdGroup, adGroup1, payload, adGroupIds);
    }

    @Test
    public void updateAdGroupMinusKeywords_RemoveNotLinkedPack_AdGroupNotChangedAndContainsInSuccessList() {
        List<Long> packsToRemove = singletonList(packId1);
        List<Long> adGroupIds = singletonList(adGroupId1);

        GdUpdateAdGroupMinusKeywordsPayload payload = updateAdGroupMinusKeywords(REMOVE, adGroupIds, packsToRemove);
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, adGroupIds).get(0);

        assertAdGroupsUpdated(actualAdGroup, adGroup1, payload, adGroupIds);
    }

    @Test
    public void updateAdGroupMinusKeywords_AddMoreThanMaxPacks_AdGroupNotUpdated() {
        List<Long> packsToAdd = singletonList(packId1);

        List<Long> adGroupIds = singletonList(adGroupId1);
        libPackIds.forEach(x -> linkToAdGroup(x, adGroup1));

        GdUpdateAdGroupMinusKeywordsPayload payload = updateAdGroupMinusKeywords(ADD, adGroupIds, packsToAdd);
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, adGroupIds).get(0);

        assertAdGroupsUpdated(actualAdGroup, adGroup1, payload, emptyList());
    }

    @Test
    public void updateAdGroupMinusKeywords_RemoveNotExistingLibraryPack_OperationIsSuccessPacksNotChanged() {
        List<Long> packsToRemove = singletonList(packId2);
        List<Long> adGroupIds = singletonList(adGroupId1);
        linkToAdGroup(packId1, adGroup1);

        GdUpdateAdGroupMinusKeywordsPayload payload = updateAdGroupMinusKeywords(REMOVE, adGroupIds, packsToRemove);
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, adGroupIds).get(0);

        assertAdGroupsUpdated(actualAdGroup, adGroup1, payload, adGroupIds);
    }

    private void assertAdGroupsUpdated(AdGroup actualAdGroup, AdGroup expectedAdGroup,
                                       GdUpdateAdGroupMinusKeywordsPayload actualPayload,
                                       List<Long> expectedPayloadAdGroupIds) {
        assertSoftly(assertions -> {
            assertions.assertThat(actualPayload.getUpdatedAdGroupIds())
                    .containsOnlyElementsOf(expectedPayloadAdGroupIds);
            assertions.assertThat(actualAdGroup)
                    .is(matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(WITHOUT_FIELDS)));
        });
    }

    @Test
    public void updateAdGroupMinusKeywords_OneOfTwoAdGroupsIsNotExist_OnlyOneUpdated() {
        AdGroup otherClientAdGroup = steps.adGroupSteps().createDefaultAdGroup().getAdGroup();

        List<Long> packsToAdd = singletonList(packId1);
        List<Long> adGroupIds = asList(adGroupId1, otherClientAdGroup.getId());

        GdUpdateAdGroupMinusKeywordsPayload payload = updateAdGroupMinusKeywords(ADD, adGroupIds, packsToAdd);
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, adGroupIds);

        AdGroup expectedAdGroup = toExpectedAdGroup(adGroup1, packsToAdd, true);

        assertSoftly(assertions -> {
            assertions.assertThat(payload.getUpdatedAdGroupIds()).containsOnly(adGroupId1);
            assertions.assertThat(adGroups.get(0))
                    .is(matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(WITHOUT_FIELDS)));
            assertions.assertThat(adGroups.get(1))
                    .is(matchedBy(beanDiffer(otherClientAdGroup).useCompareStrategy(WITHOUT_FIELDS)));
        });
    }

    @Test
    public void updateAdGroupMinusKeywords_OneOfTwoAdGroupsHaveTooMuchPacks_OnlyOneUpdated() {
        AdGroup adGroupWithTooMuchPacks = steps.adGroupSteps().createDefaultAdGroup(clientInfo).getAdGroup();
        Long adGroupId2 = adGroupWithTooMuchPacks.getId();

        List<Long> packsToAdd = singletonList(packId1);
        List<Long> adGroupIds = asList(adGroupId1, adGroupId2);

        libPackIds.forEach(x -> linkToAdGroup(x, adGroupWithTooMuchPacks));


        GdUpdateAdGroupMinusKeywordsPayload payload = updateAdGroupMinusKeywords(ADD, adGroupIds, packsToAdd);
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, adGroupIds);

        AdGroup expectedAdGroup = toExpectedAdGroup(adGroup1, packsToAdd, true);

        assertSoftly(assertions -> {
            assertions.assertThat(payload.getUpdatedAdGroupIds()).containsOnly(adGroupId1);
            assertions.assertThat(adGroups.get(0))
                    .is(matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(WITHOUT_FIELDS)));
            assertions.assertThat(adGroups.get(1))
                    .is(matchedBy(beanDiffer(adGroupWithTooMuchPacks).useCompareStrategy(WITHOUT_FIELDS)));
        });
    }

    private AdGroup toExpectedAdGroup(AdGroup adGroup, List<Long> packIds, boolean isAdd) {

        List<Long> actualPackIds = adGroup.getLibraryMinusKeywordsIds();
        List<Long> newPackIds = new ArrayList<>(actualPackIds);
        if (isAdd) {
            newPackIds.addAll(packIds);
            newPackIds = uniqueList(newPackIds);
        } else {
            newPackIds.removeAll(packIds);
        }

        return adGroup
                .withStatusBsSynced(StatusBsSynced.NO)
                .withLibraryMinusKeywordsIds(newPackIds);
    }

    private void linkToAdGroup(Long packId, AdGroup adGroup) {
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(shard, packId, adGroup.getId());

        List<Long> newPackIds = new ArrayList<>(adGroup.getLibraryMinusKeywordsIds());
        newPackIds.add(packId);
        adGroup.setLibraryMinusKeywordsIds(newPackIds);
    }


    private Long createLibraryPack(ClientInfo client) {
        return steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(client).getMinusKeywordPackId();
    }

    private GdUpdateAdGroupMinusKeywordsPayload updateAdGroupMinusKeywords(
            GdUpdateAdGroupMinusKeywordsAction action, List<Long> adGroupIds, List<Long> mwIds) {
        GdUpdateAdGroupMinusKeywords request = new GdUpdateAdGroupMinusKeywords()
                .withAction(action)
                .withAdGroupIds(adGroupIds)
                .withMinusKeywordsPackIds(mwIds);

        String query = String.format(MUTATION_TEMPLATE, MUTATION_NAME, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        return GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupMinusKeywordsPayload.class);
    }

}
