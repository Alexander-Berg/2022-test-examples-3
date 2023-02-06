package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class BannerTextFormatterTest {

    private BannerTextFormatter bannerTextFormatter = new BannerTextFormatter();

    static Stream<Arguments> params() {
        return Stream.of(
                arguments("Text banner", new TextBanner(), true),
                arguments("Dynamic banner", new DynamicBanner(), false),
                arguments("Performance banner", new PerformanceBanner(), false),
                arguments("Mobile content banner", new MobileAppBanner(), true));
    }

    @ParameterizedTest
    @MethodSource("params")
    void replaceTemplateLabelTest(String name, Banner banner,
                                  boolean needReplaceTemplateLabel) {
        String stringWithOldTemplateLabel = "Услуги *шаблон*";

        var gotReplacedOldString = bannerTextFormatter.replaceTemplateLabel(stringWithOldTemplateLabel, banner);
        String expectedReplacedOldString = needReplaceTemplateLabel ? "Услуги {PHRASE}" : stringWithOldTemplateLabel;

        String stringWithTemplateNewLabel = "#Ремонт стиральных машин INDESIT#. Гарантия.";
        var gotReplacedNewString = bannerTextFormatter.replaceTemplateLabel(stringWithTemplateNewLabel, banner);
        String expectedReplacedNewString = needReplaceTemplateLabel ? "{PHRASEРемонт стиральных машин INDESIT}. " +
                "Гарантия" +
                "." : stringWithTemplateNewLabel;

        String stringWithoutTemplateLabel = "#Ремонт стиральных машин INDESIT. Гарантия.";
        var gotReplacedStringWithouLablel = bannerTextFormatter.replaceTemplateLabel(stringWithoutTemplateLabel,
                banner);
        assertThat(gotReplacedOldString).isEqualTo(expectedReplacedOldString);
        assertThat(gotReplacedNewString).isEqualTo(expectedReplacedNewString);
        assertThat(gotReplacedStringWithouLablel).isEqualTo(stringWithoutTemplateLabel);
    }

    @Test
    void unescapeHtml_EmptyStringTest() {
        var unescapedString = bannerTextFormatter.unescapeHtml("");
        assertThat(unescapedString).isEmpty();
    }

    @Test
    void unescapeHtml_simpleStringTest() {
        var unescapedString = bannerTextFormatter.unescapeHtml("qwerty");
        assertThat(unescapedString).isEqualTo("qwerty");
    }

    @Test
    void unescapeHtmlTest() {
        var unescapedString = bannerTextFormatter.unescapeHtml("&amp;&amp;&lt;qwe&lt&gt;&quot;&apos;");
        assertThat(unescapedString).isEqualTo("&&<qwe&lt>\"&apos;");
    }
}
