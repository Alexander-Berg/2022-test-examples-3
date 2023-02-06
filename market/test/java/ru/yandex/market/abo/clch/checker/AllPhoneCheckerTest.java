package ru.yandex.market.abo.clch.checker;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.clch.model.PhoneParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author kukabara
 */
class AllPhoneCheckerTest extends EmptyTest {

    @Autowired
    private Checker allPhoneChecker;

    @Test
    void test() {
        allPhoneChecker.configure(new CheckerDescriptor(17, "allPhone"));

        List<Long> shops = Arrays.asList(19057L, 130687L);

        for (int i = 0; i < shops.size() - 1; ++i) {
            for (int j = i + 1; j < shops.size(); ++j) {
                if (!Objects.equals(shops.get(i), shops.get(j))) {
                    System.out.println(allPhoneChecker.checkShops(shops.get(i), shops.get(j)));
                }
            }
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testPhones() {
        assertEquals("9641235990", PhoneParser.parseSingle("1234)8964()1235990").toString());
        assertEquals("9857669685", PhoneParser.parseSingle("985-766-96-85 в").toString());
        assertEquals("9857669685", PhoneParser.parseSingle("985-766-96-85 в").toString());
        assertEquals("9857669685", PhoneParser.parseSingle("8(985)-766-96-85 в").toString());
        assertEquals("9857669685", PhoneParser.parseSingle("8-985-766-96-85 в").toString());
        assertEquals("9857669685", PhoneParser.parseSingle("8 - 985 - 766 - 96 - 85 в").toString());
        assertEquals("9647008281", PhoneParser.parseSingle("8 964 700-82-81").toString());
        // не считаем кодом города, т.к. начинается на 9ку
        assertEquals("9647008281", PhoneParser.parseSingle("8 (964) 700-82-81").toString());
        // не считаем кодом города, т.к. 800
        assertEquals("8002509266", PhoneParser.parseSingle("8 (800) 250 92 66").toString());
        // не считаем кодом города, т.к. больше 4ех цифр
        assertEquals("12345509266", PhoneParser.parseSingle("8 (12345) 50 92 66").toString());
        // код города - 124
        assertEquals("(124)7008281", PhoneParser.parseSingle("8(124)7008281").toString());
        assertEquals("1247008281", PhoneParser.parseSingle("8(124)7008281").toDigits());
        assertEquals("1247008281", PhoneParser.parseSingle("8(124)700-82-81").toDigits());
        assertEquals("1247008281", PhoneParser.parseSingle("+7 (124) 700 82 81").toDigits());
    }
}
