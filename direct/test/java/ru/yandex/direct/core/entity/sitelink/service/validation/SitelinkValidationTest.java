package ru.yandex.direct.core.entity.sitelink.service.validation;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkConstants.MAX_SITELINK_DESC_LENGTH;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkConstants.MAX_SITELINK_HREF_LENGTH;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkConstants.MAX_SITELINK_TITLE_LENGTH;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.allowedSymbolsSitelinkDesc;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.allowedSymbolsSitelinkTitle;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.core.validation.defects.Defects.hrefOrTurboRequired;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class SitelinkValidationTest {

    private Sitelink defaultSitelink;

    @Before
    public void before() {
        defaultSitelink = defaultSitelink();
    }

    @Test
    public void positiveValidationResultWhenNoErrors() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink).getResult();
        assertThat(actual.flattenErrors(), hasSize(0));
    }

    @Test
    public void validate_HrefNull() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink.withHref(null)).getResult();

        assertThat(actual.flattenErrors(), contains(validationError(path(field("href")), hrefOrTurboRequired())));
    }

    @Test
    public void validate_HrefLengthGreaterMax() {

        ValidationResult<Sitelink, Defect> actual = SitelinkValidationService.baseChecks(defaultSitelink.withHref(
                "http://" + RandomStringUtils.randomAlphabetic(MAX_SITELINK_HREF_LENGTH))).getResult();

        assertThat(actual.flattenErrors(), contains(validationError(path(field("href")),
                SitelinkDefects.sitelinkHrefTooLong(MAX_SITELINK_HREF_LENGTH))));
    }

    @Test
    public void validate_HrefProtocol() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink.withHref("ya.ru")).getResult();

        assertThat(actual.flattenErrors(), contains(validationError(path(field("href")),
                SitelinkDefects.invalidSitelinkHref())));
    }

    @Test
    public void validate_HrefContainsExclamation() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink.withHref("http://ja!ja.ru")).getResult();

        assertThat(actual.flattenErrors(), contains(validationError(path(field("href")),
                SitelinkDefects.invalidSitelinkHref())));
    }

    @Test
    public void validate_HrefContainsValidUnicodeUrl() {

        ValidationResult<Sitelink, Defect> actual = SitelinkValidationService.baseChecks(defaultSitelink.withHref(
                "http://профи-мастера.рф/услуги-сантехника-екб/#1")).getResult();
        assertThat(actual, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_TitleNull() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink.withTitle(null)).getResult();

        assertThat(actual.flattenErrors(), contains(validationError(path(field("title")), notNull())));
    }

    @Test
    public void validate_TitleEmpty() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink.withTitle("")).getResult();

        assertThat(actual.flattenErrors(), contains(validationError(path(field("title")),
                SitelinkDefects.sitelinksTitleEmpty())));
    }

    @Test
    public void validate_TitleLengthGreaterMax() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink
                        .withTitle(RandomStringUtils.randomAlphabetic(MAX_SITELINK_TITLE_LENGTH + 1))).getResult();

        assertThat(actual.flattenErrors(), contains(validationError(path(field("title")),
                SitelinkDefects.sitelinkTitleTooLong(MAX_SITELINK_TITLE_LENGTH))));
    }

    @Test
    public void validate_TitleAllowExclamationLetter() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink.withTitle("test!")).getResult();

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("title")), allowedSymbolsSitelinkTitle())));
    }

    @Test
    public void validate_TitleAllowQuestionMarkLetter() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink.withTitle("test?")).getResult();

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("title")), allowedSymbolsSitelinkTitle())));
    }

    @Test
    public void validate_TitleHasValidNationalLetter() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink.withTitle("ў")).getResult();

        assertThat(actual, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_DescEmpty() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink.withDescription("")).getResult();

        assertThat(actual.flattenErrors(), contains(validationError(path(field("description")),
                SitelinkDefects.sitelinksDescriptionEmpty())));
    }


    @Test
    public void validate_DescLengthGreaterMax() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink
                        .withDescription(RandomStringUtils.randomAlphabetic(MAX_SITELINK_DESC_LENGTH + 1))).getResult();

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("description")),
                        SitelinkDefects.sitelinkDescriptionTooLong(MAX_SITELINK_DESC_LENGTH))));
    }

    @Test
    public void validate_DescAllowExclamationLetter() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink.withDescription("test!")).getResult();

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("description")), allowedSymbolsSitelinkDesc())));
    }

    @Test
    public void validate_DescAllowQuestionMarkLetter() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink.withDescription("test?")).getResult();

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("description")), allowedSymbolsSitelinkDesc())));
    }

    @Test
    public void validate_DescHasValidNationalLetter() {

        ValidationResult<Sitelink, Defect> actual =
                SitelinkValidationService.baseChecks(defaultSitelink.withDescription("ў")).getResult();

        assertThat(actual, hasNoDefectsDefinitions());
    }
}
