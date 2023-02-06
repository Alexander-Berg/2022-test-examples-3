package ru.yandex.canvas.service.rtbhost;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.Bundle;
import ru.yandex.canvas.model.CreativeData;
import ru.yandex.canvas.model.CreativeDocument;
import ru.yandex.canvas.model.MediaSet;
import ru.yandex.canvas.model.MediaSetItem;
import ru.yandex.canvas.model.MediaSetSubItem;
import ru.yandex.canvas.model.SmartCenter;
import ru.yandex.canvas.model.elements.Image;
import ru.yandex.canvas.service.rtbhost.helpers.CreativesDspUploadFacade;
import ru.yandex.direct.bs.dspcreative.model.DspCreativeExportEntry;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CreativesServiceDspCreativeEntityTest {

    private static final String CREATIVE_DATA_HTML = "<!DOCTYPE html>\n"
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
            "\"available\":true,\"mediaSet\":\"uniq1\",\"options\":{\"width\":100,\"height\":100}}]," +
            "\"mediaSets\":{\"uniq1\":{\"items\":[{\"type\":\"image\",\"items\":[{\"url\":\"url\",\"width\":100," +
            "\"height\":100,\"alias\":null,\"smartCenters\":[{\"w\":40,\"h\":50,\"x\":70,\"y\":60}],\"fileId\":null," +
            "\"croppedFileId\":null}]}]}},\"bundle\":{\"name\":\"media-banner_theme_pic-button-color-background\"," +
            "\"version\":1},\"clickUrl\":\"\"};\n"
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

    private static final String CREATIVE_DATA_NO_SMARTCENTERS_HTML = "<!DOCTYPE html>\n"
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
            "\"available\":true,\"mediaSet\":\"uniq1\",\"options\":{\"width\":100,\"height\":100}}]," +
            "\"mediaSets\":{\"uniq1\":{\"items\":[{\"type\":\"image\",\"items\":[{\"url\":\"url\",\"width\":100," +
            "\"height\":100,\"alias\":null,\"fileId\":null,\"croppedFileId\":null}]}]}}," +
            "\"bundle\":{\"name\":\"media-banner_theme_pic-button-color-background\",\"version\":1}," +
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

    private static final String CREATIVE_DATA_EMPTY_SMARTCENTERS_HTML = "<!DOCTYPE html>\n"
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
            "\"available\":true,\"mediaSet\":\"uniq1\",\"options\":{\"width\":100,\"height\":100}}]," +
            "\"mediaSets\":{\"uniq1\":{\"items\":[{\"type\":\"image\",\"items\":[{\"url\":\"url\",\"width\":100," +
            "\"height\":100,\"alias\":null,\"smartCenters\":[],\"fileId\":null,\"croppedFileId\":null}]}]}}," +
            "\"bundle\":{\"name\":\"media-banner_theme_pic-button-color-background\",\"version\":1}," +
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

    @Autowired
    private CreativesDspUploadFacade creativesDspUploadHelper;

    @Test
    public void toImportDSPCreativeEntry_CreativeWithSmartCenters() {
        DspCreativeExportEntry dspCreativeExportEntry =
                creativesDspUploadHelper.toImportDspCreativeEntry(getCreative(singletonList(new SmartCenter(40, 50,
                                60, 70))),
                        new ObjectMapper());

        assertThat("Полученная html соответствует ожидаемой", dspCreativeExportEntry.getData(), is(CREATIVE_DATA_HTML));
    }

    @Test
    public void toImportDSPCreativeEntry_CreativeWithNoSmartCenters() {
        DspCreativeExportEntry dspCreativeExportEntry =
                creativesDspUploadHelper.toImportDspCreativeEntry(getCreative(null), new ObjectMapper());

        assertThat("Полученная html соответствует ожидаемой", dspCreativeExportEntry.getData(),
                is(CREATIVE_DATA_NO_SMARTCENTERS_HTML));
    }

    @Test
    public void toImportDSPCreativeEntry_CreativeWithEmptySmartCenters() {
        DspCreativeExportEntry dspCreativeExportEntry =
                creativesDspUploadHelper.toImportDspCreativeEntry(getCreative(emptyList()), new ObjectMapper());

        assertThat("Полученная html соответствует ожидаемой", dspCreativeExportEntry.getData(),
                is(CREATIVE_DATA_EMPTY_SMARTCENTERS_HTML));
    }

    @Test
    public void toImportDSPCreativeEntry_AdaptiveCreative_ZeroSizeSentToRtbHost() {
        DspCreativeExportEntry dspCreativeExportEntry =
                creativesDspUploadHelper.toImportDspCreativeEntry(getAdaptiveCreative(singletonList(new SmartCenter(40, 50, 60, 70))),
                        new ObjectMapper());

        assertThat(dspCreativeExportEntry.getWidth(), is(0));
        assertThat(dspCreativeExportEntry.getHeight(), is(0));
    }

    private CreativeDocument getCreative(List<SmartCenter> smartCenters) {
        return getCreative(smartCenters, false);
    }

    private CreativeDocument getAdaptiveCreative(List<SmartCenter> smartCenters) {
        return getCreative(smartCenters, true);
    }

    private CreativeDocument getCreative(List<SmartCenter> smartCenters, boolean isAdaptive) {
        Bundle bundle = new Bundle();
        bundle.setName("media-banner_theme_pic-button-color-background");
        bundle.setVersion(1);

        CreativeData.Options creativeOptions = new CreativeData.Options();
        creativeOptions.setIsAdaptive(isAdaptive);
        if (isAdaptive) {
            creativeOptions.setMinWidth(100);
            creativeOptions.setMinHeight(100);
        }
        creativeOptions.setBackgroundColor("#F7F7F7");
        creativeOptions.setBorderColor("#DDDCDA");

        Image image = new Image();
        Image.Options options = new Image.Options();
        options.setHeight(100);
        options.setWidth(100);
        image.setOptions(options);
        image.setMediaSet("uniq1");

        CreativeData data = new CreativeData();
        data.setBundle(bundle);
        data.setOptions(creativeOptions);
        data.setElements(singletonList(image));
        data.setClickUrl("");

        MediaSetSubItem mediaSetSubItem = new MediaSetSubItem();
        mediaSetSubItem.setSmartCenters(smartCenters);
        mediaSetSubItem.setWidth(100);
        mediaSetSubItem.setHeight(100);

        MediaSetItem mediaSetItem = new MediaSetItem();
        mediaSetSubItem.setUrl("url");
        mediaSetItem.setType("image");
        mediaSetItem.setItems(singletonList(mediaSetSubItem));

        MediaSet mediaSet = new MediaSet();
        mediaSet.setItems(singletonList(mediaSetItem));

        data.setMediaSets(ImmutableMap.of("uniq1", mediaSet));
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
