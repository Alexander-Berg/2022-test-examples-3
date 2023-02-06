package ru.yandex.direct.api.v5.entity.retargetinglists.converter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.yandex.direct.api.v5.retargetinglists.AddRequest;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListAddItem;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListRuleArgumentItem;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListRuleItem;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListRuleOperatorEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;

import static com.yandex.direct.api.v5.retargetinglists.RetargetingListTypeEnum.AUDIENCE;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType.short_term;

@RunWith(Parameterized.class)
public class AddRequestConverterTest {

    private static final String NAME = "some name";
    private static final String DESC = "some desc";
    private static final long GOAL_ID = 345;
    private static final long GOAL_INTERESTS_ID = 2_499_002_000L;
    private static final Long EXTERNAL_INTERESTS_ID = 102_499_002_000L;
    private static final int GOAL_TIME = 48;

    private AddRequestConverter converter = new AddRequestConverter();

    @Parameterized.Parameter(0)
    public AddRequest sourceRequest;

    @Parameterized.Parameter(1)
    public List<RetargetingCondition> expectedList;

    @Parameterized.Parameter(2)
    @SuppressWarnings("unused")
    public String description;

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> parameters() {
        AddRequest fullReq = buildActualFullRequest();
        List<RetargetingCondition> fullList = buildExpectedFullConditionList();

        AddRequest emptyReq = new AddRequest();
        List<RetargetingCondition> emptyList = Collections.emptyList();

        AddRequest nonRulesReq = new AddRequest()
                .withRetargetingLists(new RetargetingListAddItem().withName(NAME).withDescription(DESC));

        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition.withName(NAME)
                .withType(ConditionType.metrika_goals)
                .withDescription(DESC)
                .withRules(Collections.emptyList());

        List<RetargetingCondition> nonRulesList = Collections.singletonList(retargetingCondition);

        return Arrays.asList(new Object[][]{
                {fullReq, fullList, "заполненный запрос"},
                {emptyReq, emptyList, "пустой запрос"},
                {nonRulesReq, nonRulesList, "запрос без правил"}
        });
    }

    @Test
    public void conversionValid() {
        List<RetargetingCondition> actualList = converter.convert(sourceRequest);
        assertThat(actualList).containsExactlyInAnyOrderElementsOf(expectedList);
    }

    private static AddRequest buildActualFullRequest() {
        RetargetingListRuleArgumentItem argument = new RetargetingListRuleArgumentItem()
                .withExternalId(GOAL_ID)
                .withMembershipLifeSpan(GOAL_TIME);

        RetargetingListRuleArgumentItem interestsArgument = new RetargetingListRuleArgumentItem()
                .withExternalId(EXTERNAL_INTERESTS_ID);

        RetargetingListRuleItem itemAll = new RetargetingListRuleItem()
                .withOperator(RetargetingListRuleOperatorEnum.ALL)
                .withArguments(Collections.singletonList(argument));

        RetargetingListRuleItem itemAny = new RetargetingListRuleItem()
                .withOperator(RetargetingListRuleOperatorEnum.ANY)
                .withArguments(Collections.singletonList(interestsArgument));

        RetargetingListRuleItem itemNone = new RetargetingListRuleItem()
                .withOperator(RetargetingListRuleOperatorEnum.NONE)
                .withArguments(Collections.singletonList(argument));

        RetargetingListAddItem retargetingList = new RetargetingListAddItem()
                .withType(AUDIENCE)
                .withName(NAME)
                .withDescription(DESC)
                .withRules(Arrays.asList(itemAll, itemAny, itemNone));

        return new AddRequest().withRetargetingLists(Collections.singletonList(retargetingList));
    }

    private static List<RetargetingCondition> buildExpectedFullConditionList() {
        Goal goal = new Goal();
        goal.withId(GOAL_ID)
                .withTime(GOAL_TIME);

        Goal interestsGoal = new Goal();
        interestsGoal.withId(GOAL_INTERESTS_ID);

        Rule ruleAll = new Rule();
        ruleAll.withGoals(Collections.singletonList(goal))
                .withType(RuleType.ALL);

        Rule ruleOr = new Rule();
        ruleOr.withGoals(Collections.singletonList(interestsGoal))
                .withInterestType(short_term)
                .withType(RuleType.OR);

        Rule ruleNot = new Rule();
        ruleNot.withGoals(Collections.singletonList(goal))
                .withType(RuleType.NOT);

        RetargetingCondition retCondition = new RetargetingCondition();
        retCondition.withName(NAME)
                .withType(ConditionType.interests)
                .withDescription(DESC)
                .withRules(Arrays.asList(ruleAll, ruleOr, ruleNot));

        return Collections.singletonList(retCondition);
    }
}
