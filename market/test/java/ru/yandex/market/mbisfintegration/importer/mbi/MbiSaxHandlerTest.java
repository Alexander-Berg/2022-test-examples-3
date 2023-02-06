package ru.yandex.market.mbisfintegration.importer.mbi;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.xml.sax.SAXException;

import ru.yandex.market.mbisfintegration.AbstractFunctionalTest;


class MbiSaxHandlerTest extends AbstractFunctionalTest {
    @Mock
    ImportExecutionContext context;

    @Captor
    ArgumentCaptor<Map<String, Object>> dtoCaptor;

    @Autowired
    ResourceLoader resourceLoader;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void parseFile() throws ParserConfigurationException, SAXException, IOException {
        Mockito.when(context.getEntityXmlElementName())
                .thenReturn("entity");

        InputStream inputStream = resourceLoader.getResource("classpath:/commonTest.xml").getInputStream();
        SAXParserFactory.newInstance()
                .newSAXParser()
                .parse(inputStream, new MbiImportSaxHandler(context));

        Mockito.verify(context, Mockito.times(5))
                .addDto(dtoCaptor.capture());

        Map<String, Object> dto = dtoCaptor.getAllValues().get(0);
        Assertions.assertEquals(1, dto.size());
        Assertions.assertEquals("b", dto.get("name"));

        dto = dtoCaptor.getAllValues().get(1);
        Assertions.assertEquals(1, dto.size());
        Map<String, Object> objectWithAttributes = (Map<String, Object>) dto.get("object_with_attributes");
        Assertions.assertEquals(2, objectWithAttributes.size());
        Assertions.assertEquals("a", objectWithAttributes.get("a"));
        Assertions.assertEquals("b", objectWithAttributes.get("b"));

        dto = dtoCaptor.getAllValues().get(2);
        Assertions.assertEquals(1, dto.size());
        Map<String, Object> object = (Map<String, Object>) dto.get("object");
        Assertions.assertEquals(1, object.size());
        Assertions.assertEquals("b", objectWithAttributes.get("b"));

        dto = dtoCaptor.getAllValues().get(3);
        Assertions.assertEquals(1, dto.size());
        Map<String, Object> objectWithAttributeAndFields = (Map<String, Object>) dto.get("object_with_attribute_and_fields");
        Assertions.assertEquals(3, objectWithAttributeAndFields.size());
        Assertions.assertEquals("a", objectWithAttributeAndFields.get("a"));
        Assertions.assertEquals("b", objectWithAttributeAndFields.get("b"));
        Assertions.assertEquals("c", objectWithAttributeAndFields.get("c"));

        dto = dtoCaptor.getAllValues().get(4);
        Assertions.assertEquals(1, dto.size());
        Map<String, Object> list = (Map<String, Object>) dto.get("list");
        Assertions.assertEquals(1, list.size());
        List<Map<String, Object>> element = (List<Map<String, Object>>) list.get("element");
        Assertions.assertEquals(2, element.size());
        Assertions.assertEquals("a", element.get(0).get("a"));
        Assertions.assertEquals("a", element.get(1).get("a"));
    }
}