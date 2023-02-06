package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BANNERS;
import static ru.yandex.direct.core.entity.banner.container.ComplexBanner.SITELINK_SET;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidSitelinkSetIdUsage;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredButEmptyHrefOrTurboOrVcardIdOrPermalink;
import static ru.yandex.direct.core.entity.sitelink.model.Sitelink.HREF;
import static ru.yandex.direct.core.entity.sitelink.model.SitelinkSet.SITELINKS;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.invalidSitelinkHref;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinkSets.defaultSitelinkSet;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexTextAddValidationTest extends ComplexTextAddValidationTestBase {

    @Test
    public void hasErrorWhenBannerLangIsNotFitToAdGroupGeo() {
        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(clientTextBanner()
                        .withLanguage(null)
                        .withTitle("Київ"));
        ComplexTextAdGroup complexAdGroup = fullAdGroup()
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withGeo(singletonList(RUSSIA_REGION_ID));

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath,
                BannerDefectIds.LanguageDefect.INCONSISTENT_LANGUAGE_WITH_GEO)));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorWhenBothHrefAndVcardAreNull() {
        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(clientTextBanner()
                        .withHref(null)
                        .withDisplayHref(null));
        ComplexTextAdGroup complexAdGroup = fullAdGroup()
                .withComplexBanners(singletonList(complexBanner));

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath,
                requiredButEmptyHrefOrTurboOrVcardIdOrPermalink())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasNoErrorsWhenHrefIsNullAndVcardIsSet() {
        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(clientTextBanner()
                        .withHref(null)
                        .withDisplayHref(null))
                .withVcard(fullVcard());
        ComplexTextAdGroup complexAdGroup = fullAdGroup()
                .withComplexBanners(singletonList(complexBanner));

        addAndCheckComplexAdGroups(complexAdGroup);
    }

    @Test
    public void hasNoErrorsWhenHrefIsSetAndVcardIsNull() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        complexAdGroup.getComplexBanners().get(0)
                .withVcard(null)
                .withSitelinkSet(null);

        addAndCheckComplexAdGroups(complexAdGroup);
    }

    @Test
    public void hasNoErrorsWhenHrefIsSetAndSitelinkSetIsSet() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        complexAdGroup.getComplexBanners().get(0).withVcard(null);

        addAndCheckComplexAdGroups(complexAdGroup);
    }

    @Test
    public void hasNoErrorsWhenHrefIsSetAndSitelinkSetIsSetAndVcardIsSet() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        addAndCheckComplexAdGroups(complexAdGroup);
    }

    @Test
    public void hasErrorWhenSitelinkSetIsSetAndHrefIsNull() {
        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(clientTextBanner()
                        .withHref(null)
                        .withDisplayHref(null))
                .withVcard(fullVcard())
                .withSitelinkSet(defaultSitelinkSet());
        ComplexTextAdGroup complexAdGroup = fullAdGroup()
                .withComplexBanners(singletonList(complexBanner));

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, invalidSitelinkSetIdUsage())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasTwoErrorsWhenSitelinkSetIsSetAndHrefIsNullAndVcardIsNull() {
        ComplexBanner complexBanner = new ComplexBanner()
                .withSitelinkSet(defaultSitelinkSet())
                .withBanner(clientTextBanner()
                        .withHref(null)
                        .withDisplayHref(null));
        ComplexTextAdGroup complexAdGroup = fullAdGroup()
                .withComplexBanners(singletonList(complexBanner));

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath,
                requiredButEmptyHrefOrTurboOrVcardIdOrPermalink())));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, invalidSitelinkSetIdUsage())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должно присутствовать всего две ошибки", vr.flattenErrors(), hasSize(2));
    }

    @Test
    public void complexValidationDoesNotFailWhenBannerHrefIsInvalid() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        var banner = complexAdGroup.getComplexBanners().get(0).getBanner();

        ((TextBanner) banner).withHref("urtwebn");

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0),
                field(OldBanner.HREF.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, invalidHref())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void complexValidationDoesNotFailWhenSitelinkHrefIsInvalid() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup();
        complexAdGroup.getComplexBanners().get(0).getSitelinkSet().getSitelinks().get(0).withHref("urtwebn");

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0), field(SITELINK_SET.name()),
                field(SITELINKS.name()), index(0), field(HREF.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, invalidSitelinkHref())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }
}
