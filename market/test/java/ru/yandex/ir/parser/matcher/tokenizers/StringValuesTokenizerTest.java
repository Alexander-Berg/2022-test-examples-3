package ru.yandex.ir.parser.matcher.tokenizers;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
@SuppressWarnings("checkstyle:magicnumber")
public class StringValuesTokenizerTest {

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
        check("1|a|b|2|3", new String[]{"1", "a", "b", "2", "3"}, new int[]{0, 2, 4, 6, 8});
        check("текст с упоминанием 1000", new String[]{"tekct", "c", "yпomиhahиem", "1000"}, new int[]{0, 6, 8, 20});
        check(
                "дробные числа: 2.34, 3.35, 2/3",
                new String[]{"дpoбhыe", "чиcлa", "2", "34", "3", "35", "2", "3"},
                new int[]{0, 8, 15, 17, 21, 23, 27, 29}
        );
        check(
                "смешанная запись: 1, 2, 3,4, 5,6 7.8,9.10,11,12,13.14.15",
                new String[]{"cmeшahhaя", "зaпиcь", "1", "2", "3", "4", "5", "6", "7", "8",
                        "9", "10", "11", "12", "13", "14", "15"},
                new int[]{0, 10, 18, 21, 24, 26, 29, 31, 33, 35, 37, 39, 42, 45, 48, 51, 54}
        );
    }

    private void check(String rawString, String[] tokens, int[] positions) {
        TokenSequence tokenized = StringValuesTokenizer.tokenize(rawString);
        TokenSequence expected = buildPair(tokens, positions);
        assertEquals(tokenized.getTokens().size(), tokenized.getPositions().size());
        assertEquals(expected.getTokens(), tokenized.getTokens());
        assertEquals(expected.getPositions(), tokenized.getPositions());

        checkEqualityOfToSearchValueAndOriginalString(rawString);
    }

    private void checkEqualityOfToSearchValueAndOriginalString(String rawString) {
        TokenSequence tokenized = StringValuesTokenizer.tokenize(rawString);
        TokenSequence toSearchValueTokenized = StringValuesTokenizer.tokenize(tokenized.toSearchValue());
        // SearchValue has same tokens but may differ in positions
        assertEquals(toSearchValueTokenized.getTokens(), tokenized.getTokens());
        // positions will reduced to single separator between tokens
        IntList simpleSpaces = new IntArrayList();
        int c = 0;
        for (String token : toSearchValueTokenized.getTokens()) {
            simpleSpaces.add(c);
            c += token.length() + 1;
        }
        assertEquals(toSearchValueTokenized.getPositions(), simpleSpaces);
        assertEquals(toSearchValueTokenized.toSearchValue(), tokenized.toSearchValue());
        // After second iteration of toSearchValue positions, tokens and toSearchValue are the same
        TokenSequence toSearchValueTokenizedTwice = StringValuesTokenizer.tokenize(
                toSearchValueTokenized.toSearchValue());
        assertEquals(toSearchValueTokenizedTwice.getTokens(), toSearchValueTokenized.getTokens());
        assertEquals(toSearchValueTokenizedTwice.getPositions(), toSearchValueTokenized.getPositions());
        assertEquals(toSearchValueTokenizedTwice.toSearchValue(), toSearchValueTokenized.toSearchValue());
    }

    private TokenSequence buildPair(String[] tokens, int[] positions) {
        return new TokenSequence(Arrays.asList(tokens), new IntArrayList(positions));
    }
}
