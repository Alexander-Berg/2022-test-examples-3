package ru.yandex.canvas.service.html5;

import java.util.List;
import java.util.Locale;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.CPM_PRICE;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_BANNER;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_YNDX_FRONTPAGE;
import static ru.yandex.canvas.service.html5.AdHtmlParser.STRETCHABLE_HEIGHT;
import static ru.yandex.canvas.service.html5.AdHtmlParser.STRETCHABLE_WIDTH;

@RunWith(SpringJUnit4ClassRunner.class)
public class AdHtmlParserTest {
    public static final String VALID_HTML5_FILE_CONTENT =
            "<!doctype html><html><head>"
                    + "<script type=\"text/javascript\" src=\"https://awaps.yandex.net/data/lib/adsdk.js\"></script> "
                    + "<meta name=\"ad.size\" content=\"width=300,height=300\">"
                    + "</head>"
                    + "<body>"
                    + "<a id=\"click1_area\" href=\"#\" target=\"_blank\"><canvas\"></canvas></a>"
                    + "<script>document.getElementById(\"click1_area\").href = yandexHTML5BannerApi.getClickURLNum(1);</script>"
                    + "</body></html>";

    public static final String VALID_HTML5_STRETCHABLE_WIDTH_FILE_CONTENT =
            "<!doctype html><html><head>"
                    + "<script type=\"text/javascript\" src=\"https://awaps.yandex.net/data/lib/adsdk.js\"></script> "
                    + "<meta name=\"ad.size\" content=\"width=100%,height=300\">"
                    + "</head>"
                    + "<body>"
                    + "<a id=\"click1_area\" href=\"#\" target=\"_blank\"><canvas\"></canvas></a>"
                    + "<script>document.getElementById(\"click1_area\").href = yandexHTML5BannerApi.getClickURLNum(1);</script>"
                    + "</body></html>";

    public static final String VALID_HTML5_STRETCHABLE_HEIGHT_FILE_CONTENT =
            "<!doctype html><html><head>"
                    + "<script type=\"text/javascript\" src=\"https://awaps.yandex.net/data/lib/adsdk.js\"></script> "
                    + "<meta name=\"ad.size\" content=\"width=300,height=100%\">"
                    + "</head>"
                    + "<body>"
                    + "<a id=\"click1_area\" href=\"#\" target=\"_blank\"><canvas\"></canvas></a>"
                    + "<script>document.getElementById(\"click1_area\").href = yandexHTML5BannerApi.getClickURLNum(1);</script>"
                    + "</body></html>";

    public static final String VALID_HTML5_STRETCHABLE_FILE_CONTENT =
            "<!doctype html><html><head>"
                    + "<script type=\"text/javascript\" src=\"https://awaps.yandex.net/data/lib/adsdk.js\"></script> "
                    + "<meta name=\"ad.size\" content=\"width=100%,height=100%\">"
                    + "</head>"
                    + "<body>"
                    + "<a id=\"click1_area\" href=\"#\" target=\"_blank\"><canvas\"></canvas></a>"
                    + "<script>document.getElementById(\"click1_area\").href = yandexHTML5BannerApi.getClickURLNum(1);</script>"
                    + "</body></html>";

    public static final String HTML5_FILE_NO_META =
            "<!doctype html><html><head>"
                    + "<script type=\"text/javascript\" src=\"https://awaps.yandex.net/data/lib/adsdk.js\"></script> "
                    + "</head>"
                    + "<body>"
                    + "<a id=\"click1_area\" href=\"#\" target=\"_blank\"><canvas\"></canvas></a>"
                    + "<script>document.getElementById(\"click1_area\").href = yandexHTML5BannerApi.getClickURLNum(1);</script>"
                    + "</body></html>";

    public static final String HTML5_FILE_NO_META_IN_HEAD =
            "<!doctype html><html>"
                    + "<head></head>"
                    + "<body>"
                    + "<meta name=\"ad.size\" content=\"width=300,height=300\">"
                    + "<a id=\"click1_area\" href=\"#\" target=\"_blank\"><canvas\"></canvas></a>"
                    + "<script>document.getElementById(\"click1_area\").href = yandexHTML5BannerApi.getClickURLNum(1);</script>"
                    + "</body></html>";

    public static final String HTML5_FILE_EXTENDED =
            "<!doctype html><html><head>"
                    + "<meta name=\"ad.bgrcolor\" content=\"FFFFFF\"> "
                    + "</head>"
                    + "<body>"
                    + "<a id=\"click1_area\" href=\"#\" target=\"_blank\"><canvas\"></canvas></a>"
                    + "<script>document.getElementById(\"click1_area\").href = yandexHTML5BannerApi.getClickURLNum(1);</script>"
                    + "</body></html>";

