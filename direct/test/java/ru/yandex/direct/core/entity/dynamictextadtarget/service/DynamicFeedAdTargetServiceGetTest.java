package ru.yandex.direct.core.entity.dynamictextadtarget.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.dynamictextadtarget.container.DynamicTextAdTargetSelectionCriteria;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTargetState;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.DynamicTextAdTargetSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Arrays.asList;
import static java.util.Comparator.naturalOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicFeedAdTargetServiceGetTest {
    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("price")).useDiffer(new BigDecimalDiffer())
            .forFields(newPath("priceContext")).useDiffer(new BigDecimalDiffer());

    private static final Set<DynamicTextAdTargetState> ALL_STATES =
            ImmutableSet.of(DynamicTextAdTargetState.ON,
                    DynamicTextAdTargetState.OFF,
                    DynamicTextAdTargetState.SUSPENDED,
                    DynamicTextAdTargetState.DELETED);

    @Autowired
    private Steps steps;

    @Autowired
    private DynamicTextAdTargetSteps dynamicTextAdTargetSteps;

    @Autowired
    private DynamicTextAdTargetService dynamicTextAdTargetService;

    private AdGroupInfo adGroupInfo;
    private Long operatorUid;
    private ClientId clientId;
    private DynamicFeedAdTarget dynamicFeedAdTarget;
    private List<Long> dynamicFeedAdTargetIds;
    private List<Long> dynamicFeedAdTargetsIdsOrdered;


    @Before
    public void before() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed();
        adGroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo);
        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createActiveDynamicFeedAdGroup(adGroupInfo.getCampaignInfo());
        operatorUid = adGroupInfo.getUid();
        clientId = adGroupInfo.getClientId();
        dynamicFeedAdTarget = dynamicTextAdTargetSteps.createDefaultDynamicFeedAdTarget(adGroupInfo);
        var dynamicFeedAdTarget2 = dynamicTextAdTargetSteps.createDefaultDynamicFeedAdTarget(adGroupInfo2);

        dynamicFeedAdTargetIds = asList(dynamicFeedAdTarget.getDynamicConditionId(),
                dynamicFeedAdTarget2.getDynamicConditionId());
        dynamicFeedAdTargetsIdsOrdered = new ArrayList<>(dynamicFeedAdTargetIds);
        dynamicFeedAdTargetsIdsOrdered.sort(naturalOrder());
    }

    @Test
    public void get_OneIdPassed_ReturnsOneCondition() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withConditionIds(dynamicFeedAdTarget.getDynamicConditionId());

        List<DynamicFeedAdTarget> dynamicFeedAdTargets = dynamicTextAdTargetService
                .getDynamicFeedAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());

        assertThat("вернулся один id", dynamicFeedAdTargets, hasSize(1));
        assertThat(dynamicFeedAdTargets.get(0), beanDiffer(dynamicFeedAdTarget).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void get_IdsInDifferentAdGroups_ReturnsAllConditionsInValidOrder() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withConditionIds(new HashSet<>(dynamicFeedAdTargetIds));

        List<DynamicFeedAdTarget> dynamicFeedAdTargets = dynamicTextAdTargetService
                .getDynamicFeedAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(dynamicFeedAdTargets, DynamicFeedAdTarget::getDynamicConditionId);

        assertThat(fetchedIds, contains(dynamicFeedAdTargetsIdsOrdered.toArray()));
    }

    @Test
    public void get_ByAdGroupId_ReturnsOneCondition() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withAdGroupIds(adGroupInfo.getAdGroupId());

        List<DynamicFeedAdTarget> dynamicFeedAdTargets = dynamicTextAdTargetService
                .getDynamicFeedAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());

        assertThat("вернулся один id", dynamicFeedAdTargets, hasSize(1));
        assertThat(dynamicFeedAdTargets.get(0), beanDiffer(dynamicFeedAdTarget).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void get_ByCampaignId_ReturnsAllConditionsInValidOrder() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withCampaignIds(adGroupInfo.getCampaignId());

        List<DynamicFeedAdTarget> dynamicFeedAdTargets = dynamicTextAdTargetService
                .getDynamicFeedAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(dynamicFeedAdTargets, DynamicFeedAdTarget::getDynamicConditionId);

        assertThat("вернулись все условия", dynamicFeedAdTargets, hasSize(dynamicFeedAdTargetIds.size()));
        assertThat(fetchedIds, contains(dynamicFeedAdTargetsIdsOrdered.toArray()));
    }

    @Test
    public void get_ByAllStates_ReturnsAllConditionsInValidOrder() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withStates(ALL_STATES);

        List<DynamicFeedAdTarget> dynamicFeedAdTargets = dynamicTextAdTargetService
                .getDynamicFeedAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(dynamicFeedAdTargets, DynamicFeedAdTarget::getDynamicConditionId);

        assertThat("вернулись все условия", dynamicFeedAdTargets, hasSize(dynamicFeedAdTargetIds.size()));
        assertThat(fetchedIds, contains(dynamicFeedAdTargetsIdsOrdered.toArray()));
    }

    @Test
    public void get_ByOnStates_ReturnsAllConditionsInValidOrder() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withStates(DynamicTextAdTargetState.ON);

        List<DynamicFeedAdTarget> dynamicFeedAdTargets = dynamicTextAdTargetService
                .getDynamicFeedAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(dynamicFeedAdTargets, DynamicFeedAdTarget::getDynamicConditionId);

        assertThat("вернулись все условия", dynamicFeedAdTargets, hasSize(dynamicFeedAdTargetIds.size()));
        assertThat(fetchedIds, contains(dynamicFeedAdTargetsIdsOrdered.toArray()));
    }

    @Test
    public void get_ByNonExistentState_ReturnsEmptyList() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withStates(DynamicTextAdTargetState.SUSPENDED);

        List<DynamicFeedAdTarget> dynamicFeedAdTargets = dynamicTextAdTargetService
                .getDynamicFeedAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(dynamicFeedAdTargets, DynamicFeedAdTarget::getDynamicConditionId);

        assertThat("вернулся пустой список условий", fetchedIds, emptyIterable());
    }

    @Test
    public void get_OneIdPassed_WrongClientId() {
        ClientId anotherClient = steps.clientSteps().createDefaultClient().getClientId();
        Long dynamicFeedAdTargetId = dynamicFeedAdTarget.getDynamicConditionId();
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withConditionIds(dynamicFeedAdTargetId);

        List<DynamicFeedAdTarget> dynamicFeedAdTargets = dynamicTextAdTargetService
                .getDynamicFeedAdTargets(anotherClient, operatorUid, selectionCriteria, LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(dynamicFeedAdTargets, DynamicFeedAdTarget::getDynamicConditionId);

        assertThat(fetchedIds, emptyIterable());
    }

    @Test
    public void get_OneIdPassed_WrongOperator() {
        Long dynamicFeedAdTargetId = dynamicFeedAdTarget.getDynamicConditionId();
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withConditionIds(dynamicFeedAdTargetId);

        List<DynamicFeedAdTarget> dynamicFeedAdTargets = dynamicTextAdTargetService
                .getDynamicFeedAdTargets(clientId, (long) Integer.MAX_VALUE, selectionCriteria,
                        LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(dynamicFeedAdTargets, DynamicFeedAdTarget::getDynamicConditionId);

        assertThat(fetchedIds, emptyIterable());
    }
}
