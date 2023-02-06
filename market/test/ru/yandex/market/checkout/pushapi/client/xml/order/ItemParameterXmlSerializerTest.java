package ru.yandex.market.checkout.pushapi.client.xml.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.yandex.market.checkout.checkouter.order.ItemParameter;
import ru.yandex.market.checkout.checkouter.order.ItemParameterBuilder;
import ru.yandex.market.checkout.checkouter.order.UnitValueBuilder;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.sameXmlAs;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.serialize;

public class ItemParameterXmlSerializerTest {

    private ItemParameterXmlSerializer serializer = new ItemParameterXmlSerializer();

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(new Object[][]{
                {
                        (new ItemParameterBuilder()).build(),
                        "<item-parameter type='number' name='Длина' value='20' unit='м'/>"
                },
                {
                        (new ItemParameterBuilder())
                                .withUnits(Arrays.asList((new UnitValueBuilder()).build()))
                                .withName("Размер")
                                .withSubType("size")
                                .build(),
                        "<item-parameter type='number' sub-type='size' name='Размер' value='20' unit='м'>" +
                                "<units>" +
                                "<unit-value values='1,2,3' shop-values='1,2' unit-id='RU' default-unit='true'/>" +
                                "</units>" +
                                "</item-parameter>"
                },
                {
                        (new ItemParameterBuilder())
                                .withUnits(Arrays.asList(
                                        (new UnitValueBuilder()).build(),
                                        (new UnitValueBuilder()).withDefaultUnit(false).withUnitId("EU").build()
                                ))
                                .withName("Размер")
                                .withSubType("size")
                                .build(),
                        "<item-parameter type='number' sub-type='size' name='Размер' value='20' unit='м'>" +
                                "<units>" +
                                "<unit-value values='1,2,3' shop-values='1,2' unit-id='RU' default-unit='true'/>" +
                                "<unit-value values='1,2,3' shop-values='1,2' unit-id='EU' default-unit='false'/>" +
                                "</units>" +
                                "</item-parameter>"
                },
                {
                        (new ItemParameterBuilder())
                                .withUnits(Arrays.asList(
                                        (new UnitValueBuilder()).build(),
                                        (new UnitValueBuilder()).withValues(Arrays.asList()).build(),
                                        (new UnitValueBuilder()).withShopValues(Arrays.asList()).build(),
                                        (new UnitValueBuilder()).withValues(null).withShopValues(null).build()
                                ))
                                .withName("Размер")
                                .withSubType("size")
                                .build(),
                        "<item-parameter type='number' sub-type='size' name='Размер' value='20' unit='м'>" +
                                "<units>" +
                                "<unit-value values='1,2,3' shop-values='1,2' unit-id='RU' default-unit='true'/>" +
                                "<unit-value values='' shop-values='1,2' unit-id='RU' default-unit='true'/>" +
                                "<unit-value values='1,2,3' shop-values='' unit-id='RU' default-unit='true'/>" +
                                "<unit-value unit-id='RU' default-unit='true'/>" +
                                "</units>" +
                                "</item-parameter>"
                }
        }).stream().map(Arguments::of);
    }

    @BeforeEach
    public void setUp() {
        serializer.setUnitValueXmlSerializer(new UnitValueXmlSerializer());
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testUnitValueXmlSerializer(ItemParameter itemParameter, String expectedXml) throws Exception {
        String actual = serialize(serializer, itemParameter);
        assertThat(
                actual,
                is(sameXmlAs(expectedXml))
        );
    }
}
