package ru.yandex.market.pers.qa.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonUtilsTest {

    private final static String TEXT = "123456789 12345 1234567 1234 12 123 123";

    @Test
    public void testOk1() {
        List<String> result = Arrays.asList("123456789", "12345", "1234567", "1234 12", "123 123");
        final List<String> parts = CommonUtils.splitTextBySentences(TEXT, 10).get();
        assertContainsStrings(parts, result);
        assertContainsStrings(result, parts);
    }

    @Test
    public void testOk2() {
        List<String> result = Arrays.asList("123456789 12345", "1234567 1234 12 123", "123");
        final List<String> parts = CommonUtils.splitTextBySentences(TEXT, 20).get();
        assertContainsStrings(parts, result);
        assertContainsStrings(result, parts);
    }

    @Test
    public void testOk3() {
        List<String> result = Collections.singletonList(TEXT);
        final List<String> parts = CommonUtils.splitTextBySentences(TEXT, 100).get();
        assertContainsStrings(parts, result);
        assertContainsStrings(result, parts);
    }

    @Test
    public void testOk4() {
        List<String> result = Arrays.asList("123456789 12345 1234567 1234 12 123", "123");
        final List<String> parts = CommonUtils.splitTextBySentences(TEXT, 36).get();
        assertContainsStrings(parts, result);
        assertContainsStrings(result, parts);
    }

    @Test
    public void testOk5() {
        assertContainsStrings(Collections.singletonList(TEXT), CommonUtils.splitTextBySentences(TEXT, 100).get());
    }

    @Test
    public void testTooLongWord() {
        assertFalse(CommonUtils.splitTextBySentences(TEXT, 5).isPresent());
    }


    protected void assertContainsStrings(List<String> result, List<String> parts) {
        assertTrue(result.containsAll(parts), result + " contains " + parts);
    }

}