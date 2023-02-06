package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.dynamicTextDomainIsNotSet;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.invalidDynamicTextDomain;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.DynamicTextAdGroupValidation.MAX_DOMAIN_URL_SIZE;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.DynamicTextAdGroupValidation.MAX_TRACKING_PARAMS_SIZE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class DynamicTextAdGroupValidationTest {
    private static final String VALID_DOMAIN_URL = "www.yandex.ru";

    private DynamicTextAdGroup dynamicText;
    private DynamicTextAdGroupValidation validation;

    @Before
    public void setUp() {
        dynamicText = new DynamicTextAdGroup()
                .withDomainUrl(VALID_DOMAIN_URL);

        validation = new DynamicTextAdGroupValidation();
    }

    @Test
    public void validateAdGroup_trackingParamsIsValid_noErrors() {
        ValidationResult<DynamicTextAdGroup, Defect> result =
                validation.validateAdGroup(
                        dynamicText.withTrackingParams("a=b"));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAdGroup_trackingParamsIsTooLong_validationError() {
        ValidationResult<DynamicTextAdGroup, Defect> result =
                validation.validateAdGroup(
                        dynamicText.withTrackingParams(
                                "a=" + RandomStringUtils.randomAlphanumeric(MAX_TRACKING_PARAMS_SIZE)));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(DynamicTextAdGroup.TRACKING_PARAMS.name())),
                        CollectionDefects.maxStringLength(MAX_TRACKING_PARAMS_SIZE)))));
    }

    @Test
    public void validateAdGroup_dynamicTextDomainUrlIsValid_noErrors() {
        ValidationResult<DynamicTextAdGroup, Defect> result =
                validation.validateAdGroup(dynamicText);

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAdGroup_dynamicTextDomainUrlIsNull_validationError() {
        ValidationResult<DynamicTextAdGroup, Defect> result =
                validation.validateAdGroup(dynamicText.withDomainUrl(null));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(DynamicTextAdGroup.DOMAIN_URL.name())), dynamicTextDomainIsNotSet()))));
    }

    @Test
    public void validateAdGroup_dynamicTextDomainUrlIsEmpty_validationError() {
        ValidationResult<DynamicTextAdGroup, Defect> result =
                validation.validateAdGroup(dynamicText.withDomainUrl(""));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(DynamicTextAdGroup.DOMAIN_URL.name())), dynamicTextDomainIsNotSet()))));
    }

    @Test
    public void validateAdGroup_dynamicTextDomainUrlIsBlank_validationError() {
        ValidationResult<DynamicTextAdGroup, Defect> result =
                validation.validateAdGroup(dynamicText.withDomainUrl("  "));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(DynamicTextAdGroup.DOMAIN_URL.name())), invalidDynamicTextDomain()))));
    }

    @Test
    public void validateAdGroup_dynamicTextDomainUrlIsTooLong_validationError() {
        ValidationResult<DynamicTextAdGroup, Defect> result =
                validation.validateAdGroup(
                        dynamicText.withDomainUrl(
                                RandomStringUtils.randomAlphabetic(MAX_DOMAIN_URL_SIZE) + ".ru"));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(DynamicTextAdGroup.DOMAIN_URL.name())),
                        CollectionDefects.maxStringLength(MAX_DOMAIN_URL_SIZE)))));
    }

    @Test
    public void validateAdGroup_dynamicTextDomainUrlIsNotADomain_validationError() {
        ValidationResult<DynamicTextAdGroup, Defect> result =
                validation.validateAdGroup(dynamicText.withDomainUrl("wwwyandexru"));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(DynamicTextAdGroup.DOMAIN_URL.name())), invalidDynamicTextDomain()))));
    }
}
