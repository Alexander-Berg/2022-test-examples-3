package ru.yandex.canvas.model;

import java.util.Collections;
import java.util.Date;

import com.google.common.collect.Maps;
import org.junit.Test;

import ru.yandex.canvas.model.CreativeData.Options;
import ru.yandex.canvas.model.elements.Headline;
import ru.yandex.canvas.model.html_builder.CreativeHtmlPreviewWriter;
import ru.yandex.canvas.model.html_builder.CreativeHtmlRtbHostWriter;
import ru.yandex.canvas.model.html_builder.GeoPinCreativeHtmlWriter;

import static org.junit.Assert.assertEquals;

public class CreativeDocumentHtmlTest {

    CreativeHtmlPreviewWriter creativeHtmlPreviewWriter = new CreativeHtmlPreviewWriter();
    CreativeHtmlRtbHostWriter creativeHtmlRtbHostWriter = new CreativeHtmlRtbHostWriter();
    GeoPinCreativeHtmlWriter geoPinCreativeHtmlWriter = new GeoPinCreativeHtmlWriter();

    // feel free to copy it from debugger dump, just check what <script> and non ASCII symbols are escaped
    private final String expectedHtmlForRtbHost = "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n"
            + "<head>\n"
            + "    <meta charset=\"UTF-8\">\n"
            + "    <title>Title</title>\n"
            + "</head>\n"
            + "<body style=\"margin:0;\">\n"
            + "<script src=\"https://yastatic.net/pcode/media/loader.js\"></script>\n"
            + "<script id=\"extra\" type=\"application/json\">\n"
            + "    ${AUCTION_DC_PARAMS}\n"
            + "</script>\n"
            + "<script>\n"
            + "    var data = {\"width\":728,\"height\":90,\"options\":{\"backgroundColor\":\"#F7F7F7\"," +
            "\"borderColor\":\"#DDDCDA\",\"hasAnimation\":null,\"hasSocialLabel\":null,\"isAdaptive\":false},\"elements\":[{\"type\":null," +
            "\"available\":true,\"mediaSet\":null,\"options\":{\"placeholder\":null,\"content\":\"#FFFFFF\"," +
            "\"color\":\"\\u0420\\u0430\\u0437\\u0414\\u0432\\u0430\\u0422\\u0440\\u0438123\\\"\\u003E\\u003C/script" +
            "\\u003E\\u003Csvg/onload=alert(1)\\u003E\"},\"maxLength\":60}],\"mediaSets\":{}," +
            "\"bundle\":{\"name\":\"media-banner_theme_pic-button-color-background\\\"\",\"version\":1}," +
            "\"clickUrl\":\"\"};\n"
            + "\n"
            + "    try {\n"
            + "        var d = JSON.parse(document.getElementById(\"extra\").innerHTML);\n"
            + "        var dcParams = d.data_params || {};\n"
            + "        data[\"AUCTION_DC_PARAMS\"] = d;\n"
            + "        for (var c in dcParams) {\n"
            + "            if (c !== \"misc\" && dcParams[c] && dcParams[c].click_url) {\n"
            + "                data.clickUrl = dcParams[c].click_url.image_orig;\n"
            + "            }\n"
            + "        }\n"
            + "    } catch (e) {}\n"
            + "\n"
            + "    window.Ya.mediaCode.create(data.bundle, data, \"body\");\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>\n";
    private final String expectedHtmlForPreview = "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n"
            + "<head>\n"
            + "    <meta charset=\"UTF-8\">\n"
            + "    <title>Title</title>\n"
            + "</head>\n"
            + "<body style=\"margin:0;\">\n"
            + "<script src=\"https://yandex.ru/ads/system/preview.js\"></script>\n"
            + "<script id=\"extra\" type=\"application/json\">\n"
            + "    ${AUCTION_DC_PARAMS}\n"
            + "</script>\n"
            + "<div id=\"media\" ></div>\n"
            + "<script>\n"
            + "    var data = {\"width\":728,\"height\":90,\"options\":{\"backgroundColor\":\"#F7F7F7\"," +
            "\"borderColor\":\"#DDDCDA\",\"hasAnimation\":null,\"hasSocialLabel\":null,\"isAdaptive\":false},\"elements\":[{\"type\":null," +
            "\"available\":true,\"mediaSet\":null,\"options\":{\"placeholder\":null,\"content\":\"#FFFFFF\"," +
            "\"color\":\"\\u0420\\u0430\\u0437\\u0414\\u0432\\u0430\\u0422\\u0440\\u0438123\\\"\\u003E\\u003C/script" +
            "\\u003E\\u003Csvg/onload=alert(1)\\u003E\"},\"maxLength\":60}],\"mediaSets\":{}," +
            "\"bundle\":{\"name\":\"media-banner_theme_pic-button-color-background\\\"\",\"version\":1}," +
            "\"clickUrl\":\"\"};\n"
            + "\n"
            + "    try {\n"
            + "        var d = JSON.parse(document.getElementById(\"extra\").innerHTML);\n"
            + "        var dcParams = d.data_params || {};\n"
            + "        data[\"AUCTION_DC_PARAMS\"] = d;\n"
            + "        for (var c in dcParams) {\n"
            + "            if (c !== \"misc\" && dcParams[c] && dcParams[c].click_url) {\n"
            + "                data.clickUrl = dcParams[c].click_url.image_orig;\n"
            + "            }\n"
            + "        }\n"
            + "    } catch (e) {}\n"
            + "\n"
            + "    var api = new window.Ya.PcodePreview({\n"
            + "        nonce: 'NONCE_REPLACE_PLACEHOLDER',\n"
            + "    });\n"
            + "    var media = document.getElementById('media');\n"
            + "    media.style = `width: ${data.width}px; height: ${data.height}px`\n"
            + "    api.render([{\n"
            + "        elementId: 'media',\n"
            + "        data: {\n"
            + "            media: {\n"
            + "                'type': data.bundle.name,\n"
            + "                'params': data\n"
            + "            }\n"
            + "        }\n"
            + "    }])\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>\n";

