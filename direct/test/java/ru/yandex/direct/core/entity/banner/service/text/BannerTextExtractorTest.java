package ru.yandex.direct.core.entity.banner.service.text;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class BannerTextExtractorTest {

    private static final String TEXT_BANNER_TEXT = "title title ext body 123";
    private static final TextBanner TEXT_BANNER =
            new TextBanner()
                    .withTitle("title ")
                    .withTitleExtension("title ext ")
                    .withBody(" body 123");

    private static final String CONTENT_PROMO_BANNER_TEXT = "title body 123";
    private static final ContentPromotionBanner CONTENT_PROMO_BANNER =
            new ContentPromotionBanner()
                    .withTitle("title ")
                    .withBody(" body 123 ");

    private static final String OUTDOOR_BANNER_TEXT = "";
    private static final CpmOutdoorBanner OUTDOOR_BANNER =
            new CpmOutdoorBanner();


    private static final String DYNAMIC_BANNER_TEXT = "body 123";
    private static final DynamicBanner DYNAMIC_BANNER =
            new DynamicBanner()
                    .withTitle("title ")
                    .withBody(" body 123");

    private BannerTextExtractor extractor = new BannerTextExtractor();

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public List<Banner> banners;

    @Parameterized.Parameter(2)
    public List<Map.Entry> expectedResult;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "текстовый баннер",
                        singletonList(TEXT_BANNER),
                        singletonList(Map.entry(TEXT_BANNER, TEXT_BANNER_TEXT))
                },
                {
                        "контент-промоушен баннер",
                        singletonList(CONTENT_PROMO_BANNER),
                        singletonList(Map.entry(CONTENT_PROMO_BANNER, CONTENT_PROMO_BANNER_TEXT))
                },
                {
                        "outdoor-баннер",
                        singletonList(OUTDOOR_BANNER),
                        singletonList(Map.entry(OUTDOOR_BANNER, OUTDOOR_BANNER_TEXT))
                },
                // банер с динамическим полем (оно должно игнориться)
                {
                        "dynamic-баннер",
                        singletonList(DYNAMIC_BANNER),
                        singletonList(Map.entry(DYNAMIC_BANNER, DYNAMIC_BANNER_TEXT))
                },
                // смешанный кейс с несколькими типами баннеров
                {
                        "баннеры с текстами и без",
                        asList(TEXT_BANNER, CONTENT_PROMO_BANNER, OUTDOOR_BANNER, DYNAMIC_BANNER),
                        asList(
                                Map.entry(TEXT_BANNER, TEXT_BANNER_TEXT),
                                Map.entry(CONTENT_PROMO_BANNER, CONTENT_PROMO_BANNER_TEXT),
                                Map.entry(OUTDOOR_BANNER, OUTDOOR_BANNER_TEXT),
                                Map.entry(DYNAMIC_BANNER, DYNAMIC_BANNER_TEXT))
                },
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void extractorWorksFine() {
        Map<Banner, String> result = extractor.extractTexts(banners);
        assertThat(result)
                .containsOnly(expectedResult.toArray(new Map.Entry[0]));
    }
}
