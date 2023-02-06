package ru.yandex.direct.core.entity.dynamictextadtarget.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.dynamictextadtarget.container.DynamicTextAdTargetSelectionCriteria;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTargetState;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.DynamicTextAdTargetInfo;
import ru.yandex.direct.core.testing.steps.DynamicTextAdTargetSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.naturalOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.multitype.entity.LimitOffset.limited;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicTextAdTargetRepositoryTest {

    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("price")).useDiffer(new BigDecimalDiffer())
            .forFields(newPath("priceContext")).useDiffer(new BigDecimalDiffer());

    @Autowired
    private Steps steps;

    @Autowired
    private DynamicTextAdTargetSteps dynamicTextAdTargetSteps;

    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    private AdGroupInfo adGroupInfo1;
    private AdGroupInfo adGroupInfo2;
    private ClientId clientId;
    private int shard;
    private DynamicTextAdTargetInfo dynamicTextAdTargetInfo1;
    private DynamicTextAdTargetInfo dynamicTextAdTargetInfo2;
    private List<Long> dynamicTextAdTargetIds;
    private ArrayList<Long> dynamicTextAdTargetIdsOrdered;
    private AdGroupInfo adGroupInfoAnotherClient;
    private DynamicTextAdTargetInfo dynamicTextAdTargetAnotherClient;

    @Before
    public void before() {
        adGroupInfo1 = steps.adGroupSteps().createActiveDynamicTextAdGroup();
        adGroupInfo2 = steps.adGroupSteps().createActiveDynamicTextAdGroup(adGroupInfo1.getCampaignInfo());
        clientId = adGroupInfo1.getClientId();
        shard = adGroupInfo1.getShard();

        dynamicTextAdTargetInfo1 = dynamicTextAdTargetSteps.createDefaultDynamicTextAdTarget(adGroupInfo1);
        dynamicTextAdTargetInfo2 = dynamicTextAdTargetSteps.createDefaultDynamicTextAdTarget(adGroupInfo2);

        dynamicTextAdTargetIds =
                asList(dynamicTextAdTargetInfo1.getDynamicConditionId(),
                        dynamicTextAdTargetInfo2.getDynamicConditionId());
        dynamicTextAdTargetIdsOrdered = new ArrayList<>(dynamicTextAdTargetIds);
        dynamicTextAdTargetIdsOrdered.sort(naturalOrder());
    }

    // getDynamicTextAdTargetsWithDomainType()

    @Test
    public void get_OneIdPassed_ReturnsOneCondition() {
        List<DynamicTextAdTarget> dynamicTextAdTargets =
                dynamicTextAdTargetRepository
                        .getDynamicTextAdTargetsWithDomainType(shard, clientId,
                                singletonList(dynamicTextAdTargetInfo1.getDynamicConditionId()), true,
                                LimitOffset.maxLimited());

        assertThat("вернулся один id", dynamicTextAdTargets, hasSize(1));
        assertThat(dynamicTextAdTargets.get(0),
                beanDiffer(dynamicTextAdTargetInfo1.getDynamicTextAdTarget())
                        .useCompareStrategy(COMPARE_STRATEGY));

    }

    @Test
    public void get_IdsInDifferentAdGroups_ReturnsAllConditionsInValidOrder() {
        List<DynamicTextAdTarget>
                fetchedRetConditions =
                dynamicTextAdTargetRepository
                        .getDynamicTextAdTargetsWithDomainType(shard, clientId, dynamicTextAdTargetIds, true,
                                LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(fetchedRetConditions, DynamicTextAdTarget::getDynamicConditionId);
        assertThat(fetchedIds, contains(dynamicTextAdTargetIdsOrdered.toArray()));
    }

    @Test
    public void get_OneOfPassedIdsExists_ReturnsOneCondition() {
        long existingId = dynamicTextAdTargetInfo1.getDynamicConditionId();
        List<DynamicTextAdTarget>
                fetchedRetConditions =
                dynamicTextAdTargetRepository
                        .getDynamicTextAdTargetsWithDomainType(shard, clientId, asList(existingId, 123L), true,
                                LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(fetchedRetConditions, DynamicTextAdTarget::getId);
        assertThat(fetchedIds, contains(existingId));
    }

    @Test
    public void get_OneOfPassedIdsExistsAndAnotherBelongsToOtherClient_ReturnsConditionOfPassedClient() {
        Long client1DynamicCondId = dynamicTextAdTargetIds.get(0);

        adGroupInfoAnotherClient = steps.adGroupSteps().createActiveDynamicTextAdGroup();
        dynamicTextAdTargetAnotherClient =
                dynamicTextAdTargetSteps.createDefaultDynamicTextAdTarget(adGroupInfoAnotherClient);
        Long client2DynamicCondId = dynamicTextAdTargetAnotherClient.getDynamicConditionId();

        List<Long> retCondIdsToFetch = asList(client1DynamicCondId, client2DynamicCondId);

        List<DynamicTextAdTarget>
                fetchedRetConditions =
                dynamicTextAdTargetRepository.getDynamicTextAdTargetsWithDomainType(shard, clientId, retCondIdsToFetch, true,
                        LimitOffset.maxLimited());

        List<Long> fetchedIds = mapList(fetchedRetConditions, DynamicTextAdTarget::getDynamicConditionId);
        assertThat(fetchedIds, hasSize(1));
        assertThat(fetchedIds, contains(client1DynamicCondId));
    }

    @Test
    public void get_NoneOfPassedIdsExists_ReturnsEmptyList() {
        List<DynamicTextAdTarget>
                fetchedRetConditions =
                dynamicTextAdTargetRepository.getDynamicTextAdTargetsWithDomainType(shard, clientId, asList(123L, 456L), true,
                        LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(fetchedRetConditions, DynamicTextAdTarget::getDynamicConditionId);
        assertThat(fetchedIds, emptyIterable());
    }

    @Test
    public void get_LimitIsDefined_LimitWorksFine() {
        int limit = 1;

        List<DynamicTextAdTarget> fetchedRetConditions =
                dynamicTextAdTargetRepository
                        .getDynamicTextAdTargetsWithDomainType(shard, clientId, dynamicTextAdTargetIds, true, limited(limit));

        assertThat(fetchedRetConditions, hasSize(limit));
    }

    @Test
    public void get_LimitAndOffsetAreDefined_OffsetWorksFine() {
        int offset = 1;

        List<DynamicTextAdTarget> fetchedRetConditions =
                dynamicTextAdTargetRepository
                        .getDynamicTextAdTargetsWithDomainType(shard, clientId, dynamicTextAdTargetIds, true,
                                limited(10, offset));

        assertThat(fetchedRetConditions, hasSize(dynamicTextAdTargetIds.size() - offset));
    }

    @Test
    public void get_DeletedCondition_ReturnsOneCondition() {
        Long dynamicConditionId = dynamicTextAdTargetInfo1.getDynamicConditionId();

        dynamicTextAdTargetRepository.deleteDynamicTextAdTargets(shard, singletonList(dynamicConditionId));


        List<DynamicTextAdTarget> dynamicTextAdTargets =
                dynamicTextAdTargetRepository
                        .getDynamicTextAdTargetsWithDomainType(shard, clientId,
                                singletonList(dynamicTextAdTargetInfo1.getDynamicConditionId()), true,
                                LimitOffset.maxLimited());

        assertThat("вернулся один id", dynamicTextAdTargets, hasSize(1));
        DynamicTextAdTarget actualDynamicTextAdTarget = dynamicTextAdTargets.get(0);
        assertNull(actualDynamicTextAdTarget.getId());
        assertNotNull(actualDynamicTextAdTarget.getDynamicConditionId());
    }

    // getCampaignIdToConditionIdsBySelectionCriteria()

    @Test
    public void getBySelection_ByOneId() {
        DynamicTextAdTargetSelectionCriteria selection = new DynamicTextAdTargetSelectionCriteria()
                .withConditionIds(dynamicTextAdTargetInfo1.getDynamicConditionId());

        Map<Long, List<Long>> resultMap =
                dynamicTextAdTargetRepository.getCampaignIdToConditionIdsBySelectionCriteria(shard, selection);

        Long campaignId = adGroupInfo1.getCampaignId();

        assertThat(resultMap.keySet(), hasSize(1));
        assertThat(resultMap, hasKey(campaignId));

        List<Long> ids = resultMap.get(campaignId);
        assertThat(ids, hasSize(1));
        assertThat(ids, contains(dynamicTextAdTargetInfo1.getDynamicConditionId()));
    }

    @Test
    public void getBySelection_ByTwoId() {
        DynamicTextAdTargetSelectionCriteria selection = new DynamicTextAdTargetSelectionCriteria()
                .withConditionIds(new HashSet<>(dynamicTextAdTargetIds));

        Map<Long, List<Long>> resultMap =
                dynamicTextAdTargetRepository.getCampaignIdToConditionIdsBySelectionCriteria(shard, selection);

        Long campaignId = adGroupInfo1.getCampaignId();

        assertThat(resultMap.keySet(), hasSize(1));
        assertThat(resultMap, hasKey(campaignId));

        List<Long> ids = resultMap.get(campaignId);
        assertThat(ids, hasSize(2));
        assertThat(ids, contains(dynamicTextAdTargetIds.toArray()));
    }

    @Test
    public void getBySelection_ByAdGroupId() {
        DynamicTextAdTargetSelectionCriteria selection = new DynamicTextAdTargetSelectionCriteria()
                .withAdGroupIds(adGroupInfo1.getAdGroupId());

        Map<Long, List<Long>> resultMap =
                dynamicTextAdTargetRepository.getCampaignIdToConditionIdsBySelectionCriteria(shard, selection);

        Long campaignId = adGroupInfo1.getCampaignId();

        assertThat(resultMap.keySet(), hasSize(1));
        assertThat(resultMap, hasKey(campaignId));

        List<Long> ids = resultMap.get(campaignId);
        assertThat(ids, hasSize(1));
        assertThat(ids, contains(dynamicTextAdTargetInfo1.getDynamicConditionId()));
    }

    @Test
    public void getBySelection_ByCampaignId() {
        Long campaignId = adGroupInfo1.getCampaignId();

        DynamicTextAdTargetSelectionCriteria selection = new DynamicTextAdTargetSelectionCriteria()
                .withCampaignIds(campaignId);

        Map<Long, List<Long>> resultMap =
                dynamicTextAdTargetRepository.getCampaignIdToConditionIdsBySelectionCriteria(shard, selection);

        assertThat(resultMap.keySet(), hasSize(1));
        assertThat(resultMap, hasKey(campaignId));

        List<Long> ids = resultMap.get(campaignId);
        assertThat(ids, hasSize(2));
        assertThat(ids, contains(dynamicTextAdTargetIds.toArray()));
    }

    @Test
    public void getBySelection_ByOneIdAndStateExisted() {
        DynamicTextAdTargetSelectionCriteria selection = new DynamicTextAdTargetSelectionCriteria()
                .withConditionIds(dynamicTextAdTargetInfo1.getDynamicConditionId())
                .withStates(DynamicTextAdTargetState.ON);

        Map<Long, List<Long>> resultMap =
                dynamicTextAdTargetRepository.getCampaignIdToConditionIdsBySelectionCriteria(shard, selection);

        Long campaignId = adGroupInfo1.getCampaignId();

        assertThat(resultMap.keySet(), hasSize(1));
        assertThat(resultMap, hasKey(campaignId));

        List<Long> ids = resultMap.get(campaignId);
        assertThat(ids, hasSize(1));
        assertThat(ids, contains(dynamicTextAdTargetInfo1.getDynamicConditionId()));
    }

    @Test
    public void getBySelection_ByOneIdAndSuspendedStateNotExisted() {
        DynamicTextAdTargetSelectionCriteria selection = new DynamicTextAdTargetSelectionCriteria()
                .withConditionIds(dynamicTextAdTargetInfo1.getDynamicConditionId())
                .withStates(DynamicTextAdTargetState.SUSPENDED);

        Map<Long, List<Long>> resultMap =
                dynamicTextAdTargetRepository.getCampaignIdToConditionIdsBySelectionCriteria(shard, selection);

        assertThat(resultMap.keySet(), emptyIterable());
    }

    // delete

    @Test
    public void delete_ByOneId_ConditionDeleted() {
        Long dynamicTextAdTargetId = dynamicTextAdTargetInfo1.getDynamicConditionId();
        dynamicTextAdTargetRepository.deleteDynamicTextAdTargets(shard, singletonList(dynamicTextAdTargetId));

        List<DynamicTextAdTarget> dynamicTextAdTargets = dynamicTextAdTargetRepository
                .getDynamicTextAdTargetsWithDomainType(shard, clientId, singletonList(dynamicTextAdTargetId), true,
                        LimitOffset.maxLimited());

        assertThat(dynamicTextAdTargets, hasSize(1));
        assertNull(dynamicTextAdTargets.get(0).getId());
    }

    @Test
    public void delete_AllPassedIdsExist_DeletesAllConditions() {
        dynamicTextAdTargetRepository.deleteDynamicTextAdTargets(shard, dynamicTextAdTargetIds);

        List<DynamicTextAdTarget> dynamicTextAdTargets = dynamicTextAdTargetRepository
                .getDynamicTextAdTargetsWithDomainType(shard, clientId, dynamicTextAdTargetIds, true,
                        LimitOffset.maxLimited());

        assertThat(dynamicTextAdTargets, hasSize(2));
        assertNull(dynamicTextAdTargets.get(0).getId());
        assertNull(dynamicTextAdTargets.get(1).getId());
    }

    @Test
    public void delete_OneOfPassedIdsExists_DeletesOneCondition() {
        dynamicTextAdTargetRepository.deleteDynamicTextAdTargets(shard, asList(dynamicTextAdTargetIds.get(0), 123L));

        List<DynamicTextAdTarget> dynamicTextAdTargets = dynamicTextAdTargetRepository
                .getDynamicTextAdTargetsWithDomainType(shard, clientId, dynamicTextAdTargetIds, true,
                        LimitOffset.maxLimited());

        assertThat(dynamicTextAdTargets, hasSize(2));
        assertNull(dynamicTextAdTargets.get(0).getId());
        assertThat(dynamicTextAdTargets.get(1).getId(), is(dynamicTextAdTargetIds.get(1)));
    }

    // update suspended
    @Test
    public void updateSuspended_OneIdPassed_OneTargetUpdated() {
        DynamicTextAdTarget target = this.dynamicTextAdTargetInfo1.getDynamicTextAdTarget();

        dynamicTextAdTargetRepository.updateSuspended(shard, singletonList(target.getDynamicConditionId()), true);

        List<DynamicTextAdTarget> dynamicTextAdTargets = dynamicTextAdTargetRepository
                .getDynamicTextAdTargetsWithDomainType(shard, clientId, singletonList(target.getDynamicConditionId()), true,
                        LimitOffset.maxLimited());

        assertTrue(dynamicTextAdTargets.get(0).getIsSuspended());
    }
}