    private final String expectedGeoPinHtml = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <title>Title</title>\n" +
            "</head>\n" +
            "<body style=\"margin:0;\">\n" +
            "<div style=\"position:absolute; width: 336px; height: 600px; border-color: rgb(211, 211, 211); " +
            "background-image: url(https://yastatic.net/s3/front-maps-static/front-pins-agency-cabinet/0.0" +
            ".769/out/assets/_/d23ddfd51ff511a29464ad68ef139d3c.jpg); background-size: cover;\">\n" +
            "    <div style=\"top: 190px; left: 85px; position:absolute;\">\n" +
            "        <div id=\"logo\" style=\"left: -14px; width: 36px; height: 36px; position: absolute; " +
            "border-radius: 50%; z-index: 1; background-size: cover; top: 0; bottom: 0; margin: auto 0; cursor: " +
            "pointer; user-select: none;\" onclick=\"changeBannerVisibility()\">\n" +
            "        </div>\n" +
            "        <div style=\"height: 30px; min-width: 141px; max-width: 180px; border-radius: 22px; " +
            "background-color: #FFF; padding: 7px 7px 7px 0; font-family: 'Yandex Sans Text',Arial,sans-serif; " +
            "opacity: .9; display: flex; flex-direction: column;\">\n" +
            "            <span id=\"headline\" style=\"color: #000000; font-size: 14px; font-weight: 500; " +
            "line-height: " +
            "1.29; margin-left: 28px; white-space: nowrap; overflow: hidden;\">Заголовок</span>\n" +
            "            <span id=\"description\" style=\"color: rgba(0, 0, 0, .6); font-size: 12px; " +
            "margin-left: 28px; line-height: 1.29;\">Категория</span>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    <div id=\"banner\" style=\"bottom: 0px; position:absolute; background-color: #FFFFFF\">\n" +
            "        <div id=\"image\" style=\"width: 336px; height: 112px; background-size: contain; border-radius: " +
            "8px 8px 0 0;\">\n" +
            "        </div>\n" +
            "        <div style=\"right: 16px; top: 120px; width: 24px; height: 24px; position: absolute; " +
            "border-radius: 50%; background-color: rgba(0,0,0,0.1); cursor: pointer;\" " +
            "onclick=\"changeBannerVisibility()\">\n" +
            "            <span style=\"left: 11px; top: 5px; width: 1px; height: 14px; position: absolute; " +
            "background-color: #030303; border-radius: 10px; transform: rotate(45deg);\"></span>\n" +
            "            <span style=\"left: 11px; top: 5px; width: 1px; height: 14px; position: absolute; " +
            "background-color: #030303; border-radius: 10px; transform: rotate(-45deg);\"></span>\n" +
            "        </div>\n" +
            "        <div style=\"display: grid; padding: 10px; grid-row-gap: 10px;\">\n" +
            "            <div>\n" +
            "                <h3 id=\"headline_2\" style=\"font-family: 'YS Text', Arial; font-size: 18px; margin: 0;" +
            " " +
            "line-height: 1.29; overflow: hidden; max-width: 90%; text-overflow: clip; white-space: nowrap;" +
            "\"></h3>\n" +
            "                <div id=\"description_2\" style=\"font-family: 'Yandex Sans Text',Arial,sans-serif; " +
            "font-size: 14px; color: rgba(0,0,0,.6); margin: 0; line-height: 1.5;\"> • Адрес из организации в Яндекс" +
            ".Справочнике</div>\n" +
            "                <div id=\"legal\" style=\"font-family: 'Yandex Sans Text',Arial,sans-serif; font-size: " +
            "12px; color: rgba(0,0,0,.3); line-height: 1;\">Реклама.</div>\n" +
            "            </div>\n" +
            "            <div style=\"display: flex; margin-top: 6px; justify-content: space-between;\">\n" +
            "                <div id=\"domain\" style=\"background-color: #e0e6e9; height: 40px; line-height: 40px; " +
            "border-radius: 8px; text-align: center; color: rgba(0,0,0,0.8); font-size: 16px; font-family: 'YS Text'," +
            " Arial; font-weight: 500; flex: 1 1; margin-right: 8px; display: none; cursor: pointer;\">На " +
            "сайт</div>\n" +
            "                <div id=\"phone\" style=\"background-color: #e0e6e9; height: 40px; line-height: 40px; " +
            "border-radius: 8px; text-align: center; color: rgba(0,0,0,0.8); font-size: 16px; font-family: 'YS Text'," +
            " Arial; font-weight: 500; flex: 1 1; margin-right: 8px; display: " +
            "none; cursor: pointer;\">Позвонить</div>\n" +
            "                <div id=\"path\" style=\"background-color: #fedd60; height: 40px; line-height: 40px; " +
            "border-radius: 8px; text-align: center; color: rgba(0,0,0,0.8); font-size: 16px; font-family: 'YS Text'," +
            " Arial; font-weight: 500; flex: 1 1; cursor: pointer;\">Заехать</div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>\n" +
            "<script>\n" +
            "    var data = {\"width\":728,\"height\":90,\"options\":{\"backgroundColor\":\"#F7F7F7\"," +
            "\"borderColor\":\"#DDDCDA\",\"hasAnimation\":null,\"hasSocialLabel\":null,\"isAdaptive\":false},\"elements\":[{\"type\":null," +
            "\"available\":true,\"mediaSet\":null,\"options\":{\"placeholder\":null,\"content\":\"#FFFFFF\"," +
            "\"color\":\"\\u0420\\u0430\\u0437\\u0414\\u0432\\u0430\\u0422\\u0440\\u0438123\\\"\\u003E\\u003C/script" +
            "\\u003E\\u003Csvg/onload=alert(1)\\u003E\"},\"maxLength\":60}],\"mediaSets\":{}," +
            "\"bundle\":{\"name\":\"media-banner_theme_pic-button-color-background\\\"\",\"version\":1}," +
            "\"clickUrl\":\"\"};\n" +
            "\n" +
            "    try {\n" +
            "        var elements = data[\"elements\"];\n" +
            "        var mediaSets = data[\"mediaSets\"];\n" +
            "        for (var i=0; i<elements.length; i=i+1) {\n" +
            "            var e = elements[i];\n" +
            "            if (e[\"available\"] == false) {\n" +
            "                continue;\n" +
            "            }\n" +
            "            var type = e[\"type\"];\n" +
            "            if (type==\"logo\" || type==\"image\") {\n" +
            "                var img = mediaSets[e[\"mediaSet\"]][\"items\"][0][\"items\"][0][\"url\"];\n" +
            "                document.getElementById(type).style.backgroundImage = 'url('+img+')';\n" +
            "            } else if (type==\"headline\" || type==\"description\") {\n" +
            "                var text = e[\"options\"][\"content\"];\n" +
            "                document.getElementById(type).innerHTML = text;\n" +
            "                var el = document.getElementById(type+\"_2\");\n" +
            "                el.innerHTML = text + el.innerHTML;\n" +
            "            } else if (type==\"domain\" || type==\"phone\") {\n" +
            "                var n = document.getElementById(type);\n" +
            "                n.style.display = \"\";\n" +
            "                document.getElementById(\"path\").style.width = n.style.width;\n" +
            "                if (type==\"domain\") {\n" +
            "                    var domain = e[\"options\"][\"content\"];\n" +
            "                    n.title = domain;\n" +
            "                    n.onclick = function() {window.open(domain, '_blank')};\n" +
            "                } else if (type==\"phone\") {\n" +
            "                    var phone = e[\"options\"][\"content\"];\n" +
            "                    n.title = phone;\n" +
            "                    n.onclick = function() {window.open(\"tel:\" + phone, '_blank')};\n" +
            "                }\n" +
            "            }  else if (type==\"legal\") {\n" +
            "                var n = document.getElementById(type);\n" +
            "                var text = n.innerHTML + \" \" + e[\"options\"][\"content\"];\n" +
            "                n.innerHTML = text;\n" +
            "            }\n" +
            "        }\n" +
            "    } catch (e) {}\n" +
            "\n" +
            "    function changeBannerVisibility() {\n" +
            "        var n = document.getElementById(\"banner\");\n" +
            "        if (n.style.display == \"none\") {\n" +
            "            n.style.display = \"\";\n" +
            "        } else {\n" +
            "            n.style.display = \"none\";\n" +
            "        }\n" +
            "    }\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>\n" +
            "";

