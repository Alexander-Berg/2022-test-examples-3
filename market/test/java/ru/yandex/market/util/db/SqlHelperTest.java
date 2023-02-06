package ru.yandex.market.util.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author artemmz
 * created on 02.06.16.
 */
public class SqlHelperTest {
    protected static final Random RND = new Random();

    @Test
    public void testGetInSectionStrings() throws Exception {
        String expected = "(me in ('1','2','3'))";
        assertEquals(expected, SqlHelper.getInSectionStrings(Arrays.asList("1", "2", "3"), "me"));

        int batchSize = 1000;
        List<String> intList = new ArrayList<>();
        int count = RND.nextInt(batchSize * 10) + batchSize + 1;
        for (int i = 0;
             i < count;
             i++) {
            intList.add(String.valueOf(i));
        }

        expected = "(me in (";
        for (int i = 0;
             i < count;
             i += batchSize) {
            for (int j = i;
                 j < Math.min(count, i + batchSize);
                 j++) {
                expected += "'" + String.valueOf(j) + "',";
            }
            expected = expected.substring(0, expected.length() - 2);
            if (i + batchSize < count) {
                expected += "') or me in (";
            } else {
                expected += "'))";
            }
        }

        assertEquals(expected, SqlHelper.getInSectionStrings(intList, "me"));
    }

    @Test
    public void testGetInSectionNumbers() throws Exception {
        String expected = "(me in (1,2,3))";
        assertEquals(expected, SqlHelper.getInSectionNumbers(Arrays.asList(1L, 2L, 3L), "me"));
        assertEquals(expected, SqlHelper.getInSectionNumbers(Arrays.asList(1, 2, 3), "me"));

        int batchSize = 1000;
        List<Integer> intList = new ArrayList<>();
        int count = RND.nextInt(batchSize * 10) + batchSize + 1;
        for (int i = 0;
             i < count;
             i++) {
            intList.add(i);
        }

        expected = "(me in (";
        for (int i = 0;
             i < count;
             i += batchSize) {
            for (int j = i;
                 j < Math.min(count, i + batchSize);
                 j++) {
                expected += String.valueOf(j) + ",";
            }
            expected = expected.substring(0, expected.length() - 1);
            if (i + batchSize < count) {
                expected += ") or me in (";
            } else {
                expected += "))";
            }
        }

        assertEquals(expected, SqlHelper.getInSectionNumbers(intList, "me"));
    }
}
