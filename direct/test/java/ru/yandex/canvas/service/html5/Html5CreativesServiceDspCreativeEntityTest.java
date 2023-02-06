package ru.yandex.canvas.service.html5;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.model.html5.Creative;
import ru.yandex.canvas.model.html5.Source;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.rtbhost.helpers.creative.Html5DspUploadHelper;
import ru.yandex.direct.bs.dspcreative.model.DspCreativeExportEntry;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class Html5CreativesServiceDspCreativeEntityTest {

    @Autowired
    protected Html5DspUploadHelper html5DspUploadHelper;

    private final String toRtbCreativeData = "<html>\n"
            + " <head>\n"
            + "  <script>var yandexHTML5BannerApi = (function() {\n"
            + "\n"
            + "    var _clickUrls = [${CLICK_URLS}];\n"
            + "\n"
            + "    function getClickMacro() {\n"
            + "        var clickMacro = '';\n"
            + "        var res = /click_macro=([^&$]+)/.exec(getBaseURI());\n"
            + "        if (res) {\n"
            + "            try {\n"
            + "                clickMacro = decodeURIComponent(res[1]);\n"
            + "            } catch (err) {}\n"
            + "        }\n"
            + "        return clickMacro;\n"
            + "    }\n"
            + "\n"
            + "    function getBaseURI() {\n"
            + "        var baseURI = document.baseURI;\n"
            + "        if (baseURI === null || typeof baseURI === 'undefined') {\n"
            + "            var baseTag = document.querySelector('base');\n"
            + "            if (baseTag) {\n"
            + "                baseURI = baseTag.href;\n"
            + "            } else {\n"
            + "                baseURI = document.URL;\n"
            + "            }\n"
            + "        }\n"
            + "        return baseURI;\n"
            + "    }\n"
            + "\n"
            + "    var clickMacro = getClickMacro();\n"
            + "    return {\n"
            + "        getClickURLNum: function(num) {\n"
            + "            var url = _clickUrls[--num];\n"
            + "            if (clickMacro) {\n"
            + "                return \"\" + clickMacro + encodeURIComponent(url);\n"
            + "            }\n"
            + "            return url;\n"
            + "        }\n"
            + "    };\n"
            + "})();\n"
            + "</script>\n"
            + "  <script></script>\n"
            + " </head>\n"
            + " <body>\n"
            + "  some html\n"
            + " </body>\n"
            + "</html>";

    private final String toRtbCreativeConstructorData = "{\"elements\":[{\"type\":\"image\",\"url\":\"https://avatars" +
            ".mds.yandex.net/get-canvas/224059/2a000001694eabc332e29cc48b0e28c2a727/cropSource\"}],\"OnlyClickUrlsMacro\":true}";

    @Test
    public void toImportDSPCreativeEntry() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Creative creative = makeCreative();
        DspCreativeExportEntry dspCreativeExportEntry =
                html5DspUploadHelper.toImportDspCreativeEntry(creative, objectMapper);
        assertEquals("data ok", dspCreativeExportEntry.getData(), toRtbCreativeData);
        assertEquals("data ok", dspCreativeExportEntry.getConstructorData(), toRtbCreativeConstructorData);
    }

    public Creative makeCreative() throws IOException {
        Html5Zip html5Zip = mock(Html5Zip.class);
        when(html5Zip.getFileAsUtf8String(any())).thenReturn("<html><head><script></script></head>some html</html>");

        Source source = mock(Source.class);
        when(source.unzipArchiveContent()).thenReturn(html5Zip);

        Source.ImageStillageInfo imageStillageInfo = new Source.ImageStillageInfo();
        imageStillageInfo.setUrl("https://avatars.mds.yandex" +
                ".net/get-canvas/224059/2a000001694eabc332e29cc48b0e28c2a727/cropSource");

        Creative creative = new Creative();
        creative.setSource(source);
        creative.setWidth(636);
        creative.setHeight(956);
        creative.setSourceImageInfo(imageStillageInfo);

        return creative;
    }

    @TestConfiguration
    public static class Html5SourcesServiceConfiguration {

        @MockBean
        public DirectService directService;

        @Bean
        public Html5DspUploadHelper html5DspUploadHelper() {
            return new Html5DspUploadHelper(directService);
        }
    }
}

