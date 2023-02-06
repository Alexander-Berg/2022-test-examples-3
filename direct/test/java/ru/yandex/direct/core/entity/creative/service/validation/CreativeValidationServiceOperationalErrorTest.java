package ru.yandex.direct.core.entity.creative.service.validation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.service.add.validation.CreativeValidationService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CreativeValidationServiceOperationalErrorTest {

    private static final ClientId CLIENT_ID = ClientId.fromLong(2L);

    private CreativeValidationService creativeValidationService;

    @Before
    public void setUp() {
        creativeValidationService = new CreativeValidationService(null);
    }

    @Test
    public void operationalErrorDefectDefinitionInvalidValueWhenNull() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService.generateValidation(null, CLIENT_ID, emptyMap(), emptyMap());
        assertThat(actual.flattenErrors(),
                contains(validationError(path(), notNull())));
    }

    @Test
    public void operationalErrorDefectDefinitionInvalidValueWhenListIsEmpty() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService.generateValidation(emptyList(), CLIENT_ID, emptyMap(), emptyMap());
        assertThat(actual.flattenErrors(),
                contains(validationError(path(), notEmptyCollection())));
    }
}
