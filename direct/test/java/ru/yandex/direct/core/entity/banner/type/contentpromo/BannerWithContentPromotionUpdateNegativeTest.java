package ru.yandex.direct.core.entity.banner.type.contentpromo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithContentPromotion;
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
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithContentPromotionUpdateNegativeTest extends BannerBannerInfoUpdateOperationTestBase {

    private static final String INVALID_VISIT_URL = " ";

    @Autowired
    private ContentPromotionBannerSteps contentPromotionBannerSteps;

    @Test
    public void invalidContentPromotionForContentPromotionBanner() {
        bannerInfo = contentPromotionBannerSteps.createDefaultBanner(ContentPromotionContentType.VIDEO);

        ModelChanges<ContentPromotionBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), ContentPromotionBanner.class)
                        .process(INVALID_VISIT_URL, BannerWithContentPromotion.VISIT_URL);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithContentPromotion.VISIT_URL)),
                notEmptyString())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
