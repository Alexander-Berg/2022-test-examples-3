package ru.yandex.market.ir.matcher2.matcher.matcher.books.patterns;


import org.junit.Test;

import ru.yandex.market.ir.matcher2.matcher.books.patterns.BooksIsbnPattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class BooksIsbnPatternTest {

    @Test
    public void testCheckIsbn() {
        assertTrue(BooksIsbnPattern.checkIsbn("5845908590"));
        assertTrue(BooksIsbnPattern.checkIsbn("9789851404830"));
        assertTrue(BooksIsbnPattern.checkIsbn("9854564681"));
        assertTrue(BooksIsbnPattern.checkIsbn("9785090076791"));
        assertTrue(BooksIsbnPattern.checkIsbn("3938484802"));
        assertTrue(BooksIsbnPattern.checkIsbn("9782707319302"));
        assertTrue(BooksIsbnPattern.checkIsbn("0752582763"));
        assertTrue(BooksIsbnPattern.checkIsbn("9785855822373"));
        assertTrue(BooksIsbnPattern.checkIsbn("5373021504"));
        assertTrue(BooksIsbnPattern.checkIsbn("9780898694314"));
        assertTrue(BooksIsbnPattern.checkIsbn("5786700755"));
        assertTrue(BooksIsbnPattern.checkIsbn("9789854564685"));
        assertTrue(BooksIsbnPattern.checkIsbn("0321113586"));
        assertTrue(BooksIsbnPattern.checkIsbn("9780764535086"));
        assertTrue(BooksIsbnPattern.checkIsbn("9851404837"));
        assertTrue(BooksIsbnPattern.checkIsbn("9780138401177"));
        assertTrue(BooksIsbnPattern.checkIsbn("5958401718"));
        assertTrue(BooksIsbnPattern.checkIsbn("9788861300668"));
        assertTrue(BooksIsbnPattern.checkIsbn("8861300669"));
        assertTrue(BooksIsbnPattern.checkIsbn("9780425179239"));
        assertTrue(BooksIsbnPattern.checkIsbn("538600624x"));
        assertTrue(BooksIsbnPattern.checkIsbn("985140036x"));

        assertFalse(BooksIsbnPattern.checkIsbn("5845908591"));
        assertFalse(BooksIsbnPattern.checkIsbn("9789851404832"));
        assertFalse(BooksIsbnPattern.checkIsbn("9854564683"));
        assertFalse(BooksIsbnPattern.checkIsbn("9785090076794"));
        assertFalse(BooksIsbnPattern.checkIsbn("3938484805"));
        assertFalse(BooksIsbnPattern.checkIsbn("9782707319306"));
        assertFalse(BooksIsbnPattern.checkIsbn("0752582767"));
        assertFalse(BooksIsbnPattern.checkIsbn("9785855822378"));
        assertFalse(BooksIsbnPattern.checkIsbn("5373021509"));
        assertFalse(BooksIsbnPattern.checkIsbn("9780898694310"));
        assertFalse(BooksIsbnPattern.checkIsbn("5786700751"));
        assertFalse(BooksIsbnPattern.checkIsbn("9789854564682"));
        assertFalse(BooksIsbnPattern.checkIsbn("0321113583"));
        assertFalse(BooksIsbnPattern.checkIsbn("9780764535084"));
        assertFalse(BooksIsbnPattern.checkIsbn("9851404835"));
        assertFalse(BooksIsbnPattern.checkIsbn("9780138401176"));
        assertFalse(BooksIsbnPattern.checkIsbn("5958401717"));
        assertFalse(BooksIsbnPattern.checkIsbn("9788861300669"));
        assertFalse(BooksIsbnPattern.checkIsbn("8861300660"));
        assertFalse(BooksIsbnPattern.checkIsbn("9780425179231"));
        assertFalse(BooksIsbnPattern.checkIsbn("5386006242"));
        assertFalse(BooksIsbnPattern.checkIsbn("9851400363"));
    }
}
