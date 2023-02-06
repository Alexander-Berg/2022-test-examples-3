package ru.yandex.direct.core.entity.banner.type.href;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners.clientContentPromoBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithHrefAddNegativeTest extends BannerNewAdGroupInfoAddOperationTestBase {

    private static final String INVALID_HREF = "test";

    @Test
    public void invalidHrefForContentPromotionBanner() {
        adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(ContentPromotionAdgroupType.VIDEO);
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.VIDEO);
        ContentPromotionBanner banner = clientContentPromoBanner(content.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withHref(INVALID_HREF);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithHref.HREF)),
                isNull())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
