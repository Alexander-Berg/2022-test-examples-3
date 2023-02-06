package ru.yandex.market.shared.fields.types;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.robot.shared.fields.types.NumberFieldType;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"/>
 * @date 10.10.11
 */
public class NumberFieldTypeTest extends Assert {

    private final NumberFieldType numberFieldType = new NumberFieldType();
    private final NumberFieldType fixedDelimiterNumberFieldType = new NumberFieldType(2);
    private final NumberFieldType priceFieldType = new NumberFieldType(2);


    @Test
    public void testNumberParse() throws Exception {
        assertEquals(2, parseNumber(" 2 "), 0);
        assertEquals(3082.8, parseNumber("3082.8"), 0);
        assertEquals(308.280, parseNumber("308.280"), 0);
    }

    @Test
    public void testPriceParse() throws Exception {
        assertEquals(5, parsePrice("Отзыв: 5"), 0);
        assertEquals(308280, parsePrice("308.280 TL"), 0);
        assertEquals(30280, parsePrice("30 280"), 0);
        assertEquals(100500.42, parsePrice("100500,42 TL, with KDV"), 0);
        assertEquals(101.00, parsePrice("101,00 TL"), 0);
        assertEquals(100500, parsePrice("100500 TL"), 0);
        assertEquals(100500, parsePrice("100500 TL, with KDV"), 0);
        assertEquals(101.00, parsePrice("101, 00 TL"), 0);
        assertEquals(42210.77, parsePrice("42. 210, 77"), 0);
        assertEquals(42210.77, parsePrice("42, 210. 77"), 0);
        assertEquals(2470.05, parsePrice("2,470.05 TL"), 0);
        assertEquals(101.00, parsePrice("101, 00 TL"), 0);
        assertEquals(308.12, parsePrice("308.12 TL"), 0);
        assertEquals(308.1, parsePrice("308.1 TL"), 0);
        assertEquals(100500.4, parsePrice("100 500.4 TL"), 0);
        assertEquals(435.23, parsePrice("435 23 TL KDV dahil"), 0);
        assertEquals(1276, parsePrice("1.276 TL"), 0);
        assertEquals(613, parsePrice("613 TL"), 0);
        assertEquals(2, parsePrice(" 2 "), 0);
        assertEquals(2499, parsePrice("2 499 руб."), 0);
        assertEquals(34.0005, parseFixedDelimiterPrice("34,0005", ","), 0);
    }

    private double parseFixedDelimiterPrice(String string, String delimiter) {
        Map<String, String> params = new HashMap<>();
        params.put("fractionalPartDelimiter", delimiter);
        return (Double) numberFieldType.parse(string, null, params);
    }

    private double parseNumber(String string) {
        return (Double) numberFieldType.parse(string);
    }

    private double parsePrice(String string) {
        return (Double) priceFieldType.parse(string, null);
    }


}
