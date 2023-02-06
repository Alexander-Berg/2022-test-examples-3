package ru.yandex.market.books.diff.util;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * todo описать предназначение
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class IsbnOperationsTest extends TestCase {
    public void testMaxSet() {
        List<Integer> srcArray = Arrays.asList(11, 12, 13, 14, 21, 31, 41, 51, 61, 62, 63);
        Set<Integer> maxSet = IsbnOperations.maxSet(
                srcArray,
                new Comparator<Integer>() {
                    public int compare(Integer o1, Integer o2) {
                        return o1 / 10 - o2 / 10;
                    }
                }
        );
        assertEquals(3, maxSet.size());
        assertTrue(maxSet.contains(61));
        assertTrue(maxSet.contains(62));
        assertTrue(maxSet.contains(63));

        maxSet = IsbnOperations.maxSet(
                srcArray,
                new Comparator<Integer>() {
                    public int compare(Integer o1, Integer o2) {
                        return o2 / 10 - o1 / 10;
                    }
                }
        );
        assertEquals(4, maxSet.size());
        assertTrue(maxSet.contains(11));
        assertTrue(maxSet.contains(12));
        assertTrue(maxSet.contains(13));
        assertTrue(maxSet.contains(14));
    }

    public void testIsbnTextRepresentations() {
        assertIsbn10TextRepresentation("5987656117");
        assertIsbn10TextRepresentation("5-987656-11-7");
        failIsbn10TextRepresentation("5987656117");

        assertIsbn10TextRepresentation("5170107188");
        assertIsbn10TextRepresentation("5-17-010718-8");
        failIsbn10TextRepresentation("5170107188");

        assertIsbn10TextRepresentation("5699204547");
        assertIsbn10TextRepresentation("5-699-20454-7");
        failIsbn10TextRepresentation("5699204547");

        assertIsbn13TextRepresentation("9780321349606");
        assertIsbn13TextRepresentation("978-0-321-34960-6");
        failIsbn13TextRepresentation("9780321349606");

        assertIsbn13TextRepresentation("9785952428355");
        assertIsbn13TextRepresentation("978-5-9524-2835-5");
        failIsbn13TextRepresentation("9785952428355");

        assertIsbn10TextRepresentation("5-04-003183-1");
        assertIsbn10TextRepresentation("5-17-003009-6");
        assertIsbn10TextRepresentation("0-7494-2360-9");
        assertIsbn10TextRepresentation("5-7695-0653-9");
        assertIsbn10TextRepresentation("5-7811-0100-4");
        assertIsbn10TextRepresentation("3-8228-7150-8");
        assertIsbn10TextRepresentation("5-9204-0002-1");
        assertIsbn10TextRepresentation("5-85684-448-3");
        assertIsbn10TextRepresentation("5-94045-038-5");
        assertIsbn10TextRepresentation("5-900451-22-4");
        assertIsbn10TextRepresentation("985-13-1282-7");

        assertIsbn13TextRepresentation("978-5-09-016379-8");
        assertIsbn13TextRepresentation("978-5-17-004785-7");
        assertIsbn13TextRepresentation("978-5-279-02169-7");
        assertIsbn13TextRepresentation("978-5-353-00395-3");
        assertIsbn13TextRepresentation("978-5-358-02822-7");
        assertIsbn13TextRepresentation("978-5-699-05708-5");
        assertIsbn13TextRepresentation("978-5-86225-756-4");
        assertIsbn13TextRepresentation("978-5-91181-359-8");
        assertIsbn13TextRepresentation("978-5-93642-127-3");
        assertIsbn13TextRepresentation("978-5-94774-625-9");
        assertIsbn13TextRepresentation("978-5-902326-37-3");
        assertIsbn13TextRepresentation("978-5-902357-62-9");
        assertIsbn13TextRepresentation("978-5-903036-45-5");
        assertIsbn13TextRepresentation("978-5-903036-58-5");
        assertIsbn13TextRepresentation("978-5-903182-10-7");
        assertIsbn13TextRepresentation("978-985-15-0012-9");
        assertIsbn13TextRepresentation("978-985-483-913-4");
        assertIsbn13TextRepresentation("978-985-1399-49-5");

        assertRepresentationsEquality("5170363443", "9785170363445");
        assertRepresentationsEquality("5-17-036344-3", "978-5-17-036344-5");

        assertRepresentationsEquality("5040034059", "9785040034055");
        assertRepresentationsEquality("5-04-003405-9", "978-5-04-003405-5");

        assertRepresentationsEquality("5699185143", "9785699185146");
        assertRepresentationsEquality("5-699-18514-3", "978-5-699-18514-6");

        assertRepresentationsEquality("571077281x", "9785710772812");
        assertRepresentationsEquality("5-7107-7281-x", "978-5-7107-7281-2");

        assertRepresentationsEquality("5358009051", "9785358009059");
        assertRepresentationsEquality("5-358-00905-1", "978-5-358-00905-9");

        assertRepresentationsEquality("5170433646", "9785170433643");
        assertRepresentationsEquality("5-17-043364-6", "978-5-17-043364-3");

        assertRepresentationsEquality("9851615323", "9789851615328");
        assertRepresentationsEquality("985-16-1532-3", "978-985-16-1532-8");
    }

    private void assertIsbn10TextRepresentation(String isbnString) {
        long value = IsbnOperations.getIsbnValue(isbnString);
        String isbnString2 = IsbnOperations.outputIsbn10(value);
        assertEquals(isbnString, isbnString2);
    }

    private void assertIsbn13TextRepresentation(String isbnString) {
        long value = IsbnOperations.getIsbnValue(isbnString);
        String isbnString2 = IsbnOperations.outputIsbn13(value);
        assertEquals(isbnString, isbnString2);
    }

    private void failIsbn10TextRepresentation(String isbnString) {
        long value = IsbnOperations.getIsbnValue(isbnString);
        String isbnString2 = IsbnOperations.outputIsbn10(value);
        assertFalse(isbnString.equals(isbnString2));
    }

    private void failIsbn13TextRepresentation(String isbnString) {
        long value = IsbnOperations.getIsbnValue(isbnString);
        String isbnString2 = IsbnOperations.outputIsbn13(value);
        assertFalse(isbnString.equals(isbnString2));
    }

    private void assertRepresentationsEquality(String str10, String str13) {
        long value1 = IsbnOperations.getIsbnValue(str10);
        long value2 = IsbnOperations.getIsbnValue(str13);
        assertEquals(value1, value2);
        assertEquals(str10, IsbnOperations.outputIsbn10(value1));
        assertEquals(str13, IsbnOperations.outputIsbn13(value1));
    }

    public void testRemoveChar() {
        assertEquals("abc", IsbnOperations.removeChar("abc", 'X'));
        assertEquals("", IsbnOperations.removeChar("", 'X'));

        assertEquals("bc", IsbnOperations.removeChar("abc", 'a'));
        assertEquals("ac", IsbnOperations.removeChar("abc", 'b'));
        assertEquals("ab", IsbnOperations.removeChar("abc", 'c'));

        assertEquals("bbcc", IsbnOperations.removeChar("aabbcc", 'a'));
        assertEquals("aacc", IsbnOperations.removeChar("aabbcc", 'b'));
        assertEquals("aabb", IsbnOperations.removeChar("aabbcc", 'c'));

        assertEquals("bc", IsbnOperations.removeChar("aabc", 'a'));
        assertEquals("ac", IsbnOperations.removeChar("abbc", 'b'));
        assertEquals("ab", IsbnOperations.removeChar("abcc", 'c'));

        assertEquals("bc", IsbnOperations.removeChar("aabca", 'a'));
        assertEquals("ac", IsbnOperations.removeChar("babcb", 'b'));
        assertEquals("ab", IsbnOperations.removeChar("cabcc", 'c'));
    }
}
