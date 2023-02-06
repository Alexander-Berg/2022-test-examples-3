package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.sitelink;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexTextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BANNERS;
import static ru.yandex.direct.core.entity.banner.container.ComplexBanner.SITELINK_SET;
import static ru.yandex.direct.core.entity.sitelink.model.Sitelink.HREF;
import static ru.yandex.direct.core.entity.sitelink.model.Sitelink.TITLE;
import static ru.yandex.direct.core.entity.sitelink.model.SitelinkSet.SITELINKS;
import static ru.yandex.direct.core.validation.defects.Defects.hrefOrTurboRequired;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexUpdateSitelinkValidationLinkingTest extends ComplexUpdateSitelinkTestBase {

    // в контексте добавления баннеров

    @Test
    public void invalidFirstSitelinkInAddedBanner() {
        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks();
        complexBanner.getSitelinkSet().getSitelinks().get(0).setHref(null);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(adGroupForUpdate);

        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0),
                field(SITELINK_SET.name()), field(SITELINKS.name()), index(0), field(HREF.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, hrefOrTurboRequired())));
    }

    @Test
    public void invalidSecondSitelinkInAddedBanner() {
        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks();
        complexBanner.getSitelinkSet().getSitelinks().get(1).setHref(null);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(adGroupForUpdate);

        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0),
                field(SITELINK_SET.name()), field(SITELINKS.name()), index(1), field(HREF.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, hrefOrTurboRequired())));
    }

    @Test
    public void invalidSitelinkAfterNullSitelinkInAddedBanner() {
        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks();
        complexBanner.getSitelinkSet().getSitelinks().set(0, null);
        complexBanner.getSitelinkSet().getSitelinks().get(1).setHref(null);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(adGroupForUpdate);

        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0),
                field(SITELINK_SET.name()), field(SITELINKS.name()), index(1), field(HREF.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, hrefOrTurboRequired())));
    }

    @Test
    public void invalidSitelinkAfterValidAndNullSitelinksInAddedBanner() {
        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks();
        complexBanner.getSitelinkSet().getSitelinks().set(0, null);
        complexBanner.getSitelinkSet().getSitelinks().get(2).setHref(null);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(adGroupForUpdate);

        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0),
                field(SITELINK_SET.name()), field(SITELINKS.name()), index(2), field(HREF.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, hrefOrTurboRequired())));
    }

    @Test
    public void twoInvalidSitelinksInRarefiedSitelinkSetInAddedBanner() {
        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks();
        complexBanner.getSitelinkSet().getSitelinks().get(0).setTitle(null);
        complexBanner.getSitelinkSet().getSitelinks().set(1, null);
        complexBanner.getSitelinkSet().getSitelinks().get(2).setHref(null);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(adGroupForUpdate);

        Path errPath1 = path(index(0), field(COMPLEX_BANNERS.name()), index(0),
                field(SITELINK_SET.name()), field(SITELINKS.name()), index(0), field(TITLE.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath1, notNull())));

        Path errPath2 = path(index(0), field(COMPLEX_BANNERS.name()), index(0),
                field(SITELINK_SET.name()), field(SITELINKS.name()), index(2), field(HREF.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath2, hrefOrTurboRequired())));
    }

    @Test
    public void adGroupWithValidSitelinkSetAndAdGroupWithEmptyFirstBannerAndInvalidSecondSitelinkInSecondBanner() {
        createSecondAdGroup();

        ComplexTextBanner complexBanner1 = bannerWithRandomDescriptionSitelinks();
        ComplexTextBanner complexBanner2 = randomTitleTextComplexBanner();
        ComplexTextBanner complexBanner3 = bannerWithRandomDescriptionSitelinks();
        complexBanner3.getSitelinkSet().getSitelinks().get(1).setTitle(null);
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner1));
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(asList(complexBanner2, complexBanner3));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(adGroupForUpdate1, adGroupForUpdate2);

        Path errPath = path(index(1), field(COMPLEX_BANNERS.name()), index(1),
                field(SITELINK_SET.name()), field(SITELINKS.name()), index(1), field(TITLE.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, notNull())));
    }

    // в контексте обновления баннеров

    @Test
    public void invalidFirstSitelinkInUpdatedBanner() {
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);
        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks(bannerInfo);
        complexBanner.getSitelinkSet().getSitelinks().get(0).setHref(null);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(adGroupForUpdate);

        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0),
                field(SITELINK_SET.name()), field(SITELINKS.name()), index(0), field(HREF.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, hrefOrTurboRequired())));
    }

    @Test
    public void invalidSitelinkAfterValidSitelinkAndNullSitelinkInUpdatedBanner() {
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);
        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks(bannerInfo);
        complexBanner.getSitelinkSet().getSitelinks().set(0, null);
        complexBanner.getSitelinkSet().getSitelinks().get(2).setHref(null);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(adGroupForUpdate);

        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0),
                field(SITELINK_SET.name()), field(SITELINKS.name()), index(2), field(HREF.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, hrefOrTurboRequired())));
    }

    // в контексте обновления и добавления баннеров

    @Test
    public void invalidSitelinkInAddedBannerAndTwoInvalidSitelinksInRarefiedSitelinkSetInUpdatedBanner() {
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        ComplexTextBanner addedComplexBanner = bannerWithRandomDescriptionSitelinks();
        addedComplexBanner.getSitelinkSet().getSitelinks().get(2).setHref(null);
        ComplexTextBanner updatedComplexBanner = bannerWithRandomDescriptionSitelinks(bannerInfo);
        updatedComplexBanner.getSitelinkSet().getSitelinks().get(0).setHref(null);
        updatedComplexBanner.getSitelinkSet().getSitelinks().set(1, null);
        updatedComplexBanner.getSitelinkSet().getSitelinks().get(2).setTitle(null);

        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(addedComplexBanner, updatedComplexBanner));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(adGroupForUpdate);

        result.getValidationResult().flattenErrors().forEach(System.out::println);

        Path errPath1 = path(index(0), field(COMPLEX_BANNERS.name()), index(0),
                field(SITELINK_SET.name()), field(SITELINKS.name()), index(2), field(HREF.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath1, hrefOrTurboRequired())));

        Path errPath2 = path(index(0), field(COMPLEX_BANNERS.name()), index(1),
                field(SITELINK_SET.name()), field(SITELINKS.name()), index(0), field(HREF.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath2, hrefOrTurboRequired())));

        Path errPath3 = path(index(0), field(COMPLEX_BANNERS.name()), index(1),
                field(SITELINK_SET.name()), field(SITELINKS.name()), index(2), field(TITLE.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath3, notNull())));
    }
}
