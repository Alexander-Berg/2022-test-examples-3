package ru.yandex.direct.core.entity.creative.service.validation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.ModerationInfo;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoText;
import ru.yandex.direct.core.entity.creative.service.add.validation.Constants;
import ru.yandex.direct.core.entity.creative.service.add.validation.CreativeValidationService;
import ru.yandex.direct.core.testing.data.TestCreatives;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThan;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CreativeValidationServiceCanvasTest {

    private static final ClientId CLIENT_ID = ClientId.fromLong(2L);
    private static final Long CREATIVE_ID = 1L;

    private CreativeValidationService creativeValidationService;

    @Before
    public void setUp() {
        creativeValidationService = new CreativeValidationService(null);
    }

    @Test
    public void positiveValidationResultWhenNoErrors() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas()), CLIENT_ID, emptyMap(), emptyMap());
        assertThat(actual.getErrors(), hasSize(0));
    }

    @Test
    public void validate_widthNull() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultCanvas().withWidth(null)),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("width")),
                        notNull())));
    }

    @Test
    public void validate_widthNegative() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultCanvas().withWidth(-1L)),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual, hasDefectWithDefinition(validationError(greaterThan(-1L).defectId())));
    }

    @Test
    public void validate_widthGreaterThenMax() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultCanvas().withWidth(Constants.UNSIGNED_SMALLINT_MAX_VALUE + 1)),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual, hasDefectWithDefinition(validationError(lessThanOrEqualTo(Constants.UNSIGNED_SMALLINT_MAX_VALUE).defectId())));
    }

    @Test
    public void validate_heightNull() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultCanvas().withHeight(null)),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("height")),
                        notNull())));
    }

    @Test
    public void validate_heightNegative() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultCanvas().withHeight(-1L)),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual, hasDefectWithDefinition(validationError(greaterThan(-1L).defectId())));
    }

    @Test
    public void validate_heightGreaterThanMax() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultCanvas().withHeight(Constants.UNSIGNED_SMALLINT_MAX_VALUE + 1)),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual, hasDefectWithDefinition(validationError(lessThanOrEqualTo(Constants.UNSIGNED_SMALLINT_MAX_VALUE).defectId())));
    }

    //moderation info

    @Test
    public void validate_moderationInfoTextsEmptyList() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultCanvas().withModerationInfo(new ModerationInfo().withTexts(emptyList()))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("texts")),
                        notEmptyCollection())));
    }

    @Test
    public void validate_moderationInfoTextsListNullValue() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultCanvas()
                                        .withModerationInfo(new ModerationInfo().withTexts(singletonList(null)))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("texts"), index(0)),
                        notNull())));
    }

    @Test
    public void validate_moderationInfoTextNullTextField() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultCanvas().withModerationInfo(
                                        new ModerationInfo().withTexts(singletonList(new ModerationInfoText())))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("texts"), index(0), field("text")),
                        notNull())));
    }

    @Test
    public void validate_moderationInfoTextEmptyTextField() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultCanvas().withModerationInfo(new ModerationInfo()
                                        .withTexts(singletonList(new ModerationInfoText().withText(""))))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("texts"), index(0), field("text")),
                        notEmptyString())));
    }

    private Creative defaultCanvas() {
        return TestCreatives.defaultCanvas(CLIENT_ID, CREATIVE_ID);
    }
}
