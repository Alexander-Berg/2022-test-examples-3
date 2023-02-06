package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.minusKeywordsNotAllowed;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CpmGeoproductAdGroupValidationTest {

    private CpmGeoproductAdGroup cpmGeoproductAdGroup;
    private CpmGeoproductAdGroupValidation validation;

    @Before
    public void setUp() {
        cpmGeoproductAdGroup = new CpmGeoproductAdGroup();
        validation = new CpmGeoproductAdGroupValidation();
    }

    @Test
    public void validateAdGroup_MinusKeywords_validationError() {
        ValidationResult<CpmGeoproductAdGroup, Defect> result =
                validation.validateAdGroup(
                        cpmGeoproductAdGroup
                                .withMinusKeywords(singletonList("blalala")));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(cpmGeoproductAdGroup.MINUS_KEYWORDS.name())),
                        minusKeywordsNotAllowed()))));
    }

    @Test
    public void validateAdGroup_LibraryMinusKeywords_validationError() {
        ValidationResult<CpmGeoproductAdGroup, Defect> result =
                validation.validateAdGroup(
                        cpmGeoproductAdGroup
                                .withLibraryMinusKeywordsIds(singletonList(1L)));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(cpmGeoproductAdGroup.LIBRARY_MINUS_KEYWORDS_IDS)),
                        minusKeywordsNotAllowed()))));
    }
}
