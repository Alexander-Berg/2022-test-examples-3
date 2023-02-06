package ru.yandex.market.admin.outlet.errors;

import java.io.IOException;
import java.util.List;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.outlet.errorserializer.XmlErrorSerializer;
import ru.yandex.market.admin.outlet.fileupload.Column;
import ru.yandex.market.admin.outlet.fileupload.Error;
import ru.yandex.market.admin.outlet.fileupload.ErrorType;
import ru.yandex.market.admin.outlet.fileupload.Errors;
import ru.yandex.market.admin.outlet.fileupload.Row;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class XmlErrorSerializerTest extends FunctionalTest {
    private XmlErrorSerializer serializer = new XmlErrorSerializer();

    @DisplayName("Сериализовать список из одной ошибки")
    @Test
    void serializeErrorsSingletonList() throws IOException, SAXException {
        Errors errors = Errors.of(new Error(Row.withIndex(1), Column.IS_ENABLED, ErrorType.IS_MISSING));
        final String expected = IOUtils.readInputStream(getClass().getResourceAsStream("xml/error.xml"));
        assertXmlsEqual(expected, serializer.serialize(errors));
    }

    @DisplayName("Сериализовать неизвестную ошибку")
    @Test
    void serializeUnknownError() throws IOException, SAXException {
        Errors errors = Errors.of(new Error(ErrorType.UNKNOWN_ERROR));
        final String expected = IOUtils.readInputStream(getClass().getResourceAsStream("xml/unknown_error.xml"));
        assertXmlsEqual(expected, serializer.serialize(errors));
    }

    @DisplayName("Сериализовать список ошибок")
    @Test
    void serializeErrorsList() throws IOException, SAXException {
        Errors errors = Errors.of(
                new Error(Row.withIndex(0), Column.ADDRESS_BLOCK, ErrorType.WRONG_FORMAT),
                new Error(Row.withIndex(1), Column.IS_ENABLED, ErrorType.UNKNOWN_ERROR),
                new Error(Row.withIndex(2), Column.LONGITUDE, ErrorType.NOT_FOUND),
                new Error(Row.withIndex(3), Column.SCHEDULE_TUE_TO, ErrorType.IS_NOT_ERROR),
                new Error(Row.withIndex(4), List.of(
                        Column.ADDRESS_BLOCK, Column.IS_ENABLED),
                        ErrorType.COLUMNS_GROUP_IS_MISSING)
        );

        final String expected = IOUtils.readInputStream(getClass().getResourceAsStream("xml/errors.xml"));
        assertXmlsEqual(expected, serializer.serialize(errors));
    }

    @DisplayName("Десериализовать список из одной ошибки")
    @Test
    void deserializeErrorsSingletonList() throws IOException {
        Errors expectedErrors = Errors.of(new Error(Row.withIndex(1), Column.IS_ENABLED, ErrorType.IS_MISSING));
        final String actualErrorsXml = IOUtils.readInputStream(getClass().getResourceAsStream("xml/error.xml"));
        assertEquals(expectedErrors, serializer.deserialize(actualErrorsXml));
    }

    @DisplayName("Десриализовать неизвестную ошибку")
    @Test
    void deserializeUnknownError() throws IOException {
        Errors expectedErrors = Errors.of(new Error(ErrorType.UNKNOWN_ERROR));
        final String actualErrorsXml = IOUtils.readInputStream(getClass().getResourceAsStream("xml/unknown_error.xml"));
        assertEquals(expectedErrors, serializer.deserialize(actualErrorsXml));
    }

    @DisplayName("Десериализовать список ошибок")
    @Test
    void deserializeErrorsList() throws IOException {
        Errors expectedErrors = Errors.of(
                new Error(Row.withIndex(0), Column.ADDRESS_BLOCK, ErrorType.WRONG_FORMAT),
                new Error(Row.withIndex(1), Column.IS_ENABLED, ErrorType.UNKNOWN_ERROR),
                new Error(Row.withIndex(2), Column.LONGITUDE, ErrorType.NOT_FOUND),
                new Error(Row.withIndex(3), Column.SCHEDULE_TUE_TO, ErrorType.IS_NOT_ERROR),
                new Error(Row.withIndex(4), List.of(
                        Column.ADDRESS_BLOCK, Column.IS_ENABLED),
                        ErrorType.COLUMNS_GROUP_IS_MISSING)
        );
        final String actualErrorsXml = IOUtils.readInputStream(getClass().getResourceAsStream("xml/errors.xml"));
        assertEquals(expectedErrors, serializer.deserialize(actualErrorsXml));
    }

    private void assertXmlsEqual(String actual, String expected) throws SAXException, IOException {
        XMLUnit.setIgnoreWhitespace(true);
        Document expectedXml = XMLUnit.buildControlDocument(expected);
        Document actualXml = XMLUnit.buildTestDocument(actual);
        Diff diff = XMLUnit.compareXML(expectedXml, actualXml);
        assertTrue(diff.toString(), diff.identical());
    }
}
