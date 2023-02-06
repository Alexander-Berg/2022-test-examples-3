package ru.yandex.direct.core.entity.banner.type.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithInternalInfo.DESCRIPTION;
import static ru.yandex.direct.core.entity.banner.model.BannerWithInternalInfo.TEMPLATE_ID;
import static ru.yandex.direct.core.entity.banner.model.BannerWithInternalInfo.TEMPLATE_VARIABLES;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.forbiddenToChange;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.templateVariablesMismatch;
import static ru.yandex.direct.core.entity.banner.type.internal.BannerWithInternalInfoConstants.DESCRIPTION_MAX_LENGTH;
import static ru.yandex.direct.core.testing.data.TestBanners.activeInternalBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithInternalAdditionalInfoUpdateNegativeTest
        extends BannerOldBannerInfoUpdateOperationTestBase<OldBanner> {

    @Autowired
    private BannerSteps bannerSteps;

    @Test
    public void canNotChangeTemplateIdTypeForInternalBanner() {
        bannerInfo = createInternalBanner();
        Long bannerId = bannerInfo.getBannerId();

        var modelChanges = new ModelChanges<>(bannerId, InternalBanner.class)
                .process(TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1, TEMPLATE_ID);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TEMPLATE_ID)), forbiddenToChange())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidDescriptionForInternalBanner() {
        bannerInfo = createInternalBanner();
        Long bannerId = bannerInfo.getBannerId();

        var modelChanges = new ModelChanges<>(bannerId, InternalBanner.class)
                .process(randomAlphabetic(DESCRIPTION_MAX_LENGTH + 1), DESCRIPTION);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(DESCRIPTION)),
                maxStringLength(DESCRIPTION_MAX_LENGTH))));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidTemplateVariablesForInternalBanner() {
        bannerInfo = createInternalBanner();
        Long bannerId = bannerInfo.getBannerId();

        var modelChanges = new ModelChanges<>(bannerId, InternalBanner.class)
                .process(singletonList(new TemplateVariable().withTemplateResourceId(
                        TemplateResourceRepositoryMockUtils.TEMPLATE_4_RESOURCE_1_IMAGE)
                        .withInternalValue("bbb")), TEMPLATE_VARIABLES);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TEMPLATE_VARIABLES)),
                templateVariablesMismatch())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    private BannerInfo createInternalBanner() {
        var campaignInfo = steps.campaignSteps().createActiveInternalAutobudgetCampaign();
        var adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        return bannerSteps.createBanner(activeInternalBanner(null, null), adGroupInfo);
    }
}
