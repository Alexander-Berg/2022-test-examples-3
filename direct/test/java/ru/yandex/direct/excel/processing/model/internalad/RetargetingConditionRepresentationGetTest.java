package ru.yandex.direct.excel.processing.model.internalad;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.excel.processing.service.internalad.CryptaSegmentDictionariesService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_AUDIENCE_LOWER_BOUND;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;

public class RetargetingConditionRepresentationGetTest {

    private static final CryptaSegment EXPECTED_CRYPTA = CryptaSegment.create("666", "13");
    private static final String EXPECTED_SOCIAL_NAME = "someName" + RandomStringUtils.randomAlphanumeric(7);

    private RetargetingConditionRepresentation representation;
    private Goal goalContext1;
    private Goal goalContext2;
    private Goal audienceGoal;

    @Before
    public void prepareData() {
        CryptaSegmentDictionariesService cryptaSegmentDictionariesService =
                mock(CryptaSegmentDictionariesService.class);
        when(cryptaSegmentDictionariesService.getCryptaByGoalId(anyLong()))
                .thenReturn((Goal) new Goal()
                        .withKeyword(EXPECTED_CRYPTA.getKeywordId())
                        .withKeywordValue(EXPECTED_CRYPTA.getSegmentId())
                        .withName(EXPECTED_SOCIAL_NAME));
        Long expectedGoalId = defaultGoalByType(GoalType.SOCIAL_DEMO).getId();
        when(cryptaSegmentDictionariesService.getCryptaGoalIdsByParentId(anyLong())).thenReturn(List.of(expectedGoalId));
        goalContext1 = defaultGoalByType(GoalType.GOAL);
        goalContext2 = defaultGoalByType(GoalType.GOAL);
        audienceGoal = defaultGoalByType(GoalType.AUDIENCE);
        Goal cryptaGoal = defaultGoalByType(GoalType.FAMILY);
        Goal socialGoal = (Goal) new Goal().withId(expectedGoalId).withTime(3);
        representation = new RetargetingConditionRepresentation(
                List.of(
                        //GoalContext
                        new Rule()
                                .withType(RuleType.NOT).withGoals(List.of(goalContext1, goalContext2)),
                        //Audience
                        new Rule()
                                .withType(RuleType.OR).withGoals(List.of(audienceGoal)),
                        //AudienceNot
                        new Rule()
                                .withType(RuleType.NOT).withGoals(List.of(audienceGoal)),
                        //Crypta
                        new Rule()
                                .withType(RuleType.OR).withGoals(List.of(cryptaGoal)),
                        //CryptaNot
                        new Rule()
                                .withType(RuleType.NOT).withGoals(List.of(cryptaGoal)),
                        //SocialDemoGender
                        new Rule()
                                .withType(RuleType.OR).withGoals(List.of(socialGoal))

                ), cryptaSegmentDictionariesService);
    }

    @Test
    public void getGoalContextTest() {
        var expectedResult = List.of(new Rule()
                .withType(RuleType.NOT)
                .withGoals(List.of(goalContext1, goalContext2)));
        List<Rule> gotResult = representation.getGoalContext();
        assertThat(gotResult, equalTo(expectedResult));
    }

    @Test
    public void getAudienceTest() {
        var expectedResult = List.of(audienceGoal.getId() - METRIKA_AUDIENCE_LOWER_BOUND);
        List<Long> gotResult = representation.getAudience();
        assertThat(gotResult, equalTo(expectedResult));
    }

    @Test
    public void getAudienceNotTest() {
        var expectedResult = List.of(audienceGoal.getId() - METRIKA_AUDIENCE_LOWER_BOUND);
        List<Long> gotResult = representation.getAudienceNot();
        assertThat(gotResult, equalTo(expectedResult));
    }

    @Test
    public void getCryptaTest() {
        var expectedResult = List.of(EXPECTED_CRYPTA);
        List<CryptaSegment> gotResult = representation.getCrypta();
        assertThat(gotResult, equalTo(expectedResult));
    }

    @Test
    public void getCryptaNotTest() {
        var expectedResult = List.of(EXPECTED_CRYPTA);
        List<CryptaSegment> gotResult = representation.getCryptaNot();
        assertThat(gotResult, equalTo(expectedResult));
    }

    @Test
    public void getSocialDemoGenderTest() {
        var expectedResult = List.of(EXPECTED_SOCIAL_NAME);
        List<String> gotResult = representation.getSocialDemoGender();
        assertThat(gotResult, equalTo(expectedResult));
    }

    @Test
    public void getSocialDemoAgeTest() {
        var expectedResult = List.of(EXPECTED_SOCIAL_NAME);
        List<String> gotResult = representation.getSocialDemoAge();
        assertThat(gotResult, equalTo(expectedResult));
    }

    @Test
    public void getSocialDemoIncomeTest() {
        var expectedResult = List.of(EXPECTED_SOCIAL_NAME);
        List<String> gotResult = representation.getSocialDemoIncome();
        assertThat(gotResult, equalTo(expectedResult));
    }

}
