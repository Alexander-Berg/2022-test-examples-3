package ru.yandex.market.books.diff.util;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static ru.yandex.market.books.diff.util.TitlesComparator.*;

/**
 * todo описать предназначение
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class TitlesComparatorTest extends TestCase {
//	public void testAreEqualTitles() {
//		assertTrue(areEqualTitles("AaBbCc", "aabbcc"));
//		assertTrue(TitlesComparator.areEqualTitles("AaBbCc ", " aabbcc"));
//		assertTrue(TitlesComparator.areEqualTitles("Aa\nBb Cc ", " aa bb\tcc"));
//
//		assertTrue(TitlesComparator.areEqualTitles("Энциклопедия. Том 1", "Энциклопедия, Том 1"));
//		assertTrue(TitlesComparator.areEqualTitles("Энциклопедия. Том 1", "Энциклопедия. Том 1"));
//		assertTrue(TitlesComparator.areEqualTitles("Энциклопедия. Том 1", "Энциклопедия. том 1"));
//		assertTrue(TitlesComparator.areEqualTitles("Энциклопедия. Том 1", "энциклопедия. Том 1"));
//		assertTrue(TitlesComparator.areEqualTitles("Энциклопедия. Том 1", "энциклопедия. том 1"));
//
//		assertTrue(TitlesComparator.areEqualTitles("авеик", "abeuk"));
//		assertTrue(TitlesComparator.areEqualTitles("кмнорстух", "kmhopctyx"));
//		assertTrue(TitlesComparator.areEqualTitles("авеикмнорстух", "abeukmhopctyx"));
//		assertTrue(TitlesComparator.areEqualTitles("Энциклопедия. Том XCII", "Энциклопедия. Том ХСII"));
//
//		assertFalse(TitlesComparator.areEqualTitles("Энциклопедия. Том 1", "Энциклопедия. Том 2"));
//		assertFalse(TitlesComparator.areEqualTitles("Энциклопедия. Том I", "Энциклопедия. Том II"));
//	}

//	public void testIsPrefix() {
//		assertTrue(isPrefix("AaBbCc", "aabbcc"));
//		assertTrue(TitlesComparator.isPrefix("Хоббит", "Хоббит, или Туда и Обратно"));
//		assertTrue(TitlesComparator.isPrefix(
//				"Хоббит",
//				"Хоббит, или Туда и Обратно. Приключения Тома Бомбадила и другие истории"
//		));
//		assertTrue(TitlesComparator.isPrefix(
//				"Хоббит, или Туда и Обратно",
//				"Хоббит, или Туда и Обратно. Приключения Тома Бомбадила и другие истории"
//		));
//
//		assertFalse(TitlesComparator.isPrefix(
//				"Хоббит, или Туда и Обратно",
//				"Хоббит. Приключения Тома Бомбадила и другие истории"
//		));
//		assertFalse(TitlesComparator.isPrefix("Энциклопедия. Том 1", "Энциклопедия. Том 2"));
//	}

    public void testContainsCommonPrefix() {
        assertTrue(containsCommonPrefix(Arrays.asList(
                "", "aaa", "bbb"
        )));
        assertTrue(containsCommonPrefix(Arrays.asList(
                "a", "aaa", "aaaa"
        )));
        assertTrue(containsCommonPrefix(Arrays.asList(
                "Хоббит, или Туда и Обратно",
                "Хоббит",
                "Хоббит. Приключения Тома Бомбадила и другие истории"
        )));

        assertFalse(containsCommonPrefix(Arrays.asList(
                "a", "aaa", "bbb"
        )));
        assertFalse(containsCommonPrefix(Arrays.asList(
                "A", "aaa", "aaaa"
        )));
    }

    public void testDiffersOnlyInVolumeNumber() {
        assertTrue(areNormalizedDiffersOnlyInVolumeNumber("Том 1", "Том 2", "Том 3"));
        assertTrue(areNormalizedDiffersOnlyInVolumeNumber("Том 11", "Том 12", "Том 13"));

        assertTrue(areNormalizedDiffersOnlyInVolumeNumber("Том i", "Том v", "Том x"));
        assertTrue(areNormalizedDiffersOnlyInVolumeNumber("Том iv", "Том ix", "Том vi"));

        assertTrue(areNormalizedDiffersOnlyInVolumeNumber("Том 1 Начало", "Том 2 Продолжение", "Том 3 Финал"));
        assertTrue(areNormalizedDiffersOnlyInVolumeNumber("Том i Начало", "Том ii Продолжение", "Том iii Финал"));

        assertTrue(areNormalizedDiffersOnlyInVolumeNumber("Том 1", "Том 10", "Том 11"));
        assertTrue(areNormalizedDiffersOnlyInVolumeNumber("Том i", "Том ii", "Том iii"));

        assertTrue(areNormalizedDiffersOnlyInVolumeNumber("Том 1", "Том 1 Начало", "Том 10 Продожение"));

        assertTrue(areNormalizedDiffersOnlyInVolumeNumber("Том первый", "Том второй", "Том третий"));

        assertFalse(areNormalizedDiffersOnlyInVolumeNumber("Том 1", "Том 1 Начало"));
    }

    private boolean areNormalizedDiffersOnlyInVolumeNumber(String... strings) {
        List<String> titles = Arrays.asList(strings);
        Collection<String> normalizedTitles = new HashSet<String>();
        for (String title : titles) {
            normalizedTitles.add(normalize(title));
        }
        return (null != getVolumesMapByTitles(normalizedTitles));
    }

    public void testNormalize() {
        assertEquals(normalize(""), "");
        assertEquals(normalize(" "), "");
        assertEquals(normalize("a"), "а");
        assertEquals(normalize(" x  y z "), "10 у z");
        assertEquals(normalize("авеикмнорстух"), "авеикмнорстух");
        assertEquals(normalize(" a "), "а");
        assertEquals(normalize(" T34 "), "т 34");
        assertEquals(normalize(" T-34 "), "т 34");
        assertEquals(normalize("а1б2в3г4е5"), "а 1 б 2 в 3 г 4 е 5");
        assertEquals(normalize("'а1б2в3г4е5'"), "а 1 б 2 в 3 г 4 е 5");
    }

    public void testGetValueOfRoman() {
        assertEquals(Numberer.getValueOfRoman("i"), 1);
        assertEquals(Numberer.getValueOfRoman("v"), 5);
        assertEquals(Numberer.getValueOfRoman("x"), 10);
        assertEquals(Numberer.getValueOfRoman("l"), 50);
        assertEquals(Numberer.getValueOfRoman("c"), 100);
        assertEquals(Numberer.getValueOfRoman("d"), 500);
        assertEquals(Numberer.getValueOfRoman("m"), 1000);

        assertEquals(Numberer.getValueOfRoman("ii"), 2);
        assertEquals(Numberer.getValueOfRoman("xx"), 20);
        assertEquals(Numberer.getValueOfRoman("cc"), 200);
        assertEquals(Numberer.getValueOfRoman("mm"), 2000);

        assertEquals(Numberer.getValueOfRoman("iii"), 3);
        assertEquals(Numberer.getValueOfRoman("xxx"), 30);
        assertEquals(Numberer.getValueOfRoman("ccc"), 300);
        assertEquals(Numberer.getValueOfRoman("mmm"), 3000);

        assertEquals(Numberer.getValueOfRoman("iv"), 4);
        assertEquals(Numberer.getValueOfRoman("ix"), 9);
        assertEquals(Numberer.getValueOfRoman("xc"), 90);
        assertEquals(Numberer.getValueOfRoman("cm"), 900);

        assertEquals(Numberer.getValueOfRoman("i"), 1);
        assertEquals(Numberer.getValueOfRoman("ii"), 2);
        assertEquals(Numberer.getValueOfRoman("iii"), 3);
        assertEquals(Numberer.getValueOfRoman("iiii"), 4);
        assertEquals(Numberer.getValueOfRoman("iv"), 4);
        assertEquals(Numberer.getValueOfRoman("v"), 5);
        assertEquals(Numberer.getValueOfRoman("vi"), 6);
        assertEquals(Numberer.getValueOfRoman("vii"), 7);
        assertEquals(Numberer.getValueOfRoman("viii"), 8);
        assertEquals(Numberer.getValueOfRoman("ix"), 9);
        assertEquals(Numberer.getValueOfRoman("x"), 10);
        assertEquals(Numberer.getValueOfRoman("xi"), 11);
        assertEquals(Numberer.getValueOfRoman("xii"), 12);
        assertEquals(Numberer.getValueOfRoman("xiii"), 13);
        assertEquals(Numberer.getValueOfRoman("xiv"), 14);
        assertEquals(Numberer.getValueOfRoman("xv"), 15);
        assertEquals(Numberer.getValueOfRoman("xvi"), 16);
        assertEquals(Numberer.getValueOfRoman("xvii"), 17);
        assertEquals(Numberer.getValueOfRoman("xviii"), 18);
        assertEquals(Numberer.getValueOfRoman("xix"), 19);
        assertEquals(Numberer.getValueOfRoman("xx"), 20);
        assertEquals(Numberer.getValueOfRoman("xxi"), 21);
        assertEquals(Numberer.getValueOfRoman("xxii"), 22);
        assertEquals(Numberer.getValueOfRoman("xxiii"), 23);
        assertEquals(Numberer.getValueOfRoman("xxiiii"), 24);
        assertEquals(Numberer.getValueOfRoman("xxiv"), 24);
        assertEquals(Numberer.getValueOfRoman("xxv"), 25);
        assertEquals(Numberer.getValueOfRoman("xxvi"), 26);
        assertEquals(Numberer.getValueOfRoman("xxvii"), 27);
        assertEquals(Numberer.getValueOfRoman("xxviii"), 28);
        assertEquals(Numberer.getValueOfRoman("xxix"), 29);
    }

    public void testReplaceRomanNumbersWithArabic() {
        checkRomanNumbersReplacement("", "");
        checkRomanNumbersReplacement("a b", "a b");
        checkRomanNumbersReplacement("a b c", "a b 100");
        checkRomanNumbersReplacement("a b 20", "a b 20");
        checkRomanNumbersReplacement("Энциклопедия в 20 томах. Том 16", "Энциклопед в 20 том. Том 16");
        checkRomanNumbersReplacement("Энциклопедия в XX томах. Том XVI", "Энциклопед в 20 том. Том 16");
    }

    private void checkRomanNumbersReplacement(String srcStr, String expectedStr) {
        assertEquals(normalize(expectedStr), forceNormalizeNumbers(normalize(srcStr)));
    }

    public void testDiffersWithInfixes() {
        assertTrue(areNormalizedDiffersWithInfixes("basestring", "baseINFIXstring"));
        assertTrue(areNormalizedDiffersWithInfixes("basestring", "basestring"));
        assertTrue(areNormalizedDiffersWithInfixes("basestring", "basestringbasestring"));
        assertTrue(areNormalizedDiffersWithInfixes("basestring", "basestringbasestring"));
        assertTrue(areNormalizedDiffersWithInfixes(
                "basestring", "baseINFIXstring", "basestring", "basestringbasestring", "basestringbasestring"
        ));
        assertTrue(areNormalizedDiffersWithInfixes(
                "basestring", "basestring", "basestring", "baseINFIXstring"
        ));

        assertTrue(areNormalizedDiffersWithInfixes("", "..FIX"));
        assertTrue(areNormalizedDiffersWithInfixes("str", "strSUFFIX"));
        assertTrue(areNormalizedDiffersWithInfixes("str", "PREFIXstr"));
        assertTrue(areNormalizedDiffersWithInfixes("str", "PREFIXstr", "stINFIXr", "strSUFFIX"));

        assertFalse(areNormalizedDiffersWithInfixes("baseAAAstring", "baseBBstring"));
    }

    private boolean areNormalizedDiffersWithInfixes(String... strings) {
        List<String> titles = Arrays.asList(strings);
        Collection<String> normalizedStrings = new HashSet<String>();
        for (String title : titles) {
            normalizedStrings.add(normalize(title));
        }
        return differsWithInfixes(normalizedStrings);
    }

    public void testFindCommonPrefixLength() {
        assertEquals(0, findCommonPrefixLength("A", "B"));
        assertEquals(1, findCommonPrefixLength("aA", "aB"));
        assertEquals(2, findCommonPrefixLength("abA", "abB"));

        assertEquals(0, findCommonPrefixLength("ABC", ""));
    }

    public void testFindCommonSuffixLength() {
        assertEquals(0, findCommonSuffixLength("A", "B"));
        assertEquals(1, findCommonSuffixLength("Aa", "Ba"));
        assertEquals(2, findCommonSuffixLength("Aaa", "Baa"));

        assertEquals(0, findCommonSuffixLength("ABC", ""));
    }

    public void testHaveCommonTokenStarts() {
        assertTrue(areNormalizedHaveCommonTokenStarts(" a b c ", "a b c"));
        assertTrue(areNormalizedHaveCommonTokenStarts(" a b f ", "aa bb ff"));

        assertFalse(areNormalizedHaveCommonTokenStarts(" a b c ", "a b c d"));
        assertFalse(areNormalizedHaveCommonTokenStarts(" a b c ", "a b c d e f"));
        assertFalse(areNormalizedHaveCommonTokenStarts("a b c d", " a b c "));
        assertFalse(areNormalizedHaveCommonTokenStarts("a b c d e f", " a b c "));
        assertFalse(areNormalizedHaveCommonTokenStarts("12 23 34", "1 2 3 4 5"));
        assertFalse(areNormalizedHaveCommonTokenStarts("1 2 3", "11 22 333 444 555"));
        assertFalse(areNormalizedHaveCommonTokenStarts("111111111 222222222 33333333", "1 2 3 4 5"));
        assertFalse(areNormalizedHaveCommonTokenStarts("aa bb cc dd", " a b c"));
        assertFalse(areNormalizedHaveCommonTokenStarts(" a b c ", "aa bb cc dd"));
    }

    private boolean areNormalizedHaveCommonTokenStarts(String... strings) {
        List<String> titles = Arrays.asList(strings);
        Collection<String> normalizedStrings = new HashSet<String>();
        for (String title : titles) {
            normalizedStrings.add(normalize(title));
        }
        return haveCommonTokenStarts(normalizedStrings);
    }

    public void testExtractWord() {
        assertEquals("abc", extractWord("abc", 0));
        assertEquals("bc", extractWord("abc", 1));
        assertEquals("c", extractWord("abc", 2));
        assertEquals("", extractWord("abc", 3));

        assertEquals("abcABC", extractWord("abcABC", 0));
        assertEquals("bcABC", extractWord("abcABC", 1));
        assertEquals("cABC", extractWord("abcABC", 2));
        assertEquals("ABC", extractWord("abcABC", 3));

        assertEquals("abc", extractWord("abc-", 0));
        assertEquals("bc", extractWord("abc-", 1));
        assertEquals("c", extractWord("abc-", 2));
        assertEquals("", extractWord("abc-", 3));
        assertEquals("", extractWord("abc-", 4));

        assertEquals("abc", extractWord("abc1", 0));
        assertEquals("bc", extractWord("abc1", 1));
        assertEquals("c", extractWord("abc1", 2));
        assertEquals("", extractWord("abc1", 3));
        assertEquals("", extractWord("abc1", 4));
    }

    public void testExtractNumber() {
        assertEquals("", extractNumber("abc", 0));

        assertEquals("123", extractNumber("123ABC", 0));
        assertEquals("23", extractNumber("123ABC", 1));
        assertEquals("3", extractNumber("123ABC", 2));
        assertEquals("", extractNumber("123ABC", 3));

        assertEquals("123", extractNumber("123-", 0));
        assertEquals("23", extractNumber("123-", 1));
        assertEquals("3", extractNumber("123-", 2));
        assertEquals("", extractNumber("123-", 3));
        assertEquals("", extractNumber("123-", 4));
    }

    public void testWordSequence() {
        List<String> words;
        words = getNumerizedAndDecodedWords("Несколько слов (с цифрами и знаками препинания): 1234, 567, 89");
        assertEquals(10, words.size());
        assertEquals("несколько", words.get(0));
        assertEquals("слов", words.get(1));
        assertEquals("100", words.get(2));        // латинская 'C' - это '100' римскими цифрами
        assertEquals("цифрами", words.get(3));
        assertEquals("и", words.get(4));
        assertEquals("знаками", words.get(5));
        assertEquals("препинания", words.get(6));
        assertEquals("1234", words.get(7));
        assertEquals("567", words.get(8));
        assertEquals("89", words.get(9));

        words = getNumerizedAndDecodedWords("Литература XIХ beka");
        assertEquals(3, words.size());
        assertEquals("литература", words.get(0));
        assertEquals("19", words.get(1));
        assertEquals("века", words.get(2));

        words = getNumerizedAndDecodedWords("Windоws2000. Cпpaвочник в ХVI tomax");
        assertEquals(6, words.size());
        assertEquals("windows", words.get(0));
        assertEquals("2000", words.get(1));
        assertEquals("справочник", words.get(2));
        assertEquals("в", words.get(3));
        assertEquals("16", words.get(4));
        assertEquals("томах", words.get(5));
    }

    public void testDecode() {
        List<String> words = Arrays.asList("heckолько", "рycскux", "слоb", "русскumu", "и", "латинсkumu", "бykbaми");
        decode(words);
        assertEquals(
                words,
                Arrays.asList("несколько", "русских", "слов", "русскими", "и", "латинскими", "буквами")
        );

        words = Arrays.asList("sеveral", "еnglisн", "wоrds", "in", "lаtin", "аnd", "суrilliс");
        decode(words);
        assertEquals(
                words,
                Arrays.asList("several", "english", "words", "in", "latin", "and", "cyrillic")
        );

        words = Arrays.asList("pycckue", "u", "ahглийckue", "words", "in", "singlе", "рнrаse");
        decode(words);
        assertEquals(
                words,
                Arrays.asList("русские", "и", "английские", "words", "in", "single", "phrase")
        );
    }

    public void testNumerize() {
        List<String> words = Arrays.asList(
                "xix", "сх", "lх", "LX", "пятнадцать", "шести", "раз", "сто", "ххх", "xXx", "нечисло", "раз и два"
        );
        numerize(words);
        assertEquals(
                words,
                Arrays.asList("19", "110", "60", "LX", "15", "6", "1", "100", "30", "xXx", "нечисло", "раз и два")
        );
    }

    public void testStem() {
        List<String> words = Arrays.asList(
                "три", "веселых", "рассказа", "про", "кошку", "и", "собак",
                "tree", "funny", "stories", "about", "cat", "and", "dogs"
        );
        stem(words);
        assertEquals(
                words,
                Arrays.asList(
                        "три", "весел", "рассказ", "про", "кошк", "и", "собак",
                        "tree", "funni", "stori", "about", "cat", "and", "dog"
                )
        );
    }

    public void testYeYo() {
        List<String> words = getSimpleWords("ЕеЁё");
        assertEquals("ееёё", words.get(0));

        words = getNumerizedAndDecodedWords("ЕеЁё");
        assertEquals("ееее", words.get(0));
    }

    public void testTokensCount() {
        assertEquals(0, TitlesComparator.getTokensCount(""));
        assertEquals(0, TitlesComparator.getTokensCount(" "));
        assertEquals(0, TitlesComparator.getTokensCount("-"));
        assertEquals(1, TitlesComparator.getTokensCount("1"));
        assertEquals(1, TitlesComparator.getTokensCount("1 "));
        assertEquals(1, TitlesComparator.getTokensCount("1-"));
        assertEquals(1, TitlesComparator.getTokensCount(" 1"));
        assertEquals(1, TitlesComparator.getTokensCount("-1"));
        assertEquals(2, TitlesComparator.getTokensCount("1a"));
        assertEquals(2, TitlesComparator.getTokensCount("1 a"));
        assertEquals(2, TitlesComparator.getTokensCount("1-a"));
        assertEquals(3, TitlesComparator.getTokensCount("1a2"));
        assertEquals(3, TitlesComparator.getTokensCount("1 a 2"));
        assertEquals(3, TitlesComparator.getTokensCount("1-a-2"));
        assertEquals(3, TitlesComparator.getTokensCount("1a2 "));
        assertEquals(3, TitlesComparator.getTokensCount("1 a 2 "));
        assertEquals(3, TitlesComparator.getTokensCount("1-a-2 "));
        assertEquals(3, TitlesComparator.getTokensCount(" 1a2"));
        assertEquals(3, TitlesComparator.getTokensCount(" 1 a 2"));
        assertEquals(3, TitlesComparator.getTokensCount(" 1-a-2"));
    }

    public void testTokenBorders() {
        String cleanStr = "Некая строка без знаков препинания";
        assertEquals(34, cleanStr.length());
        assertEquals(5, TitlesComparator.getTokensCount(cleanStr));
        assertEquals(0, TitlesComparator.getFirstTokensEnd(cleanStr, 0));
        assertEquals(5, TitlesComparator.getFirstTokensEnd(cleanStr, 1));
        assertEquals(12, TitlesComparator.getFirstTokensEnd(cleanStr, 2));
        assertEquals(16, TitlesComparator.getFirstTokensEnd(cleanStr, 3));
        assertEquals(23, TitlesComparator.getFirstTokensEnd(cleanStr, 4));
        assertEquals(34, TitlesComparator.getFirstTokensEnd(cleanStr, 5));
        assertEquals(-1, TitlesComparator.getFirstTokensEnd(cleanStr, 6));
        assertEquals(34, TitlesComparator.getLastTokensStart(cleanStr, 0));
        assertEquals(24, TitlesComparator.getLastTokensStart(cleanStr, 1));
        assertEquals(17, TitlesComparator.getLastTokensStart(cleanStr, 2));
        assertEquals(13, TitlesComparator.getLastTokensStart(cleanStr, 3));
        assertEquals(6, TitlesComparator.getLastTokensStart(cleanStr, 4));
        assertEquals(0, TitlesComparator.getLastTokensStart(cleanStr, 5));
        assertEquals(-1, TitlesComparator.getLastTokensStart(cleanStr, 6));

        String strInBraces = "(Некая строка в скобках)";
        assertEquals(24, strInBraces.length());
        assertEquals(4, TitlesComparator.getTokensCount(strInBraces));
        assertEquals(0, TitlesComparator.getFirstTokensEnd(strInBraces, 0));
        assertEquals(6, TitlesComparator.getFirstTokensEnd(strInBraces, 1));
        assertEquals(13, TitlesComparator.getFirstTokensEnd(strInBraces, 2));
        assertEquals(15, TitlesComparator.getFirstTokensEnd(strInBraces, 3));
        assertEquals(23, TitlesComparator.getFirstTokensEnd(strInBraces, 4));
        assertEquals(-1, TitlesComparator.getFirstTokensEnd(strInBraces, 5));
        assertEquals(16, TitlesComparator.getLastTokensStart(strInBraces, 1));
        assertEquals(14, TitlesComparator.getLastTokensStart(strInBraces, 2));
        assertEquals(7, TitlesComparator.getLastTokensStart(strInBraces, 3));
        assertEquals(1, TitlesComparator.getLastTokensStart(strInBraces, 4));
        assertEquals(-1, TitlesComparator.getLastTokensStart(strInBraces, 5));

        String longStr = "Некий длинный заголовок (со скобками) и словами \"в кавычках\".";
        assertEquals(9, TitlesComparator.getTokensCount(longStr));
        assertEquals(9, TitlesComparator.getTokensCount(TitlesComparator.normalize(longStr)));
        assertEquals(0, TitlesComparator.getFirstTokensEnd(longStr, 0));
        assertEquals(61, TitlesComparator.getLastTokensStart(longStr, 0));
        assertEquals(longStr, longStr.substring(
                TitlesComparator.getFirstTokensEnd(longStr, 0), TitlesComparator.getLastTokensStart(longStr, 0)
        ));
        assertEquals("", longStr.substring(
                TitlesComparator.getFirstTokensEnd(longStr, 0), TitlesComparator.getLastTokensStart(longStr, 9)
        ));
        assertEquals("\".", longStr.substring(
                TitlesComparator.getFirstTokensEnd(longStr, 9), TitlesComparator.getLastTokensStart(longStr, 0)
        ));
        assertEquals("Некий ",
                longStr.substring(
                        TitlesComparator.getFirstTokensEnd(longStr, 0), TitlesComparator.getLastTokensStart(longStr, 8)
                )
        );
        assertEquals(" длинный заголовок (",
                longStr.substring(
                        TitlesComparator.getFirstTokensEnd(longStr, 1), TitlesComparator.getLastTokensStart(longStr, 6)
                )
        );
        assertEquals(" длинный заголовок (со скобками) ",
                longStr.substring(
                        TitlesComparator.getFirstTokensEnd(longStr, 1), TitlesComparator.getLastTokensStart(longStr, 4)
                )
        );
        assertEquals(" длинный заголовок (со скобками) и словами \"",
                longStr.substring(
                        TitlesComparator.getFirstTokensEnd(longStr, 1), TitlesComparator.getLastTokensStart(longStr, 2)
                )
        );
        assertEquals(" \"в кавычках\".",
                longStr.substring(
                        TitlesComparator.getFirstTokensEnd(longStr, 7), TitlesComparator.getLastTokensStart(longStr, 0)
                )
        );
    }
}
