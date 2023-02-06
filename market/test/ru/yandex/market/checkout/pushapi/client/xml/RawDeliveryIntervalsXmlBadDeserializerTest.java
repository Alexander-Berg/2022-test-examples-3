package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

public class RawDeliveryIntervalsXmlBadDeserializerTest {

    private RawDeliveryIntervalXmlDeserializer deserializer = new RawDeliveryIntervalXmlDeserializer();

    @ParameterizedTest
    @ValueSource(strings = {
            "<interval date='16-09-2018' from-time='02:00' to-time='01:00'/>",
            "<interval date='16-09-2018' from-time='10:00' to-time='70:00'/>",
            "<interval date='16-09-2018' from-time='11:00' to-time='11:00'/>",
            "<interval date='hhh'/>",
            "<interval date='01/01/2013'/>"
    })
    public void testBadDeserialization(String xml) {
        Assertions.assertThrows(Exception.class, () -> {
            XmlTestUtil.deserialize(deserializer, xml);
        });
    }
}
