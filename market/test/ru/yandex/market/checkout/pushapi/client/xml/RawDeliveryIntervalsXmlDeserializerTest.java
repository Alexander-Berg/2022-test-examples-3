package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import java.time.LocalTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RawDeliveryIntervalsXmlDeserializerTest {

    public static Stream<Arguments> parameterizedTestData() {
        CheckoutDateFormat format = new CheckoutDateFormat();
        return Stream.of(
                Arguments.of(
                        "<interval date='15-09-2018' from-time='08:00' to-time='12:00' default='true'/>",
                        new RawDeliveryInterval(format.parse("15-09-2018", false), LocalTime.parse("08:00"), LocalTime.parse("12:00"), true)
                ),
                Arguments.of(
                        "<interval date='15-09-2018' from-time='08:00' to-time='12:00'/>",
                        new RawDeliveryInterval(format.parse("15-09-2018", false), LocalTime.parse("08:00"), LocalTime.parse("12:00"))
                ),
                Arguments.of(
                        "<interval date='15-09-2018' from-time='11:00' to-time='15:00'/>",
                        new RawDeliveryInterval(format.parse("15-09-2018", false), LocalTime.parse("11:00"), LocalTime.parse("15:00"))
                ),
                Arguments.of(
                        "<interval date='16-09-2018' from-time='01:00' to-time='15:00'/>",
                        new RawDeliveryInterval(format.parse("16-09-2018", false), LocalTime.parse("01:00"), LocalTime.parse("15:00"))
                ),
                Arguments.of(
                        "<interval date='17-09-2018'/>",
                        new RawDeliveryInterval(format.parse("17-09-2018", false))
                )
        );
    }

    private RawDeliveryIntervalXmlDeserializer deserializer = new RawDeliveryIntervalXmlDeserializer();

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testIntervalDeserialization(String xml, RawDeliveryInterval interval) throws Exception {
        final RawDeliveryInterval actual = XmlTestUtil.deserialize(deserializer, xml);

        assertEquals(interval,actual,"Intervals aren't equals.");
    }
}
