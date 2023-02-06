package ru.yandex.direct.web.entity.banner.converter;

import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.web.entity.banner.model.WebContentPromotionBanner;
import ru.yandex.direct.web.entity.banner.model.WebContentPromotionBannerContentRes;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.web.entity.banner.converter.ContentPromotionBannerConverter.webBannerToCoreContentPromotionBanner;

public class ContentPromotionBannerConverterTest {

    private static final Long ID = 111L;
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final Long VALID_CONTENT_PROMOTION_ID = 12345L;

    @Test
    public void webBannerToCoreContentPromotionBanner_VideoType_PrimitiveFields() {
        WebContentPromotionBanner webContentPromotionBanner = new WebContentPromotionBanner()
                .withId(ID)
                .withAdType(BannersBannerType.content_promotion.getLiteral())
                .withBannerType(OldBannerType.CONTENT_PROMOTION.name())
                .withTitle(TITLE)
                .withDescription(DESCRIPTION)
                .withContentResource(new WebContentPromotionBannerContentRes()
                        .withContentId(VALID_CONTENT_PROMOTION_ID));

        var banner = (ContentPromotionBanner)
                webBannerToCoreContentPromotionBanner(webContentPromotionBanner);

        var expected = new ContentPromotionBanner()
                .withId(ID)
                .withTitle(TITLE)
                .withBody(DESCRIPTION)
                .withContentPromotionId(VALID_CONTENT_PROMOTION_ID);
        assertThat(banner, beanDiffer(expected));
    }

    @Test
    public void webBannerToCoreContentPromotionBanner_VisitUrlAndContent_TakenValid() {
        WebContentPromotionBanner webContentPromotionBanner = new WebContentPromotionBanner()
                .withId(ID)
                .withAdType(BannersBannerType.content_promotion.getLiteral())
                .withBannerType(OldBannerType.CONTENT_PROMOTION.name())
                .withTitle(TITLE)
                .withDescription(DESCRIPTION)
                .withVisitUrl("VISIT_URL")
                .withContentResource(new WebContentPromotionBannerContentRes()
                        .withContentId(VALID_CONTENT_PROMOTION_ID));

        var banner = (ContentPromotionBanner)
                webBannerToCoreContentPromotionBanner(webContentPromotionBanner);

        var expected = new ContentPromotionBanner()
                .withId(ID)
                .withTitle(TITLE)
                .withBody(DESCRIPTION)
                .withContentPromotionId(VALID_CONTENT_PROMOTION_ID)
                .withVisitUrl("VISIT_URL");
        assertThat(banner, beanDiffer(expected));
    }

    @Test
    public void webBannerToCoreContentPromotionBanner_CollectionType_PrimitiveFields() {
        WebContentPromotionBanner webContentPromotionBanner = new WebContentPromotionBanner()
                .withId(ID)
                .withAdType(BannersBannerType.content_promotion.getLiteral())
                .withBannerType(OldBannerType.CONTENT_PROMOTION.name())
                .withTitle(TITLE)
                .withDescription(DESCRIPTION)
                .withContentResource(new WebContentPromotionBannerContentRes()
                        .withContentId(VALID_CONTENT_PROMOTION_ID));

        var banner = (ContentPromotionBanner)
                webBannerToCoreContentPromotionBanner(webContentPromotionBanner);

        var expected = new ContentPromotionBanner()
                .withId(ID)
                .withTitle(TITLE)
                .withBody(DESCRIPTION)
                .withContentPromotionId(VALID_CONTENT_PROMOTION_ID);
        assertThat(banner, beanDiffer(expected));
    }

    @Test
    public void webBannerToCoreContentPromotionBanner_NoContentResource() {
        WebContentPromotionBanner webContentPromotionBanner = new WebContentPromotionBanner()
                .withId(ID)
                .withAdType(BannersBannerType.content_promotion.getLiteral())
                .withBannerType(OldBannerType.CONTENT_PROMOTION.name())
                .withTitle(TITLE)
                .withDescription(DESCRIPTION);

        var banner = (ContentPromotionBanner)
                webBannerToCoreContentPromotionBanner(webContentPromotionBanner);

        var expected = new ContentPromotionBanner()
                .withId(ID)
                .withTitle(TITLE)
                .withBody(DESCRIPTION)
                .withContentPromotionId(null);
        assertThat(banner, beanDiffer(expected));
    }
}
