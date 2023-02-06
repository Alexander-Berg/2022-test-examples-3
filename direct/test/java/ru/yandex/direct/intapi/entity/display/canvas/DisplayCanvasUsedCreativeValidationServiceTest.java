package ru.yandex.direct.intapi.entity.display.canvas;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.creative.repository.CreativeConstants;
import ru.yandex.direct.intapi.entity.display.canvas.model.GetUsedCreativesRequest;
import ru.yandex.direct.intapi.entity.display.canvas.model.GetUsedCreativesType;
import ru.yandex.direct.intapi.entity.display.canvas.validation.DisplayCanvasUsedCreativeValidationService;
import ru.yandex.direct.validation.defect.StringDefects;
import ru.yandex.direct.validation.defect.ids.NumberDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class DisplayCanvasUsedCreativeValidationServiceTest {

    private static final Long CLIENT_ID = 1L;
    private static final String DEFAULT_TOKEN = "ASC-10";

    @Autowired
    private DisplayCanvasUsedCreativeValidationService usedCreativeValidationService;

    @Before
    public void setUp() throws Exception {
        usedCreativeValidationService = new DisplayCanvasUsedCreativeValidationService();
    }

    @Test
    public void positiveValidationResultWhenNoErrors() {
        ValidationResult<GetUsedCreativesRequest, Defect> actual =
                usedCreativeValidationService.validate(defaultRequest());
        assertThat(actual.flattenErrors(), hasSize(0));
    }

    @Test
    public void positiveValidationResultWithTokenWhenNoErrors() {
        ValidationResult<GetUsedCreativesRequest, Defect> actual = usedCreativeValidationService
                .validate(defaultRequest().withNextPageToken(DEFAULT_TOKEN));
        assertThat(actual.flattenErrors(), hasSize(0));
    }

    @Test
    public void validate_clientIdNull() {
        ValidationResult<GetUsedCreativesRequest, Defect> actual =
                usedCreativeValidationService.validate(defaultRequest().withClientId(null));

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("clientId")), notNull())));
    }

    @Test
    public void validate_clientIdNegative() {
        ValidationResult<GetUsedCreativesRequest, Defect> actual =
                usedCreativeValidationService.validate(defaultRequest().withClientId(-1L));

        assertThat(actual, hasDefectDefinitionWith(validationError(NumberDefectIds.MUST_BE_GREATER_THAN_MIN)));
    }

    @Test
    public void validate_creativeTypeNull() {
        ValidationResult<GetUsedCreativesRequest, Defect> actual =
                usedCreativeValidationService.validate(defaultRequest().withCreativeType(null));

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("creativeType")), notNull())));
    }

    @Test
    public void validate_nextPageTokenEmpty() {
        ValidationResult<GetUsedCreativesRequest, Defect> actual =
                usedCreativeValidationService.validate(defaultRequest().withNextPageToken(""));

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("nextPageToken")), StringDefects.notEmptyString())));
    }

    @Test
    public void validate_nextPageTokenInvalidSort() {
        ValidationResult<GetUsedCreativesRequest, Defect> actual =
                usedCreativeValidationService.validate(defaultRequest().withNextPageToken("asc-23"));

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("nextPageToken")), invalidValue())));
    }

    @Test
    public void validate_nextPageTokenSecondParamNotNumber() {
        ValidationResult<GetUsedCreativesRequest, Defect> actual =
                usedCreativeValidationService.validate(defaultRequest().withNextPageToken("ASC-bt"));

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("nextPageToken")), invalidValue())));
    }

    @Test
    public void validate_limitGreaterMax() {
        ValidationResult<GetUsedCreativesRequest, Defect> actual =
                usedCreativeValidationService
                        .validate(defaultRequest().withLimit(CreativeConstants.GET_USED_CREATIVES_LIMIT + 1));

        assertThat(actual, hasDefectDefinitionWith(validationError(NumberDefectIds.MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX)));
    }

    @Test
    public void validate_nextPageTokenParamsCount() {
        ValidationResult<GetUsedCreativesRequest, Defect> actual =
                usedCreativeValidationService.validate(defaultRequest().withNextPageToken("---"));

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("nextPageToken")), invalidValue())));
    }

    private GetUsedCreativesRequest defaultRequest() {
        return new GetUsedCreativesRequest()
                .withClientId(CLIENT_ID)
                .withCreativeType(GetUsedCreativesType.IMAGE);
    }
}
