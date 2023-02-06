package ru.yandex.market.checkout.pushapi.client.xml.order;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.yandex.market.checkout.checkouter.order.ItemParameter;
import ru.yandex.market.checkout.checkouter.order.ItemParameterBuilder;
import ru.yandex.market.checkout.checkouter.order.UnitValueBuilder;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author Mikhail Usachev <mailto:musachev@yandex-team.ru>
 * Date: 31/05/2017.
 */
public class ItemParameterXmlDeserializerTest {

    private ItemParameterXmlDeserializer itemParameterXmlDeserializer = new ItemParameterXmlDeserializer();
    private UnitValueXmlDeserializer unitValueXmlDeserializer = new UnitValueXmlDeserializer();

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(new Object[][]{
                {
                        (new ItemParameterBuilder()).build(),
                        "<item-parameter type='number' name='Длина' value='20' unit='м' />"
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
                                .withUnits(
                                        Arrays.asList(
                                                (new UnitValueBuilder()).build(),
                                                (new UnitValueBuilder()).withDefaultUnit(false).build(),
                                                (new UnitValueBuilder())
                                                        .withDefaultUnit(false)
                                                        .withUnitId("EU")
                                                        .withShopValues(null)
                                                        .build()
                                        )
                                )
                                .withName("Размер")
                                .withSubType("size")
                                .build(),
                        "<item-parameter type='number' sub-type='size' name='Размер' value='20' unit='м'>" +
                                "<units>" +
                                "<unit-value values='1,2,3' shop-values='1,2' unit-id='RU' default-unit='true'/>" +
                                "<unit-value values='1,2,3' shop-values='1,2' unit-id='RU' default-unit='false'/>" +
                                "<unit-value values='1,2,3' unit-id='EU' default-unit='false'/>" +
                                "</units>" +
                                "</item-parameter>"
                },
                {
                        (new ItemParameterBuilder())
                                .withUnits(
                                        Arrays.asList(
                                                (new UnitValueBuilder())
                                                        .withDefaultUnit(false)
                                                        .withUnitId("EU")
                                                        .withValues(Arrays.asList("AA", "A", "B", "C", "D"))
                                                        .withShopValues(null)
                                                        .build(),
                                                (new UnitValueBuilder())
                                                        .withDefaultUnit(false)
                                                        .withUnitId("INT")
                                                        .withValues(Arrays.asList("AA", "A", "B", "C", "D"))
                                                        .withShopValues(null)
                                                        .build(),
                                                (new UnitValueBuilder())
                                                        .withDefaultUnit(true)
                                                        .withUnitId("RU")
                                                        .withValues(Arrays.asList("AA", "A", "B", "C", "D"))
                                                        .withShopValues(null)
                                                        .build(),
                                                (new UnitValueBuilder())
                                                        .withDefaultUnit(false)
                                                        .withUnitId("UK")
                                                        .withValues(Arrays.asList("AA", "A", "B", "C", "D"))
                                                        .withShopValues(Arrays.asList("AA", "A", "B", "C", "D"))
                                                        .build()
                                        )
                                )
                                .withName("Чашка")
                                .withSubType("size")
                                .withType("enum")
                                .withUnit(null)
                                .withValue(null)
                                .withCode(null)
                                .build(),
                        "       <item-parameter type='enum' sub-type='size' name='Чашка'>" +
                                "           <units>" +
                                "               <unit-value values='AA,A,B,C,D' unit-id='EU' default-unit='false'/>" +
                                "               <unit-value values='AA,A,B,C,D' unit-id='INT' default-unit='false'/>" +
                                "               <unit-value values='AA,A,B,C,D' unit-id='RU' default-unit='true'/>" +
                                "               <unit-value values='AA,A,B,C,D' shop-values='AA,A,B,C,D' unit-id='UK' default-unit='false'/>" +
                                "           </units>" +
                                "       </item-parameter>"
                },

        }).stream().map(Arguments::of);
    }

    @BeforeEach
    public void setUp() {
        itemParameterXmlDeserializer.setUnitValueXmlDeserializer(unitValueXmlDeserializer);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testUnitValueXmlDeserializer(ItemParameter itemParameter, String xmlString) throws Exception {
        final ItemParameter actual = XmlTestUtil.deserialize(itemParameterXmlDeserializer, xmlString);
        Assertions.assertTrue(itemParameter.equals(actual));
    }

}
