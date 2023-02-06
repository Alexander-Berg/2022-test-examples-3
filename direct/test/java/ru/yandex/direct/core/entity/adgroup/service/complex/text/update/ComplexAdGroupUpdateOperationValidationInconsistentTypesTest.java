package ru.yandex.direct.core.entity.adgroup.service.complex.text.update;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BANNERS;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.fullAdGroupWithImageCreativeBannerForUpdate;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.fullAdGroupWithImageHashBannerForUpdate;
import static ru.yandex.direct.core.entity.banner.container.ComplexBanner.SITELINK_SET;
import static ru.yandex.direct.core.entity.banner.container.ComplexBanner.VCARD;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndAdgroupType;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.imageAdImageFormat;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.core.testing.data.TestNewDynamicBanners.clientDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceBanners.clientPerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinkSets.defaultSitelinkSet;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexAdGroupUpdateOperationValidationInconsistentTypesTest extends ComplexAdGroupUpdateOperationValidationTestBase {

    @Test
    public void hasErrorWhenAddedBannerTypeIsNotFitToAdGroupType() {
        ComplexTextAdGroup adGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(new ComplexBanner().withBanner(clientCpmBanner(null))));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(adGroup);
        Path errPath = path(index(0), field(ComplexTextAdGroup.COMPLEX_BANNERS.name()), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, inconsistentStateBannerTypeAndAdgroupType())));
    }

    @Test
    public void hasNoErrorsWhenAddedBannerTypeFitsToTextAdGroup() {
        ComplexTextAdGroup adGroup = fullAdGroup(adGroupId)
                .withComplexBanners(singletonList(new ComplexBanner().withBanner(clientDynamicBanner())));

        ValidationResult<?, Defect> vr = updateAndCheckResultIsSuccess(adGroup);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void throwsExceptionWhenUpdatedBannerTypeIsNotSupported() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo1);
        ComplexTextAdGroup adGroup = fullAdGroup(adGroupId);
        PerformanceBanner performanceBanner = clientPerformanceBanner(1L)
                .withId(bannerInfo.getBannerId());
        adGroup.withComplexBanners(singletonList(new ComplexBanner().withBanner(performanceBanner)));

        updateAndCheckResultIsFailed(adGroup);
    }

    @Test
    public void hasErrorWhenImageHashBannerHasSitelinkSet() {
        BannerImageFormat imageFormat = steps.bannerSteps()
                .createBannerImageFormat(campaignInfo.getClientInfo(), imageAdImageFormat(null));

        ComplexTextAdGroup complexAdGroup = fullAdGroupWithImageHashBanner(imageFormat.getImageHash());
        complexAdGroup.getComplexBanners().get(0).withSitelinkSet(defaultSitelinkSet());

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0), field(SITELINK_SET.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, isNull())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorWhenImageHashBannerHasVcard() {
        BannerImageFormat imageFormat = steps.bannerSteps()
                .createBannerImageFormat(campaignInfo.getClientInfo(), imageAdImageFormat(null));

        ComplexTextAdGroup complexAdGroup = fullAdGroupWithImageHashBanner(imageFormat.getImageHash());
        complexAdGroup.getComplexBanners().get(0).withVcard(fullVcard());

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0), field(VCARD.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, isNull())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorWhenImageCreativeBannerHasSitelinkSet() {
        CreativeInfo creative = steps.creativeSteps().createCreative(
                defaultCanvas(null, null), campaignInfo.getClientInfo());

        ComplexTextAdGroup complexAdGroup = fullAdGroupWithImageCreativeBanner(creative.getCreativeId());
        complexAdGroup.getComplexBanners().get(0).withSitelinkSet(defaultSitelinkSet());

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0), field(SITELINK_SET.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, isNull())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorWhenImageCreativeBannerHasVcard() {
        CreativeInfo creative = steps.creativeSteps().createCreative(
                defaultCanvas(null, null), campaignInfo.getClientInfo());

        ComplexTextAdGroup complexAdGroup = fullAdGroupWithImageCreativeBanner(creative.getCreativeId());
        complexAdGroup.getComplexBanners().get(0).withVcard(fullVcard());

        ValidationResult<?, Defect> vr = updateAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0), field(VCARD.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, isNull())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    protected ComplexTextAdGroup fullAdGroupWithImageHashBanner(String imageHash) {
        return fullAdGroupWithImageHashBannerForUpdate(adGroupId, retConditionId, imageHash);
    }

    protected ComplexTextAdGroup fullAdGroupWithImageCreativeBanner(Long creativeId) {
        return fullAdGroupWithImageCreativeBannerForUpdate(adGroupId, retConditionId, creativeId);
    }
}