    private final String expectedAdaptiveCreativeHtmlForPreview = "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n"
            + "<head>\n"
            + "    <meta charset=\"UTF-8\">\n"
            + "    <title>Title</title>\n"
            + "</head>\n"
            + "<body style=\"margin:0;width:728px;height:90px\">\n"
            + "<script src=\"https://yandex.ru/ads/system/preview.js\"></script>\n"
            + "<script id=\"extra\" type=\"application/json\">\n"
            + "    ${AUCTION_DC_PARAMS}\n"
            + "</script>\n"
            + "<div id=\"media\" ></div>\n"
            + "<script>\n"
            + "    var data = {\"width\":728,\"height\":90,\"options\":{\"backgroundColor\":\"#F7F7F7\"," +
            "\"borderColor\":\"#DDDCDA\",\"hasAnimation\":null,\"hasSocialLabel\":null,\"isAdaptive\":true},\"elements\":[{\"type\":null," +
            "\"available\":true,\"mediaSet\":null,\"options\":{\"placeholder\":null,\"content\":\"#FFFFFF\"," +
            "\"color\":\"\\u0420\\u0430\\u0437\\u0414\\u0432\\u0430\\u0422\\u0440\\u0438123\\\"\\u003E\\u003C/script" +
            "\\u003E\\u003Csvg/onload=alert(1)\\u003E\"},\"maxLength\":60}],\"mediaSets\":{}," +
            "\"bundle\":{\"name\":\"media-banner_theme_pic-button-color-background\\\"\",\"version\":1}," +
            "\"clickUrl\":\"\"};\n"
            + "\n"
            + "    try {\n"
            + "        var d = JSON.parse(document.getElementById(\"extra\").innerHTML);\n"
            + "        var dcParams = d.data_params || {};\n"
            + "        data[\"AUCTION_DC_PARAMS\"] = d;\n"
            + "        for (var c in dcParams) {\n"
            + "            if (c !== \"misc\" && dcParams[c] && dcParams[c].click_url) {\n"
            + "                data.clickUrl = dcParams[c].click_url.image_orig;\n"
            + "            }\n"
            + "        }\n"
            + "    } catch (e) {}\n"
            + "\n"
            + "    var api = new window.Ya.PcodePreview({\n"
            + "        nonce: 'NONCE_REPLACE_PLACEHOLDER',\n"
            + "    });\n"
            + "    var media = document.getElementById('media');\n"
            + "    media.style = `width: ${data.width}px; height: ${data.height}px`\n"
            + "    api.render([{\n"
            + "        elementId: 'media',\n"
            + "        data: {\n"
            + "            media: {\n"
            + "                'type': data.bundle.name,\n"
            + "                'params': data\n"
            + "            }\n"
            + "        }\n"
            + "    }])\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>\n";

