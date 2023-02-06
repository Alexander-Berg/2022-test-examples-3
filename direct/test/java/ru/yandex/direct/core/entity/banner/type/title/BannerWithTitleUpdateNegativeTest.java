package ru.yandex.direct.core.entity.banner.type.title;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithTitle;
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
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.stringShouldNotBeBlank;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTitleUpdateNegativeTest extends BannerBannerInfoUpdateOperationTestBase {

    private static final String INVALID_TITLE = " ";

    @Autowired
    private ContentPromotionBannerSteps contentPromotionBannerSteps;

    @Test
    public void invalidTitleForContentPromotionBanner() {
        bannerInfo = contentPromotionBannerSteps.createDefaultBanner(ContentPromotionContentType.VIDEO);

        ModelChanges<ContentPromotionBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), ContentPromotionBanner.class)
                        .process(INVALID_TITLE, BannerWithTitle.TITLE);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithTitle.TITLE)),
                stringShouldNotBeBlank())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

}
