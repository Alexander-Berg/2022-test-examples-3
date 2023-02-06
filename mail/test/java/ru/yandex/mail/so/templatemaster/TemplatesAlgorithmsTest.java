package ru.yandex.mail.so.templatemaster;

import java.util.function.BiFunction;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.LongList;
import ru.yandex.mail.so.templatemaster.templates.BaseTemplate;
import ru.yandex.mail.so.templatemaster.templates.StableTemplate;
import ru.yandex.test.util.TestBase;

public class TemplatesAlgorithmsTest extends TestBase {
    public TemplatesAlgorithmsTest() {
        super(false, 0L);
    }

    @Test
    public void testFullMatch() {
        // full match
        testSingleMatch("898", "898", "");
        // partial match
        testSingleMatch("9", "898", null);
        testSingleMatch("*9*", "898", "8*8");
        //begin and end
        testSingleMatch("4*7", "4567", "56");
        //consequency
        testSingleMatch("*13*", "123", null);

        testSingleMatch("*23", "123", "1");
        testSingleMatch("1*123*6", "1121236", "12*");
        testSingleMatch("1213*", "12134", "4");

        // Let's treat end of template as * by default
        testSingleMatch("1213", "12134", "4");
    }

    /**
     * Checks if mail matches to template, compares delta
     *
     * @param template      string, each character representing single token
     *                      * is a SEP_TOKEN
     * @param mail          format similar to templateRaw
     * @param expectedDelta format similar to templateRaw.
     *                      null - doesn't match
     */
    void testSingleMatch(String template, String mail, String expectedDelta) {
        LongList res =
            new StableTemplate(
                parseStringOfTokens(template),
                0L,
                "[]",
                0)
                .checkMatch(parseStringOfTokens(mail));
        Assert.assertEquals(
            "Match " + mail
                + " against " + template,
            expectedDelta,
            tokensToString(res));
    }

    @Test
    public void testLcs() {
        checkLcs(BaseTemplate::lcs);
    }

    @Test
    public void testLcsHirschberg() {
        checkLcs(BaseTemplate::lcsHirschberg);
    }

    private void checkLcs(BiFunction<long[], long[], long[]> lcsFunc) {
        checkSingleLcs("aba", "aba", "aba", lcsFunc);
        checkSingleLcs("aba", "aca", "a*a", lcsFunc);
        checkSingleLcs("aba", "aa", "a*a", lcsFunc);
        try { // multiple correct answers
            checkSingleLcs("aaa", "aa", "*aa", lcsFunc);
        } catch (AssertionError e) {
            // Optimization of SEPs required:
            checkSingleLcs("aaa", "aa", "a*a", lcsFunc);
        }
        checkSingleLcs("abcd", "bc", "*bc*", lcsFunc);
        checkSingleLcs("abd", "acd", "a*d", lcsFunc);

        checkSingleLcs("ab*d", "a*cd", "a*d", lcsFunc);
        checkSingleLcs("abd*", "abd", "abd*", lcsFunc);

    }

//    @Test
//    public void testLcsPref() {
//        long start = System.currentTimeMillis();
//        String[] arr = Collections.nCopies(4000, "a").toArray(new String[0]);
//        lcs(arr, arr);
//        System.err.println("DDBG basic lcs 4'000^2 took "
//                                   + (System.currentTimeMillis() - start));
//        start = System.currentTimeMillis();
//        lcsHirschberg(arr, arr);
//        System.err.println("DDBG Hirschberg lcs 4'000^2 took "
//                                   + (System.currentTimeMillis() - start));
//    }

    private void checkSingleLcs(
        String first,
        String second,
        String expected,
        BiFunction<long[], long[], long[]> lcsFunc)
    {
        Assert.assertEquals(
            "Lcs of " + first + " and " + second,
            expected,
            tokensToString(lcsFunc.apply(
                parseStringOfTokens(first),
                parseStringOfTokens(second))));
    }



    public static long[] parseStringOfTokens(String string) {
        if (string == null) {
            return null;
        }
        return string.chars()
                   .mapToLong(c -> c == '*' ? BaseTemplate.SEP_TOKEN : c)
                   .toArray();
    }

    // Inverse of parseStringOfTokens
    public static String tokensToString(long[] tokens) {
        if (tokens == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (long token : tokens) {
            if (token == BaseTemplate.SEP_TOKEN) {
                sb.append('*');
            } else {
                sb.append((char) token);
            }
        }
        return sb.toString();
    }

    public static String tokensToString(LongList tokens) {
        if (tokens == null) {
            return null;
        }
        return tokensToString(tokens.toLongArray());
    }

}
