package ru.yandex.direct.api.v5.entity.retargetinglists.validation;

import java.util.Collections;

import com.yandex.direct.api.v5.retargetinglists.AddRequest;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListAddItem;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListRuleArgumentItem;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListRuleItem;
import com.yandex.direct.api.v5.retargetinglists.RetargetingListRuleOperatorEnum;
import org.junit.Test;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static ru.yandex.direct.api.v5.entity.retargetinglists.Constants.MAX_RET_CONDITIONS_PER_REQUEST;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class AddUpdateRequestValidatorTest {
    private static final String NAME = "some name";
    private static final String DESC = "some desc";
    private static final long SHORT_TERM_GOAL_ID_1 = 102_499_002_000L;
    private static final long LONG_TERM_GOAL_ID = 202_499_002_000L;
    private static final long SHORT_TERM_GOAL_ID_2 = 102_499_002_001L;

    @Test
    public void validateRequest_tooMany() {
        ValidationResult<AddRequest, DefectType> result = AddUpdateRequestValidator.validateAddRequest(
                new AddRequest().withRetargetingLists(
                        Collections.nCopies(MAX_RET_CONDITIONS_PER_REQUEST + 1,
                                new RetargetingListAddItem().withName(NAME).withDescription(DESC)
                        )));

        assertThat(result).is(matchedBy(hasDefectWith(validationError(path(field("RetargetingLists")), 9300))));
    }

    @Test
    @Description("Ошибка при передаче целей-интересов разной продолжительности в одном условии")
    public void validateRequest_differentInterestTerms() {

        //Префикс 10 соответствует короткому интересу
        RetargetingListRuleArgumentItem shortInterest =
                new RetargetingListRuleArgumentItem().withExternalId(SHORT_TERM_GOAL_ID_1);

        //Префикс 20 соответствует длинному интересу
        RetargetingListRuleArgumentItem longInterest =
                new RetargetingListRuleArgumentItem().withExternalId(LONG_TERM_GOAL_ID);

        RetargetingListRuleItem rule = new RetargetingListRuleItem()
                .withOperator(RetargetingListRuleOperatorEnum.ANY)
                .withArguments(asList(shortInterest, longInterest));

        AddRequest addRequest = new AddRequest().withRetargetingLists(
                new RetargetingListAddItem().withName(NAME).withDescription(DESC)
                        .withRules(rule));

        ValidationResult<AddRequest, DefectType> result = AddUpdateRequestValidator.validateAddRequest(addRequest);

        assertThat(result).is(matchedBy(hasDefectWith(
                validationError(path(field("RetargetingLists"), index(0), field("Rules"), index(0)), 8000))));
    }

    @Test
    @Description("Не должно быть ошибки при передаче целей-интересов одинаковой продолжительности")
    public void validateRequest_sameInterestTerms() {

        //Префикс 10 соответствует короткому интересу
        RetargetingListRuleArgumentItem shortInterest =
                new RetargetingListRuleArgumentItem().withExternalId(SHORT_TERM_GOAL_ID_1);

        RetargetingListRuleArgumentItem longInterest =
                new RetargetingListRuleArgumentItem().withExternalId(SHORT_TERM_GOAL_ID_2);

        RetargetingListRuleItem rule = new RetargetingListRuleItem()
                .withOperator(RetargetingListRuleOperatorEnum.ANY)
                .withArguments(asList(shortInterest, longInterest));

        AddRequest addRequest = new AddRequest().withRetargetingLists(
                new RetargetingListAddItem().withName(NAME).withDescription(DESC)
                        .withRules(rule));

        ValidationResult<AddRequest, DefectType> result = AddUpdateRequestValidator.validateAddRequest(addRequest);
        //noinspection ConstantConditions
        assertFalse(result.hasAnyErrors());
    }

}
