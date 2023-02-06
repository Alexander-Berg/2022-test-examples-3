package ru.yandex.direct.core.entity.banner.type.contentpromo;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners.clientContentPromoBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithContentPromotionAddPositiveTest extends BannerNewAdGroupInfoAddOperationTestBase {

    private static final String VISIT_URL = "https://www.yandex.ru";
    @Autowired
    private BannersUrlHelper bannersUrlHelper;

    @Test
    public void validContentPromotionForContentPromotionBanner() {
        adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(ContentPromotionAdgroupType.VIDEO);
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.VIDEO);
        ContentPromotionBanner banner = clientContentPromoBanner(content.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withVisitUrl(VISIT_URL);

        Long id = prepareAndApplyValid(banner);

        ContentPromotionBanner actualBanner = getBanner(id, ContentPromotionBanner.class);

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(actualBanner.getContentPromotionId())
                    .as("contentPromotionId")
                    .isEqualTo(content.getId());
            assertions.assertThat(actualBanner.getVisitUrl())
                    .as("visitUrl")
                    .isEqualTo(VISIT_URL);
            assertions.assertThat(actualBanner.getHref())
                    .as("href")
                    .isEqualTo(content.getUrl());
            assertions.assertThat(actualBanner.getDomain())
                    .as("domain")
                    .isEqualTo(bannersUrlHelper.extractHostFromHrefWithWwwOrNull(content.getUrl()));
            assertions.assertThat(actualBanner.getDomainId())
                    .as("domainId")
                    .isNotNull();
        });
    }
}
