package ru.yandex.direct.core.entity.creative.service.validation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.service.add.validation.Constants;
import ru.yandex.direct.core.entity.creative.service.add.validation.CreativeDefects;
import ru.yandex.direct.core.entity.creative.service.add.validation.CreativeValidationService;
import ru.yandex.direct.core.testing.data.TestCreatives;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.creative.service.add.validation.Constants.MAX_CREATIVE_NAME_LENGTH;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThan;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CreativeValidationServiceCommonTest {

    private static final ClientId CLIENT_ID = ClientId.fromLong(2L);
    private static final Long ANOTHER_CLIENT_ID = 3L;
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
        assertThat(actual.flattenErrors(), hasSize(0));
    }

    @Test
    public void validate_duplicatedCreativeIdInRequest() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(Arrays.asList(defaultCanvas(), defaultCanvas()), CLIENT_ID,
                                emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(validationError(path(),
                duplicatedElement())));

        assertThat(actual.getSubResults().get(index(1)).flattenErrors(), contains(validationError(path(),
                duplicatedElement())));
    }

    @Test
    public void validate_idNull() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withId(null)), CLIENT_ID, emptyMap(),
                                emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(validationError(path(field("id")),
                notNull())));
    }

    @Test
    public void validate_idNegative() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withId(-1L)), CLIENT_ID, emptyMap(),
                                emptyMap());

        assertThat(actual, hasDefectDefinitionWith(validationError(greaterThan(-1L).defectId())));
    }

    @Test
    public void validate_duplicatedCreativeIdAnotherClientInDB() {
        Map<Long, Long> creativeToClient = new HashMap<>();
        creativeToClient.put(CREATIVE_ID, ANOTHER_CLIENT_ID);

        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas()), CLIENT_ID, creativeToClient, emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(validationError(path(field("id")),
                CreativeDefects.creativeNotBelongToClient(CLIENT_ID.asLong()))));
    }

    @Test
    public void validate_nameNull() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withName(null)), CLIENT_ID, emptyMap(),
                                emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(validationError(path(field("name")),
                notNull())));
    }

    @Test
    public void validate_nameEmpty() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withName("")), CLIENT_ID, emptyMap(),
                                emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(validationError(path(field("name")),
                notEmptyString())));
    }

    @Test
    public void validate_nameGreaterMax() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas()
                                        .withName(RandomStringUtils.randomAlphabetic(MAX_CREATIVE_NAME_LENGTH + 1))), CLIENT_ID,
                                emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(validationError(path(field("name")),
                maxStringLength(Constants.MAX_CREATIVE_NAME_LENGTH))));
    }

    @Test
    public void validate_typeNull() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withType(null)), CLIENT_ID, emptyMap(),
                                emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(validationError(path(field("type")),
                notNull())));
    }

    @Test
    public void validate_clientIdNull() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withClientId(null)), CLIENT_ID,
                                emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("clientId")), notNull())));
    }

    @Test
    public void validate_previewUrlNull() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withPreviewUrl(null)), CLIENT_ID,
                                emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("previewUrl")), notNull())));
    }

    @Test
    public void validate_previewUrlEmpty() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withPreviewUrl("")), CLIENT_ID,
                                emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("previewUrl")), notEmptyString())));
    }

    @Test
    public void validate_previewUrlGreaterMax() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withPreviewUrl(
                                RandomStringUtils.randomAlphabetic(Constants.MAX_PREVIEW_URL_LENGTH + 1))), CLIENT_ID,
                                emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(
                        validationError(path(field("previewUrl")), maxStringLength(Constants.MAX_PREVIEW_URL_LENGTH))));
    }

    @Test
    public void validate_livePreviewUrlNull() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withLivePreviewUrl(null)), CLIENT_ID,
                                emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("livePreviewUrl")), notNull())));
    }

    @Test
    public void validate_livePreviewUrlEmpty() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withLivePreviewUrl("")), CLIENT_ID,
                                emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("livePreviewUrl")), notEmptyString())));
    }

    @Test
    public void validate_livePreviewUrlGreaterMax() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withLivePreviewUrl(
                                RandomStringUtils.randomAlphabetic(Constants.MAX_PREVIEW_URL_LENGTH + 1))), CLIENT_ID,
                                emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("livePreviewUrl")),
                        maxStringLength(Constants.MAX_PREVIEW_URL_LENGTH))));
    }

    @Test
    public void validate_layoutIdNegative() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultCanvas().withLayoutId(-1L)), CLIENT_ID, emptyMap(),
                                emptyMap());

        assertThat(actual, hasDefectDefinitionWith(validationError(greaterThan(-1L).defectId())));
    }

    private Creative defaultCanvas() {
        return TestCreatives.defaultCanvas(CLIENT_ID, CREATIVE_ID);
    }
}
