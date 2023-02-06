package ru.yandex.market.pers.notify.util;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author zloddey
 */
class WordFormUtilTest {
    List<String> CAT_WORDS = Arrays.asList("кошка", "кошки", "кошек");

    @ParameterizedTest
    @CsvSource(value = {
            "1,кошка", "2,кошки", "3,кошки", "4,кошки", "5,кошек",
            "6,кошек", "7,кошек", "8,кошек", "9,кошек", "10,кошек",
            "11,кошек", "12,кошек", "13,кошек", "14,кошек", "15,кошек",
            "16,кошек", "17,кошек", "18,кошек", "19,кошек", "20,кошек",
            "21,кошка", "22,кошки", "23,кошки", "24,кошки", "25,кошек",
            "26,кошек", "27,кошек", "28,кошек", "29,кошек",
            "0,кошек", "1001,кошка", "9994,кошки",
            // Странный кейс, но зачем-то оказался нужен персам.
            // Скорее всего, они округляют большие числа.
            "10001,кошек", "10002,кошек", "10003,кошек"
    })
    public void testWords(String strAmount, String expected) {
        int amount = Integer.parseInt(strAmount);
        String actual = WordFormUtil.prepareFormForWord(amount, CAT_WORDS);
        assertEquals(expected, actual);
    }
}
