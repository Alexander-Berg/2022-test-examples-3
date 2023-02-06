package ru.yandex.direct.excel.processing.model.internalad;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.core.entity.retargeting.Constants;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.excel.processing.exception.ExcelValidationException;
import ru.yandex.direct.excel.processing.service.internalad.CryptaSegmentDictionariesService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_AUDIENCE_LOWER_BOUND;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.excel.processing.validation.defects.ExcelDefectIds.SOCIAL_DEMO_GOAL_NOT_FOUND;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

public class RetargetingConditionRepresentationSetTest {

    private static final Long SOCIAL_GOAL_ID = defaultGoalByType(GoalType.SOCIAL_DEMO).getId();
    private static final Long AUDIENCE_GOAL_ID = defaultGoalByType(GoalType.AUDIENCE).getId();
    private static final Long CRYPTA_GOAL_ID = defaultGoalByType(GoalType.FAMILY).getId();
    private static final CryptaSegment EXPECTED_CRYPTA = CryptaSegment.create("666", "13");
    private static final String EXPECTED_SOCIAL_NAME = "bla bla";
    private static final String EXPECTED_KEYWORD = EXPECTED_CRYPTA.getKeywordId();
    private static final String EXPECTED_KEYWORD_VALUE = EXPECTED_CRYPTA.getSegmentId();

    private CryptaSegmentDictionariesService cryptaSegmentDictionariesService;
    private RetargetingConditionRepresentation representation;

    @org.junit.Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void prepareData() {
        cryptaSegmentDictionariesService = mock(CryptaSegmentDictionariesService.class);
        when(cryptaSegmentDictionariesService.getCryptaByGoalId(anyLong()))
                .thenReturn((Goal) new Goal().withKeyword(EXPECTED_KEYWORD).withKeywordValue(EXPECTED_KEYWORD_VALUE));
        when(cryptaSegmentDictionariesService.getCryptaGoalIdsByParentId(anyLong()))
                .thenReturn(List.of(SOCIAL_GOAL_ID));
        when(cryptaSegmentDictionariesService.getGoalIdByKeywordAndType(EXPECTED_KEYWORD, EXPECTED_KEYWORD_VALUE))
                .thenReturn((Goal) new Goal().withId(CRYPTA_GOAL_ID)
                        .withKeyword(EXPECTED_KEYWORD)
                        .withKeywordValue(EXPECTED_KEYWORD_VALUE));
        when(cryptaSegmentDictionariesService.getGoalIdByParentIdAndName(anyLong(), anyString()))
                .thenReturn((Goal) new Goal().withId(SOCIAL_GOAL_ID).withName(EXPECTED_SOCIAL_NAME));
        representation = new RetargetingConditionRepresentation(cryptaSegmentDictionariesService);
    }

    @Test
    public void setGoalContextTest() {
        List<Rule> expectedResult = List.of(
                new Rule()
                        .withType(RuleType.NOT)
                        .withGoals(List.of(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))));
        representation.setGoalContext(expectedResult);
        check(expectedResult);
    }

    @Test
    public void setAudienceTest() {
        List<Rule> expectedResult = List.of(
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(List.of((Goal) new Goal()
                                .withTime(Constants.AUDIENCE_TIME_VALUE)
                                .withId(AUDIENCE_GOAL_ID))));
        representation.setAudience(List.of(AUDIENCE_GOAL_ID - METRIKA_AUDIENCE_LOWER_BOUND));
        check(expectedResult);
    }

    @Test
    public void setAudienceNotTest() {
        List<Rule> expectedResult = List.of(
                new Rule()
                        .withType(RuleType.NOT)
                        .withGoals(List.of((Goal) new Goal()
                                .withTime(Constants.AUDIENCE_TIME_VALUE)
                                .withId(AUDIENCE_GOAL_ID))));
        representation.setAudienceNot(List.of(AUDIENCE_GOAL_ID - METRIKA_AUDIENCE_LOWER_BOUND));
        check(expectedResult);
    }

    @Test
    public void setCryptaTest() {
        List<Rule> expectedResult = List.of(
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(List.of((Goal) new Goal().withId(CRYPTA_GOAL_ID).withTime(3))));
        representation.setCrypta(List.of(EXPECTED_CRYPTA));
        check(expectedResult);
    }

    @Test
    public void setCryptaNotTest() {
        List<Rule> expectedResult = List.of(
                new Rule()
                        .withType(RuleType.NOT)
                        .withGoals(List.of((Goal) new Goal().withId(CRYPTA_GOAL_ID).withTime(3))));
        representation.setCryptaNot(List.of(EXPECTED_CRYPTA));
        check(expectedResult);
    }

    @Test
    public void setSocialDemoGenderTest() {
        List<Rule> expectedResult = List.of(
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(List.of((Goal) new Goal().withId(SOCIAL_GOAL_ID).withTime(3))));
        representation.setSocialDemoGender(List.of(EXPECTED_SOCIAL_NAME));
        check(expectedResult);
    }

    @Test
    public void setSocialDemoAgeTest() {
        List<Rule> expectedResult = List.of(
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(List.of((Goal) new Goal().withId(SOCIAL_GOAL_ID).withTime(3))));
        representation.setSocialDemoAge(List.of(EXPECTED_SOCIAL_NAME));
        check(expectedResult);
    }

    @Test
    public void setSocialDemoIncomeTest() {
        List<Rule> expectedResult = List.of(
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(List.of((Goal) new Goal().withId(SOCIAL_GOAL_ID).withTime(3))));
        representation.setSocialDemoIncome(List.of(EXPECTED_SOCIAL_NAME));
        check(expectedResult);
    }

    @Test
    public void setSocialDemoIncome_IgnoreValueCaseTest() {
        List<Rule> expectedResult = List.of(
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(List.of((Goal) new Goal().withId(SOCIAL_GOAL_ID).withTime(3))));
        representation.setSocialDemoIncome(List.of(EXPECTED_SOCIAL_NAME.toUpperCase()));
        check(expectedResult);
    }

    @Test
    public void setSocialDemoIncome_InvalidValue_GotExcelValidationExceptionTest() {
        List<String> values = List.of("invalid value");
        when(cryptaSegmentDictionariesService.getGoalIdByParentIdAndName(anyLong(), eq(values.get(0))))
                .thenReturn(null);
        var expectedValidationException = ExcelValidationException.create(SOCIAL_DEMO_GOAL_NOT_FOUND, values);
        thrown.expect(equalTo(expectedValidationException));

        representation.setSocialDemoIncome(values);
    }

    private void check(List<Rule> expectedResult) {
        RetargetingCondition retargetingCondition = representation.getRetargetingCondition();
        assumeThat("rules not null", retargetingCondition.getRules().size(), is(1));
        assertThat(retargetingCondition.getRules(), equalTo(expectedResult));
    }

}
