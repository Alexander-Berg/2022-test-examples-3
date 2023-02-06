package ru.yandex.direct.core.entity.banner.type.system;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithStatusShow;
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
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.forbiddenToChange;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class BannerWithStatusShowUpdateNegativeTest extends BannerBannerInfoUpdateOperationTestBase {

    @Autowired
    private ContentPromotionBannerSteps contentPromotionBannerSteps;

    @Before
    public void initTestData() {
        bannerInfo = contentPromotionBannerSteps.createDefaultBanner(ContentPromotionContentType.VIDEO);
    }


    @Test
    public void canNotChangeStatusShow() {
        boolean newStatusShow = !((BannerWithStatusShow) bannerInfo.getBanner()).getStatusShow();
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), ContentPromotionBanner.class)
                .process(newStatusShow, BannerWithStatusShow.STATUS_SHOW);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(field(BannerWithStatusShow.STATUS_SHOW)), forbiddenToChange())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

}
