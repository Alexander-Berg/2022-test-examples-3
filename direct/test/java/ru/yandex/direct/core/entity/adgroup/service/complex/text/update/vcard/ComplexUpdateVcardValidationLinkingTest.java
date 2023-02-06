package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.vcard;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexTextBanner;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexUpdateVcardValidationLinkingTest extends ComplexUpdateVcardTestBase {

    // в контексте добавления баннеров

    @Test
    public void adGroupWithInvalidVcardInAddedBanner() {
        ComplexTextBanner validComplexBanner = randomTitleTextComplexBanner();
        Vcard invalidVcard = randomApartVcard().withPhone(null);
        ComplexTextBanner invalidComplexBanner = randomTitleTextComplexBanner()
                .withVcard(invalidVcard);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(validComplexBanner, invalidComplexBanner));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(adGroupForUpdate);
        Path errPath = path(index(0), field(ComplexTextAdGroup.COMPLEX_BANNERS), index(1),
                field(ComplexBanner.VCARD), field(Vcard.PHONE));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, notNull())));
        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithValidVcardAndInvalidVcardInAddedBanners() {
        Vcard validVcard = randomApartVcard();
        ComplexTextBanner validComplexBanner = randomTitleTextComplexBanner()
                .withVcard(validVcard);
        Vcard invalidVcard = randomApartVcard().withPhone(null);
        ComplexTextBanner invalidComplexBanner = randomTitleTextComplexBanner()
                .withVcard(invalidVcard);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(validComplexBanner, invalidComplexBanner));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(adGroupForUpdate);
        Path errPath = path(index(0), field(ComplexTextAdGroup.COMPLEX_BANNERS), index(1),
                field(ComplexBanner.VCARD), field(Vcard.PHONE));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, notNull())));
        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithValidVcardAndAdGroupWithInvalidVcardInAddedBanners() {
        createSecondAdGroup();

        Vcard validVcard = randomApartVcard();
        ComplexTextBanner validComplexBanner = randomTitleTextComplexBanner()
                .withVcard(validVcard);
        Vcard invalidVcard = randomApartVcard().withPhone(null);
        ComplexTextBanner invalidComplexBanner = randomTitleTextComplexBanner()
                .withVcard(invalidVcard);

        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(validComplexBanner));
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(singletonList(invalidComplexBanner));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(adGroupForUpdate1, adGroupForUpdate2);
        Path errPath = path(index(1), field(ComplexTextAdGroup.COMPLEX_BANNERS), index(0),
                field(ComplexBanner.VCARD), field(Vcard.PHONE));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, notNull())));
        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    // в контексте обновления баннеров

    @Test
    public void adGroupWithValidAddedBannerAndInvalidVcardInUpdatedBanner() {
        VcardInfo oldVcardInfo = createRandomApartVcard();
        TextBannerInfo bannerInfo = createRandomTitleBanner(adGroupInfo1, oldVcardInfo);

        ComplexTextBanner validComplexBanner = randomTitleTextComplexBanner();
        Vcard invalidVcard = randomApartVcard().withPhone(null);
        ComplexTextBanner invalidComplexBanner = randomTitleTextComplexBanner(bannerInfo)
                .withVcard(invalidVcard);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(validComplexBanner, invalidComplexBanner));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(adGroupForUpdate);
        Path errPath = path(index(0), field(ComplexTextAdGroup.COMPLEX_BANNERS), index(1),
                field(ComplexBanner.VCARD), field(Vcard.PHONE));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, notNull())));
        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithValidAddedBannerAndAdGroupWithValidAndInvalidVcardInUpdatedBanners() {
        createSecondAdGroup();

        VcardInfo oldVcardInfo = createRandomApartVcard();
        TextBannerInfo bannerInfo = createRandomTitleBanner(adGroupInfo2, oldVcardInfo);

        ComplexTextBanner validAddedComplexBanner = randomTitleTextComplexBanner();

        Vcard validVcard = randomApartVcard();
        ComplexTextBanner validUpdatedComplexBanner = randomTitleTextComplexBanner()
                .withVcard(validVcard);
        Vcard invalidVcard = randomApartVcard().withPhone(null);
        ComplexTextBanner invalidUpdatedComplexBanner = randomTitleTextComplexBanner(bannerInfo)
                .withVcard(invalidVcard);

        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(validAddedComplexBanner));
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(asList(validUpdatedComplexBanner, invalidUpdatedComplexBanner));

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(adGroupForUpdate1, adGroupForUpdate2);
        Path errPath = path(index(1), field(ComplexTextAdGroup.COMPLEX_BANNERS), index(1),
                field(ComplexBanner.VCARD), field(Vcard.PHONE));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, notNull())));
        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }
}
