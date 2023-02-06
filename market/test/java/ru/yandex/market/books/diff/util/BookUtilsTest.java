package ru.yandex.market.books.diff.util;

import junit.framework.TestCase;
import ru.yandex.market.books.utils.BookUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * todo описать предназначение
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class BookUtilsTest extends TestCase {
    public void testSimplyCheckIsbn() {
        // корректные к.с.
        assertEquals(978584590859L, BookUtils.checkIsbn("5845908590"));
        assertEquals(978985140483L, BookUtils.checkIsbn("9789851404830"));
        assertEquals(979863471507L, BookUtils.checkIsbn("9798634715070"));
        assertEquals(978985456468L, BookUtils.checkIsbn("9854564681"));
        assertEquals(978509007679L, BookUtils.checkIsbn("9785090076791"));
        assertEquals(979980106569L, BookUtils.checkIsbn("9799801065691"));
        assertEquals(978393848480L, BookUtils.checkIsbn("3938484802"));
        assertEquals(978270731930L, BookUtils.checkIsbn("9782707319302"));
        assertEquals(979297569742L, BookUtils.checkIsbn("9792975697422"));
        assertEquals(978075258276L, BookUtils.checkIsbn("0752582763"));
        assertEquals(978585582237L, BookUtils.checkIsbn("9785855822373"));
        assertEquals(979725258919L, BookUtils.checkIsbn("9797252589193"));
        assertEquals(978537302150L, BookUtils.checkIsbn("5373021504"));
        assertEquals(978089869431L, BookUtils.checkIsbn("9780898694314"));
        assertEquals(979351000282L, BookUtils.checkIsbn("9793510002824"));
        assertEquals(978578670075L, BookUtils.checkIsbn("5786700755"));
        assertEquals(978985456468L, BookUtils.checkIsbn("9789854564685"));
        assertEquals(979653807090L, BookUtils.checkIsbn("9796538070905"));
        assertEquals(978032111358L, BookUtils.checkIsbn("0321113586"));
        assertEquals(978076453508L, BookUtils.checkIsbn("9780764535086"));
        assertEquals(979885951551L, BookUtils.checkIsbn("9798859515516"));
        assertEquals(978985140483L, BookUtils.checkIsbn("9851404837"));
        assertEquals(978013840117L, BookUtils.checkIsbn("9780138401177"));
        assertEquals(979061714429L, BookUtils.checkIsbn("9790617144297"));
        assertEquals(978595840171L, BookUtils.checkIsbn("5958401718"));
        assertEquals(978886130066L, BookUtils.checkIsbn("9788861300668"));
        assertEquals(979868800867L, BookUtils.checkIsbn("9798688008678"));
        assertEquals(978886130066L, BookUtils.checkIsbn("8861300669"));
        assertEquals(978042517923L, BookUtils.checkIsbn("9780425179239"));
        assertEquals(979033481078L, BookUtils.checkIsbn("9790334810789"));
        assertEquals(978538600624L, BookUtils.checkIsbn("538600624x"));
        assertEquals(978985140036L, BookUtils.checkIsbn("985140036x"));

        // некорректные к.с.
        assertEquals(-1, BookUtils.checkIsbn("5845908591"));
        assertEquals(-1, BookUtils.checkIsbn("9789851404832"));
        assertEquals(-1, BookUtils.checkIsbn("9854564683"));
        assertEquals(-1, BookUtils.checkIsbn("9785090076794"));
        assertEquals(-1, BookUtils.checkIsbn("3938484805"));
        assertEquals(-1, BookUtils.checkIsbn("9782707319306"));
        assertEquals(-1, BookUtils.checkIsbn("0752582767"));
        assertEquals(-1, BookUtils.checkIsbn("9785855822378"));
        assertEquals(-1, BookUtils.checkIsbn("5373021509"));
        assertEquals(-1, BookUtils.checkIsbn("9780898694310"));
        assertEquals(-1, BookUtils.checkIsbn("5786700751"));
        assertEquals(-1, BookUtils.checkIsbn("9789854564682"));
        assertEquals(-1, BookUtils.checkIsbn("0321113583"));
        assertEquals(-1, BookUtils.checkIsbn("9780764535084"));
        assertEquals(-1, BookUtils.checkIsbn("9851404835"));
        assertEquals(-1, BookUtils.checkIsbn("9780138401176"));
        assertEquals(-1, BookUtils.checkIsbn("5958401717"));
        assertEquals(-1, BookUtils.checkIsbn("9788861300669"));
        assertEquals(-1, BookUtils.checkIsbn("8861300660"));
        assertEquals(-1, BookUtils.checkIsbn("9780425179231"));
        assertEquals(-1, BookUtils.checkIsbn("5386006242"));
        assertEquals(-1, BookUtils.checkIsbn("9851400363"));
    }

    public void testCheckIsbn13WrongPrefix() {
        Set<String> possiblePrefixes = new HashSet<String>(Arrays.asList("978", "979"));
        for (int i = 100; i < 1000; i++) {
            String prefix = Integer.toString(i);
            String pseudoData = "000000000";
            boolean checksumOk = false;
            for (int checksum = 0; checksum < 10; checksum++) {
                String pseudoISBN = prefix + pseudoData + checksum;
                if (BookUtils.checkIsbn(pseudoISBN) > 0) {
                    checksumOk = true;
                    break;
                }
            }
            assertEquals(prefix, possiblePrefixes.contains(prefix), checksumOk);
        }
    }
}
