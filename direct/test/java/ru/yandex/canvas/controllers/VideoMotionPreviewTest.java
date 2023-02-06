package ru.yandex.canvas.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.controllers.video.PreviewResponseEntity;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VideoMotionPreviewTest {

    @Autowired
    private MockMvc mockMvc;

    private Node xmlRoot;

    @Before
    public void makeRequest() throws Exception {
        String data = "{\n"
                + "\"previewData\": {\n"
                + "             \"age\": \"18\",\n"
                + "             \"adId\": \"1490779883991\",\n"
                + "             \"title\": \"Развивающая пирамидка «Жираф»\",\n"
                + "             \"body\": \"Малышу понравится ощупывать и жевать голову жирафа и кольца пирамидки\",\n"
                + "             \"url\": \"http://www.elc-russia"
                + ".ru/razvivajuschaja-sensornaja-piramidka-zhiraf-elc-130560.html?yclid=977686101426285\",\n"
                + "             \"domain\": \"www.elc-russia.ru\",\n"
                + "             \"punyDomain\": \"www.elc-russia.ru\",\n"
                + "             \"favicon\": \"https://favicon.yandex.net/favicon/www.elc-russia.ru/\",\n"
                + "             \"vcardUrl\": \"#\",\n"
                + "\t\t     \"callUrl\": \"\",\n"
                + "             \"warning\": \"Внимание пирамидка вызывает увлечение пирамидками у детей и "
                + "взрослых\",\n"
                + "             \"dynamic_disclaimer\": \"1\",\n"
                + "             \"banner_flags\": \"medicine,weapon\",\n"
                + "             \"sitelinks\": [{\"title\": \"мсмвымывы\", \"url\": \"http://www.elc-russia.ru/\", "
                + "\"description\": \"\"}],\n"
                + "             \"linkTail\": \"\",\n"
                + "             \"debug\": \"\",\n"
                + "             \"images\": [[\"https://example.com/image1.jpg\", \"251\", \"300\"],"
                + "[\"https://example.com/image2.jpg\", \"100\", \"100\"] ]\n"
                + "         }\n"
                + "}";

        final String[] result = new String[1];

        mockMvc.perform(post("/video/additions/video-motion/preview")
                .locale(Locale.forLanguageTag("ru"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(data)
                .param("client_id", String.valueOf(1L))
                .param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(e -> result[0] = e.getResponse().getContentAsString())
                .andExpect(content().string(Matchers.not(StringContains.containsString("vpaid-creative.js"))))
                .andExpect(status().is(200));

        PreviewResponseEntity previewResponseEntity = new ObjectMapper().readValue(result[0],
                PreviewResponseEntity.class);

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(previewResponseEntity.getVast().getBytes());

        xmlRoot = docBuilder.parse(byteArrayInputStream);
    }

    @Test
    public void checkXml() {
        assertThat("creative id", xmlRoot, allOf(
                hasXPath("/VAST/Ad/InLine/Creatives/Creative[1]/@id", equalTo("777")),
                hasXPath("/VAST/Ad/InLine/Creatives/Creative[1]/Linear/Duration", equalTo("00:00:15")),
                hasXPath("/VAST/Ad/InLine/Creatives/Creative[1]/Linear/MediaFiles/MediaFile",
                        containsString("https://yastatic.net/awaps-ad-sdk-js/1_0/interactive_viewer.js"))
                )
        );
    }

    @Test
    public void checkUnmoderated() throws XPathExpressionException, IOException {
        Map<String, Object> paramsContent = getAdParametersMap();

        Map<String, Object> unmoderated =
                mapPath(paramsContent, "AUCTION_DC_PARAMS", "data_params", "offer123", "unmoderated");

        assertThat("unmoderated images", unmoderated, hasKey("images"));

        List<List<String>> images = (List<List<String>>) unmoderated.get("images");
        assertThat("images are in list", images, instanceOf(List.class));
        assertThat("images has 2 records", images, hasSize(2));
        assertThat("images has", images.get(0), contains("https://example.com/image1.jpg", "251", "300"));
        assertThat("images has", images.get(1), contains("https://example.com/image2.jpg", "100", "100"));

        assertThat("unmoderated sitelinks", unmoderated, hasKey("sitelinks"));
        /*
        [{\"title\": \"мсмвымывы\", \"url\": \"http://www.elc-russia.ru/\", "
                + "\"description\": \"\"}],
         */
        List<Map<String, String>> sitelinks = (List<Map<String, String>>) unmoderated.get("sitelinks");
        assertThat("sitelinks are in list", sitelinks, instanceOf(List.class));
        assertThat("sitelinks has one record", sitelinks, hasSize(1));
        assertThat("sitelinks has all fields", sitelinks.get(0), allOf(
                hasEntry("title", "мсмвымывы"), hasEntry("url", "http://www.elc-russia.ru/"),
                hasEntry("description", "")
        ));

    }

    @Test
    public void checkAdParameters() throws XPathExpressionException, IOException {

        Map<String, Object> paramsContent = getAdParametersMap();

        assertThat("HAS_DOMAIN", paramsContent.get("HAS_DOMAIN"), equalTo(false));
        assertThat("HAS_TITLE", paramsContent.get("HAS_TITLE"), equalTo(false));
        assertThat("theme", paramsContent.get("theme"), equalTo("video-banner_motion"));

        Map<String, Object> offer = mapPath(paramsContent, "AUCTION_DC_PARAMS", "data_params", "offer123");

        assertThat("offer contains target_url", offer, hasEntry("target_url",
                "http://www.elc-russia.ru/razvivajuschaja-sensornaja-piramidka-zhiraf-elc-130560"
                        + ".html?yclid=977686101426285"));

        assertThat("offer contains text", offer, hasKey("text"));

        Map<String, Object> text = (Map) offer.get("text");

        assertThat("title", text, hasEntry("title", "Развивающая пирамидка «Жираф»"));
        assertThat("domain", text, hasEntry("domain", "www.elc-russia.ru"));
        assertThat("dynamic_disclaimer", text, hasEntry("dynamic_disclaimer", "1"));
        assertThat("punyDomain", text, hasEntry("punyDomain", "www.elc-russia.ru"));
        assertThat("age", text, hasEntry("age", "18"));
        assertThat("body", text,
                hasEntry("body", "Малышу понравится ощупывать и жевать голову жирафа и кольца пирамидки"));
        assertThat("warning", text,
                hasEntry("warning", "Внимание пирамидка вызывает увлечение пирамидками у детей и взрослых"));

        Map<String, Object> trackingEvents = (Map) paramsContent.get("trackingEvents");
        assertThat("trackingEvents", trackingEvents, notNullValue());
        ArrayList<String> startTrackingEvents = (ArrayList) trackingEvents.get("start");
        assertThat("startTrackingEvents", startTrackingEvents, hasItem("${AUCTION_RENDER_URL}"));
    }

    private Map<String, Object> getAdParametersMap() throws XPathExpressionException, IOException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xPath.evaluate("/VAST/Ad/InLine/Creatives/Creative[1]/Linear/AdParameters",
                xmlRoot, XPathConstants.NODESET);

        String params = nodes.item(0).getTextContent();

        return new ObjectMapper().readValue(params, Map.class);
    }

    private Map<String, Object> mapPath(Map<String, Object> root, String... keys) {
        for (String key : keys) {
            assertThat("Key <" + key + "> exists in map", root.get(key), notNullValue());
            root = (Map) root.get(key);
        }

        return root;
    }

}
