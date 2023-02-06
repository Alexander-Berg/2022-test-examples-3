package ru.yandex.direct.core.entity.banner.type.adgroupid;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.type.BannerBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.banner.ContentPromotionBannerSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adNotFound;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Название теста не совсем корректное, т.к. этот тест больше не тестирует
 * BannerWithAdGroupIdUpdateValidationTypeSupport, т.к. этот сапоорт был удалён.
 * Он просто тестирует всю операцию.
 */
@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithAdGroupIdUpdateNegativeTest extends BannerBannerInfoUpdateOperationTestBase {

    @Autowired
    private ContentPromotionBannerSteps contentPromotionBannerSteps;

    @Test
    public void invalidBannerIdForContentPromotionBanner() {

        bannerInfo = contentPromotionBannerSteps.createDefaultBanner(ContentPromotionContentType.VIDEO);
        var otherClientBannerInfo =
                contentPromotionBannerSteps.createDefaultBanner(ContentPromotionContentType.VIDEO);

        ModelChanges<ContentPromotionBanner> modelChanges =
                new ModelChanges<>(otherClientBannerInfo.getBannerId(), ContentPromotionBanner.class);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(ContentPromotionBanner.ID)),
                adNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
