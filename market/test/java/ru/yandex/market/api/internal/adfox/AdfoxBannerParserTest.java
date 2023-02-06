package ru.yandex.market.api.internal.adfox;

import org.hamcrest.Matcher;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.ImageMatcher.image;

@WithContext
public class AdfoxBannerParserTest extends UnitTestBase {
    @Test
    public void adfoxBannerParse() {
        Banner banner = parse("adfox-banner.json");

        assertThat(
            banner.getImage().getThumbnails(),
            containsInAnyOrder(
                cast(
                    image(
                        "https://banners.adfox.ru/180117/market/685321/2364586_1.jpg",
                        286,
                        400
                    )
                ),
                cast(
                    image(
                        "https://banners.adfox.ru/180117/market/685321/2364586_4.jpg",
                        286,
                        400
                    )
                )
            )
        );

        assertThat(
            banner.getLink(),
            is("https://ads6.adfox.ru/252616/goLink?hash=369ba5987f660c6")
        );

        assertThat(
            banner.getAdfoxUrl(),
            is("https://ads6.adfox.ru/252616/event?hash=d3ef454e30")
        );


        assertThat(
            banner.getClientUrl(),
            is("https://banners.adfox.ru/transparent.gif")
        );
    }

    private static Banner parse(String filename) {
        AdfoxBannerParser parser = new AdfoxBannerParser();
        return parser.parse(ResourceHelpers.getResource(filename));
    }

    private static <T> Matcher<T> cast(Matcher<?> matcher) {
        return (Matcher<T>) matcher;
    }

}
