package ru.yandex.market.personal.data.cleaner;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonalDataCleanerServiceTest {

    private final PersonalDataCleanerService cleaner = PersonalDataCleanerService.builder().build();

    @Test
    void checkJson() throws JSONException {
        String actual = cleaner.clearPersonalData("application/json", loadFile("/files/json/response1.json"));

        JSONAssert.assertEquals(
                loadFile("/files/json/response1_expected.json"),
                actual,
                JSONCompareMode.STRICT_ORDER
        );
        assertThat(actual).doesNotContain("\n", "\r");
    }

    @Test
    void checkJsonWithCharset() throws JSONException {
        String actual = cleaner.clearPersonalData("application/json;charset=utf-8",
                loadFile("/files/json/response1.json"));

        JSONAssert.assertEquals(
                loadFile("/files/json/response1_expected.json"),
                actual,
                JSONCompareMode.STRICT_ORDER
        );
        assertThat(actual).doesNotContain("\n", "\r");
    }

    @Test
    void checkJsonBytes() throws JSONException {
        byte[] actual = cleaner.clearPersonalData(
                "application/json",
                loadFile("/files/json/response1.json").getBytes(StandardCharsets.UTF_8)
        );

        JSONAssert.assertEquals(
                loadFile("/files/json/response1_expected.json"),
                new String(actual, StandardCharsets.UTF_8),
                JSONCompareMode.STRICT_ORDER
        );
    }

    @Test
    void checkXmlWithCharset() throws IOException, SAXException {
        String actual = cleaner.clearPersonalData("application/xml;charset=utf-8",
                loadFile("/files/xml/response1.xml"));

        XMLAssert.assertXMLEqual(
                new StringReader(loadFile("/files/xml/response1_expected.xml")),
                new StringReader(actual)
        );
    }

    @Test
    void checkXml() throws IOException, SAXException {
        String actual = cleaner.clearPersonalData("application/xml", loadFile("/files/xml/response1.xml"));

        XMLAssert.assertXMLEqual(
                new StringReader(loadFile("/files/xml/response1_expected.xml")),
                new StringReader(actual)
        );
    }

    @Test
    void checkXmlBytes() throws IOException, SAXException {
        byte[] actual = cleaner.clearPersonalData(
                "application/xml",
                loadFile("/files/xml/response1.xml").getBytes(StandardCharsets.UTF_8)
        );

        XMLAssert.assertXMLEqual(
                new StringReader(loadFile("/files/xml/response1_expected.xml")),
                new StringReader(new String(actual, StandardCharsets.UTF_8))
        );
    }

    @Test
    void unknownMimeType() {
        assertThatThrownBy(() -> cleaner.clearPersonalData("json/xml", loadFile("/files/xml/response1.xml")))
                .as("Unknown mimeType: json/xml")
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsingErrorJson() {
        assertThatThrownBy(() -> cleaner.clearPersonalData("application/json", loadFile("/files/xml/response1.xml")))
                .as("Cannot remove personal data from given JSON")
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsingErrorXml() {
        assertThatThrownBy(() -> cleaner.clearPersonalData("application/xml", loadFile("/files/json/response1.json")))
                .as("Cannot remove personal data from given XML")
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customReplacementTextJson() throws JSONException {
        PersonalDataCleanerService custom = PersonalDataCleanerService.builder()
                .withReplacementText("REMOVED")
                .build();
        String actual = custom.clearPersonalData("application/json", loadFile("/files/json/response1.json"));

        JSONAssert.assertEquals(
                loadFile("/files/json/response1_custom_replacement_text_expected.json"),
                actual,
                JSONCompareMode.STRICT_ORDER
        );
    }

    @Test
    void customReplacementTextXml() throws IOException, SAXException {
        PersonalDataCleanerService custom = PersonalDataCleanerService.builder()
                .withReplacementText("REMOVED")
                .build();
        String actual = custom.clearPersonalData("application/xml", loadFile("/files/xml/response1.xml"));

        XMLAssert.assertXMLEqual(
                new StringReader(loadFile("/files/xml/response1_custom_replacement_text_expected.xml")),
                new StringReader(actual)
        );
    }

    @Test
    void customElementsToCleanJson() throws JSONException {
        PersonalDataCleanerService custom = PersonalDataCleanerService.builder()
                .addNode("order", false, "paymentType", "paymentMethod")
                .addNode("items", true, "feedId", "offerId")
                .build();
        String actual = custom.clearPersonalData("application/json", loadFile("/files/json/response1.json"));

        JSONAssert.assertEquals(
                loadFile("/files/json/response1_custom_elements_expected.json"),
                actual,
                JSONCompareMode.STRICT_ORDER
        );
    }

    @Test
    void customElementsToCleanXml() throws IOException, SAXException {
        PersonalDataCleanerService custom = PersonalDataCleanerService.builder()
                .addNode("order", false, "paymentType", "paymentMethod")
                .addNode("items", true, "feedId", "offerId")
                .build();
        String actual = custom.clearPersonalData("application/xml", loadFile("/files/xml/response1.xml"));

        XMLAssert.assertXMLEqual(
                new StringReader(loadFile("/files/xml/response1_custom_elements_expected.xml")),
                new StringReader(actual)
        );
    }

    private static String loadFile(String file) {
        try (InputStream is = PersonalDataCleanerServiceTest.class.getResourceAsStream(file)) {
            return IOUtils.toString(Objects.requireNonNull(is));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
