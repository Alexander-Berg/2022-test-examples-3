package ru.yandex.market.partner.notification.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.partner.notification.AbstractFunctionalTest;

public class JsonToNotificationContextDataConverterTest extends AbstractFunctionalTest {

    @Autowired
    private JsonToNotificationContextDataConverter converter;

    @Test
    void testConversion() throws IOException, JDOMException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = getClass().getResourceAsStream("converter.data.json");
        Object parsedData = mapper.readValue(is, Object.class);
        String xmlData = converter.getXml(parsedData);

        SAXBuilder builder = new SAXBuilder();
        Element body = builder.build(new StringReader(xmlData)).getRootElement();
        body.detach();
        List<String> result = new ArrayList<>();
        for (Element child : (List<Element>) body.getChildren()) {
            if (CollectionUtils.isNotEmpty(child.getChildren())) {
                for (Element ch : (List<Element>) child.getChildren()) {
                    result.add(ch.getName());
                }
                result.add(child.getName());
            } else {
                result.add(child.getName());
            }
        }
        Assertions.assertThat(result).contains("test-data", "key-data", "user-id",
                "campaign-id", "note-numbers-count").doesNotContain("testData", "keyData", "userId",
                "campaignId", "noteNumbersCount");
    }
}