    public static final String HTML5_FILE_BAD_ADSIZE =
            "<!doctype html><html><head>"
                    + "<script type=\"text/javascript\" src=\"https://awaps.yandex.net/data/lib/adsdk.js\"></script> "
                    + "<meta name=\"ad.size\" content=\"width=300,height=3000\">"
                    + "</head>"
                    + "<body>"
                    + "<a id=\"click1_area\" href=\"#\" target=\"_blank\"><canvas\"></canvas></a>"
                    + "<script>document.getElementById(\"click1_area\").href = yandexHTML5BannerApi.getClickURLNum(1);</script>"
                    + "</body></html>";

    public static final String HTML5_FILE_URLS =
            "<!doctype html><html><head>"
                    + "<script type=\"text/javascript\" src=\"https://awaps.yandex.net/data/lib/adsdk.js\"></script> "
                    + "<meta name=\"ad.size\" content=\"width=300,height=300\">"
                    + "</head>"
                    + "<body>"
                    + "<script src=\"https://code.createjs.com/1.0.0/createjs.min.js\"></script>"
                    + "<script src=\"relative/js/js.js\"></script>"
                    + "<link href=\"gwdpage_style.css\" rel=\"stylesheet\">"
                    + "<script src=\"https://yastatic.net/underscore/\"></script>"
                    + "<script src=\"https://example.com/js/js.js\"></script>"
                    + "<a id=\"click1_area\" href=\"#\" target=\"_blank\"><canvas\"></canvas></a>"
                    + "<script>document.getElementById(\"click1_area\").href = yandexHTML5BannerApi.getClickURLNum(1);</script>"
                    + "</body></html>";

    public static final String HTML5_INLINE_EVENT_HANDLERS =
            "<!doctype html><html><head>"
                    + "<script type=\"text/javascript\" src=\"https://awaps.yandex.net/data/lib/adsdk.js\"></script> "
                    + "<meta name=\"ad.size\" content=\"width=300,height=300\">"
                    + "</head>"
                    + "<body onload=\"init();\">"
                    + "<a id=\"click1_area\" href=\"#\" target=\"_blank\"><canvas\"></canvas></a>"
                    + "<script>document.getElementById(\"click1_area\").href = yandexHTML5BannerApi.getClickURLNum(1);</script>"
                    + "</body></html>";

    @Test
    public void testCheckValidHtml5() {
        AdHtmlParser adHtmlParser = new AdHtmlParser(toHtmlFileList(VALID_HTML5_FILE_CONTENT), HTML5_CPM_BANNER);
        adHtmlParser.check();
        assertThat("Valid html processed without errors", adHtmlParser.getErrors(), Matchers.empty());
    }

    @Test
    public void testCheckHtml5WithoutMeta() {
        LocaleContextHolder.setLocale(Locale.TRADITIONAL_CHINESE);
        Locale.setDefault(new Locale("unknown", "UK"));
        AdHtmlParser adHtmlParser = new AdHtmlParser(toHtmlFileList(HTML5_FILE_NO_META, HTML5_FILE_NO_META_IN_HEAD),
                HTML5_CPM_BANNER);
        adHtmlParser.check();
        assertThat("No ad.size in tag meta", adHtmlParser.getErrors(), Matchers.contains("no_ad_size_found"));
    }

    @Test
    public void testCheckHtml5WithIncorrectSize() {
        AdHtmlParser adHtmlParser = new AdHtmlParser(toHtmlFileList(HTML5_FILE_BAD_ADSIZE), HTML5_CPM_BANNER);
        adHtmlParser.check();
        assertThat("Width got correct", adHtmlParser.getWidth(), Matchers.equalTo(300));
        assertThat("Height got correct", adHtmlParser.getHeight(), Matchers.equalTo(3000));
    }

    @Test
    public void testCheckHtml5StretchableWidthWithCorrectSize() {
        AdHtmlParser adHtmlParser =
                new AdHtmlParser(toHtmlFileList(VALID_HTML5_STRETCHABLE_WIDTH_FILE_CONTENT), HTML5_CPM_BANNER);
        adHtmlParser.check();
        assertThat("Width got correct", adHtmlParser.getWidth(), Matchers.equalTo(STRETCHABLE_WIDTH));
        assertThat("Height got correct", adHtmlParser.getHeight(), Matchers.equalTo(300));
    }

    @Test
    public void testCheckHtml5StretchableHeightWithCorrectSize() {
        AdHtmlParser adHtmlParser =
                new AdHtmlParser(toHtmlFileList(VALID_HTML5_STRETCHABLE_HEIGHT_FILE_CONTENT), HTML5_CPM_BANNER);
        adHtmlParser.check();
        assertThat("Width got correct", adHtmlParser.getWidth(), Matchers.equalTo(300));
        assertThat("Height got correct", adHtmlParser.getHeight(), Matchers.equalTo(STRETCHABLE_HEIGHT));
    }

