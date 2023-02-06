package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.banner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.stringShouldNotBeBlank;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты линковки ошибок в создаваемых/обновляемых объектах
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexUpdateBannerValidationLinkingTest extends ComplexUpdateBannerTestBase {

    // добавление

    @Test
    public void adGroupWithInvalidAddedBanner() {
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(emptyTitleTextComplexBanner()));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(adGroupForUpdate));

        Path bannerErrPath = path(index(0), field(ComplexTextAdGroup.COMPLEX_BANNERS.name()),
                index(0), field(OldTextBanner.TITLE.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(bannerErrPath, stringShouldNotBeBlank())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithValidBannersAndAdGroupWithInvalidAddedBannerAndValidUpdated() {
        createSecondAdGroup();
        TextBannerInfo bannerInfo1 = createRandomTitleTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createRandomTitleTextBanner(adGroupInfo2);

        ComplexTextBanner updatedBanner1 = randomTitleTextComplexBanner(bannerInfo1);
        ComplexTextBanner updatedBanner2 = randomTitleTextComplexBanner(bannerInfo2);
        ComplexTextBanner addedBanner1 = randomTitleTextComplexBanner();
        ComplexTextBanner addedBanner2 = emptyTitleTextComplexBanner();
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(updatedBanner1, addedBanner1));
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(asList(addedBanner2, updatedBanner2));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(
                asList(adGroupForUpdate1, adGroupForUpdate2));

        Path bannerErrPath = path(index(1), field(ComplexTextAdGroup.COMPLEX_BANNERS.name()),
                index(0), field(OldTextBanner.TITLE.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(bannerErrPath, stringShouldNotBeBlank())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithValidBannersAndAdGroupWithValidUpdatedBannerAndInvalidAdded() {
        createSecondAdGroup();
        TextBannerInfo bannerInfo1 = createRandomTitleTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createRandomTitleTextBanner(adGroupInfo2);

        ComplexTextBanner updatedBanner1 = randomTitleTextComplexBanner(bannerInfo1);
        ComplexTextBanner updatedBanner2 = randomTitleTextComplexBanner(bannerInfo2);
        ComplexTextBanner addedBanner1 = randomTitleTextComplexBanner();
        ComplexTextBanner addedBanner2 = emptyTitleTextComplexBanner();
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(updatedBanner1, addedBanner1));
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(asList(updatedBanner2, addedBanner2));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(
                asList(adGroupForUpdate1, adGroupForUpdate2));

        Path bannerErrPath = path(index(1), field(ComplexTextAdGroup.COMPLEX_BANNERS.name()),
                index(1), field(OldTextBanner.TITLE.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(bannerErrPath, stringShouldNotBeBlank())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    // обновление

    @Test
    public void adGroupWithInvalidUpdatedBanner() {
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(emptyTitleTextComplexBanner(bannerInfo)));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(adGroupForUpdate));

        Path bannerErrPath = path(index(0), field(ComplexTextAdGroup.COMPLEX_BANNERS.name()),
                index(0), field(OldTextBanner.TITLE.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(bannerErrPath, stringShouldNotBeBlank())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithValidBannersAndAdGroupWithInvalidUpdatedBannerAndValidAdded() {
        createSecondAdGroup();
        TextBannerInfo bannerInfo1 = createRandomTitleTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createRandomTitleTextBanner(adGroupInfo2);

        ComplexTextBanner updatedBanner1 = randomTitleTextComplexBanner(bannerInfo1);
        ComplexTextBanner updatedBanner2 = emptyTitleTextComplexBanner(bannerInfo2);
        ComplexTextBanner addedBanner1 = randomTitleTextComplexBanner();
        ComplexTextBanner addedBanner2 = randomTitleTextComplexBanner();
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(updatedBanner1, addedBanner1));
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(asList(updatedBanner2, addedBanner2));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(
                asList(adGroupForUpdate1, adGroupForUpdate2));

        Path bannerErrPath = path(index(1), field(ComplexTextAdGroup.COMPLEX_BANNERS.name()),
                index(0), field(OldTextBanner.TITLE.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(bannerErrPath, stringShouldNotBeBlank())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithValidBannersAndAdGroupWithValidAddedBannerAndInvalidUpdated() {
        createSecondAdGroup();
        TextBannerInfo bannerInfo1 = createRandomTitleTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createRandomTitleTextBanner(adGroupInfo2);

        ComplexTextBanner updatedBanner1 = randomTitleTextComplexBanner(bannerInfo1);
        ComplexTextBanner updatedBanner2 = emptyTitleTextComplexBanner(bannerInfo2);
        ComplexTextBanner addedBanner1 = randomTitleTextComplexBanner();
        ComplexTextBanner addedBanner2 = randomTitleTextComplexBanner();
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(updatedBanner1, addedBanner1));
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(asList(addedBanner2, updatedBanner2));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(
                asList(adGroupForUpdate1, adGroupForUpdate2));

        Path bannerErrPath = path(index(1), field(ComplexTextAdGroup.COMPLEX_BANNERS.name()),
                index(1), field(OldTextBanner.TITLE.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(bannerErrPath, stringShouldNotBeBlank())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    // добавление + обновление

    @Test
    public void adGroupWithValidBannersAndAdGroupWithInvalidAddedAndUpdatedBanners() {
        createSecondAdGroup();
        TextBannerInfo bannerInfo1 = createRandomTitleTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createRandomTitleTextBanner(adGroupInfo2);

        ComplexTextBanner updatedBanner1 = randomTitleTextComplexBanner(bannerInfo1);
        ComplexTextBanner updatedBanner2 = emptyTitleTextComplexBanner(bannerInfo2);
        ComplexTextBanner addedBanner1 = randomTitleTextComplexBanner();
        ComplexTextBanner addedBanner2 = randomTitleTextComplexBanner();
        addedBanner2.getBanner().withBody("");
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(updatedBanner1, addedBanner1));
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(asList(updatedBanner2, addedBanner2));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(
                asList(adGroupForUpdate1, adGroupForUpdate2));

        Path bannerErrPath1 = path(index(1), field(ComplexTextAdGroup.COMPLEX_BANNERS.name()),
                index(0), field(OldTextBanner.TITLE.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(bannerErrPath1, stringShouldNotBeBlank())));

        Path bannerErrPath2 = path(index(1), field(ComplexTextAdGroup.COMPLEX_BANNERS.name()),
                index(1), field(OldTextBanner.BODY.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(bannerErrPath2, stringShouldNotBeBlank())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(2));
    }
}
