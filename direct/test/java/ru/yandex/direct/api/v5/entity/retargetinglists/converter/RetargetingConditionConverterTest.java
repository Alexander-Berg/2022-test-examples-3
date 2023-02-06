package ru.yandex.direct.api.v5.entity.retargetinglists.converter;

import java.util.Collections;

import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.retargetinglists.AvailableForTargetsInAdGroupTypesArray;
import com.yandex.direct.api.v5.retargetinglists.ObjectFactory;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListGetItem;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListRuleArgumentItem;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListRuleItem;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListRuleOperatorEnum;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListScopeEnum;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListTypeEnum;
import org.junit.Test;

import ru.yandex.direct.core.entity.retargeting.container.AllowedRetargetingComponentsInUserProfile;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;

import static com.yandex.direct.api.v5.general.AdGroupTypesEnum.CPM_BANNER_AD_GROUP;
import static com.yandex.direct.api.v5.general.AdGroupTypesEnum.CPM_VIDEO_AD_GROUP;
import static com.yandex.direct.api.v5.general.AdGroupTypesEnum.MOBILE_APP_AD_GROUP;
import static com.yandex.direct.api.v5.general.AdGroupTypesEnum.TEXT_AD_GROUP;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.interests;

public class RetargetingConditionConverterTest {

    private static final long ID = 3456789;
    private static final String NAME = "some name";
    private static final String DESC = "some desc";
    private static final boolean AVAILABLE = true;
    private static final Long GOAL_ID = 345L;
    private static final Long GOAL_INTERESTS_ID = 2_499_002_000L;
    private static final Long EXTERNAL_INTERESTS_ID = 102_499_002_000L;
    private static final int GOAL_TIME = 48;

    private static final ObjectFactory FACTORY = new ObjectFactory();

    private RetargetingConditionConverter converter = new RetargetingConditionConverter();
    private AllowedRetargetingComponentsInUserProfile allowedComponents =
            new AllowedRetargetingComponentsInUserProfile();

    @Test
    public void convertFullPositiveRetCondition() {
        RetargetingCondition fullCondition = buildFullRetCondition();
        RetargetingListGetItem expectedResponse = buildExpectedGetItem();
        RetargetingListGetItem actualResponse = converter.convert(fullCondition, allowedComponents);
        assertThat(actualResponse)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
    }

    @Test
    public void convertNegativeRetCondition() {
        RetargetingCondition fullCondition = buildFullRetCondition();
        fullCondition.getRules().forEach(r -> r.setType(RuleType.NOT));

        RetargetingListGetItem expectedResponse = buildExpectedGetItem();
        expectedResponse.getRules().forEach(r -> r.setOperator(RetargetingListRuleOperatorEnum.NONE));
        expectedResponse.withScope(RetargetingListScopeEnum.FOR_ADJUSTMENTS_ONLY);
        expectedResponse.withAvailableForTargetsInAdGroupTypes(
                FACTORY.createRetargetingListGetItemAvailableForTargetsInAdGroupTypes(null));

        RetargetingListGetItem actualResponse = converter.convert(fullCondition, allowedComponents);
        assertThat(actualResponse)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
    }

    @Test
    public void convertRetConditionWithNullDescription() {
        RetargetingCondition fullCondition = buildFullRetCondition();
        fullCondition.withDescription(null);
        RetargetingListGetItem expectedResponse = buildExpectedGetItem()
                .withDescription(FACTORY.createRetargetingListBaseDescription(null));

        RetargetingListGetItem actualResponse = converter.convert(fullCondition, allowedComponents);
        assertThat(actualResponse)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
    }

