package ru.yandex.direct.result;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.validation.result.PathHelper.index;

public class MassResultTest {
    private static final List<Long> ID_LIST = Arrays.asList(11L, 22L, 33L);

    @Test
    public void successfulMassActionWithCanceledElements() {
        ValidationResult<List<Long>, Defect> massValidation = new ValidationResult<>(ID_LIST);
        massValidation.getOrCreateSubValidationResult(index(1), ID_LIST.get(1))
                .addError(new Defect<>(DefectIds.CANNOT_BE_NULL));
        MassResult<Long> massResult =
                MassResult.successfulMassAction(ID_LIST, massValidation, Collections.singleton(2));

        assertThat(massResult.getState()).isEqualTo(ResultState.SUCCESSFUL);
        assertThat(massResult.getResult().get(0).getState()).isEqualTo(ResultState.SUCCESSFUL);
        assertThat(massResult.getResult().get(1).getState()).isEqualTo(ResultState.BROKEN);
        assertThat(massResult.getResult().get(2).getState()).isEqualTo(ResultState.CANCELED);
    }
}
