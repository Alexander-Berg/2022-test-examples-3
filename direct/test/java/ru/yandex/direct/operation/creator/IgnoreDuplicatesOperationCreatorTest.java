package ru.yandex.direct.operation.creator;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.operation.Operation;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static com.google.common.primitives.Ints.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithItems;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;


public class IgnoreDuplicatesOperationCreatorTest {
    private static final Consumer<Result<Integer>> DO_NOTHING = r -> {
    };
    private static final OperationCreator<Integer, ? extends Operation<Integer>> INC_OPERATION_CREATOR =
            OperationCreators.fromFunction(listOfInteger -> mapList(listOfInteger, v -> v + 1));

    private OperationCreator<Integer, Operation<Integer>> operationCreator;

    @Before
    public void setUp() throws Exception {
        //noinspection unchecked
        operationCreator = mock(OperationCreator.class);
    }

    @Test
    public void emptyInputListIsPassedToInnerOperationCreator() {
        createOperation(operationCreator, emptyList());
        verify(operationCreator).create(eq(emptyList()));
    }

    @Test
    public void inputListWithOutDuplicatedIsPassedToInnerOperationCreator() {
        List<Integer> inputList = asList(1, 2, 3);
        createOperation(operationCreator, inputList);
        verify(operationCreator).create(eq(inputList));
    }

    @Test
    public void onlyUniqueValuesArePassedToInnerOperationCreator() {
        createOperation(operationCreator, asList(1, 2, 1, 2));
        verify(operationCreator).create(eq(asList(1, 2)));
    }

    @Test
    public void checkResultForEmptyList() {
        MassResult<Integer> result = createOperation(INC_OPERATION_CREATOR, emptyList()).prepareAndApply();
        Assert.assertThat(result, isSuccessful());
        Assert.assertThat(result.getResult(), empty());
    }

    @Test
    public void checkResultForSingletonList() {
        MassResult<Integer> result = createOperation(INC_OPERATION_CREATOR, singletonList(1)).prepareAndApply();
        Assert.assertThat(result, isSuccessfulWithItems(2));
    }

    @Test
    public void checkResultForListWithoutDuplicates() {
        MassResult<Integer> result = createOperation(INC_OPERATION_CREATOR, asList(1, 2, 3)).prepareAndApply();
        Assert.assertThat(result, isSuccessfulWithItems(2, 3, 4));
    }

    @Test
    public void checkResultForListWithDuplicates() {
        MassResult<Integer> result = createOperation(INC_OPERATION_CREATOR, asList(1, 2, 1)).prepareAndApply();
        Assert.assertThat(result, isSuccessfulWithItems(2, 3, 2));
    }

    private Operation<Integer> createOperation(
            OperationCreator<Integer, ? extends Operation<Integer>> innerOperationCreator,
            List<Integer> inputList) {
        IgnoreDuplicatesOperationCreator<Integer, Integer> creator =
                new IgnoreDuplicatesOperationCreator<>(innerOperationCreator, Comparator.naturalOrder(), DO_NOTHING);
        return creator.create(inputList);
    }
}
