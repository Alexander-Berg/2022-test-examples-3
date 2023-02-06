package ru.yandex.direct.operation.creator;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.operation.PartiallyApplicableOperation;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.ResultState;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithItems;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPath;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class GroupOperationCreatorTest {
    @Test
    public void emptyInput_ReturnsEmptyResult() {
        MassResult<List<Integer>> result = createOperation(emptyList()).prepareAndApply();
        assertThat(result, isSuccessful());
        assertThat(result.getResult(), empty());
    }

    @Test
    public void inputWithEmptyList_ReturnsEmptyResult() {
        MassResult<List<Integer>> result = createOperation(singletonList(emptyList())).prepareAndApply();
        assertThat(result, isSuccessfulWithItems(emptyList()));
    }

    @Test
    public void someInput_ReturnsProperResult() {
        MassResult<List<Integer>> result =
                createOperation(asList(asList(0, 0), asList(9, 99, 999), emptyList())).prepareAndApply();
        assertThat(result, isSuccessfulWithItems(asList(1, 1), asList(10, 100, 1000), emptyList()));
    }

    @Test
    public void someInput_InvalidElementAtFirstGroup_ReturnsProperResult() {
        MassResult<List<Integer>> result =
                createOperation(asList(asList(0, IncTestOperation.INVALID_VALUE), asList(9, 99)))
                        .prepareAndApply();
        assertThat(result, isSuccessful(false, true));
        assertThat(result.get(0).getState(), is(ResultState.BROKEN));
        assertThat(result.get(0).getValidationResult(),
                hasDefectDefinitionWith(anyValidationErrorOnPath(path(index(1)))));
        assertThat(result.get(1).getResult(), equalTo(asList(10, 100)));
    }

    @Test
    public void someInput_InvalidElementAtEachGroup_ReturnsProperResult() {
        MassResult<List<Integer>> result = createOperation(
                asList(asList(0, IncTestOperation.INVALID_VALUE), asList(IncTestOperation.INVALID_VALUE, 99))
        ).prepareAndApply();
        assertThat(result, isSuccessful(false, false));
        assertThat(result.get(0).getValidationResult(),
                hasDefectDefinitionWith(anyValidationErrorOnPath(path(index(1)))));
        assertThat(result.get(1).getValidationResult(),
                hasDefectDefinitionWith(anyValidationErrorOnPath(path(index(0)))));
    }

    private PartiallyApplicableOperation<List<Integer>> createOperation(List<List<Integer>> inputList) {
        return new GroupOperationCreator<>(IncTestOperation::new).create(inputList);
    }
}
