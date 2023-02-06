package ru.yandex.direct.core.entity.banner.type.title;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners.clientContentPromoBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTitleAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final String TITLE = "some test title";

    @Test
    public void validTitleForContentPromotionBanner() {
        adGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO);
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.VIDEO);
        ContentPromotionBanner banner = clientContentPromoBanner(content.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTitle(TITLE);
        Long id = prepareAndApplyValid(banner);
        ContentPromotionBanner actualBanner = getBanner(id, ContentPromotionBanner.class);
        assertThat(actualBanner.getTitle(), equalTo(TITLE));
    }
}
