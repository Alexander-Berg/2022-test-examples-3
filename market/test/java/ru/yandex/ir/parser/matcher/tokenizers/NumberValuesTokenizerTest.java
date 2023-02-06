package ru.yandex.ir.parser.matcher.tokenizers;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class NumberValuesTokenizerTest {

    @Test
    public void testTokenizing() {
        check(null, new String[]{}, new int[]{});
        check("", new String[]{}, new int[]{});
        check(" ", new String[]{}, new int[]{});
        check("  ", new String[]{}, new int[]{});
        check("a", new String[]{"a"}, new int[]{0});
        check(" a", new String[]{"a"}, new int[]{1});
        check("a ", new String[]{"a"}, new int[]{0});
        check("a b", new String[]{"a", "b"}, new int[]{0, 2});
        check("a  b", new String[]{"a", "b"}, new int[]{0, 3});
        check("A B", new String[]{"a", "b"}, new int[]{0, 2});
        check("aA bB", new String[]{"aa", "bb"}, new int[]{0, 3});
        check("текст с упоминанием 1000", new String[]{"tekct", "c", "yпomиhahиem", "1000"}, new int[]{0, 6, 8, 20});
        check(
            "дробные числа: 2.34, 3.35, 2/3",
            new String[]{"дpoбhыe", "чиcлa", "2.34", "3.35", "2", "/", "3"},
            new int[]{0, 8, 15, 21, 27, 28, 29}
        );
        check(".", new String[]{"."}, new int[]{0});
        check(",23 с запятой до:", new String[]{"23", "c", "зaпяtoй", "дo"}, new int[]{1, 4, 6, 14});
        check("с запятой до: ,23", new String[]{"c", "зaпяtoй", "дo", "23"}, new int[]{0, 2, 10, 15});
        check(".23 с точкой до:", new String[]{".23", "c", "toчkoй", "дo"}, new int[]{0, 4, 6, 13});
        check("с точкой до: .23", new String[]{"c", "toчkoй", "дo", ".23"}, new int[]{0, 2, 9, 13});
        check(
            "с точкой и запятой до: ,23 .45 67",
            new String[]{"c", "toчkoй", "и", "зaпяtoй", "дo", "23", ".45", "67"},
            new int[]{0, 2, 9, 11, 19, 24, 27, 31}
        );
        check("с запятой после: 23,", new String[]{"c", "зaпяtoй", "пocлe", "23"}, new int[]{0, 2, 10, 17});
        check("с запятой после: 23, 45", new String[]{"c", "зaпяtoй", "пocлe", "23", "45"}, new int[]{0, 2, 10, 17, 21});
        check("с точкой после: 23.", new String[]{"c", "toчkoй", "пocлe", "23", "."}, new int[]{0, 2, 9, 16, 18});
        check("с точкой после: 23. 45", new String[]{"c", "toчkoй", "пocлe", "23", ".", "45"}, new int[]{0, 2, 9, 16, 18, 20});
        check(
            "с точкой и запятой после: 23, 45. 67",
            new String[]{"c", "toчkoй", "и", "зaпяtoй", "пocлe", "23", "45", ".", "67"},
            new int[]{0, 2, 9, 11, 19, 26, 30, 32, 34}
        );
        check(
            "смешанная запись: 1, 2, 3,4, 5,6 7.8,9.10,11,12,13.14.15",
            new String[]{"cmeшahhaя", "зaпиcь", "1", "2", "3", "4", "5.6", "7.8", "9.10", "11", "12", "13.14.15"},
            new int[]{0, 10, 18, 21, 24, 26, 29, 33, 37, 42, 45, 48}
        );
        check(
            "Gazpromneft. Diesel .Extra 15W-40 боч.205л (.181 кг)",
            new String[]{"gazpromneft", ".", "diesel", ".", "extra", "15", "w", "-", "40", "бoч", ".", "205", "л",
                ".181", "kг"},
            new int[]{0, 11, 13, 20, 21, 27, 29, 30, 31, 34, 37, 38, 41, 44, 49}
        );
    }

    private void check(String rawString, String[] tokens, int[] positions) {
        TokenSequence tokenized = NumberValuesTokenizer.tokenize(rawString);
        TokenSequence expected = buildPair(tokens, positions);
        assertEquals(tokenized.getTokens().size(), tokenized.getPositions().size());
        assertEquals(expected.getTokens(), tokenized.getTokens());
        assertEquals(expected.getPositions(), tokenized.getPositions());
    }

    private TokenSequence buildPair(String[] tokens, int[] positions) {
        return new TokenSequence(Arrays.asList(tokens), new IntArrayList(positions));
    }
}