    @Test
    public void convertAudienceRetCondition() {
        Goal goal = new Goal();
        goal.withId(GOAL_INTERESTS_ID);

        Rule ruleOr = new Rule();
        ruleOr.withGoals(Collections.singletonList(goal))
                .withInterestType(CryptaInterestType.short_term)
                .withType(RuleType.OR);

        RetargetingCondition interestCondition = new RetargetingCondition();
        interestCondition
                .withType(interests)
                .withId(ID)
                .withName(NAME)
                .withDescription(DESC)
                .withAvailable(AVAILABLE)
                .withRules(Collections.singletonList(ruleOr));


        RetargetingListRuleArgumentItem argument = new RetargetingListRuleArgumentItem()
                .withExternalId(EXTERNAL_INTERESTS_ID)
                .withMembershipLifeSpan(0);

        RetargetingListRuleItem itemAny = new RetargetingListRuleItem()
                .withOperator(RetargetingListRuleOperatorEnum.ANY)
                .withArguments(Collections.singletonList(argument));

        RetargetingListGetItem expectedResponse = new RetargetingListGetItem()
                .withType(RetargetingListTypeEnum.AUDIENCE)
                .withId(ID)
                .withName(NAME)
                .withDescription(FACTORY.createRetargetingListBaseDescription(DESC))
                .withIsAvailable(YesNoEnum.YES)
                .withRules(Collections.singletonList(itemAny))
                .withScope(RetargetingListScopeEnum.FOR_TARGETS_ONLY)
                .withAvailableForTargetsInAdGroupTypes(
                        FACTORY.createRetargetingListGetItemAvailableForTargetsInAdGroupTypes(
                                new AvailableForTargetsInAdGroupTypesArray()
                                        .withItems(TEXT_AD_GROUP, CPM_BANNER_AD_GROUP, CPM_VIDEO_AD_GROUP)));

        RetargetingListGetItem actualResponse = converter.convert(interestCondition, allowedComponents);
        assertThat(actualResponse).isEqualToComparingFieldByFieldRecursively(expectedResponse);
    }

    private static RetargetingCondition buildFullRetCondition() {
        Goal goal = new Goal();
        goal.withId(GOAL_ID)
                .withTime(GOAL_TIME);

        Rule ruleAll = new Rule();
        ruleAll.withGoals(Collections.singletonList(goal))
                .withType(RuleType.ALL);

        Rule ruleOr = new Rule();
        ruleOr.withGoals(Collections.singletonList(goal))
                .withType(RuleType.OR);

        Rule ruleNot = new Rule();
        ruleNot.withGoals(Collections.singletonList(goal))
                .withType(RuleType.NOT);

        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition
                .withId(ID)
                .withName(NAME)
                .withDescription(DESC)
                .withAvailable(AVAILABLE)
                .withRules(asList(ruleAll, ruleOr, ruleNot));
        return retargetingCondition;
    }

    private static RetargetingListGetItem buildExpectedGetItem() {
        RetargetingListRuleArgumentItem argument = new RetargetingListRuleArgumentItem()
                .withExternalId(GOAL_ID)
                .withMembershipLifeSpan(GOAL_TIME);

        RetargetingListRuleItem itemAll = new RetargetingListRuleItem()
                .withOperator(RetargetingListRuleOperatorEnum.ALL)
                .withArguments(Collections.singletonList(argument));

        RetargetingListRuleItem itemAny = new RetargetingListRuleItem()
                .withOperator(RetargetingListRuleOperatorEnum.ANY)
                .withArguments(Collections.singletonList(argument));

        RetargetingListRuleItem itemNone = new RetargetingListRuleItem()
                .withOperator(RetargetingListRuleOperatorEnum.NONE)
                .withArguments(Collections.singletonList(argument));

        return new RetargetingListGetItem()
                .withId(ID)
                .withName(NAME)
                .withDescription(FACTORY.createRetargetingListBaseDescription(DESC))
                .withIsAvailable(YesNoEnum.YES)
                .withRules(asList(itemAll, itemAny, itemNone))
                .withScope(RetargetingListScopeEnum.FOR_TARGETS_AND_ADJUSTMENTS)
                .withAvailableForTargetsInAdGroupTypes(
                        FACTORY.createRetargetingListGetItemAvailableForTargetsInAdGroupTypes(
                                new AvailableForTargetsInAdGroupTypesArray()
                                        .withItems(TEXT_AD_GROUP, MOBILE_APP_AD_GROUP)));
    }
}
