package ru.yandex.direct.core.entity.banner.type.additionalhrefs;

import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.BannerAdditionalHref;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BannerAdditionalHrefValidatorTest {

    @Test
    public void success() {
        BannerAdditionalHref additionalHref = validAdditionalHref();

        ValidationResult<BannerAdditionalHref, Defect> result = validate(additionalHref);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void incorrectHref() {
        BannerAdditionalHref additionalHref = validAdditionalHref()
                .withHref("not a link");

        ValidationResult<BannerAdditionalHref, Defect> result = validate(additionalHref);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(field(BannerAdditionalHref.HREF)),
                invalidHref()))));
    }

    @Test
    public void nullHref() {
        BannerAdditionalHref additionalHref = validAdditionalHref()
                .withHref(null);

        ValidationResult<BannerAdditionalHref, Defect> result = validate(additionalHref);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(field(BannerAdditionalHref.HREF)),
                notNull()))));
    }

    private BannerAdditionalHref validAdditionalHref() {
        return new BannerAdditionalHref().withHref("http://ya.ru");
    }

    private ValidationResult<BannerAdditionalHref, Defect> validate(BannerAdditionalHref additionalHref) {
        return new BannerAdditionalHrefValidator().apply(additionalHref);
    }

}
