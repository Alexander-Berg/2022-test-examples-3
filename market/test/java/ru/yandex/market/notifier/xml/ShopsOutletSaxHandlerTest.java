package ru.yandex.market.notifier.xml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public class ShopsOutletSaxHandlerTest {
    @Test
    public void shouldParseXmlCorrectly() throws IOException, SAXException {
        try(InputStream inputStream = ShopsOutletSaxHandlerTest.class.getResourceAsStream("/files/outlets.xml")) {
            ShopsOutletSaxHandler contentHandler = new ShopsOutletSaxHandler();
            ShopsOutletSaxHandler.parseXmlStream(inputStream, contentHandler);
            Assertions.assertEquals(
                    "Служба с аутлетами разного типа",
                    contentHandler.getDeliveryServiceIdToNameMap().get(100501L)
            );
            Assertions.assertEquals(
                    "Служба настроенная по умолчанию на INTAKE у 774",
                    contentHandler.getDeliveryServiceIdToNameMap().get(100502L)
            );
        }
    }
}
