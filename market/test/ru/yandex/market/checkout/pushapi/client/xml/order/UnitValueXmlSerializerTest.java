package ru.yandex.market.checkout.pushapi.client.xml.order;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.yandex.market.checkout.checkouter.order.UnitValue;
import ru.yandex.market.checkout.checkouter.order.UnitValueBuilder;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.sameXmlAs;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.serialize;

/**
 * @author Mikhail Usachev <mailto:musachev@yandex-team.ru>
 * Date: 31/05/2017.
 */
public class UnitValueXmlSerializerTest {

    private UnitValueXmlSerializer serializer = new UnitValueXmlSerializer();

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
                        (new UnitValueBuilder()).withShopValues(null).withValues(null).withDefaultUnit(false).build(),
                        "<unit-value unit-id='RU' default-unit='false'/>"
                }
        }).stream().map(Arguments::of);
    }


    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testUnitValueXmlSerializer(UnitValue unitValue, String expectedXml) throws Exception {
        String actual = serialize(serializer, unitValue);
        assertThat(
                actual,
                is(sameXmlAs(expectedXml))
        );
    }
}
