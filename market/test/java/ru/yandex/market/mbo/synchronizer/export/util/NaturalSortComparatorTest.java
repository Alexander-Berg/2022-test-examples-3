package ru.yandex.market.mbo.synchronizer.export.util;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@SuppressWarnings("checkstyle:MagicNumber")
public class NaturalSortComparatorTest {
    private static final int LEN = 10;
    private NaturalSortComparator comparator;
    private Random random;

    @Before
    public void setup() {
        random = new Random(53820046);
        comparator = new NaturalSortComparator();
    }

    @Test
    public void testBulk() {
        for (int attempt = 0; attempt < 1000; attempt++) {
            List<String> sorted = new ArrayList<>();
            for (int wordCounter = 0; wordCounter < 100; ++wordCounter) {
                sorted.add(generateRandomString());
            }
            sorted.stream().sorted(comparator).collect(Collectors.toList());
        }
    }

    @Test
    public void testDigitDiff() {
        String a = "aaa";
        String b = "1aaa";
        String c = "2aaa";
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(b);
        Assertions.assertThat(b).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(b);
    }

    @Test
    public void testDigitDiff2() {
        String a = "aaa 1ccc";
        String b = "aaa 4bbb";
        String c = "aaa 6aaa";
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(b);
        Assertions.assertThat(b).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(b);
    }

    @Test
    public void testDigitDiff3() {
        String a = "1aaa 6ccc";
        String b = "2aaa 4bbb";
        String c = "3aaa 1aaa";
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(b);
        Assertions.assertThat(b).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(b);
    }

    @Test
    public void testDigitDiff4() {
        String a = "2";
        String b = "1";
        String c = "отсутствует";
        Assertions.assertThat(a).usingComparator(comparator).isGreaterThan(b);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(a);
        Assertions.assertThat(a).usingComparator(comparator).isGreaterThan(c);
        Assertions.assertThat(b).usingComparator(comparator).isGreaterThan(c);
        Assertions.assertThat(c).usingComparator(comparator).isLessThan(a);
        Assertions.assertThat(c).usingComparator(comparator).isLessThan(b);
    }

    @Test
    public void testDigitDiff5() {
        String a = "9\"-9.7\"";
        String b = "менее 7\"";
        String c = "11.6\" и выше";
        Assertions.assertThat(a).usingComparator(comparator).isGreaterThan(b);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(a);
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(b);
    }

    @Test
    public void testDigitDiff6() {
        String a = "1 mm";
        String b = "0.09 mm";
        String c = "1.3 mm";
        String d = "1.25 mm";
        Assertions.assertThat(a).usingComparator(comparator).isGreaterThan(b);
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(d);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(a);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(d);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(b);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(d);
        Assertions.assertThat(d).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(d).usingComparator(comparator).isGreaterThan(b);
        Assertions.assertThat(d).usingComparator(comparator).isLessThan(c);
    }

    @Test
    public void testDigitDiff7() {
        String a = "5 - 7 лет";
        String b = "3 - 5 лет";
        String c = "14+";
        String d = "7 - 14 лет";
        Assertions.assertThat(a).usingComparator(comparator).isGreaterThan(b);
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(d);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(a);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(d);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(b);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(d);
        Assertions.assertThat(d).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(d).usingComparator(comparator).isGreaterThan(b);
        Assertions.assertThat(d).usingComparator(comparator).isLessThan(c);
    }

    @Test
    public void testDigitDiff8() {
        String a = "213..6213.11.";
        String b = "114..671.3.";
        String c = "213..6213.2";
        Assertions.assertThat(a).usingComparator(comparator).isGreaterThan(b);
        Assertions.assertThat(a).usingComparator(comparator).isGreaterThan(c);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(a);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(c).usingComparator(comparator).isLessThan(a);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(b);
    }

    @Test
    public void testLetterDiff() {
        String a = "aaa 6ccc";
        String b = "aba 4bbb";
        String c = "abc 1aaa";
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(b);
        Assertions.assertThat(b).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(b);
    }

    @Test
    public void testLetterDiff2() {
        String a = "aaa aaa";
        String b = "aba bbb";
        String c = "aba ccc";
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(b);
        Assertions.assertThat(b).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(a).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(b).usingComparator(comparator).isLessThan(c);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(a);
        Assertions.assertThat(c).usingComparator(comparator).isGreaterThan(b);
    }

    private String generateRandomString() {
        String candidateChars = "ABCDEFGHIJ1234567890";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LEN; i++) {
            if (i % 3 == 0 && i > 0 && random.nextBoolean()) {
                sb.append(" ");
            }
            sb.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
        }
        return sb.toString();
    }

}