    private final String expectedAdaptiveCreativeHtmlForRtbHost = "<!DOCTYPE html>\n"
            + "<html lang=\"en\" style=\"width:100%;height:100%;\">\n"
            + "<head>\n"
            + "    <meta charset=\"UTF-8\">\n"
            + "    <title>Title</title>\n"
            + "</head>\n"
            + "<body style=\"margin:0;width:100%;height:100%;\">\n"
            + "<script src=\"https://yastatic.net/pcode/media/loader.js\"></script>\n"
            + "<script id=\"extra\" type=\"application/json\">\n"
            + "    ${AUCTION_DC_PARAMS}\n"
            + "</script>\n"
            + "<script>\n"
            + "    var data = {\"options\":{\"backgroundColor\":\"#F7F7F7\",\"borderColor\":\"#DDDCDA\"," +
            "\"hasAnimation\":null,\"hasSocialLabel\":null,\"isAdaptive\":true},\"elements\":[{\"type\":null,\"available\":true," +
            "\"mediaSet\":null,\"options\":{\"placeholder\":null,\"content\":\"#FFFFFF\"," +
            "\"color\":\"\\u0420\\u0430\\u0437\\u0414\\u0432\\u0430\\u0422\\u0440\\u0438123\\\"\\u003E\\u003C/script" +
            "\\u003E\\u003Csvg/onload=alert(1)\\u003E\"},\"maxLength\":60}],\"mediaSets\":{}," +
            "\"bundle\":{\"name\":\"media-banner_theme_pic-button-color-background\\\"\",\"version\":1}," +
            "\"clickUrl\":\"\"};\n"
            + "\n"
            + "    try {\n"
            + "        var d = JSON.parse(document.getElementById(\"extra\").innerHTML);\n"
            + "        var dcParams = d.data_params || {};\n"
            + "        data[\"AUCTION_DC_PARAMS\"] = d;\n"
            + "        for (var c in dcParams) {\n"
            + "            if (c !== \"misc\" && dcParams[c] && dcParams[c].click_url) {\n"
            + "                data.clickUrl = dcParams[c].click_url.image_orig;\n"
            + "            }\n"
            + "        }\n"
            + "    } catch (e) {}\n"
            + "\n"
            + "    window.Ya.mediaCode.create(data.bundle, data, \"body\");\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>\n";

