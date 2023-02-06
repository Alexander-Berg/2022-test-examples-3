package ru.yandex.direct.api.v5.entity.dynamictextadtargets.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.dynamictextadtargets.SuspendRequest;
import com.yandex.direct.api.v5.general.IdsCriteria;
import one.util.streamex.LongStreamEx;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.dynamictextadtargets.DynamicAdTargetsDefectTypes.maxDynamicTextAdTargetsPerSuspendRequest;
import static ru.yandex.direct.api.v5.entity.dynamictextadtargets.validation.DynamicTextAdTargetsSuspendResumeRequestValidator.MAX_SUSPEND_RESUME_IDS_COUNT;
import static ru.yandex.direct.api.v5.validation.DefectTypes.absentElementInArray;
import static ru.yandex.direct.api.v5.validation.DefectTypes.emptyElementsList;
import static ru.yandex.direct.api.v5.validation.DefectTypes.invalidValue;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class DynamicTextAdTargetsSuspendRequestValidatorTest {
    private final DynamicTextAdTargetsSuspendResumeRequestValidator validator =
            new DynamicTextAdTargetsSuspendResumeRequestValidator();

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public SuspendRequest request;

    @Parameterized.Parameter(2)
    public Matcher<ValidationResult<SuspendRequest, DefectType>> resultMatcher;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
        return asList(
                // Negative
                testCase("IdsCriteria is null",
                        new SuspendRequest().withSelectionCriteria(null),
                        err(path(field("SelectionCriteria")), invalidValue())),

                testCase("Empty request",
                        suspendRequest(Collections.emptyList()),
                        err(path(field("SelectionCriteria"), field("Ids")), emptyElementsList())),

                testCase("One correct Id and One null",
                        suspendRequest(Arrays.asList(1L, null)),
                        err(path(field("SelectionCriteria"), field("Ids")), absentElementInArray())),

                testCase("Max array length exceeded in suspend",
                        suspendRequest(LongStreamEx.range(MAX_SUSPEND_RESUME_IDS_COUNT + 1)
                                .boxed()
                                .toList()),
                        err(path(field("SelectionCriteria"), field("Ids")),
                                maxDynamicTextAdTargetsPerSuspendRequest(MAX_SUSPEND_RESUME_IDS_COUNT))),

                // Positive
                testCase("Normal request with one Id",
                        suspendRequest(Arrays.asList(1L)),
                        hasNoDefects()),
                testCase("Normal request with two Id",
                        suspendRequest(Arrays.asList(1L, 2L)),
                        hasNoDefects()),
                testCase("Normal request with max count of Id",
                        suspendRequest(LongStreamEx.range(MAX_SUSPEND_RESUME_IDS_COUNT)
                                .boxed()
                                .toList()),
                        hasNoDefects())
        );
    }

    private static SuspendRequest suspendRequest(List<Long> ids) {
        return new SuspendRequest().withSelectionCriteria(new IdsCriteria().withIds(ids));
    }

    private static Object[] testCase(String description, SuspendRequest request,
                                     Matcher<ValidationResult<SuspendRequest, DefectType>> matcher) {
        return new Object[]{description, request, matcher};
    }

    private static Matcher<ValidationResult<SuspendRequest, DefectType>> err(Path path, DefectType defectType) {
        return hasDefectWith(validationError(path, defectType));
    }

    @Test
    public void validateTest() {
        assertThat(validator.validate(request)).is(matchedBy(resultMatcher));
    }
}
