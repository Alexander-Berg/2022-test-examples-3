package ru.yandex.direct.core.entity.dynamictextadtarget.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.dynamictextadtarget.container.DynamicTextAdTargetSelectionCriteria;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetTab;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTargetState;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DynamicTextAdTargetInfo;
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
import static ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils.getHashForDynamicFeedRules;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicFeedAdTarget;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicTextAdTargetServiceGetTest {

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
    private AdGroupInfo adGroupInfo1;
    private AdGroupInfo adGroupInfo2;
    private Long operatorUid;
    private ClientId clientId;
    private DynamicTextAdTargetInfo dynamicTextAdTargetInfo1;
    private DynamicTextAdTargetInfo dynamicTextAdTargetInfo2;
    private List<Long> dynamicTextAdTargetIds;
    private List<Long> dynamicTextAdTargetsIdsOrdered;


    @Before
    public void before() {
        adGroupInfo1 = steps.adGroupSteps().createActiveDynamicTextAdGroup();
        adGroupInfo2 = steps.adGroupSteps().createActiveDynamicTextAdGroup(adGroupInfo1.getCampaignInfo());
        operatorUid = adGroupInfo1.getUid();
        clientId = adGroupInfo1.getClientId();
        dynamicTextAdTargetInfo1 = dynamicTextAdTargetSteps.createDefaultDynamicTextAdTarget(adGroupInfo1);
        dynamicTextAdTargetInfo2 = dynamicTextAdTargetSteps.createDefaultDynamicTextAdTarget(adGroupInfo2);

        dynamicTextAdTargetIds =
                asList(dynamicTextAdTargetInfo1.getDynamicConditionId(),
                        dynamicTextAdTargetInfo2.getDynamicConditionId());
        dynamicTextAdTargetsIdsOrdered = new ArrayList<>(dynamicTextAdTargetIds);
        dynamicTextAdTargetsIdsOrdered.sort(naturalOrder());
    }


    @Test
    public void get_OneIdPassed_ReturnsOneCondition() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withConditionIds(dynamicTextAdTargetInfo1.getDynamicConditionId());

        List<DynamicTextAdTarget> dynamicTextAdTargets =
                dynamicTextAdTargetService
                        .getDynamicTextAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());

        assertThat("вернулся один id", dynamicTextAdTargets, hasSize(1));
        assertThat(dynamicTextAdTargets.get(0),
                beanDiffer(dynamicTextAdTargetInfo1.getDynamicTextAdTarget())
                        .useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void get_IdsInDifferentAdGroups_ReturnsAllConditionsInValidOrder() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withConditionIds(new HashSet<>(dynamicTextAdTargetIds));

        List<DynamicTextAdTarget>
                fetchedRetConditions =
                dynamicTextAdTargetService
                        .getDynamicTextAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(fetchedRetConditions, DynamicTextAdTarget::getDynamicConditionId);
        assertThat(fetchedIds, contains(dynamicTextAdTargetsIdsOrdered.toArray()));
    }

    @Test
    public void get_ByAdGroupId_ReturnOneCondition() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withAdGroupIds(adGroupInfo1.getAdGroupId());

        List<DynamicTextAdTarget> dynamicTextAdTargets =
                dynamicTextAdTargetService
                        .getDynamicTextAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());

        assertThat("вернулся один id", dynamicTextAdTargets, hasSize(1));
        assertThat(dynamicTextAdTargets.get(0),
                beanDiffer(dynamicTextAdTargetInfo1.getDynamicTextAdTarget())
                        .useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void get_ByCampaignId_ReturnAllConditionsInValidOrder() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withCampaignIds(adGroupInfo1.getCampaignId());

        List<DynamicTextAdTarget> dynamicTextAdTargets =
                dynamicTextAdTargetService
                        .getDynamicTextAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(dynamicTextAdTargets, DynamicTextAdTarget::getDynamicConditionId);

        assertThat("вернулись все условия", dynamicTextAdTargets, hasSize(dynamicTextAdTargetIds.size()));
        assertThat(fetchedIds, contains(dynamicTextAdTargetsIdsOrdered.toArray()));
    }

    @Test
    public void get_ByAllStates_ReturnAllConditionsInValidOrder() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withStates(ALL_STATES);

        List<DynamicTextAdTarget> dynamicTextAdTargets =
                dynamicTextAdTargetService
                        .getDynamicTextAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(dynamicTextAdTargets, DynamicTextAdTarget::getDynamicConditionId);

        assertThat("вернулись все условия", dynamicTextAdTargets, hasSize(dynamicTextAdTargetIds.size()));
        assertThat(fetchedIds, contains(dynamicTextAdTargetsIdsOrdered.toArray()));
    }

    @Test
    public void get_ByOnStates_ReturnAllConditionsInValidOrder() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withStates(DynamicTextAdTargetState.ON);

        List<DynamicTextAdTarget> dynamicTextAdTargets =
                dynamicTextAdTargetService
                        .getDynamicTextAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited());
        List<Long> fetchedIds = mapList(dynamicTextAdTargets, DynamicTextAdTarget::getDynamicConditionId);

        assertThat("вернулись все условия", dynamicTextAdTargets, hasSize(dynamicTextAdTargetIds.size()));
        assertThat(fetchedIds, contains(dynamicTextAdTargetsIdsOrdered.toArray()));
    }

    @Test
    public void get_ByNonExistentState_ReturnEmptyList() {
        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withStates(DynamicTextAdTargetState.SUSPENDED);

        List<Long> fetchedIds = mapList(
                dynamicTextAdTargetService
                        .getDynamicTextAdTargets(clientId, operatorUid, selectionCriteria, LimitOffset.maxLimited()),
                DynamicTextAdTarget::getDynamicConditionId);

        assertThat("вернулся пустой список условий", fetchedIds, emptyIterable());
    }

    @Test
    public void get_OneIdPassed_WrongClientId() {
        ClientInfo anotherClient = steps.clientSteps().createDefaultClient();
        Long dynamicTextAdTargetId = dynamicTextAdTargetInfo1.getDynamicConditionId();

        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withConditionIds(dynamicTextAdTargetId);

        List<DynamicTextAdTarget> dynamicTextAdTargets =
                dynamicTextAdTargetService
                        .getDynamicTextAdTargets(anotherClient.getClientId(), operatorUid, selectionCriteria,
                                LimitOffset.maxLimited());

        List<Long> fetchedIds = mapList(dynamicTextAdTargets, DynamicTextAdTarget::getDynamicConditionId);
        assertThat(fetchedIds, emptyIterable());
    }

    @Test
    public void get_OneIdPassed_WrongOperator() {
        Long dynamicTextAdTargetId = dynamicTextAdTargetInfo1.getDynamicConditionId();

        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withConditionIds(dynamicTextAdTargetId);

        List<DynamicTextAdTarget> dynamicTextAdTargets =
                dynamicTextAdTargetService
                        .getDynamicTextAdTargets(clientId, (long) Integer.MAX_VALUE, selectionCriteria,
                                LimitOffset.maxLimited());

        List<Long> fetchedIds = mapList(dynamicTextAdTargets, DynamicTextAdTarget::getDynamicConditionId);
        assertThat(fetchedIds, emptyIterable());
    }

    @Test
    public void get_DynamicFeedAdTargetHasDefaultCondition_ReturnWithoutDefault() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup();
        dynamicTextAdTargetSteps
                .createDynamicFeedAdTarget(adGroupInfo, dynamicFeedAdTargetWithUacDefaultCondition(adGroupInfo));

        DynamicTextAdTargetSelectionCriteria selectionCriteria = new DynamicTextAdTargetSelectionCriteria()
                .withAdGroupIds(adGroupInfo.getAdGroupId());
        List<DynamicFeedAdTarget> dynamicFeedAdTargets = dynamicTextAdTargetService.getDynamicFeedAdTargets(
                adGroupInfo.getClientId(), adGroupInfo.getUid(), selectionCriteria, LimitOffset.maxLimited());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(dynamicFeedAdTargets).hasSize(1);
            softly.assertThat(dynamicFeedAdTargets.get(0).getCondition()).isEmpty();
        });
    }

    private DynamicFeedAdTarget dynamicFeedAdTargetWithUacDefaultCondition(AdGroupInfo adGroupInfo) {
        List<DynamicFeedRule> rules = List.of(
                new DynamicFeedRule<>("available", Operator.NOT_EQUALS, "false")
                        .withParsedValue(false)
        );

        return defaultDynamicFeedAdTarget(adGroupInfo)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withCondition(rules)
                .withConditionHash(getHashForDynamicFeedRules(rules))
                .withTab(DynamicAdTargetTab.ALL_PRODUCTS);
    }
}
