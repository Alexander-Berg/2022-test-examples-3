package ru.yandex.direct.web.entity.contentpromotion.common.service;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.entity.contentpromotion.common.model.GetContentPromotionMetaRequest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds.Gen.EMPTY_HREF;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.DefectIds.CANNOT_BE_NULL;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class ContentPromotionValidationServiceTest {

    private ContentPromotionValidationService validationService;

    @Before
    public void before() {
        validationService = new ContentPromotionValidationService();
    }

    @Test
    public void validateGetMetaRequest_NullVideoHref_ValidationError() {
        ValidationResult<GetContentPromotionMetaRequest, Defect> vr =
                validationService.validateGetMetaRequest(new GetContentPromotionMetaRequest().setUrl(null));

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(path(field("url")), CANNOT_BE_NULL))));
    }

    @Test
    public void validateGetMetaRequest_BlankVideoHref_ValidationError() {
        ValidationResult<GetContentPromotionMetaRequest, Defect> vr =
                validationService.validateGetMetaRequest(new GetContentPromotionMetaRequest().setUrl("      "));

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(path(field("url")), EMPTY_HREF))));
    }

    @Test
    public void validateGetMetaRequest_ValidVideoHref_NoError() {
        ValidationResult<GetContentPromotionMetaRequest, Defect> vr = validationService
                .validateGetMetaRequest(new GetContentPromotionMetaRequest().setUrl("https://www.youtube.com"));

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }
}
