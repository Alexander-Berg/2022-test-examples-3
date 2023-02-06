package ru.yandex.direct.validation.result;

import java.util.List;

import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class ValidationResultUtilsTest {
    private static final Object SOME_ERROR = new Object();

    @Test
    public void splitValidationResult_empty() {
        var vrList = ValidationResultUtils.splitValidationResult(emptyList(), ValidationResult.success(emptyList()));
        assertThat(vrList, empty());
    }

    @Test
    public void splitValidationResult_two() {
        var items = List.of(1, 2);

        var firstElementVr = ValidationResult.success(items.get(0));
        var secondElementVr = ValidationResult.failed(items.get(1), SOME_ERROR);
        var vr = new ValidationResult<>(items);
        vr.addSubResult(index(0), firstElementVr);
        vr.addSubResult(index(1), secondElementVr);

        var vrList = ValidationResultUtils.splitValidationResult(items, vr);

        assertThat(vrList, contains(firstElementVr, secondElementVr));
    }

    @Test
    public void mergeValidationResults_empty() {
        var vr = ValidationResultUtils.mergeValidationResults(emptyList(), emptyList());
        assertThat(vr.getValue(), hasSize(0));
        assertThat(vr.hasAnyErrors(), is(false));
    }

    @Test
    public void mergeValidationResults_two() {
        var items = List.of(1, 2);
        var vr = ValidationResultUtils.mergeValidationResults(items, List.of(
                ValidationResult.success(items.get(0)),
                ValidationResult.failed(items.get(1), SOME_ERROR)
        ));
        assertThat(vr.getValue(), sameInstance(items));
        assertThat(vr.flattenErrors(), contains(new DefectInfo<>(path(index(1)), 2, SOME_ERROR)));
    }

    @Test
    public void hasAnyErrors_false() {
        var vrList = List.of(ValidationResult.success(1), ValidationResult.success(2));
        var hasAnyErrors = ValidationResultUtils.hasAnyErrors(vrList);
        assertThat(hasAnyErrors, is(false));
    }

    @Test
    public void hasAnyErrors_true() {
        var vrList = List.of(ValidationResult.success(1), ValidationResult.failed(2, SOME_ERROR));
        var hasAnyErrors = ValidationResultUtils.hasAnyErrors(vrList);
        assertThat(hasAnyErrors, is(true));
    }
}
