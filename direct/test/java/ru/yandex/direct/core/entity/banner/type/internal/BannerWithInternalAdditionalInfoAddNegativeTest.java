package ru.yandex.direct.core.entity.banner.type.internal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerWithInternalInfo;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithInternalInfo.DESCRIPTION;
import static ru.yandex.direct.core.entity.banner.model.BannerWithInternalInfo.TEMPLATE_ID;
import static ru.yandex.direct.core.entity.banner.model.BannerWithInternalInfo.TEMPLATE_VARIABLES;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adGroupNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.internalTemplateNotAllowed;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.templateVariablesMismatch;
import static ru.yandex.direct.core.entity.banner.type.internal.BannerWithInternalInfoConstants.DESCRIPTION_MAX_LENGTH;
import static ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewInternalBanners.clientInternalBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithInternalAdditionalInfoAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    private CampaignInfo campaignInfo;

    @Before
    public void initTestData() {
        campaignInfo = steps.campaignSteps().createActiveInternalAutobudgetCampaign();
    }

    @Test
    public void invalidTemplateIdForInternalBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);

        var banner = clientInternalBanner(adGroupInfo.getAdGroupId())
                .withTemplateId(Long.MAX_VALUE);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        checkBanner(vr, TEMPLATE_ID, internalTemplateNotAllowed());
    }

    @Test
    public void invalidDescriptionForInternalBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);

        var banner = clientInternalBanner(adGroupInfo.getAdGroupId())
                .withDescription(randomAlphabetic(DESCRIPTION_MAX_LENGTH + 1));

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);


        checkBanner(vr, DESCRIPTION, maxStringLength(DESCRIPTION_MAX_LENGTH));
    }

    @Test
    public void invalidTemplateVariablesForInternalBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);

        var banner = clientInternalBanner(adGroupInfo.getAdGroupId())
                .withTemplateVariables(singletonList(new TemplateVariable().withTemplateResourceId(
                        TemplateResourceRepositoryMockUtils.TEMPLATE_4_RESOURCE_1_IMAGE)
                        .withInternalValue("bbb")));

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        checkBanner(vr, TEMPLATE_VARIABLES, templateVariablesMismatch());
    }

    @Test
    public void notExistAdGroupIdForInternalBanner() {
        long notExistAdGroupId = Long.MAX_VALUE;
        InternalAdGroup adGroup = activeInternalAdGroup(campaignInfo.getCampaignId(), 0L)
                .withId(notExistAdGroupId);
        adGroupInfo = new AdGroupInfo()
                .withCampaignInfo(campaignInfo)
                .withAdGroup(adGroup);

        var banner = clientInternalBanner(adGroupInfo.getAdGroupId());

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(field(BannerWithInternalInfo.AD_GROUP_ID)), adGroupNotFound())));
    }

    private void checkBanner(ValidationResult<?, Defect> vr, ModelProperty property, Defect defect) {
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(property)), defect)));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