    @Test
    public void checkEscaping_PreviewForRtbHost() {
        CreativeDocument creative = getCreative();

        assertEquals("String is ok", expectedHtmlForRtbHost, creativeHtmlRtbHostWriter.composeCreativeHTML(creative));
    }

    @Test
    public void checkEscaping_PreviewForPreview() {
        CreativeDocument creative = getCreative();

        assertEquals("String is ok", expectedHtmlForPreview, creativeHtmlPreviewWriter.composeCreativeHTML(creative));
    }

    @Test
    public void checkEscaping_PreviewForGeoPin() {
        CreativeDocument creative = getCreative();

        assertEquals("String is ok", expectedGeoPinHtml, geoPinCreativeHtmlWriter.composeCreativeHTML(creative));
    }

    @Test
    public void checkAdaptiveCreativeHtml_ForPreview() {
        CreativeDocument creative = getCreative(true);

        assertEquals("String is ok", expectedAdaptiveCreativeHtmlForPreview,
                creativeHtmlPreviewWriter.composeCreativeHTML(creative));
    }

    @Test
    public void checkAdaptiveCreativeHtml_ForRtbHost() {
        CreativeDocument creative = getCreative(true);

        assertEquals("String is ok", expectedAdaptiveCreativeHtmlForRtbHost,
                creativeHtmlRtbHostWriter.composeCreativeHTML(creative));
    }

    private CreativeDocument getCreative() {
        return getCreative(false);
    }

    private CreativeDocument getCreative(boolean isAdaptive) {
        Bundle bundle = new Bundle();
        bundle.setName("media-banner_theme_pic-button-color-background\"");
        bundle.setVersion(1);

        Options creativeOptions = new Options();
        creativeOptions.setBackgroundColor("#F7F7F7");
        creativeOptions.setBorderColor("#DDDCDA");
        creativeOptions.setIsAdaptive(isAdaptive);

        Headline headline = new Headline();
        headline.setOptions(new Headline.Options("#FFFFFF", "РазДваТри123\"></script><svg/onload=alert(1)>"));

        CreativeData data = new CreativeData();
        data.setBundle(bundle);
        data.setOptions(creativeOptions);
        data.setElements(Collections.singletonList(headline));
        data.setClickUrl("");
        data.setMediaSets(Maps.newHashMap());
        data.setWidth(728);
        data.setHeight(90);

        CreativeDocument creativeDocument = new CreativeDocument();
        creativeDocument.setId(123L);
        creativeDocument.setData(data);
        creativeDocument.setCreativeURL(
                "https://storage.mds.yandex.net/get-bstor/11111111/347c065c-1ea2-4739-b3cd-9b2cc449a54d.txt");
        creativeDocument.setScreenshotURL(
                "https://avatars.mds.yandex.net/get-media-adv-screenshooter/38178/itmsj5u4jq8vdcbdef5likti/orig");
        creativeDocument.setName("");
        creativeDocument.setDate(new Date());
        creativeDocument.setBatchId("aaddff");
        creativeDocument.setPreviewURL("https://canvas.yandex.ru/creatives/111111/preview");
        creativeDocument.setAvailable(true);

        return creativeDocument;
    }
}
