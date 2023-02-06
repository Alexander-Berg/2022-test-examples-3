package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BANNERS;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.fullAdGroupWithImageCreativeBannerForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.fullAdGroupWithImageHashBannerForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupTypeNotSupported;
import static ru.yandex.direct.core.entity.banner.container.ComplexBanner.SITELINK_SET;
import static ru.yandex.direct.core.entity.banner.container.ComplexBanner.VCARD;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndAdgroupType;
import static ru.yandex.direct.core.testing.data.TestBanners.imageAdImageFormat;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.core.testing.data.TestNewDynamicBanners.clientDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
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
public class ComplexTextAddValidationInconsistentTypesTest extends ComplexTextAddValidationTestBase {
    @Autowired
    protected CampaignSteps campaignSteps;

    @Test
    public void hasErrorWhenAdGroupTypeIsNotSupported() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(campaign.getClientInfo());
        ComplexTextAdGroup complexAdGroup = new ComplexTextAdGroup()
                .withAdGroup(activeDynamicTextAdGroup(null).withCampaignId(campaignInfo.getCampaignId()));

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(AdGroup.TYPE.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, adGroupTypeNotSupported())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorWhenBannerTypeIsNotFitToAdGroupType() {
        ComplexBanner complexTextBanner = new ComplexBanner().withBanner(clientTextBanner());
        ComplexBanner complexCpmBanner = new ComplexBanner().withBanner(clientCpmBanner(null));
        ComplexTextAdGroup complexAdGroup = fullAdGroup()
                .withComplexBanners(asList(complexTextBanner, complexCpmBanner));

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(1));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, inconsistentStateBannerTypeAndAdgroupType())));
    }

    @Test
    public void hasNoErrorsWhenBannerTypeFitsToTextAdGroup() {
        ComplexBanner complexTextBanner = new ComplexBanner().withBanner(clientTextBanner());
        ComplexBanner complexDynamicBanner = new ComplexBanner().withBanner(clientDynamicBanner());
        ComplexTextAdGroup complexAdGroup = fullAdGroup()
                .withComplexBanners(asList(complexTextBanner, complexDynamicBanner));

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsSuccess(complexAdGroup);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void hasErrorWhenImageHashBannerHasSitelinkSet() {
        BannerImageFormat imageFormat = steps.bannerSteps()
                .createBannerImageFormat(campaign.getClientInfo(), imageAdImageFormat(null));

        ComplexTextAdGroup complexAdGroup = fullAdGroupWithImageHashBanner(imageFormat.getImageHash());
        complexAdGroup.getComplexBanners().get(0).withSitelinkSet(defaultSitelinkSet());

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0), field(SITELINK_SET.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, isNull())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorWhenImageHashBannerHasVcard() {
        BannerImageFormat imageFormat = steps.bannerSteps()
                .createBannerImageFormat(campaign.getClientInfo(), imageAdImageFormat(null));

        ComplexTextAdGroup complexAdGroup = fullAdGroupWithImageHashBanner(imageFormat.getImageHash());
        complexAdGroup.getComplexBanners().get(0).withVcard(fullVcard());

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0), field(VCARD.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, isNull())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorWhenImageCreativeBannerHasSitelinkSet() {
        CreativeInfo creative = steps.creativeSteps().createCreative(
                defaultCanvas(null, null), campaign.getClientInfo());

        ComplexTextAdGroup complexAdGroup = fullAdGroupWithImageCreativeBanner(creative.getCreativeId());
        complexAdGroup.getComplexBanners().get(0).withSitelinkSet(defaultSitelinkSet());

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0), field(SITELINK_SET.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, isNull())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void hasErrorWhenImageCreativeBannerHasVcard() {
        CreativeInfo creative = steps.creativeSteps().createCreative(
                defaultCanvas(null, null), campaign.getClientInfo());

        ComplexTextAdGroup complexAdGroup = fullAdGroupWithImageCreativeBanner(creative.getCreativeId());
        complexAdGroup.getComplexBanners().get(0).withVcard(fullVcard());

        ValidationResult<?, Defect> vr = prepareAndCheckResultIsFailed(complexAdGroup);
        Path errPath = path(index(0), field(COMPLEX_BANNERS.name()), index(0), field(VCARD.name()));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, isNull())));
        vr.flattenErrors().forEach(validationErrorLogger::info);
        assertThat("должна присутствовать всего одна ошибка", vr.flattenErrors(), hasSize(1));
    }

    protected ComplexTextAdGroup fullAdGroupWithImageHashBanner(String imageHash) {
        return fullAdGroupWithImageHashBannerForAdd(campaignId, retConditionId, imageHash);
    }

    protected ComplexTextAdGroup fullAdGroupWithImageCreativeBanner(Long creativeId) {
        return fullAdGroupWithImageCreativeBannerForAdd(campaignId, retConditionId, creativeId);
    }
}
