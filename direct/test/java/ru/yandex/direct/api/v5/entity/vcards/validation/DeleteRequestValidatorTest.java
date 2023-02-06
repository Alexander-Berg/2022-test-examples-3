package ru.yandex.direct.api.v5.entity.vcards.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.yandex.direct.api.v5.general.IdsCriteria;
import com.yandex.direct.api.v5.vcards.DeleteRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.direct.api.v5.entity.vcards.validation.DeleteRequestValidator.MAX_IDS_COUNT_PER_DELETE_REQUEST;
import static ru.yandex.direct.api.v5.validation.DefectTypes.absentElementInArray;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class DeleteRequestValidatorTest {

    @Parameterized.Parameter
    public List<Long> vcardsIds;
    @Parameterized.Parameter(value = 1)
    public DefectType expectedDefect;
    private DeleteRequestValidator validator;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{Arrays.asList(null, 11L), absentElementInArray()},
                new Object[]{Collections.nCopies(MAX_IDS_COUNT_PER_DELETE_REQUEST + 1, 22L),
                        DeleteRequestValidator.DELETE_MAX_IDS_DETAILED}
        );
    }

    @Before
    public void before() {
        validator = new DeleteRequestValidator();
    }

    @Test
    public void validateInternalRequest() throws Exception {
        DeleteRequest request = new DeleteRequest().withSelectionCriteria(new IdsCriteria().withIds(vcardsIds));
        ValidationResult<DeleteRequest, DefectType> actualResult = validator.validateRequest(request);
        assertThat(actualResult.flattenErrors(),
                contains(validationError(path(field("SelectionCriteria"), field("Ids")), expectedDefect)));
    }
}
