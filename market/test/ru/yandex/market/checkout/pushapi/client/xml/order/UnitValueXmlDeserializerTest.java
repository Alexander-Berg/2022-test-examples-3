package ru.yandex.market.checkout.pushapi.client.xml.order;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.yandex.market.checkout.checkouter.order.UnitValue;
import ru.yandex.market.checkout.checkouter.order.UnitValueBuilder;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author Mikhail Usachev <mailto:musachev@yandex-team.ru>
 * Date: 31/05/2017.
 */
public class UnitValueXmlDeserializerTest {

    private UnitValueXmlDeserializer deserializer = new UnitValueXmlDeserializer();

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(new Object[][]{
                {
                        (new UnitValueBuilder()).build(),
                        "<unit-value values='1,2,3' shop-values='1,2' unit-id='RU' default-unit='true'/>"
                },
                {
                        (new UnitValueBuilder()).withShopValues(null).build(),
                        "<unit-value values='1,2,3' unit-id='RU' default-unit='true'/>"
                },
                {
                        (new UnitValueBuilder())
                                .withValues(Arrays.asList("AA", "A", "B", "C", "D"))
                                .withShopValues(null)
                                .build(),
                        "<unit-value values='AA,A,B,C,D' unit-id='RU' default-unit='true'/>"
                },
        }).stream().map(Arguments::of);
    }


    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testUnitValueXmlDeserializer(UnitValue unitValue, String xmlString) throws Exception {
        final UnitValue actual = XmlTestUtil.deserialize(deserializer, xmlString);
        Assertions.assertTrue(unitValue.equals(actual));
    }

}
