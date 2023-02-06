package ru.yandex.canvas.service.html5;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Date;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.model.html5.Source;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.service.SessionParams;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static ru.yandex.canvas.Html5Constants.HTML5_VIDEO_MUTE_FEATURE;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_BANNER;
import static ru.yandex.canvas.steps.ResourceHelpers.getResource;

@RunWith(SpringJUnit4ClassRunner.class)
public class RtbGenerateTest {
    private static String sourceHtmlString;
    private static String inject;
    private static String expectedString;

    @BeforeClass
    public static void setUp() throws Exception {
        sourceHtmlString = getResource("/ru/yandex/canvas/service/html5/rtbGenerateTest/source.html");
        inject = getResource("/ru/yandex/canvas/service/html5/rtbGenerateTest/inject.js");
        expectedString = String.format(getResource("/ru/yandex/canvas/service/html5/rtbGenerateTest/expected.html"),
                inject);
    }

    @Test
    public void checkGeneratedRtbHtml() throws IOException, URISyntaxException {
        RtbhostedHtml rtbhostedHtml = new RtbhostedHtml(sourceHtmlString, makeSource().getHtmlReplacements(),
                emptySet(), HTML5_CPM_BANNER);

        String actualHtml = rtbhostedHtml.getPreview("${AUCTION_DC_PARAMS}").replace("\n", "");
        String expectedString1 = expectedString.replace("\n", "");

        assertEquals("Result is as expected",
                actualHtml.replaceAll("\\s+", ""),
                expectedString1.replaceAll("\\s+", ""));
    }

    @Test
    public void checkGeneratedRtbHtml5VideoAllowedWithoutFeature() throws IOException, URISyntaxException {
        String srcHtml = getResource("/ru/yandex/canvas/service/html5/rtbGenerateTest/html5_video_allowed_src.html");
        String expectedHtml = getResource("/ru/yandex/canvas/service/html5/rtbGenerateTest" +
                "/html5_video_allowed_expected.html");
        RtbhostedHtml rtbhostedHtml = new RtbhostedHtml(srcHtml, makeSource().getHtmlReplacements(),
                emptySet(), HTML5_CPM_BANNER);
        String actualHtml = rtbhostedHtml.getPreview("${AUCTION_DC_PARAMS}");
        assertThat(delFormatting(actualHtml),
                is(delFormatting(expectedHtml)));
    }

    @Test
    public void checkGeneratedRtbHtml5VideoAllowedWithFeature() throws IOException, URISyntaxException {
        String srcHtml = getResource("/ru/yandex/canvas/service/html5/rtbGenerateTest/html5_video_allowed_src.html");
        String expectedHtml = getResource("/ru/yandex/canvas/service/html5/rtbGenerateTest" +
                "/html5_video_allowed_expected_mute.html");
        RtbhostedHtml rtbhostedHtml = new RtbhostedHtml(srcHtml, makeSource().getHtmlReplacements(),
                ImmutableSet.of(HTML5_VIDEO_MUTE_FEATURE), HTML5_CPM_BANNER);
        String actualHtml = rtbhostedHtml.getPreview("${AUCTION_DC_PARAMS}");
        assertThat(delFormatting(actualHtml),
                is(delFormatting(expectedHtml)));
    }

    private Source makeSource() {
        return new Source().setClientId(1212L)
                .setDate(Date.from(Instant.now()))
                .setArchive(false)
                .setName("file.zip")
                .setWidth(300)
                .setHeight(300)
                .setPreviewUrl(null)
                .setScreenshotUrl(null)
                .setUrl("http://mds.yandex.ru/12/12.kfoe2jf.zip")
                .setBasePath("index.html")
                .setStillageInfo(new Source.ZipStillageInfo(new StillageFileInfo()))
                .setHtmlFilename("index.html")
                .setHtmlReplacements(ImmutableList.of(ImmutableList.of("https://code.createjs.com/createjs-12.min.js",
                        "https://cached.js.com/1234"
                )));
    }

    private static String delFormatting(String s) {
        return s.replace("\n", "").replaceAll("\\s+", "");
    }

    @Test
    public void bodyOnload() throws IOException, URISyntaxException {
        String srcHtml = getResource("/ru/yandex/canvas/service/html5/rtbGenerateTest/source_body_onload.html");
        String expectedHtml = getResource("/ru/yandex/canvas/service/html5/rtbGenerateTest/expected_body_onload.html");
        RtbhostedHtml rtbhostedHtml = new RtbhostedHtml(srcHtml, makeSource().getHtmlReplacements(), emptySet(),
                SessionParams.Html5Tag.CPM_PRICE);
        String actualHtml = rtbhostedHtml.getPreview("${AUCTION_DC_PARAMS}");
        assertThat(delFormatting(actualHtml),
                is(delFormatting(expectedHtml)));
    }

    @Test
    public void checkGeneratedRtbClickUrlMacros() throws IOException, URISyntaxException {
        //тест что для RTB отправляется макрос CLICK_URLS
        String srcHtml = getResource("/ru/yandex/canvas/service/html5/rtbGenerateTest/html5_video_allowed_src.html");
        RtbhostedHtml rtbhostedHtml = new RtbhostedHtml(srcHtml, makeSource().getHtmlReplacements(),
                emptySet(), HTML5_CPM_BANNER);
        rtbhostedHtml.setRtbHost(true);

        String actualHtml = rtbhostedHtml.getPreview("${AUCTION_DC_PARAMS}");
        assertThat(actualHtml, containsString("CLICK_URLS"));
    }
}
