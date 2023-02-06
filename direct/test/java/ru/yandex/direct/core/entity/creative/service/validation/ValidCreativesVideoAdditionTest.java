package ru.yandex.direct.core.entity.creative.service.validation;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.service.add.validation.CreativeValidationService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class ValidCreativesVideoAdditionTest {
    private Creative creative;
    private CreativeValidationService validatorService;

    public ValidCreativesVideoAdditionTest(Creative creative) {
        this.creative = creative;
        this.validatorService = new CreativeValidationService(null);
    }

    @Test
    public void nonEmptyTextsCheckForValidValuesService() throws Exception {
        ValidationResult<List<Creative>, Defect> actual =
                validatorService.generateValidation(Collections.singletonList(creative),
                        ClientId.fromLong(1L),
                        Collections.emptyMap(),
                        Collections.emptyMap()
                );
        List<DefectInfo<Defect>> errors = actual.flattenErrors();
        List<DefectInfo<Defect>> warnings = actual.flattenWarnings();
        assertThat("результат валидации не должен содержать ошибок", errors, emptyIterable());
        assertThat("результат валидации не должен содержать предупреждений", warnings, emptyIterable());
    }

    @Parameterized.Parameters
    public static Collection<Creative> generateData() throws IOException {
        return CreativeLoader.loadCreatives("Creatives-video-addition-valid.json");
    }
}
