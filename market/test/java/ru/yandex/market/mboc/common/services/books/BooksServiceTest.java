package ru.yandex.market.mboc.common.services.books;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.misc.test.Assert;

public class BooksServiceTest {
    static long BOOK_PARENT_CATEGORY1 = 123L;
    static long BOOK_PARENT_CATEGORY2 = 234L;
    static long BOOK_CHILD_CATEGORY1 = 1231L;
    static long BOOK_CHILD_CATEGORY2 = 1232L;
    static Set<Long> booksCategories = Set.of(BOOK_PARENT_CATEGORY1, BOOK_PARENT_CATEGORY2);
    CategoryCachingService categoryCachingService;
    BooksService booksService;

    @Before
    public void before() {
        categoryCachingService = new CategoryCachingServiceMock()
            .addCategory(BOOK_PARENT_CATEGORY1, "book parent category 1")
            .addCategory(BOOK_PARENT_CATEGORY2, "book parent category 2")
            .addCategory(BOOK_CHILD_CATEGORY1, "book child category 1", BOOK_PARENT_CATEGORY1)
            .addCategory(BOOK_CHILD_CATEGORY2, "book child category 2", BOOK_PARENT_CATEGORY2)
            .addCategory(999L, "a category");
        booksService = new BooksService(categoryCachingService, booksCategories);
    }

    @Test
    public void isPossibleIsbnBarcode() {
        Assert.isFalse(BooksService.isPossibleIsbnBarcode("123"));
        Assert.isTrue(BooksService.isPossibleIsbnBarcode("978"));
        Assert.isTrue(BooksService.isPossibleIsbnBarcode("979"));
        Assert.isTrue(BooksService.isPossibleIsbnBarcode("9783161484100"));
        Assert.isTrue(BooksService.isPossibleIsbnBarcode("978-3-16-148410-0"));
        Assert.isTrue(BooksService.isPossibleIsbnBarcode("978 3 16 148410 0"));
        Assert.isTrue(BooksService.isPossibleIsbnBarcode("977-3-16-148410-0"));
        Assert.isFalse(BooksService.isPossibleIsbnBarcode("970-3-16-148410-0"));
        Assert.isTrue(BooksService.isPossibleIsbnBarcode("123, 978"));
        Assert.isTrue(BooksService.isPossibleIsbnBarcode("509014169X"));
    }

    @Test
    public void isBook() {
        Assert.isFalse(booksService.isBook("123", 1));
        Assert.isTrue(booksService.isBook("978", 1));
        Assert.isTrue(booksService.isBook("978 3 16 148410 0", 1));
        Assert.isTrue(booksService.isBook("123", BOOK_PARENT_CATEGORY1));
        Assert.isTrue(booksService.isBook("123", BOOK_PARENT_CATEGORY2));
        Assert.isTrue(booksService.isBook("123", BOOK_CHILD_CATEGORY1));
        Assert.isTrue(booksService.isBook("123", BOOK_CHILD_CATEGORY2));
        Assert.isFalse(booksService.isBook("509014169X", 1));
    }

    @Test
    public void normalizeIsbn() {
        Assert.equals("234534345345", BooksService.normalizeIsbn("234534345345"));
        Assert.equals(
            "9783161484100,9783161484100",
            BooksService.normalizeIsbn("978 3 16-148410-0, 9783161484 100")
        );
        Assert.equals("0306406152", BooksService.normalizeIsbn("0-306 40615-2"));
        Assert.equals("", BooksService.normalizeIsbn("0-306-40615-X"));
        Assert.equals("", BooksService.normalizeIsbn("0-306-40615-x"));
    }
}
