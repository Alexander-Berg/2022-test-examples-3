package ru.yandex.direct.core.entity.banner.type.contentpromo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithContentPromotion.CONTENT_PROMOTION_ID;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adGroupNotFound;
import static ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners.clientContentPromoBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithContentPromotionAddNegativeTest extends BannerNewAdGroupInfoAddOperationTestBase {

    @Test
    public void invalidContentPromotionIdForContentPromotionBanner() {
        adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(ContentPromotionAdgroupType.VIDEO);
        ContentPromotionBanner banner = clientContentPromoBanner(-1L)
                .withAdGroupId(adGroupInfo.getAdGroupId());
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CONTENT_PROMOTION_ID)), objectNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidContentPromotionIdWithInvalidAdGroupIdForContentPromotionBanner() {
        adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(ContentPromotionAdgroupType.VIDEO);
        ContentPromotionBanner banner = clientContentPromoBanner(-1L)
                .withAdGroupId(-1L);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithAdGroupId.AD_GROUP_ID)),
                adGroupNotFound())));
        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(CONTENT_PROMOTION_ID)),
                objectNotFound())));
        assertThat(vr.flattenErrors(), hasSize(2));
    }
}
