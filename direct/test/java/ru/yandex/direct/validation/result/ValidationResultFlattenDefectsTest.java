package ru.yandex.direct.validation.result;

import java.util.List;

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ValidationResultFlattenDefectsTest {

    private static final PathNode PATH1 = new PathNode.Field("sub1");
    private static final PathNode PATH2 = new PathNode.Field("sub2");

    // flattenErrors without conversion

    @Test
    public void flattenErrors_NoErrors_EmptyList() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        List<DefectInfo<Object>> errors = validationResult.flattenErrors();
        assertThat(errors, hasSize(0));
    }

    @Test
    public void flattenErrors_OneTopLevelError_OneError() {
        Integer value = 123;
        Object error = new Object();
        Path path = new Path(emptyList());

        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(value);
        validationResult.addError(error);

        List<DefectInfo<Object>> errors = validationResult.flattenErrors();
        checkOneFlatResult(value, error, path, errors);
    }

    @Test
    public void flattenErrors_OneTopLevelErrorWithSubResultWithoutErrors_OneError() {
        Integer value = 123;
        Object error = new Object();
        Path path = new Path(emptyList());

        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(value);
        validationResult.getOrCreateSubValidationResult(PATH1, 12345);
        validationResult.addError(error);

        List<DefectInfo<Object>> errors = validationResult.flattenErrors();
        checkOneFlatResult(value, error, path, errors);
    }

    @Test
    public void flattenErrors_OneSubSubLevelError_OneErrorWithValidLongPath() {
        Integer value = 12334;
        Object error = new Object();
        PathNode level1 = PATH2;
        PathNode level2 = new PathNode.Index(8);
        Path path = new Path(asList(level1, level2));

        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        ValidationResult<Integer, Object> subValidationResult =
                validationResult.getOrCreateSubValidationResult(level1, 1234);
        ValidationResult<Integer, Object> subSubValidationResult =
                subValidationResult.getOrCreateSubValidationResult(level2, value);
        subSubValidationResult.addError(error);

        List<DefectInfo<Object>> errors = validationResult.flattenErrors();
        checkOneFlatResult(value, error, path, errors);
    }

    private void checkOneFlatResult(Integer value, Object error, Path path,
                                    List<DefectInfo<Object>> flatResults) {
        assertThat("results list size matches", flatResults, hasSize(1));

        DefectInfo<Object> result = flatResults.get(0);
        assertThat("result checked value matches", result.getValue(), is(value));
        assertThat("result defect matches", result.getDefect(), is(error));
        assertThat("result path matches", result.getPath(), is(path));
    }

    @Test
    public void flattenErrors_OneTopLevelErrorAndOneFieldError_TwoErrors() {
        Integer value1 = 123;
        Object error1 = new Object();
        Path path1 = new Path(emptyList());

        Integer value2 = 12345;
        Object error2 = new Object();
        PathNode field2 = PATH2;
        Path path2 = new Path(singletonList(field2));

        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(value1);
        validationResult.addError(error1);

        ValidationResult<Integer, Object> subValidationResult =
                validationResult.getOrCreateSubValidationResult(field2, value2);
        subValidationResult.addError(error2);

        List<DefectInfo<Object>> errors = validationResult.flattenErrors();
        checkTwoFlatResults(value1, error1, path1, value2, error2, path2, errors);
    }

    @Test
    public void flattenErrors_OneTopLevelErrorAndOneIndexError_TwoErrors() {
        Integer value1 = 123;
        Object error1 = new Object();
        Path path1 = new Path(emptyList());

        Integer value2 = 12345;
        Object error2 = new Object();
        PathNode field2 = new PathNode.Index(4);
        Path path2 = new Path(singletonList(field2));

        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(value1);
        validationResult.addError(error1);

        ValidationResult<Integer, Object> subValidationResult =
                validationResult.getOrCreateSubValidationResult(field2, value2);
        subValidationResult.addError(error2);

        List<DefectInfo<Object>> errors = validationResult.flattenErrors();
        checkTwoFlatResults(value1, error1, path1, value2, error2, path2, errors);
    }

    private void checkTwoFlatResults(
            Integer value1, Object error1, Path path1,
            Integer value2, Object error2, Path path2,
            List<DefectInfo<Object>> defectInfos) {
        assertThat("DefectInfo list size matches", defectInfos, hasSize(2));

        DefectInfo<Object> info1 = defectInfos.get(0);
        assertThat("checked value of DefectInfo matches", info1.getValue(), is(value1));
        assertThat("defect of DefectInfo matches", info1.getDefect(), is(error1));
        assertThat("path of DefectInfo matches", info1.getPath(), is(path1));

        DefectInfo<Object> info2 = defectInfos.get(1);
        assertThat("checked value of DefectInfo matches", info2.getValue(), is(value2));
        assertThat("defect of DefectInfo matches", info2.getDefect(), is(error2));
        assertThat("path of DefectInfo matches", info2.getPath(), is(path2));
    }

    // flattenWarnings

    @Test
    public void flattenWarnings_NoWarnings_EmptyList() {
        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(123);
        List<DefectInfo<Object>> warnings = validationResult.flattenWarnings();
        assertThat(warnings, hasSize(0));
    }

    @Test
    public void flattenWarnings_OneTopLevelWarnings_OneWarnings() {
        Integer value = 123;
        Object warning = new Object();
        Path path = new Path(emptyList());

        ValidationResult<Integer, Object> validationResult = new ValidationResult<>(value);
        validationResult.addWarning(warning);

        List<DefectInfo<Object>> warnings = validationResult.flattenWarnings();
        checkOneFlatResult(value, warning, path, warnings);
    }
}
