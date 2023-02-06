package ru.yandex.direct.validation.result;

import java.util.Map;

import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class ValidationResultTest {

    private static final PathNode PATH1 = new PathNode.Field("sub1");
    private static final PathNode PATH2 = new PathNode.Field("sub2");

    // constructor

    @Test
    public void constructor_ValueIsNull_WorksFine() {
        new ValidationResult<>((Object) null);
    }

    // getValue

    @Test
    public void getValue_InitialState_Value() {
        // not cached object
        Object obj = new Object();
        assertThat(new ValidationResult<>(obj).getValue(), sameInstance(obj));
    }

    // getErrors

    @Test
    public void getErrors_InitialState_EmptyList() {
        assertThat(new ValidationResult<>(1).getErrors(), emptyIterable());
    }

    @Test
    public void getErrors_HasOneError_OneError() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        Object error = new Object();
        validationResult.addError(error);
        assertThat(validationResult.getErrors(), contains(error));
    }

    @Test
    public void getErrors_HasTwoErrors_TwoErrors() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        Object error1 = new Object();
        Object error2 = new Object();
        validationResult.addError(error1);
        validationResult.addError(error2);
        assertThat(validationResult.getErrors(), contains(error1, error2));
    }

    @Test
    public void getErrors_HasOneErrorAndNoErrorsInSubPath_OneError() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        validationResult.getOrCreateSubValidationResult(PATH1, 12345);
        Object error = new Object();
        validationResult.addError(error);
        assertThat(validationResult.getErrors(), contains(error));
    }

    @Test
    public void getErrors_HasOneErrorAndOneErrorInSubPath_OneError() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        ValidationResult<Integer, Object> subValidationResult = validationResult
                .getOrCreateSubValidationResult(PATH1, 12345);
        Object error1 = new Object();
        Object error2 = new Object();
        validationResult.addError(error1);
        subValidationResult.addError(error2);
        assertThat(validationResult.getErrors(), contains(error1));
    }

    // getWarnings

    @Test
    public void getWarnings_InitialState_EmptyList() {
        assertThat(new ValidationResult<>(1).getWarnings(), emptyIterable());
    }

    @Test
    public void getWarning_HasOneWarning_OneWarning() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        Object warn = new Object();
        validationResult.addWarning(warn);
        assertThat(validationResult.getWarnings(), contains(warn));
    }

    // getSubResults

    @Test
    public void getSubResults_InitialState_NotNull() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        assertThat(validationResult.getSubResults(), notNullValue());
    }

    @Test
    public void getSubResults_OneSubResult_OneNode() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        validationResult.getOrCreateSubValidationResult(PATH1, 12345);
        assertThat(validationResult.getSubResults().size(), is(1));
    }

    @Test
    public void getSubResults_TwoSubResults_TwoNodes() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        validationResult.getOrCreateSubValidationResult(PATH1, 12345);
        validationResult.getOrCreateSubValidationResult(PATH2, 1);
        assertThat(validationResult.getSubResults().size(), is(2));
    }

    // hasAnyErrors

    @Test
    public void hasAnyErrors_HasNoErrors_False() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        assertThat(validationResult.hasAnyErrors(), is(false));
    }

    @Test
    public void hasAnyErrors_HasOneError_True() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        validationResult.addError(new Object());
        assertThat(validationResult.hasAnyErrors(), is(true));
    }

    @Test
    public void hasAnyErrors_HasSubResultButNoErrors_False() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        validationResult.getOrCreateSubValidationResult(PATH1, 1);
        assertThat(validationResult.hasAnyErrors(), is(false));
    }

    @Test
    public void hasAnyErrors_HasSubResultWithError_True() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        ValidationResult<Integer, Object> subValidationResult =
                validationResult.getOrCreateSubValidationResult(PATH1, 1);

        subValidationResult.addError(new Object());

        assertThat(validationResult.hasAnyErrors(), is(true));
    }

    @Test
    public void hasAnyErrors_HasSubResultsWithoutAndWithError_True() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        validationResult.getOrCreateSubValidationResult(PATH1, 1);
        ValidationResult<Integer, Object> subValidationResult2 =
                validationResult.getOrCreateSubValidationResult(PATH2, 2);

        subValidationResult2.addError(new Object());

        assertThat(validationResult.hasAnyErrors(), is(true));
    }

    @Test
    public void hasAnyErrors_HasErrorAndSubResultsWithoutAndWithError_True() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        validationResult.getOrCreateSubValidationResult(PATH1, 1);
        ValidationResult<Integer, Object> subValidationResult2 =
                validationResult.getOrCreateSubValidationResult(PATH2, 2);

        validationResult.addError(new Object());
        subValidationResult2.addError(new Object());

        assertThat(validationResult.hasAnyErrors(), is(true));
    }

    @Test
    public void hasAnyErrors_HasSubResultAtLevel2WithError_True() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        ValidationResult<Integer, Object> subValidationResultLevel1 =
                validationResult.getOrCreateSubValidationResult(new PathNode.Field("subLevel1"), 1);
        ValidationResult<Integer, Object> subValidationResultLevel2 =
                subValidationResultLevel1.getOrCreateSubValidationResult(new PathNode.Field("subLevel2"), 2);

        subValidationResultLevel2.addError(new Object());

        assertThat(validationResult.hasAnyErrors(), is(true));
    }

    // getOrCreateSubValidationResult

    @Test
    public void getOrCreateSubValidationResult_CreatesValidationResultWithPassedValue() {
        Object value = new Object();
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        ValidationResult<Object, Object> subValidationResult =
                validationResult.getOrCreateSubValidationResult(PATH1, value);
        assertThat(subValidationResult.getValue(), sameInstance(value));
    }

    @Test
    public void getOrCreateSubValidationResult_InitialState_CreatesOne() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        ValidationResult<Integer, Object> subValidationResult =
                validationResult.getOrCreateSubValidationResult(PATH1, 1);

        Map<PathNode, ValidationResult<?, Object>> subResults = validationResult.getSubResults();
        assertThat(subResults.get(PATH1), sameInstance(subValidationResult));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getOrCreateSubValidationResult_CallTwiceWithDifferentPaths_CreatesTwo() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        ValidationResult<Integer, Object> subValidationResult1 =
                validationResult.getOrCreateSubValidationResult(PATH1, 1);
        ValidationResult<Integer, Object> subValidationResult2 =
                validationResult.getOrCreateSubValidationResult(PATH2, 2);

        Map<PathNode, ValidationResult<?, Object>> subResults = validationResult.getSubResults();
        assertThat(subResults.values(),
                containsInAnyOrder(sameInstance(subValidationResult1), sameInstance(subValidationResult2)));
    }

    @Test
    public void getOrCreateSubValidationResult_CallTwiceWithEqualPaths_DoesNotCreateNewResult() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        validationResult.getOrCreateSubValidationResult(PATH1, 1);
        validationResult.getOrCreateSubValidationResult(PATH1, 1);

        Map<PathNode, ValidationResult<?, Object>> subResults = validationResult.getSubResults();
        assertThat(subResults.size(), is(1));
    }

    @Test
    public void getOrCreateSubValidationResult_CallTwiceWithEqualPaths_ReturnsExistingResult() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        ValidationResult<Integer, Object> subValidationResult1 =
                validationResult.getOrCreateSubValidationResult(PATH1, 1);
        ValidationResult<Integer, Object> subValidationResult2 =
                validationResult.getOrCreateSubValidationResult(PATH1, 1);

        assertThat(subValidationResult1, sameInstance(subValidationResult2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOrCreateSubValidationResult_CallTwiceWithEqualPathsAndDifferentValues_Fails() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        validationResult.getOrCreateSubValidationResult(PATH1, 1);
        validationResult.getOrCreateSubValidationResult(PATH1, 2);
    }

    // merge

    @Test
    public void merge_EmptyResults_EmptyResult() {
        ValidationResult<Integer, Object> validationResult1 = new ValidationResult<>(1);
        ValidationResult<Integer, Object> validationResult2 = new ValidationResult<>(1);
        validationResult1.merge(validationResult2);

        assertThat(validationResult1.getErrors(), emptyIterable());
        assertThat(validationResult1.getWarnings(), emptyIterable());
    }

    @Test
    public void merge_FirstWithErrorSecondIsEmpty_OneError() {
        Object error = new Object();
        ValidationResult<Integer, Object> validationResult1 = new ValidationResult<>(1);
        validationResult1.addError(error);
        ValidationResult<Integer, Object> validationResult2 = new ValidationResult<>(1);

        validationResult1.merge(validationResult2);

        assertThat(validationResult1.getErrors(), contains(sameInstance(error)));
        assertThat(validationResult1.getWarnings(), emptyIterable());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void merge_FirstWithErrorSecondWithTwoErrors_ThreeErrors() {
        Object error1 = new Object();
        Object error2 = new Object();
        Object error3 = new Object();
        ValidationResult<Integer, Object> validationResult1 = new ValidationResult<>(1);
        validationResult1.addError(error1);
        ValidationResult<Integer, Object> validationResult2 = new ValidationResult<>(1);
        validationResult2.addError(error2);
        validationResult2.addError(error3);

        validationResult1.merge(validationResult2);

        assertThat(validationResult1.getErrors(),
                contains(sameInstance(error1), sameInstance(error2), sameInstance(error3)));
    }

    @Test
    public void merge_FirstWithWarningSecondIsEmpty_OneWarning() {
        Object warning = new Object();
        ValidationResult<Integer, Object> validationResult1 = new ValidationResult<>(1);
        validationResult1.addWarning(warning);
        ValidationResult<Integer, Object> validationResult2 = new ValidationResult<>(1);

        validationResult1.merge(validationResult2);

        assertThat(validationResult1.getWarnings(), contains(sameInstance(warning)));
        assertThat(validationResult1.getErrors(), emptyIterable());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void merge_FirstWithWarningSecondWithTwoWarnings_ThreeWarnings() {
        Object warning1 = new Object();
        Object warning2 = new Object();
        Object warning3 = new Object();
        ValidationResult<Integer, Object> validationResult1 = new ValidationResult<>(1);
        validationResult1.addWarning(warning1);
        ValidationResult<Integer, Object> validationResult2 = new ValidationResult<>(1);
        validationResult2.addWarning(warning2);
        validationResult2.addWarning(warning3);

        validationResult1.merge(validationResult2);

        assertThat(validationResult1.getWarnings(),
                contains(sameInstance(warning1), sameInstance(warning2), sameInstance(warning3)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void merge_FirstAndSecondWithErrorsAndWarnings_ContainsAllErrorsAndWarnings() {
        Object warning1 = new Object();
        Object warning2 = new Object();
        Object error1 = new Object();
        Object error2 = new Object();
        ValidationResult<Integer, Object> validationResult1 = new ValidationResult<>(1);
        validationResult1.addError(error1);
        validationResult1.addWarning(warning1);
        ValidationResult<Integer, Object> validationResult2 = new ValidationResult<>(1);
        validationResult2.addError(error2);
        validationResult2.addWarning(warning2);

        validationResult1.merge(validationResult2);

        assertThat(validationResult1.getErrors(),
                contains(sameInstance(error1), sameInstance(error2)));
        assertThat(validationResult1.getWarnings(),
                contains(sameInstance(warning1), sameInstance(warning2)));
    }

    @Test
    public void merge_FirstAndSecondWithErrorsAndWarnings_SecondIsNotChanged() {
        Object warning1 = new Object();
        Object warning2 = new Object();
        Object error1 = new Object();
        Object error2 = new Object();
        ValidationResult<Integer, Object> validationResult1 = new ValidationResult<>(1);
        validationResult1.addError(error1);
        validationResult1.addWarning(warning1);
        ValidationResult<Integer, Object> validationResult2 = new ValidationResult<>(1);
        validationResult2.addError(error2);
        validationResult2.addWarning(warning2);

        validationResult1.merge(validationResult2);

        assertThat(validationResult2.getErrors(), contains(sameInstance(error2)));
        assertThat(validationResult2.getWarnings(), contains(sameInstance(warning2)));
    }

    // transferSubNodesWithIssues

    @Test
    public void transferSubNodesWithIssues_EmptyResultWithoutSubResults_WorksFine() {
        ValidationResult<Integer, Object> source = new ValidationResult<>(1);
        ValidationResult<Integer, Object> dest = new ValidationResult<>(2);
        ValidationResult.transferSubNodesWithIssues(source, dest);
    }

    @Test
    public void transferSubNodesWithIssues_ResultWithIssuesAndWithoutSubResults_CopiesIssues() {
        ValidationResult<Integer, Object> source = new ValidationResult<>(1);
        source.addError(new Object());
        source.addError(new Object());
        source.addWarning(new Object());
        source.addWarning(new Object());

        ValidationResult<Integer, Object> dest = new ValidationResult<>(2);

        ValidationResult.transferSubNodesWithIssues(source, dest);
        assertThat("у скопированного результата ошибки соответствуют исходным",
                dest.getErrors(), contains(source.getErrors().toArray()));
        assertThat("у скопированного результата предупреждения соответствуют исходным",
                dest.getWarnings(), contains(source.getWarnings().toArray()));
    }

    @Test
    public void transferSubNodesWithIssues_OneEmptySubResult_DoesNotCopySubResult() {
        ValidationResult<Integer, Object> source = new ValidationResult<>(1);
        source.getOrCreateSubValidationResult(PATH1, 4);


        ValidationResult<Integer, Object> dest = new ValidationResult<>(2);
        ValidationResult.transferSubNodesWithIssues(source, dest);

        assertThat("под-результат без ошибки не скопирован",
                dest.getSubResults().size(), is(0));
    }

    @Test
    public void transferSubNodesWithIssues_OneSubResultWithErrors_CopiesOneSubResult() {
        Integer subValue = 4;
        Object error1 = new Object();
        Object error2 = new Object();

        ValidationResult<Integer, Object> source = new ValidationResult<>(1);
        ValidationResult<Integer, Object> sourceSubResult =
                source.getOrCreateSubValidationResult(PATH1, subValue);
        sourceSubResult.addError(error1);
        sourceSubResult.addError(error2);

        ValidationResult<?, Object> expectedDestSubResult = new ValidationResult<>(subValue);
        expectedDestSubResult.addError(error1);
        expectedDestSubResult.addError(error2);


        ValidationResult<Integer, Object> dest = new ValidationResult<>(2);
        ValidationResult.transferSubNodesWithIssues(source, dest);

        ValidationResult<?, Object> destSubResult = dest.getSubResults().get(PATH1);

        assertThat("скопированный под-результат соответствует ожидаемому",
                destSubResult, beanDiffer(expectedDestSubResult));
    }

    @Test
    public void transferSubNodesWithIssues_OneSubResultWithWarnings_CopiesOneSubResult() {
        Integer subValue = 4;
        Object warning1 = new Object();
        Object warning2 = new Object();

        ValidationResult<Integer, Object> source = new ValidationResult<>(1);
        ValidationResult<Integer, Object> sourceSubResult =
                source.getOrCreateSubValidationResult(PATH1, subValue);
        sourceSubResult.addWarning(warning1);
        sourceSubResult.addWarning(warning2);

        ValidationResult<?, Object> expectedDestSubResult = new ValidationResult<>(subValue);
        expectedDestSubResult.addWarning(warning1);
        expectedDestSubResult.addWarning(warning2);


        ValidationResult<Integer, Object> dest = new ValidationResult<>(2);
        ValidationResult.transferSubNodesWithIssues(source, dest);

        ValidationResult<?, Object> destSubResult = dest.getSubResults().get(PATH1);

        assertThat("скопированный под-результат соответствует ожидаемому",
                destSubResult, beanDiffer(expectedDestSubResult));
    }

    @Test
    public void transferSubNodesWithIssues_OneSubResultWithErrorAndOneEmpty_CopiesOneSubResult() {
        Integer subValue = 4;
        Object error = new Object();

        ValidationResult<Integer, Object> source = new ValidationResult<>(1);
        ValidationResult<Integer, Object> sourceSubResult =
                source.getOrCreateSubValidationResult(PATH1, subValue);
        sourceSubResult.addError(error);
        source.getOrCreateSubValidationResult(PATH2, 50);

        ValidationResult<?, Object> expectedDestSubResult = new ValidationResult<>(subValue);
        expectedDestSubResult.addError(error);


        ValidationResult<Integer, Object> dest = new ValidationResult<>(2);
        ValidationResult.transferSubNodesWithIssues(source, dest);

        ValidationResult<?, Object> destSubResult = dest.getSubResults().get(PATH1);

        assertThat("скопированный под-результат соответствует ожидаемому",
                destSubResult, beanDiffer(expectedDestSubResult));
        assertThat("количество под-результатов соответствует ожидаемому",
                dest.getSubResults().size(), is(1));
    }
}
