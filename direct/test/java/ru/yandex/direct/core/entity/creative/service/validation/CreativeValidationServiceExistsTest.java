package ru.yandex.direct.core.entity.creative.service.validation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.service.add.validation.CreativeValidationService;
import ru.yandex.direct.core.testing.data.TestCreatives;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.inconsistentStateAlreadyExists;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CreativeValidationServiceExistsTest {

    private static final ClientId CLIENT_ID = ClientId.fromLong(2L);
    private static final Long CREATIVE_ID = 1L;
    private static final Long SECOND_CREATIVE_ID = 2L;

    private CreativeValidationService creativeValidationService;

    @Before
    public void setUp() {
        creativeValidationService = new CreativeValidationService(null);
    }

    @Test
    public void positiveValidationResultWhenUpdateVideoAddition() {
        ValidationResult<List<Creative>, Defect> actual = creativeValidationService
                .generateValidation(singletonList(defaultVideoAddition()), CLIENT_ID, emptyMap(),
                        singletonMap(CREATIVE_ID, defaultVideoAddition()));

        assertThat(actual.flattenErrors(), hasSize(0));
    }

    @Test
    public void positiveValidationResultWhenUpdateCanvas() {
        ValidationResult<List<Creative>, Defect> actual = creativeValidationService
                .generateValidation(singletonList(defaultCanvas()), CLIENT_ID, emptyMap(),
                        singletonMap(CREATIVE_ID, defaultCanvas()));

        assertThat(actual.flattenErrors(), hasSize(0));
    }

    @Test
    public void checkDuplicatesInExistingData() {
        ValidationResult<List<Creative>, Defect> actual = creativeValidationService
                .generateValidation(singletonList(defaultVideoAddition()), CLIENT_ID, emptyMap(),
                        singletonMap(SECOND_CREATIVE_ID, defaultVideoAddition().withId(SECOND_CREATIVE_ID)));

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(
                validationError(path(), inconsistentStateAlreadyExists())));
    }

    @Test
    public void checkAnotherTypeInExisting() {
        ValidationResult<List<Creative>, Defect> actual = creativeValidationService
                .generateValidation(singletonList(defaultVideoAddition()), CLIENT_ID, emptyMap(),
                        singletonMap(CREATIVE_ID, defaultCanvas()));

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(
                validationError(path(), invalidValue())));
    }

    @Test
    public void checkAnotherTypeInExistingOldData() {
        ValidationResult<List<Creative>, Defect> actual = creativeValidationService
                .generateValidation(singletonList(defaultVideoAddition()), CLIENT_ID, emptyMap(),
                        singletonMap(CREATIVE_ID, defaultCanvas().withStockCreativeId(null)));

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(
                validationError(path(), invalidValue())));
    }

    private Creative defaultVideoAddition() {
        return TestCreatives.defaultVideoAddition(CLIENT_ID, CREATIVE_ID);
    }

    private Creative defaultCanvas() {
        return TestCreatives.defaultCanvas(CLIENT_ID, CREATIVE_ID);
    }
}
