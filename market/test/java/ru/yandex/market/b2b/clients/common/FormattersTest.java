package ru.yandex.market.b2b.clients.common;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FormattersTest {

    @Test
    public void testMoneyStr() {
        assertEquals("один рубль 10 копеек", Formatters.moneyStr(BigDecimal.valueOf(110, 2)));
        assertEquals("сто рублей 00 копеек", Formatters.moneyStr(BigDecimal.valueOf(100)));
        assertEquals("сто двадцать один рубль 03 копейки", Formatters.moneyStr(BigDecimal.valueOf(12103, 2)));
        assertEquals("двести шестьдесят два рубля 01 копейка", Formatters.moneyStr(BigDecimal.valueOf(26201, 2)));
        assertEquals("одна тысяча рублей 00 копеек", Formatters.moneyStr(BigDecimal.valueOf(100000, 2)));
        assertEquals("одна тысяча три рубля 02 копейки", Formatters.moneyStr(BigDecimal.valueOf(100302, 2)));
        assertEquals("тринадцать тысяч сорок семь рублей 11 копеек",
                Formatters.moneyStr(BigDecimal.valueOf(1304711, 2)));
        assertEquals("сто пятьдесят тысяч сто два рубля 99 копеек",
                Formatters.moneyStr(BigDecimal.valueOf(15010299, 2)));
        assertEquals("четыреста семь тысяч рублей 00 копеек", Formatters.moneyStr(BigDecimal.valueOf(40700000, 2)));
        assertEquals("два миллиона семьсот рублей 34 копейки", Formatters.moneyStr(BigDecimal.valueOf(200070034, 2)));
        assertEquals("две тысячи два рубля 13 копеек", Formatters.moneyStr(BigDecimal.valueOf(200213, 2)));
        assertEquals("пять тысяч триста двадцать один рубль 22 копейки",
                Formatters.moneyStr(BigDecimal.valueOf(532122, 2)));
        assertEquals("сто три миллиона одна тысяча рублей 00 копеек",
                Formatters.moneyStr(BigDecimal.valueOf(10300100000L, 2)));
        assertEquals("ноль рублей 01 копейка", Formatters.moneyStr(BigDecimal.valueOf(1, 2)));

    }
}
