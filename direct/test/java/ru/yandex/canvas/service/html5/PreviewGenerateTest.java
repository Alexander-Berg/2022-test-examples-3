package ru.yandex.canvas.service.html5;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Date;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.model.html5.Source;
import ru.yandex.direct.test.utils.matcher.RegexMatcher;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_BANNER;

@RunWith(SpringJUnit4ClassRunner.class)
public class PreviewGenerateTest {

    public static final String sourceHtmlString = "<!doctype html><html><head>"
            + "<script type=\"text/javascript\" src=\"https://awaps.yandex.net/data/lib/adsdk.js\"></script> "
            + "<meta name=\"ad.size\" content=\"width=300,height=300\">"
            + "</head>"
            + "<body>"
            + "<a id=\"click1_area\" href=\"#\" target=\"_blank\"><canvas></canvas></a>"
            + "<script src=\"https://code.createjs.com/createjs-12.min.js\"></script>"
            + "<script>document.getElementById(\"click1_area\").href = yandexHTML5BannerApi.getClickURLNum(1);</script>"
            + "</body></html>";

    public static final Source srcObject = new Source().setClientId(1212L)
            .setDate(Date.from(Instant.parse("2018-09-01T19:15:12Z")))
            .setArchive(false)
            .setName("file.zip")
            .setWidth(300)
            .setHeight(300)
            .setPreviewUrl(null)
            .setScreenshotUrl(null)
            .setUrl("http://mds.yandex.ru/12/12.kfoe2jf.zip")
            .setBasePath("index.html")
            .setHtmlFilename("index.html")
            .setHtmlReplacements(ImmutableList.of(ImmutableList.of("https://code.createjs.com/createjs-12.min.js",
                    "https://cached.js.com/1234"
            )));


