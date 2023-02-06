package ru.yandex.direct.grid.processing.service.group.mutation;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupAddItem;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupUpdateItem;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsYandexPlusAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingJoinType;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingInternalNetworkRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsYandexPlusRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingUnion;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddInternalAdGroupsItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateInternalAdGroupsItem;
import ru.yandex.direct.grid.processing.model.retargeting.GdGoalMinimal;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleItemReq;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdUpdateInternalAdRetargetingConditionItem;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroupType.INTERNAL;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ANY;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.SOCIAL_DEMO;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalWithId;
import static ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleType.OR;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.ListUtils.integerToLongList;
import static ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb.all;

public class GdInternalGroupConvertersTest {
    private static final Long DEFAULT_ID = 1L;
    private static final List<Integer> DEFAULT_GROUP_REGION_IDS = List.of(
            Long.valueOf(Region.SAINT_PETERSBURG_REGION_ID).intValue());
    private static final String DEFAULT_GROUP_NAME = "this is a group name!";
    private static final ClientId CLIENT_ID = ClientId.fromLong(501L);

    @Test
    public void toCoreUpdateItem_withoutTargetings() {
        var item = defaultUpdateItem();

        var converted = GdInternalGroupConverters.toCoreUpdateItem(CLIENT_ID, item);
        var expected = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(defaultModelChanges(item))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(emptyList());

        assertThat(converted).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void toCoreUpdateItem_withTargetings() {
        var isYandexPlusTargeting = new GdAdditionalTargetingIsYandexPlusRequest()
                .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                .withJoinType(GdAdditionalTargetingJoinType.ANY);
        var union = new GdAdditionalTargetingUnion().withTargetingIsYandexPlus(isYandexPlusTargeting);
        var item = defaultUpdateItem().withTargetings(List.of(union));

        var converted = GdInternalGroupConverters.toCoreUpdateItem(CLIENT_ID, item);

        var expectedTargeting = new IsYandexPlusAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(ANY);
        var expected = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(defaultModelChanges(item))
                .withAdditionalTargetings(List.of(expectedTargeting))
                .withRetargetingConditions(emptyList());

        assertThat(converted).is(matchedBy(beanDiffer(expected)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toCoreUpdateItem_withEmptyUnion() {
        var union = new GdAdditionalTargetingUnion();
        var item = defaultUpdateItem()
                .withTargetings(List.of(union));
        GdInternalGroupConverters.toCoreUpdateItem(CLIENT_ID, item);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toCoreUpdateItem_withInvalidUnion() {
        var isYandexPlusTargeting = new GdAdditionalTargetingIsYandexPlusRequest()
                .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                .withJoinType(GdAdditionalTargetingJoinType.ANY);
        var internalNetworkTargeting = new GdAdditionalTargetingInternalNetworkRequest()
                .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                .withJoinType(GdAdditionalTargetingJoinType.ANY);
        var union = new GdAdditionalTargetingUnion()
                .withTargetingIsYandexPlus(isYandexPlusTargeting)
                .withTargetingInternalNetwork(internalNetworkTargeting);
        var item = defaultUpdateItem()
                .withTargetings(List.of(union));
        GdInternalGroupConverters.toCoreUpdateItem(CLIENT_ID, item);
    }


    @Test
    public void toCoreUpdateItem_withRetargetingConditions() {
        var goal = defaultGoalWithId(2499000001L, SOCIAL_DEMO);
        var item = defaultUpdateItem().withRetargetingConditions(List.of(
                new GdUpdateInternalAdRetargetingConditionItem()
                        .withConditionRules(List.of(
                                new GdRetargetingConditionRuleItemReq()
                                        .withType(OR)
                                        .withInterestType(all)
                                        .withGoals(singletonList(new GdGoalMinimal()
                                                .withId(goal.getId())
                                                .withTime(goal.getTime())))
                        ))
        ));

        var converted = GdInternalGroupConverters.toCoreUpdateItem(CLIENT_ID, item);

        var expectedRetCondition = new RetargetingCondition()
                .withClientId(CLIENT_ID.asLong())
                .withName(GdInternalGroupConverters.DEFAULT_RETARGETING_CONDITION_NAME)
                .withType(ConditionType.interests)
                .withRules(singletonList(new Rule()
                        .withType(RuleType.OR)
                        .withInterestType(CryptaInterestType.all)
                        .withGoals(singletonList((Goal) new Goal().withId(goal.getId())))));

        var expected = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(defaultModelChanges(item))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(List.of(expectedRetCondition));

        assertThat(converted).is(matchedBy(
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void toCoreAddItem_withoutTargetings() {
        var item = defaultAddItem();

        var converted = GdInternalGroupConverters.toCoreAddItem(CLIENT_ID, item);
        var expected = new InternalAdGroupAddItem()
                .withAdGroup(defaultInternalAdGroup())
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(emptyList())
                .withRetargetingConditions(List.of());

        assertThat(converted).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void toCoreAddItem_withTargetings() {
        var isYandexPlusTargeting = new GdAdditionalTargetingIsYandexPlusRequest()
                .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                .withJoinType(GdAdditionalTargetingJoinType.ANY);
        var union = new GdAdditionalTargetingUnion().withTargetingIsYandexPlus(isYandexPlusTargeting);
        var item = defaultAddItem().withTargetings(List.of(union));

        var converted = GdInternalGroupConverters.toCoreAddItem(CLIENT_ID, item);

        var expectedTargeting = new IsYandexPlusAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(ANY);
        var expected = new InternalAdGroupAddItem()
                .withAdGroup(defaultInternalAdGroup())
                .withAdditionalTargetings(List.of(expectedTargeting))
                .withRetargetingConditions(emptyList());

        assertThat(converted).is(matchedBy(beanDiffer(expected)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toCoreAddItem_withEmptyUnion() {
        var union = new GdAdditionalTargetingUnion();
        var item = defaultAddItem()
                .withTargetings(List.of(union));
        GdInternalGroupConverters.toCoreAddItem(CLIENT_ID, item);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toCoreAddItem_withInvalidUnion() {
        var isYandexPlusTargeting = new GdAdditionalTargetingIsYandexPlusRequest()
                .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                .withJoinType(GdAdditionalTargetingJoinType.ANY);
        var internalNetworkTargeting = new GdAdditionalTargetingInternalNetworkRequest()
                .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                .withJoinType(GdAdditionalTargetingJoinType.ANY);
        var union = new GdAdditionalTargetingUnion()
                .withTargetingIsYandexPlus(isYandexPlusTargeting)
                .withTargetingInternalNetwork(internalNetworkTargeting);
        var item = defaultAddItem()
                .withTargetings(List.of(union));
        GdInternalGroupConverters.toCoreAddItem(CLIENT_ID, item);
    }

    @Test
    public void toCoreAddItem_withRetargetingConditions() {
        var goal = defaultGoalWithId(2499000001L, SOCIAL_DEMO);
        var item = defaultAddItem().withRetargetingConditions(List.of(
                new GdUpdateInternalAdRetargetingConditionItem()
                        .withConditionRules(List.of(
                                new GdRetargetingConditionRuleItemReq()
                                        .withType(OR)
                                        .withInterestType(all)
                                        .withGoals(singletonList(new GdGoalMinimal()
                                                .withId(goal.getId())
                                                .withTime(goal.getTime())))
                        ))
        ));

        var converted = GdInternalGroupConverters.toCoreAddItem(CLIENT_ID, item);

        var expectedRetCondition = new RetargetingCondition()
                .withClientId(CLIENT_ID.asLong())
                .withName(GdInternalGroupConverters.DEFAULT_RETARGETING_CONDITION_NAME)
                .withType(ConditionType.interests)
                .withRules(singletonList(new Rule()
                        .withType(RuleType.OR)
                        .withInterestType(CryptaInterestType.all)
                        .withGoals(singletonList((Goal) new Goal().withId(goal.getId())))));

        var expected = new InternalAdGroupAddItem()
                .withAdGroup(defaultInternalAdGroup())
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(List.of(expectedRetCondition));

        assertThat(converted).is(matchedBy(
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields())));
    }

    private static GdUpdateInternalAdGroupsItem defaultUpdateItem() {
        return new GdUpdateInternalAdGroupsItem()
                .withId(DEFAULT_ID)
                .withName(DEFAULT_GROUP_NAME)
                .withRegionIds(DEFAULT_GROUP_REGION_IDS);
    }

    private static GdAddInternalAdGroupsItem defaultAddItem() {
        return new GdAddInternalAdGroupsItem()
                .withCampaignId(DEFAULT_ID)
                .withName(DEFAULT_GROUP_NAME)
                .withRegionIds(DEFAULT_GROUP_REGION_IDS);
    }

    private static InternalAdGroup defaultInternalAdGroup() {
        return new InternalAdGroup()
                .withType(INTERNAL)
                .withCampaignId(DEFAULT_ID)
                .withName(DEFAULT_GROUP_NAME)
                .withGeo(integerToLongList(DEFAULT_GROUP_REGION_IDS));
    }

    private static ModelChanges<InternalAdGroup> defaultModelChanges(GdUpdateInternalAdGroupsItem item) {
        return new ModelChanges<>(item.getId(), InternalAdGroup.class)
                .process(item.getName(), InternalAdGroup.NAME)
                .process(item.getLevel(), InternalAdGroup.LEVEL)
                .process(item.getRf(), InternalAdGroup.RF)
                .process(item.getRfReset(), InternalAdGroup.RF_RESET)
                .process(item.getMaxClicksCount(), InternalAdGroup.MAX_CLICKS_COUNT)
                .process(item.getMaxClicksPeriod(), InternalAdGroup.MAX_CLICKS_PERIOD)
                .process(item.getMaxStopsCount(), InternalAdGroup.MAX_STOPS_COUNT)
                .process(item.getMaxStopsPeriod(), InternalAdGroup.MAX_STOPS_PERIOD)
                .process(item.getStartTime(), InternalAdGroup.START_TIME)
                .process(item.getFinishTime(), InternalAdGroup.FINISH_TIME)
                .process(integerToLongList(item.getRegionIds()), InternalAdGroup.GEO);
    }
}
