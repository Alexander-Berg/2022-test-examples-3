package ru.yandex.direct.api.v5.entity.ads.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import com.yandex.direct.api.v5.ads.DeleteRequest;
import com.yandex.direct.api.v5.general.IdsCriteria;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.entity.ads.Constants;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.AdsDefectTypes.maxIdsToDelete;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.DefectTypes.absentElementInArray;
import static ru.yandex.direct.api.v5.validation.DefectTypes.emptyElementsList;
import static ru.yandex.direct.api.v5.validation.DefectTypes.invalidValue;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class AdsDeleteRequestValidatorTest {
    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public DeleteRequest request;

    @Parameterized.Parameter(2)
    public DefectType expectedDefect;

    @Parameterized.Parameter(3)
    public Path expectedPath;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{"Весь запрос null", null, invalidValue(), path()},
                new Object[]{"SelectionCriteria null", new DeleteRequest().withSelectionCriteria(null),
                        invalidValue(), path(field("SelectionCriteria"))},
                new Object[]{"Ids null", testCase(null), emptyElementsList(),
                        path(field("SelectionCriteria"), field("Ids"))},
                new Object[]{"Ids пустой", testCase(emptyList()), emptyElementsList(),
                        path(field("SelectionCriteria"), field("Ids"))},
                new Object[]{"Ids содержит null", testCase(singleton(null)), absentElementInArray(),
                        path(field("SelectionCriteria"), field("Ids"))},
                new Object[]{"Ids слишком длинный", testCase(Stream.generate(() -> 0L)
                        .limit(Constants.MAX_IDS_PER_DELETE + 1).collect(toList())), maxIdsToDelete(),
                        path(field("SelectionCriteria"), field("Ids"))},
                new Object[]{"Корректный запрос", testCase(singleton(1L)), null, null}
        );
    }

    private static DeleteRequest testCase(Collection<Long> ids) {
        return new DeleteRequest().withSelectionCriteria(new IdsCriteria().withIds(ids));
    }

    @Test
    public void validate() {
        AdsDeleteRequestValidator validator = new AdsDeleteRequestValidator();
        ValidationResult<DeleteRequest, DefectType> vr = validator.validate(request);
        if (expectedDefect != null) {
            assertThat(vr).is(hasDefectWith(validationError(expectedPath, expectedDefect)));
        } else {
            assertThat(vr).is(hasNoDefects());
        }
    }
}
