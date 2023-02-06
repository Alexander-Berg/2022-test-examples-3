package ru.yandex.canvas.service.html5;

import java.io.IOException;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.canvas.steps.ResourceHelpers.getResource;

@RunWith(SpringJUnit4ClassRunner.class)
public class MdsHostedHtmlTest {
    @Test
    public void checkGeneratedRtbHtml() throws IOException {
        MdsHostedHtml mdsHostedHtml = new MdsHostedHtml(
                getResource("/ru/yandex/canvas/service/html5/MdsHostedHtmlTest/source.html"),
                ImmutableList.of(ImmutableList.of("https://code.createjs.com/createjs-12.min.js",
                        "https://cached.js.com/1234"
                )), null, MdsHostedHtml.HtmlType.SINGLE);

        String actualHtml = mdsHostedHtml.asHtml();
        assertThat(delFormatting(actualHtml),
                is(delFormatting(getResource(
                        "/ru/yandex/canvas/service/html5/MdsHostedHtmlTest/expected.html"))));
    }

    private static String delFormatting(String s) {
        return s.replace("\n", "").replaceAll("\\s+", "");
    }

    @Test
    public void checkGeneratedRtbHtmlWithVideo() throws IOException {
        var video = new Html5SourcesService.VideoUploaded(15.1, "http://example.ru/video.mp4");
        MdsHostedHtml mdsHostedHtml = new MdsHostedHtml(
                getResource("/ru/yandex/canvas/service/html5/MdsHostedHtmlTest/source_video.html"),
                ImmutableList.of(ImmutableList.of("https://code.createjs.com/createjs-12.min.js",
                        "https://cached.js.com/1234"
                )), video, MdsHostedHtml.HtmlType.EXPANDED_BIG);

        String actualHtml = mdsHostedHtml.asHtml();
        assertThat(delFormatting(actualHtml),
                is(delFormatting(getResource(
                        "/ru/yandex/canvas/service/html5/MdsHostedHtmlTest/expected_video.html"))));
    }
}