    @Test
    public void testCheckHtml5StretchableWithCorrectSize() {
        AdHtmlParser adHtmlParser =
                new AdHtmlParser(toHtmlFileList(VALID_HTML5_STRETCHABLE_FILE_CONTENT), HTML5_CPM_BANNER);
        adHtmlParser.check();
        assertThat("Width got correct", adHtmlParser.getWidth(), Matchers.equalTo(STRETCHABLE_WIDTH));
        assertThat("Height got correct", adHtmlParser.getHeight(), Matchers.equalTo(STRETCHABLE_HEIGHT));
    }

    @Test
    public void testCheckHtml5WithStrangeUrls() {
        LocaleContextHolder.setLocale(Locale.TRADITIONAL_CHINESE);
        Locale.setDefault(new Locale("unknown", "UK"));
        AdHtmlParser adHtmlParser = new AdHtmlParser(toHtmlFileList(HTML5_FILE_URLS), HTML5_CPM_BANNER);
        adHtmlParser.check();
        assertEquals("External links found", adHtmlParser.getErrors(), ImmutableList.of("has_not_allowed_external_paths"));
        assertEquals("Paths for uploading", adHtmlParser.getPatchesForUpload(), ImmutableList.of("https://code.createjs.com/1.0.0/createjs.min.js"));
        assertEquals("Absolute paths", adHtmlParser.getAbsolutePaths(), ImmutableList.of("https://example.com/js/js.js"));
        assertEquals("Relative paths", adHtmlParser.getRelativePaths(), ImmutableList.of("relative/js/js.js"));
    }

    private static List<AdHtmlParser.HtmlFile> toHtmlFileList(String html) {
        return List.of(new AdHtmlParser.HtmlFile("index.html", html));
    }

    private static List<AdHtmlParser.HtmlFile> toHtmlFileList(String main, String extended) {
        return List.of(new AdHtmlParser.HtmlFile("index.html", main),
                new AdHtmlParser.HtmlFile("extended.html", extended));
    }

    @Test
    public void testCheckCpmPriceWithoutMeta() {//расхлоп без сайзов
        LocaleContextHolder.setLocale(Locale.TRADITIONAL_CHINESE);
        Locale.setDefault(new Locale("unknown", "UK"));
        AdHtmlParser adHtmlParser = new AdHtmlParser(toHtmlFileList(HTML5_FILE_NO_META, HTML5_FILE_NO_META),
                CPM_PRICE);
        adHtmlParser.check();
        assertThat("No ad.size in tag meta", adHtmlParser.getErrors(), Matchers.hasItem("no_ad_size_found"));
    }

    @Test
    public void testCheckCpmPriceDoubleMeta() {//С двумя сайзами
        LocaleContextHolder.setLocale(Locale.TRADITIONAL_CHINESE);
        Locale.setDefault(new Locale("unknown", "UK"));
        AdHtmlParser adHtmlParser = new AdHtmlParser(toHtmlFileList(VALID_HTML5_FILE_CONTENT, VALID_HTML5_FILE_CONTENT),
                CPM_PRICE);
        adHtmlParser.check();
        assertThat("More than one html with meta", adHtmlParser.getErrors(),
                Matchers.contains("multi_ad_size_found"));
    }

    @Test
    public void testCheckCpmPriceWithoutBgrcolor() {
        LocaleContextHolder.setLocale(Locale.TRADITIONAL_CHINESE);
        Locale.setDefault(new Locale("unknown", "UK"));
        AdHtmlParser adHtmlParser = new AdHtmlParser(toHtmlFileList(VALID_HTML5_FILE_CONTENT, HTML5_FILE_NO_META),
                CPM_PRICE);
        adHtmlParser.check();
        assertThat("No ad.bgrcolor in tag meta", adHtmlParser.getErrors(),
                Matchers.contains("no_ad_bgrcolor_found"));
    }

    @Test
    public void testCheckValiCpmPrice() {
        AdHtmlParser adHtmlParser = new AdHtmlParser(toHtmlFileList(VALID_HTML5_FILE_CONTENT, HTML5_FILE_EXTENDED),
                CPM_PRICE);
        adHtmlParser.check();
        assertThat("Valid html processed without errors", adHtmlParser.getErrors(), Matchers.empty());
    }

    @Test
    public void testInlineEventHandlers_CpmYndxFrontpage() {
        AdHtmlParser adHtmlParser = new AdHtmlParser(
                toHtmlFileList(HTML5_INLINE_EVENT_HANDLERS), HTML5_CPM_YNDX_FRONTPAGE);
        adHtmlParser.check();
        assertThat("not_valid_html_file", adHtmlParser.getErrors(),
                Matchers.contains("not_valid_html_file"));
    }

    @Test
    public void testInlineEventHandlers_cpmPrice() {
        AdHtmlParser adHtmlParser = new AdHtmlParser(toHtmlFileList(HTML5_INLINE_EVENT_HANDLERS), CPM_PRICE);
        adHtmlParser.check();
        assertThat("Valid html processed without errors", adHtmlParser.getErrors(), Matchers.empty());
    }
}
