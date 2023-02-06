package ru.yandex.direct.operation;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class FunctionOperationTest {
    public static final int INVALID_VALUE = 2;
    private static final Defect INVALID_VALUE_DEFECT = new Defect<>(DefectIds.INVALID_VALUE);

    @Test
    public void prepare_Partial_OneInvalid() {
        List<Integer> items = List.of(1, INVALID_VALUE, 3);
        var operation = createOperation(Applicability.PARTIAL, items);
        Optional<MassResult<Integer>> result = operation.prepare();
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void prepare_FullAllValid_ResultIsAbsent() {
        List<Integer> items = List.of(1, 3);
        var operation = createOperation(Applicability.FULL, items);
        Optional<MassResult<Integer>> result = operation.prepare();

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void prepare_PartialAllInvalid_ResultIsPresent() {
        List<Integer> items = List.of(INVALID_VALUE, INVALID_VALUE);
        var operation = createOperation(Applicability.FULL, items);
        Optional<MassResult<Integer>> result = operation.prepare();

        assertThat(result.isPresent(), is(true));
    }

    @Test
    public void prepare_PartialAllInvalid_MassResultIsSuccess() {
        List<Integer> items = List.of(INVALID_VALUE, INVALID_VALUE);
        var operation = createOperation(Applicability.FULL, items);
        Optional<MassResult<Integer>> result = operation.prepare();
        assumeThat(sa -> sa.assertThat(result.isPresent()).isTrue());

        assertThat(result.orElseThrow(), isSuccessful(false, false));
    }

    @Test
    public void prepare_FullOneInvalid_MassResultIsSuccess() {
        List<Integer> items = List.of(1, INVALID_VALUE, 3);
        var operation = createOperation(Applicability.FULL, items);
        Optional<MassResult<Integer>> result = operation.prepare();
        assumeThat(sa -> sa.assertThat(result.isPresent()).isTrue());

        assertThat(result.orElseThrow(), isSuccessful(true, false, true));
    }

    @Test
    public void prepare_FullOneInvalid_CheckResult() {
        List<Integer> items = List.of(1, INVALID_VALUE);
        var operation = createOperation(Applicability.FULL, items);
        Optional<MassResult<Integer>> result = operation.prepare();
        assumeThat(sa -> sa.assertThat(result.isPresent()).isTrue());
        var massResult = result.orElseThrow();

        assertThat(massResult, isSuccessful(true, false));
    }

    private FunctionOperation<Integer, Integer> createOperation(Applicability applicability, List<Integer> items) {
        return new FunctionOperation<>(applicability, this::validate, this::inc, items);
    }

    public ValidationResult<Integer, Defect> validateItem(Integer item) {
        if (item == INVALID_VALUE) {
            return ValidationResult.failed(item, INVALID_VALUE_DEFECT);
        } else {
            return ValidationResult.success(item);
        }
    }

    public List<ValidationResult<Integer, Defect>> validate(List<Integer> items) {
        return mapList(items, this::validateItem);
    }

    public List<Integer> inc(List<Integer> items) {
        return mapList(items, value -> value * 10);
    }
}
