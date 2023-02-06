package ru.yandex.direct.api.v5.entity.ads.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdUpdateItem;
import com.yandex.direct.api.v5.ads.ResumeRequest;
import com.yandex.direct.api.v5.general.IdsCriteria;
import one.util.streamex.StreamEx;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.entity.ads.AdsDefectTypes.maxBannersPerResumeRequest;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.listOfUpdateItems;
import static ru.yandex.direct.api.v5.entity.ads.Constants.MAX_SUSPEND_RESUME_IDS_COUNT;
import static ru.yandex.direct.api.v5.validation.DefectTypes.emptyElementsList;
import static ru.yandex.direct.api.v5.validation.DefectTypes.invalidValue;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AdsSuspendResumeRequestValidatorTest {
    private final AdsSuspendResumeRequestValidator validator = new AdsSuspendResumeRequestValidator(
            mock(AdsAdGroupTypeValidator.class));

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public ResumeRequest request;

    @Parameterized.Parameter(2)
    public Matcher<ValidationResult<ResumeRequest, DefectType>> resultMatcher;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
        return asList(

                // Negative
                testCase("Max array length exceeded",
                        resumeRequest(StreamEx.of(listOfUpdateItems(MAX_SUSPEND_RESUME_IDS_COUNT + 1))
                                .map(AdUpdateItem::getId)
                                .collect(Collectors.toList())),
                        err(path(field("SelectionCriteria"), field("Ids")),
                                maxBannersPerResumeRequest(MAX_SUSPEND_RESUME_IDS_COUNT))),
                testCase("Empty request",
                        resumeRequest(Collections.emptyList()),
                        err(path(field("SelectionCriteria"), field("Ids")), emptyElementsList())),
                testCase("IdsCriteria is null",
                        new ResumeRequest().withSelectionCriteria(null),
                        err(path(field("SelectionCriteria")), invalidValue())),

                // Positive
                testCase("Normal request",
                        resumeRequest(Arrays.asList(1L, 2L)),
                        hasNoDefects())
        );
    }

    private static ResumeRequest resumeRequest(List<Long> ids) {
        return new ResumeRequest().withSelectionCriteria(new IdsCriteria().withIds(ids));
    }

    private static Object[] testCase(String description, ResumeRequest request,
                                     Matcher<ValidationResult<ResumeRequest, DefectType>> matcher) {
        return new Object[]{description, request, matcher};
    }

    private static Matcher<ValidationResult<ResumeRequest, DefectType>> err(Path path, DefectType defectType) {
        return hasDefectWith(validationError(path, defectType));
    }

    @Test
    public void validateTest() {
        assertThat(validator.validate(request), resultMatcher);
    }
}
