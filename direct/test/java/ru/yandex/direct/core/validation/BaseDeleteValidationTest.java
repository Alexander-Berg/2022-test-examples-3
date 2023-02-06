package ru.yandex.direct.core.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BaseDeleteValidationTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"валидные идентификаторы", Arrays.asList(1L, 2L), null, null},
                {"null значение", Arrays.asList(null, 2L), path(index(0)), notNull()},
                {"отрицательный идентификатор", Arrays.asList(1L, -1L), path(index(1)), validId()}
        });
    }

    private List<Long> ids;
    private Path expectedPath;
    private Defect expectedDefect;

    @SuppressWarnings("unused")
    public BaseDeleteValidationTest(String name, List<Long> ids, Path expectedPath,
                                    Defect expectedDefect) {
        this.ids = ids;
        this.expectedPath = expectedPath;
        this.expectedDefect = expectedDefect;
    }

    @Test
    public void testParametrized() {
        ValidationResult<List<Long>, Defect> vr = BaseDeleteValidation.idsToDeleteValidate(ids);
        if (expectedDefect != null) {
            assertThat(vr, hasDefectDefinitionWith(validationError(expectedPath, expectedDefect)));
        } else {
            assertThat(vr.hasAnyErrors(), is(false));
        }
    }
}
