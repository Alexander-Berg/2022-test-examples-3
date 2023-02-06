package ru.yandex.direct.operation.creator;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import ru.yandex.direct.operation.PartiallyApplicableOperation;
import ru.yandex.direct.result.ResultState;
import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.builder.ItemValidationBuilder;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.builder.Validator;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithItems;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class ValidationOperationCreatorTest {

    private final static Validator<List<Integer>, Defect> INT_VALIDATOR = (ints) ->
            ListValidationBuilder.<Integer, Defect>of(ints)
                    .checkEach(Constraint.fromPredicate(x -> x == null || x < 10, invalidValue()))
                    .getResult();

    private final static Validator<List<Integer>, Defect> COLLECTION_SIZE_VALIDATOR = (ints) ->
            ItemValidationBuilder.<List<Integer>, Defect>of(ints)
                    .check(Constraint.fromPredicate(x -> x.size() <= 2, maxCollectionSize(2)))
                    .getResult();

    private final static Validator<List<List<Integer>>, Defect> LIST_VALIDATOR = (ints) ->
            ListValidationBuilder.<List<Integer>, Defect>of(ints)
                    .checkEachBy(COLLECTION_SIZE_VALIDATOR)
                    .checkEachBy(INT_VALIDATOR)
                    .getResult();


    @Test
    public void prepareAndApply_EmptyInput() {
        var operation = createSimpleOperation(emptyList());
        var result = operation.prepareAndApply();

        assertThat(result, isSuccessful());
        assertThat(result.getResult(), empty());

        assertThat(operation.getValidElementIndexes(), equalTo(Set.of()));
    }

    @Test
    public void prepareAndApply_CorrectElements() {
        var operation = createSimpleOperation(asList(0, 2, 9));
        var result = operation.prepareAndApply();

        assertThat(result, isSuccessfulWithItems(1, 3, 10));

        assertThat(operation.getValidElementIndexes(), equalTo(Set.of(0, 1, 2)));
    }

    @Test
    public void prepareAndApply_OperationInvalidElement() {
        var operation = createSimpleOperation(asList(0, IncTestOperation.INVALID_VALUE, 9));
        var result = operation.prepareAndApply();

        assertThat(result.getState(), is(ResultState.SUCCESSFUL));
        assertThat(result, isSuccessful(true, false, true));
        assertThat(result.get(1).getState(), is(ResultState.BROKEN));
        assertThat(result.get(2).getResult(), equalTo(10));

        assertThat(operation.getValidElementIndexes(), equalTo(Set.of(0, 2)));
    }

    @Test
    public void prepareAndApply_ValidatorInvalidElements() {
        var operation = createSimpleOperation(asList(1, 10, 20));
        var result = operation.prepareAndApply();

        assertThat(result, isSuccessful(true, false, false));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(1)),
                invalidValue())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(2)),
                invalidValue())));

        assertThat(operation.getValidElementIndexes(), equalTo(Set.of(0)));
    }

    @Test
    public void prepareAndApply_DifferentElements() {
        var operation = createSimpleOperation(asList(1, 10, 20, IncTestOperation.INVALID_VALUE));
        var result = operation.prepareAndApply();

        assertThat(result.getSuccessfulCount(), equalTo(1));
        assertThat(result.get(0).getResult(), equalTo(2));
        assertThat(result.get(1).getState(), is(ResultState.BROKEN));
        assertThat(result.get(2).getState(), is(ResultState.BROKEN));
        assertThat(result.get(3).getState(), is(ResultState.BROKEN));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(1)),
                invalidValue())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(2)),
                invalidValue())));

        assertThat(operation.getValidElementIndexes(), equalTo(Set.of(0)));
    }

    @Test
    public void prepareAndCancel_DifferentElements() {
        var operation = createSimpleOperation(asList(1, 10, 20, IncTestOperation.INVALID_VALUE));

        operation.prepare();
        assertThat(operation.getResult(), is(Optional.empty()));
        var result = operation.cancel();

        assertThat(result.getSuccessfulCount(), equalTo(0));
        assertThat(result.get(0).getState(), is(ResultState.CANCELED));
        assertThat(result.get(1).getState(), is(ResultState.BROKEN));
        assertThat(result.get(2).getState(), is(ResultState.BROKEN));
        assertThat(result.get(3).getState(), is(ResultState.BROKEN));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(1)),
                invalidValue())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(2)),
                invalidValue())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(3)),
                new Defect<>(DefectIds.CANNOT_BE_NULL))));

        assertThat(operation.getValidElementIndexes(), equalTo(Set.of(0)));
    }

    @Test
    public void prepareAndPartiallyApplied_DifferentElements() {
        var operation = createSimpleOperation(asList(1, 10, 20, IncTestOperation.INVALID_VALUE, 5));

        operation.prepare();
        assertThat(operation.getResult(), is(Optional.empty()));
        var result = operation.apply(Set.of(0));

        assertThat(result.getSuccessfulCount(), equalTo(1));
        assertThat(result.get(0).getResult(), equalTo(2));
        assertThat(result.get(1).getState(), is(ResultState.BROKEN));
        assertThat(result.get(2).getState(), is(ResultState.BROKEN));
        assertThat(result.get(3).getState(), is(ResultState.BROKEN));
        assertThat(result.get(4).getState(), is(ResultState.CANCELED));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(1)),
                invalidValue())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(2)),
                invalidValue())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(3)),
                new Defect<>(DefectIds.CANNOT_BE_NULL))));

        assertThat(operation.getValidElementIndexes(), equalTo(Set.of(0, 4)));
    }

    @Test
    public void prepare_InvalidInput() {
        var operation =
                createSimpleOperation(asList(1, 10, 20, IncTestOperation.INVALID_VALUE, 5), COLLECTION_SIZE_VALIDATOR);

        operation.prepare();
        var result = operation.getResult();

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getState(), is(ResultState.BROKEN));
        assertThat(result.get().getValidationResult().getErrors(), equalTo(List.of(maxCollectionSize(2))));

        assertThat(operation.getValidElementIndexes(), equalTo(Set.of()));
    }

    @Test
    public void prepareAndApply_CorrectElements_WithGroupOperation() {
        var operation =
                createGroupOperation(asList(emptyList(), singletonList(1), asList(2, 3)));
        var result = operation.prepareAndApply();

        assertThat(result, isSuccessfulWithItems(emptyList(), singletonList(2), asList(3, 4)));

        assertThat(operation.getValidElementIndexes(), equalTo(Set.of(0, 1, 2)));
    }

    @Test
    public void prepareAndApply_InvalidElements_WithGroupOperation() {
        var operation = createGroupOperation(asList(
                emptyList(), asList(2, 3, 4), singletonList(15), asList(25, IncTestOperation.INVALID_VALUE), singletonList(1)));
        var result = operation.prepareAndApply();

        assertThat(result.getSuccessfulCount(), equalTo(2));
        assertThat(result.get(0).getResult(), equalTo(emptyList()));
        assertThat(result.get(1).getState(), is(ResultState.BROKEN));
        assertThat(result.get(2).getState(), is(ResultState.BROKEN));
        assertThat(result.get(3).getState(), is(ResultState.BROKEN));
        assertThat(result.get(4).getResult(), equalTo(singletonList(2)));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(1)),
                maxCollectionSize(2))));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(2), index(0)),
                invalidValue())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(3), index(0)),
                invalidValue())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(3), index(1)),
                new Defect<>(DefectIds.CANNOT_BE_NULL))));

        assertThat(operation.getValidElementIndexes(), equalTo(Set.of(0, 4)));
    }

    @Test
    public void prepareAndCancel_InvalidElements_WithGroupOperation() {
        var operation = createGroupOperation(asList(
                singletonList(0), asList(2, 3, 4), singletonList(15), asList(5, IncTestOperation.INVALID_VALUE), singletonList(1)));

        operation.prepare();
        assertThat(operation.getResult(), is(Optional.empty()));
        var result = operation.cancel();

        assertThat(result.getSuccessfulCount(), equalTo(0));
        assertThat(result.get(0).getState(), is(ResultState.CANCELED));
        assertThat(result.get(1).getState(), is(ResultState.BROKEN));
        assertThat(result.get(2).getState(), is(ResultState.BROKEN));
        assertThat(result.get(3).getState(), is(ResultState.BROKEN));
        assertThat(result.get(4).getState(), is(ResultState.CANCELED));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(1)),
                maxCollectionSize(2))));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(2), index(0)),
                invalidValue())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(3), index(1)),
                new Defect<>(DefectIds.CANNOT_BE_NULL))));

        assertThat(operation.getValidElementIndexes(), equalTo(Set.of(0, 4)));
    }

    @Test
    public void prepareAndPartiallyApplied_InvalidElements_WithGroupOperation() {
        var operation = createGroupOperation(asList(
                singletonList(0), asList(2, 3, 4), singletonList(15), asList(5, IncTestOperation.INVALID_VALUE), singletonList(1)));

        operation.prepare();
        assertThat(operation.getResult(), is(Optional.empty()));
        var result = operation.apply(Set.of(0));

        assertThat(result.getSuccessfulCount(), equalTo(1));
        assertThat(result.get(0).getResult(), equalTo(singletonList(1)));
        assertThat(result.get(1).getState(), is(ResultState.BROKEN));
        assertThat(result.get(2).getState(), is(ResultState.BROKEN));
        assertThat(result.get(3).getState(), is(ResultState.BROKEN));
        assertThat(result.get(4).getState(), is(ResultState.CANCELED));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(1)),
                maxCollectionSize(2))));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(2), index(0)),
                invalidValue())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(3), index(1)),
                new Defect<>(DefectIds.CANNOT_BE_NULL))));

        assertThat(operation.getValidElementIndexes(), equalTo(Set.of(0, 4)));
    }

    private PartiallyApplicableOperation<Integer> createSimpleOperation(List<Integer> inputList) {
        return new ValidationOperationCreator<>(IncTestOperation::new, INT_VALIDATOR).create(inputList);
    }

    private PartiallyApplicableOperation<Integer> createSimpleOperation(List<Integer> inputList,
                                                                        Validator<List<Integer>, Defect> validator) {
        return new ValidationOperationCreator<>(IncTestOperation::new, validator).create(inputList);
    }

    private PartiallyApplicableOperation<List<Integer>> createGroupOperation(List<List<Integer>> inputList) {
        var groupOperationCreator = new GroupOperationCreator<>(IncTestOperation::new);
        return new ValidationOperationCreator<>(groupOperationCreator, LIST_VALIDATOR).create(inputList);
    }
}
