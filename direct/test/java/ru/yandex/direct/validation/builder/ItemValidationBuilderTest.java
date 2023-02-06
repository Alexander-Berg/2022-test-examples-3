package ru.yandex.direct.validation.builder;

import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

import ru.yandex.direct.validation.result.PathHelper;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ItemValidationBuilderTest {

    private static final Object VALUE = new Object();
    private static final String DEFECT = "defect";
    private static final String OVERRIDE_DEFECT = "overriding defect";
    private static final Constraint<Object, String> PASSING_CONSTRAINT = t -> null;
    private static final Constraint<Object, String> FAILING_CONSTRAINT = t -> DEFECT;
    private static final When<Object, String> POSITIVE_WHEN = When.isTrue(true);
    private static final When<Object, String> NEGATIVE_WHEN = When.isTrue(false);

    public static class Bean {
        private String str;
        private List<String> strList;

        public String getStr() {
            return str;
        }

        public List<String> getStrList() {
            return strList;
        }
    }

    // item, list

    @Test
    public void item_CreatesSubResultWithPassedPath() {
        Bean bean = new Bean();
        ItemValidationBuilder<Bean, Object> v = ItemValidationBuilder.of(bean);
        v.item(bean.getStr(), "str");

        ValidationResult<Bean, Object> result = v.getResult();
        ValidationResult<?, Object> subResult = result.getSubResults().get(PathHelper.field("str"));
        assertThat("при вызове ItemValidationBuilder.item создался дочерний результат по указанному пути",
                subResult, notNullValue());
    }

    @Test
    public void list_CreatesSubResultWithPassedPath() {
        Bean bean = new Bean();
        ItemValidationBuilder<Bean, Object> v = ItemValidationBuilder.of(bean);
        v.list(bean.getStrList(), "strList");

        ValidationResult<Bean, Object> result = v.getResult();
        ValidationResult<?, Object> subResult = result.getSubResults().get(PathHelper.field("strList"));
        assertThat("при вызове ItemValidationBuilder.list создался дочерний результат по указанному пути",
                subResult, notNullValue());
    }

    // check

    @Test
    public void check_CheckPasses_DoesNotAddErrors() {
        checkWithNoExpectedDefects(vb -> vb.check(PASSING_CONSTRAINT));
    }

    @Test
    public void check_CheckFails_AddsError() {
        checkWithExpectedError(vb -> vb.check(FAILING_CONSTRAINT), DEFECT);
    }

    @Test
    public void check_CheckPassesWithOverrideDefect_DoesNotAddErrors() {
        checkWithNoExpectedDefects(vb -> vb.check(PASSING_CONSTRAINT, OVERRIDE_DEFECT));
    }

    @Test
    public void check_CheckFailsWithOverrideDefect_AddsOverrideError() {
        checkWithExpectedError(vb -> vb.check(FAILING_CONSTRAINT, OVERRIDE_DEFECT), OVERRIDE_DEFECT);
    }

    @Test
    public void check_WhenIsNegative_CheckIsNotExecuted() {
        checkWithNoExpectedDefects(vb -> vb.check(FAILING_CONSTRAINT, NEGATIVE_WHEN));
    }

    @Test
    public void check_WhenIsPositive_CheckIsExecuted() {
        checkWithExpectedError(vb -> vb.check(FAILING_CONSTRAINT, POSITIVE_WHEN), DEFECT);
    }

    @Test
    public void check_WhenIsNegativeWithOverrideDefect_CheckIsNotExecuted() {
        checkWithNoExpectedDefects(vb -> vb.check(FAILING_CONSTRAINT, OVERRIDE_DEFECT, NEGATIVE_WHEN));
    }

    @Test
    public void check_WhenIsPositiveWithOverrideDefect_CheckExecutedAndOverrideDefectAdded() {
        checkWithExpectedError(vb -> vb.check(FAILING_CONSTRAINT, OVERRIDE_DEFECT, POSITIVE_WHEN), OVERRIDE_DEFECT);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void check_TwoCallsWithFails_TwoErrorsAdded() {
        String secondError = "second err";
        ItemValidationBuilder<Object, String> vb = ItemValidationBuilder.of(VALUE);
        vb.check(FAILING_CONSTRAINT)
                .check(t -> secondError);
        assertThat(vb.getResult().getWarnings(), hasSize(0));
        assertThat(vb.getResult().getErrors(), contains(is(DEFECT), is(secondError)));
    }

    // weakCheck

    @Test
    public void weakCheck_CheckPasses_DoesNotAddErrors() {
        checkWithNoExpectedDefects(vb -> vb.weakCheck(PASSING_CONSTRAINT));
    }

    @Test
    public void weakCheck_CheckFails_AddsError() {
        checkWithExpectedWarning(vb -> vb.weakCheck(FAILING_CONSTRAINT), DEFECT);
    }

    @Test
    public void weakCheck_CheckPassesWithOverrideDefect_DoesNotAddErrors() {
        checkWithNoExpectedDefects(vb -> vb.weakCheck(PASSING_CONSTRAINT, OVERRIDE_DEFECT));
    }

    @Test
    public void weakCheck_CheckFailsWithOverrideDefect_AddsOverrideError() {
        checkWithExpectedWarning(vb -> vb.weakCheck(FAILING_CONSTRAINT, OVERRIDE_DEFECT), OVERRIDE_DEFECT);
    }

    @Test
    public void weakCheck_WhenIsNegative_CheckIsNotExecuted() {
        checkWithNoExpectedDefects(vb -> vb.weakCheck(FAILING_CONSTRAINT, NEGATIVE_WHEN));
    }

    @Test
    public void weakCheck_WhenIsPositive_CheckIsExecuted() {
        checkWithExpectedWarning(vb -> vb.weakCheck(FAILING_CONSTRAINT, POSITIVE_WHEN), DEFECT);
    }

    @Test
    public void weakCheck_WhenIsNegativeWithOverrideDefect_CheckIsNotExecuted() {
        checkWithNoExpectedDefects(vb -> vb.weakCheck(FAILING_CONSTRAINT, OVERRIDE_DEFECT, NEGATIVE_WHEN));
    }

    @Test
    public void weakCheck_WhenIsPositiveWithOverrideDefect_CheckExecutedAndOverrideDefectAdded() {
        checkWithExpectedWarning(vb -> vb.weakCheck(FAILING_CONSTRAINT, OVERRIDE_DEFECT, POSITIVE_WHEN),
                OVERRIDE_DEFECT);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void weakCheck_TwoCallsWithFails_TwoErrorsAdded() {
        String secondDefect = "second defect";
        ItemValidationBuilder<Object, String> vb = ItemValidationBuilder.of(VALUE);
        vb.weakCheck(FAILING_CONSTRAINT)
                .weakCheck(t -> secondDefect);
        assertThat(vb.getResult().getErrors(), hasSize(0));
        assertThat(vb.getResult().getWarnings(), contains(is(DEFECT), is(secondDefect)));
    }

    // checkBy

    @Test
    @SuppressWarnings("unchecked")
    public void checkBy_HasNotErrorsAndWarnings_WorksFine() {
        checkWithExpectedErrorsAndWarnings(
                vb -> vb.checkBy(validator(null, null)),
                null, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkBy_HasErrorsAndWarnings_MergesErrorsAndWarnings() {
        String error = "error";
        String warning = "warning";
        String internalError = "internal error";
        String internalWarning = "internal warning";

        ValidationResult<Object, String> vr = new ValidationResult<>(VALUE);
        vr.addError(error);
        vr.addWarning(warning);

        ItemValidationBuilder<Object, String> vb = new ItemValidationBuilder<>(vr);
        vb.checkBy(validator(internalError, internalWarning));

        assertThat(vb.getResult().getErrors(),
                containsInAnyOrder(is(error), is(internalError)));
        assertThat(vb.getResult().getWarnings(),
                containsInAnyOrder(is(warning), is(internalWarning)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkBy_WhenIsNegative_ValidationIsNotExecuted() {
        String internalError = "internal error";
        String internalWarning = "internal warning";
        checkWithExpectedErrorsAndWarnings(
                vb -> vb.checkBy(validator(internalError, internalWarning), NEGATIVE_WHEN),
                null, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkBy_WhenIsPositive_ValidationIsExecuted() {
        String internalError = "internal error";
        String internalWarning = "internal warning";
        checkWithExpectedErrorsAndWarnings(
                vb -> vb.checkBy(validator(internalError, internalWarning), POSITIVE_WHEN),
                internalError, internalWarning);
    }

    private void checkWithNoExpectedDefects(Consumer<ItemValidationBuilder<Object, String>> checkCaller) {
        checkWithExpectedErrorsAndWarnings(checkCaller, null, null);
    }

    private void checkWithExpectedError(Consumer<ItemValidationBuilder<Object, String>> checkCaller,
                                        String expectedError) {
        checkWithExpectedErrorsAndWarnings(checkCaller, expectedError, null);
    }

    private void checkWithExpectedWarning(Consumer<ItemValidationBuilder<Object, String>> checkCaller,
                                          String expectedWarning) {
        checkWithExpectedErrorsAndWarnings(checkCaller, null, expectedWarning);
    }

    private void checkWithExpectedErrorsAndWarnings(
            Consumer<ItemValidationBuilder<Object, String>> checkCaller,
            String expectedError, String expectedWarning) {
        ItemValidationBuilder<Object, String> vb = ItemValidationBuilder.of(VALUE);

        checkCaller.accept(vb);

        if (expectedError != null) {
            assertThat(vb.getResult().getErrors(), contains(is(expectedError)));
        } else {
            assertThat(vb.getResult().getErrors(), emptyIterable());
        }

        if (expectedWarning != null) {
            assertThat(vb.getResult().getWarnings(), contains(is(expectedWarning)));
        } else {
            assertThat(vb.getResult().getWarnings(), emptyIterable());
        }
    }

    private Validator<Object, String> validator(String error, String warning) {
        return t -> {
            ValidationResult<Object, String> internalVr = new ValidationResult<>(t);
            if (error != null) {
                internalVr.addError(error);
            }
            if (warning != null) {
                internalVr.addWarning(warning);
            }
            return internalVr;
        };
    }
}
