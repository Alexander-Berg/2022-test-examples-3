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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithContentPromotionUpdatePositiveTest extends BannerBannerInfoUpdateOperationTestBase {

    private static final String NEW_VISIT_URL = "https://www.yandex.ru/new";

    @Autowired
    private ContentPromotionBannerSteps contentPromotionBannerSteps;

    @Test
    public void validContentPromotionForContentPromotionBanner() {
        bannerInfo = contentPromotionBannerSteps.createDefaultBanner(ContentPromotionContentType.VIDEO);

        ModelChanges<ContentPromotionBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), ContentPromotionBanner.class)
                        .process(NEW_VISIT_URL, BannerWithContentPromotion.VISIT_URL);

        Long id = prepareAndApplyValid(modelChanges);
        ContentPromotionBanner actualBanner = getBanner(id, ContentPromotionBanner.class);
        assertThat(actualBanner.getVisitUrl(), equalTo(NEW_VISIT_URL));
    }
}
