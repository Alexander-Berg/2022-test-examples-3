package ru.yandex.direct.core.entity.adgroup.service.complex.text.update;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.queryrec.model.Language;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BANNERS;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.inconsistentGeoWithBannerLanguages;
import static ru.yandex.direct.core.entity.banner.container.ComplexBanner.SITELINK_SET;
import static ru.yandex.direct.core.entity.banner.model.Language.KK;
import static ru.yandex.direct.core.entity.banner.model.Language.NO;
import static ru.yandex.direct.core.entity.banner.model.Language.RU_;
import static ru.yandex.direct.core.entity.banner.model.Language.UK;
import static ru.yandex.direct.core.entity.banner.model.Language.UNKNOWN;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidSitelinkSetIdUsage;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredButEmptyHrefOrTurboOrVcardIdOrPermalink;
import static ru.yandex.direct.core.entity.sitelink.model.Sitelink.HREF;
import static ru.yandex.direct.core.entity.sitelink.model.SitelinkSet.SITELINKS;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.invalidSitelinkHref;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinkSets.defaultSitelinkSet;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexAdGroupUpdateOperationValidationTest extends ComplexAdGroupUpdateOperationValidationTestBase {

    @Test
    public void hasErrorOnBannerWhenAddedBannerLangIsNotFitToAdGroupGeo() {
        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(clientTextBanner()
                        .withLanguage(null)
                        .withTitle("Київ"));
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withGeo(singletonList(RUSSIA_REGION_ID));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath,
                BannerDefectIds.LanguageDefect.INCONSISTENT_LANGUAGE_WITH_GEO)));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorOnBannerWhenUpdatedBannerLangIsNotFitToAdGroupGeo() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo1);

        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(clientTextBanner()
                        .withId(bannerInfo.getBannerId())
                        .withLanguage(null)
                        .withTitle("Київ"));
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withGeo(singletonList(RUSSIA_REGION_ID));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath,
                BannerDefectIds.LanguageDefect.INCONSISTENT_LANGUAGE_WITH_GEO)));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    // проверка, что валидация в операции группы отключена и не смотрит на старое значение языка баннера в базе
    @Test
    public void hasNoErrorsWhenOldLangValueOfUpdatedBannerIsNotFitToNewAdGroupGeo() {
        NewTextBannerInfo bannerInfo = steps.textBannerSteps()
                .createBanner(
                        new NewTextBannerInfo()
                                .withBanner(fullTextBanner()
                                        .withDomain(null)
                                        .withTitle("Київ"))
                                .withAdGroupInfo(adGroupInfo1));

        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(((TextBanner) bannerInfo.getBanner()).withTitle("Киев"));
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withGeo(singletonList(RUSSIA_REGION_ID));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);
    }

    // проверка, что валидация в операции группы отключена и на баннере будет ошибка только из комплексной валидации
    @Test
    public void hasErrorOnBannerWhenExistingLangValueOfUpdatedBannerIsNotFitToNewAdGroupGeo() {
        var bannerInfo = steps.textBannerSteps()
                .createBanner(
                        new NewTextBannerInfo()
                                .withBanner(fullTextBanner()
                                        .withDomain(null)
                                        .withLanguage(UNKNOWN)
                                        .withTitle("Київ"))
                                .withAdGroupInfo(adGroupInfo1));

        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(bannerInfo.getBanner());
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withGeo(singletonList(RUSSIA_REGION_ID));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath,
                BannerDefectIds.LanguageDefect.INCONSISTENT_LANGUAGE_WITH_GEO)));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    // проверка соответствия между гео группы и языком незатронутых запросом баннеров
    @Test
    public void hasErrorOnAdGroupGeoWhenLangValueOfUntouchedBannerIsNotFitToNewAdGroupGeo() {
        TextBannerInfo bannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner()
                        .withLanguage(null)
                        .withTitle("Київ"), adGroupInfo1);

        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(fullTextBanner());
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withGeo(singletonList(RUSSIA_REGION_ID));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(AdGroup.GEO.name()));
        Defect expectedDefect = inconsistentGeoWithBannerLanguages(Language.UKRAINIAN, bannerInfo.getBannerId());
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, expectedDefect)));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    // проверка соответствия между гео группы и языком незатронутых запросом баннеров
    @Test
    public void hasErrorOnAdGroupGeoWhenLangValueOfUntouchedBannerIsNotFitToNewAdGroupGeoAndNewAdGroupHasNoBanners() {
        TextBannerInfo bannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner().
                        withLanguage(null)
                        .withTitle("Київ"), adGroupInfo1);

        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId).withComplexBanners(emptyList());
        complexAdGroup.getAdGroup().withGeo(singletonList(RUSSIA_REGION_ID));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);

        Path errPath = path(index(0), field(AdGroup.GEO.name()));
        Defect expectedDefect = inconsistentGeoWithBannerLanguages(Language.UKRAINIAN, bannerInfo.getBannerId());
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, expectedDefect)));

        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    // проверка соответствия между гео группы и языком незатронутых запросом баннеров
    @Test
    public void hasErrorsBothOnUpdatedBannerAndOnAdGroupGeoWhenLangValueOfBannersIsNotFitToNewAdGroupGeo() {
        TextBannerInfo untouchedBannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner()
                        .withLanguage(null)
                        .withTitle("Київ"), adGroupInfo1);
        var updatedBannerInfo = steps.textBannerSteps()
                .createBanner(
                        new NewTextBannerInfo()
                                .withBanner(fullTextBanner()
                                .withDomain(null)
                                .withLanguage(UNKNOWN))
                                .withAdGroupInfo(adGroupInfo1));

        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(((TextBanner) updatedBannerInfo.getBanner()).withTitle("Київ"));
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withGeo(singletonList(RUSSIA_REGION_ID));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);

        Path errPath1 = path(index(0), field(AdGroup.GEO.name()));
        Defect expectedDefect1 =
                inconsistentGeoWithBannerLanguages(Language.UKRAINIAN, untouchedBannerInfo.getBannerId());
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath1, expectedDefect1)));

        Path errPath2 = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath2,
                BannerDefectIds.LanguageDefect.INCONSISTENT_LANGUAGE_WITH_GEO)));

        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должно присутствовать всего две ошибки", vr.flattenErrors(), hasSize(2));
    }

    // проверка соответствия между гео группы и языком незатронутых запросом баннеров
    @Test
    public void hasNoErrorsWhenLangValueOfUntouchedBannerIsFitToNewAdGroupGeo() {
        steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo1);

        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(fullTextBanner());
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withGeo(singletonList(RUSSIA_REGION_ID));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);
    }

    @Test
    public void hasErrorWhenAddedBannerHasNeitherVcardNorHref() {
        var banner = fullTextBanner().withHref(null).withDisplayHref(null);
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(new ComplexBanner()
                        .withBanner(banner)
                        .withVcard(null)
                        .withSitelinkSet(null)
                ));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath,
                requiredButEmptyHrefOrTurboOrVcardIdOrPermalink())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorWhenUpdatedBannerHasNeitherVcardNorHref() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo1);

        var banner = clientTextBanner()
                .withId(bannerInfo.getBannerId())
                .withHref(null)
                .withDisplayHref(null);
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(new ComplexBanner()
                        .withBanner(banner)
                        .withVcard(null)
                        .withSitelinkSet(null)
                ));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath,
                requiredButEmptyHrefOrTurboOrVcardIdOrPermalink())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasNoErrorsWhenBannerHasVcardWithoutHref() {
        var banner = fullTextBanner().withHref(null).withDisplayHref(null);
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(new ComplexBanner()
                        .withBanner(banner)
                        .withVcard(fullVcard())
                        .withSitelinkSet(null)
                ));

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);
    }

    @Test
    public void hasNoErrorsWhenBannerHasHrefAndNoVcard() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId);
        complexAdGroup.getComplexBanners().get(0)
                .withSitelinkSet(null)
                .withVcard(null);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);
    }

    @Test
    public void hasErrorWhenAddedBannerHasSitelinksWithoutHref() {
        var banner = fullTextBanner().withHref(null).withDisplayHref(null);
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(new ComplexBanner()
                        .withBanner(banner)
                        .withVcard(fullVcard())
                        .withSitelinkSet(defaultSitelinkSet())));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, invalidSitelinkSetIdUsage())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorWhenUpdatedBannerHasSitelinksWithoutHref() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo1);

        var banner = clientTextBanner()
                .withId(bannerInfo.getBannerId())
                .withHref(null)
                .withDisplayHref(null);
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(new ComplexBanner()
                        .withBanner(banner)
                        .withVcard(fullVcard())
                        .withSitelinkSet(defaultSitelinkSet())));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, invalidSitelinkSetIdUsage())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasNoErrorsWhenBannerHasHrefAndSitelinks() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId);
        complexAdGroup.getComplexBanners().get(0).withVcard(null);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);
    }

    @Test
    public void hasTwoErrorsWhenBannerHasSitelinksWithoutHrefAndVcard() {
        var banner = fullTextBanner().withHref(null).withDisplayHref(null);
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(new ComplexBanner()
                        .withBanner(banner)
                        .withVcard(null)
                        .withSitelinkSet(defaultSitelinkSet())
                ));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);

        Path errPath1 = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath1,
                requiredButEmptyHrefOrTurboOrVcardIdOrPermalink())));

        Path errPath2 = path(index(0), field(COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath2, invalidSitelinkSetIdUsage())));

        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должно присутствовать две ошибки", vr.flattenErrors(), hasSize(2));
    }

    @Test
    public void complexValidationDoesNotFailWhenBannerHrefIsInvalid() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId);
        ((TextBanner) complexAdGroup.getComplexBanners().get(0).getBanner()).withHref("urtwebn");

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0),
                field(OldBanner.HREF.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, invalidHref())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void complexValidationDoesNotFailWhenSitelinkHrefIsInvalid() {
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId);
        complexAdGroup.getComplexBanners().get(0).getSitelinkSet().getSitelinks().get(0).withHref("urtwebn");

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0), field(SITELINK_SET.name()),
                field(SITELINKS.name()), index(0), field(HREF.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, invalidSitelinkHref())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void update_UntouchedBannerHasUkrainianTextAndRussianLanguage_RegionRussia_NoError() {
        steps.bannerSteps().createBanner(
                activeTextBanner()
                        .withLanguage(RU_)
                        .withTitle("Київ"), adGroupInfo1);

        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(fullTextBanner());
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withGeo(singletonList(RUSSIA_REGION_ID));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsSuccess(complexAdGroup);

        assertThat(vr.flattenErrors(), empty());
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void update_UntouchedBannerHasUkrainianTextAndKazakhLanguage_RegionUkraine_ValidationError() {
        TextBannerInfo bannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner()
                        .withLanguage(KK)
                        .withTitle("Київ"), adGroupInfo1);

        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(fullTextBanner().withLanguage(UK));
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withGeo(singletonList(UKRAINE_REGION_ID));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(AdGroup.GEO.name()));
        Defect expectedDefect = inconsistentGeoWithBannerLanguages(Language.KAZAKH, bannerInfo.getBannerId());

        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, expectedDefect)));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void update_UntouchedBannerHasUkrainianTextAndNoLanguage_RegionRussia_ValidationError() {
        TextBannerInfo bannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner()
                        .withLanguage(NO)
                        .withTitle("Київ"), adGroupInfo1);

        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(fullTextBanner());
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withGeo(singletonList(RUSSIA_REGION_ID));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(AdGroup.GEO.name()));
        Defect expectedDefect = inconsistentGeoWithBannerLanguages(Language.UKRAINIAN, bannerInfo.getBannerId());

        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, expectedDefect)));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void update_UntouchedBannerHasUkrainianTextAndNoLanguage_RegionUkraine_NoError() {
        steps.bannerSteps().createBanner(
                activeTextBanner()
                        .withLanguage(NO)
                        .withTitle("Київ"), adGroupInfo1);

        ComplexBanner complexBanner = new ComplexBanner()
                .withBanner(fullTextBanner().withLanguage(UK));
        ComplexTextAdGroup complexAdGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(complexBanner));
        complexAdGroup.getAdGroup().withGeo(singletonList(UKRAINE_REGION_ID));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsSuccess(complexAdGroup);

        assertThat(vr.flattenErrors(), empty());
        assertThat(vr, hasNoDefectsDefinitions());
    }
}