    public static final String expected =
            " <html> <head></head> <body style=\"margin:0;\"> <div id=\"yandex_rtb\"></div> <script nonce=\"\" "
                    + "type=\"text/javascript\"> (function(w, d, n, s, t) { w[n] = w[n] || []; w[n].push(function() { "
                    + "Ya.Context.AdvManager.render({ blockId: \"R-1-1\", renderTo: \"yandex_rtb\", async: true, data:"
                    + " {\"rtb\":{\"html\":\"PCFkb2N0eXBlIGh0bWw-CjxodG1sPgogPGhlYWQ" +
                    "-CiAgPHNjcmlwdD52YXIgeWFuZGV4SFRNTDVCYW"
                    + "5uZXJBcGkgPSAoZnVuY3Rpb24oKSB7CgogICAgdmFyIF9jbGlja1VybHMgPSBbXTsKCiAgICB0cnkgewogICAgICAgIHZhc"
                    + "iBkYXRhID0geyJkYXRhX3BhcmFtcyI6eyIxIjp7ImNsaWNrX3VybCI6eyJjbGlja1VybDEiOiJqYXZhc2NyaXB0OnZvaWQo"
                    + "MCkiLCJjbGlja1VybDIiOiJqYXZhc2NyaXB0OnZvaWQoMCkiLCJjbGlja1VybDMiOiJqYXZhc2NyaXB0OnZvaWQoMCkifX1"
                    + "9fTsKICAgICAgICBkYXRhID0gZGF0YS5kYXRhX3BhcmFtczsKCiAgICAgICAgZnVuY3Rpb24gZ2V0T3JkZXJLZXkoc3RyKS"
                    + "B7CiAgICAgICAgICAgIHZhciByZSA9IC9cZCskLzsKICAgICAgICAgICAgdmFyIG1hdGNoID0gc3RyLm1hdGNoKHJlKTsKI"
                    + "CAgICAgICAgICAgcmV0dXJuIG1hdGNoID8gcGFyc2VJbnQobWF0Y2hbMF0sIDEwKSA6IDA7CiAgICAgICAgfQoKICAgICAg"
                    + "ICBmb3IgKHZhciBjIGluIGRhdGEpIHsKICAgICAgICAgICAgaWYgKGMgIT09ICJtaXNjIiAmJiBkYXRhW2NdICYmIGRhdGF"
                    + "bY10uY2xpY2tfdXJsKSB7CiAgICAgICAgICAgICAgICB2YXIgY2xpY2tVcmwgPSBkYXRhW2NdLmNsaWNrX3VybDsKICAgIC"
                    + "AgICAgICAgICAgIHZhciBjbGljVXJsS2V5cyA9IFtdOwoKICAgICAgICAgICAgICAgIGZvciAodmFyIHUgaW4gY2xpY2tVc"
                    + "mwpIHsKICAgICAgICAgICAgICAgICAgICBjbGljVXJsS2V5cy5wdXNoKHUpOwogICAgICAgICAgICAgICAgfQoKICAgICAg"
                    + "ICAgICAgICAgIGNsaWNVcmxLZXlzLnNvcnQoZnVuY3Rpb24oYSwgYikgewogICAgICAgICAgICAgICAgICAgIGEgPSBnZXR"
                    + "PcmRlcktleShhKTsKICAgICAgICAgICAgICAgICAgICBiID0gZ2V0T3JkZXJLZXkoYik7CiAgICAgICAgICAgICAgICAgIC"
                    + "AgcmV0dXJuIGEgLSBiOwogICAgICAgICAgICAgICAgfSk7CgogICAgICAgICAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgP"
                    + "CBjbGljVXJsS2V5cy5sZW5ndGg7IGkrKykgewogICAgICAgICAgICAgICAgICAgIF9jbGlja1VybHMucHVzaChjbGlja1Vy"
                    + "bFtjbGljVXJsS2V5c1tpXV0pOwogICAgICAgICAgICAgICAgfQoKICAgICAgICAgICAgICAgIGJyZWFrOwogICAgICAgICA"
                    + "gICB9CiAgICAgICAgfQoKICAgIH0gY2F0Y2ggKGUpIHt9CgogICAgZnVuY3Rpb24gZ2V0Q2xpY2tNYWNybygpIHsKICAgIC"
                    + "AgICB2YXIgY2xpY2tNYWNybyA9ICcnOwogICAgICAgIHZhciByZXMgPSAvY2xpY2tfbWFjcm89KFteJiRdKykvLmV4ZWMoZ"
                    + "2V0QmFzZVVSSSgpKTsKICAgICAgICBpZiAocmVzKSB7CiAgICAgICAgICAgIHRyeSB7CiAgICAgICAgICAgICAgICBjbGlj"
                    + "a01hY3JvID0gZGVjb2RlVVJJQ29tcG9uZW50KHJlc1sxXSk7CiAgICAgICAgICAgIH0gY2F0Y2ggKGVycikge30KICAgICA"
                    + "gICB9CiAgICAgICAgcmV0dXJuIGNsaWNrTWFjcm87CiAgICB9CgogICAgZnVuY3Rpb24gZ2V0QmFzZVVSSSgpIHsKICAgIC"
                    + "AgICB2YXIgYmFzZVVSSSA9IGRvY3VtZW50LmJhc2VVUkk7CiAgICAgICAgaWYgKGJhc2VVUkkgPT09IG51bGwgfHwgdHlwZ"
                    + "W9mIGJhc2VVUkkgPT09ICd1bmRlZmluZWQnKSB7CiAgICAgICAgICAgIHZhciBiYXNlVGFnID0gZG9jdW1lbnQucXVlcnlT"
                    + "ZWxlY3RvcignYmFzZScpOwogICAgICAgICAgICBpZiAoYmFzZVRhZykgewogICAgICAgICAgICAgICAgYmFzZVVSSSA9IGJ"
                    + "hc2VUYWcuaHJlZjsKICAgICAgICAgICAgfSBlbHNlIHsKICAgICAgICAgICAgICAgIGJhc2VVUkkgPSBkb2N1bWVudC5VUk"
                    + "w7CiAgICAgICAgICAgIH0KICAgICAgICB9CiAgICAgICAgcmV0dXJuIGJhc2VVUkk7CiAgICB9CgogICAgdmFyIGNsaWNrT"
                    + "WFjcm8gPSBnZXRDbGlja01hY3JvKCk7CiAgICByZXR1cm4gewogICAgICAgIGdldENsaWNrVVJMTnVtOiBmdW5jdGlvbihu"
                    + "dW0pIHsKICAgICAgICAgICAgdmFyIHVybCA9IF9jbGlja1VybHNbLS1udW1dOwogICAgICAgICAgICBpZiAoY2xpY2tNYWN"
                    + "ybykgewogICAgICAgICAgICAgICAgcmV0dXJuICIiICsgY2xpY2tNYWNybyArIGVuY29kZVVSSUNvbXBvbmVudCh1cmwpOw"
                    + "ogICAgICAgICAgICB9CiAgICAgICAgICAgIHJldHVybiB1cmw7CiAgICAgICAgfQogICAgfTsKfSkoKTsKPC9zY3JpcHQ-I"
                    + "AogIDxtZXRhIG5hbWU9ImFkLnNpemUiIGNvbnRlbnQ9IndpZHRoPTMwMCxoZWlnaHQ9MzAwIj4KIDwvaGVhZD4KIDxib2R5"
                    + "PgogIDxhIGlkPSJjbGljazFfYXJlYSIgaHJlZj0iIyIgdGFyZ2V0PSJfYmxhbmsiPgogICA8Y2FudmFzPjwvY2FudmFzPjw"
                    + "vYT4KICA8c2NyaXB0IHNyYz0iaHR0cHM6Ly9jYWNoZWQuanMuY29tLzEyMzQiPjwvc2NyaXB0PgogIDxzY3JpcHQ-ZG9jdW"
                    + "1lbnQuZ2V0RWxlbWVudEJ5SWQoImNsaWNrMV9hcmVhIikuaHJlZiA9IHlhbmRleEhUTUw1QmFubmVyQXBpLmdldENsaWNrV"
                    + "VJMTnVtKDEpOzwvc2NyaXB0PgogPC9ib2R5Pgo8L2h0bWw-\",\"width\":300,\"height\":300,"
                    + "\"html5\":\"true\",\"basePath\":\"index.html\",\"abuseLink\":\"javascript:void(0)\"},"
                    + "\"common\":{\"isYandex\":\"1\",\"reloadTimeout\":\"1\"},\"settings\":{\"1\":{}}}, }); });"
                    + " t = d.getElementsByTagName(\"script\")[0]; s = d.createElement(\"script\");"
                    + " s.type = \"text/javascript\"; s.src = \"//an.yandex.ru/system/context.js\";"
                    + " s.async = true; t.parentNode.insertBefore(s, t); })(this, this.document,"
                    + " \"yandexContextAsyncCallbacks\"); </script> </body></html>";

    @Test
    public void checkGeneratedPreviewHtml() throws IOException, URISyntaxException {
        Preview preview = new Preview(srcObject.getHtmlReplacements(), sourceHtmlString, true,
                emptySet(), HTML5_CPM_BANNER);

        String actualHtml =
                preview.asHtml(srcObject.getWidth(), srcObject.getHeight(), srcObject.getBasePath()).replace("\n", "");
        actualHtml = actualHtml.replaceAll("\\s+", " ");

        assertThat("Result is as expected", actualHtml, Matchers.equalToIgnoringWhiteSpace(expected));
    }

    @Test
    public void generatePreviewWithoutCloseButton_abuseLinkIsNull() throws IOException, URISyntaxException {
        Preview preview = new Preview(srcObject.getHtmlReplacements(), sourceHtmlString, false,
                emptySet(), HTML5_CPM_BANNER);

        String actualHtml =
                preview.asHtml(srcObject.getWidth(), srcObject.getHeight(), srcObject.getBasePath()).replace("\n", "");
        actualHtml = actualHtml.replaceAll("\\s+", " ");

        assertThat("Abuse link is null", actualHtml,
                RegexMatcher.matches("(?s).*\"abuseLink\"\\s*:\\s*null.*"));
    }
}
