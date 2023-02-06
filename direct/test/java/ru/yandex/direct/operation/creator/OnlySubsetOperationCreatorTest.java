package ru.yandex.direct.operation.creator;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.direct.operation.PartiallyApplicableOperation;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.ResultState;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithItems;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPath;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.validation.result.PathHelper.emptyPath;

public class OnlySubsetOperationCreatorTest {
    @Test
    public void emptyInput_emptySubsetIndexes_ReturnsEmptyResult() {
        MassResult<Integer> result = createOperation(emptyList(), emptySet()).prepareAndApply();
        assertThat(result, isSuccessful());
        assertThat(result.getResult(), empty());
    }

    @Test
    public void inputSize1_allSubsetIndexes_ReturnsProperResult() {
        MassResult<Integer> result =
                createOperation(singletonList(1), singleton(0)).prepareAndApply();
        assertThat(result, isSuccessfulWithItems(2));
    }

    @Test
    public void inputSize2_allSubsetIndexes_ReturnsProperResult() {
        MassResult<Integer> result =
                createOperation(asList(1, 2), ImmutableSet.of(0, 1)).prepareAndApply();
        assertThat(result, isSuccessfulWithItems(2, 3));
    }

    @Test
    public void inputSize1_emptySubsetIndexes_ReturnsCanceledResult() {
        MassResult<Integer> result =
                createOperation(singletonList(1), emptySet()).prepareAndApply();
        assertThat(result, isSuccessful(false));
        assertThat(result.get(0).getState(), is(ResultState.CANCELED));
    }

    @Test
    public void inputSize2_SubsetIndexesIs0_ReturnsProperResultForFirstAndCanceledForSecond() {
        MassResult<Integer> result =
                createOperation(asList(1, 2), ImmutableSet.of(0)).prepareAndApply();
        assertThat(result, isSuccessful(true, false));
        assertThat(result.get(0).getResult(), equalTo(2));
        assertThat(result.get(1).getState(), is(ResultState.CANCELED));
    }

    @Test
    public void inputSize2_SubsetIndexesIs1_ReturnsProperResultForSecondAndCanceledForFirst() {
        MassResult<Integer> result =
                createOperation(asList(1, 2), ImmutableSet.of(1)).prepareAndApply();
        assertThat(result, isSuccessful(false, true));
        assertThat(result.get(0).getState(), is(ResultState.CANCELED));
        assertThat(result.get(1).getResult(), equalTo(3));
    }

    @Test
    public void inputSize2_SubsetIndexesIs1And2IsInvalid_ReturnsProperResults() {
        MassResult<Integer> result =
                createOperation(asList(1, IncTestOperation.INVALID_VALUE), ImmutableSet.of(1)).prepareAndApply();
        assertThat(result, isSuccessful(false, false));
        assertThat(result.get(0).getState(), is(ResultState.CANCELED));
        assertThat(result.get(1).getState(), is(ResultState.BROKEN));
        assertThat(result.get(1).getValidationResult(),
                hasDefectDefinitionWith(anyValidationErrorOnPath(emptyPath())));
    }

    @Test
    public void inputSize3_SubsetIndexesIs1And2ApplyOnly1_ReturnsProperResultForSecondAndCanceledForOthers() {
        PartiallyApplicableOperation<Integer> operation =
                createOperation(asList(1, 2, 3), ImmutableSet.of(1, 2));
        operation.prepare();
        MassResult<Integer> result = operation.apply(singleton(1));

        assertThat(result, isSuccessful(false, true, false));
        assertThat(result.get(0).getState(), is(ResultState.CANCELED));
        assertThat(result.get(1).getResult(), equalTo(3));
        assertThat(result.get(2).getState(), is(ResultState.CANCELED));
    }

    @Test
    public void inputSize2_SubsetIndexesIs1ApplyOnly0_ReturnsAllCanceled() {
        PartiallyApplicableOperation<Integer> operation = createOperation(asList(1, 2), ImmutableSet.of(1));
        operation.prepare();
        MassResult<Integer> result = operation.apply(singleton(0));

        assertThat(result, isSuccessful(false, false));
        assertThat(result.get(0).getState(), is(ResultState.CANCELED));
        assertThat(result.get(1).getState(), is(ResultState.CANCELED));
    }

    private PartiallyApplicableOperation<Integer> createOperation(List<Integer> inputList, Set<Integer> subsetIndexes) {
        return new OnlySubsetOperationCreator<>(IncTestOperation::new, subsetIndexes)
                .create(inputList);
    }
}
