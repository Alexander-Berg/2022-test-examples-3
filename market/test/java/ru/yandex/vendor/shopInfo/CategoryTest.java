package ru.yandex.vendor.shopInfo;

import org.junit.Test;
import ru.yandex.vendor.category.Category;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.junit.Assert.*;
import static ru.yandex.vendor.category.Category.*;

/**
 * Created by vsubhuman on 15/12/16.
 */
public class CategoryTest {

    @Test
    public void testParsePathFromDb() {

        assertEquals(
            emptyList(),
            parsePathFromDb(""));

        assertEquals(
            singletonList("qwe"),
            parsePathFromDb("qwe"));

        assertEquals(
            asList("qwe", "rty", "qaz"),
            parsePathFromDb(
                "qwe{^path^}rty{^path^}qaz"));

    }

    @Test
    public void testParseCategoriesFromDb() {

        assertEquals(
            emptyList(),
            parseCategoriesFromDb(""));

        assertEquals(
            singletonList(
                new Category(42L, asList("qwe", "rty", "qaz"))),
            parseCategoriesFromDb(
                "42{^id^}qwe{^path^}rty{^path^}qaz"));

        assertEquals(
            singletonList(
                new Category(42L, asList("qwe", "rty", "qaz"))),
            parseCategoriesFromDb(
                "42{^id^}qwe{^path^}rty{^path^}qaz{^cat^}"));

        assertEquals(
            singletonList(
                new Category(42L, emptyList())),
            parseCategoriesFromDb(
                "42{^id^}"));

        assertEquals(
            asList(
                new Category(42L, asList("qwe", "rty", "qaz")),
                new Category(43L, asList("aa", "bb", "cc"))),
            parseCategoriesFromDb(
                "42{^id^}qwe{^path^}rty{^path^}qaz{^cat^}43{^id^}aa{^path^}bb{^path^}cc"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_ParseCategoriesFromDb_fails_on_too_many_separators_separators() {
        parseCategoriesFromDb("42{^id^}qwe{^id^}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_ParseCategoriesFromDb_fails_on_no_separators_separators() {
        parseCategoriesFromDb("qwe");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_ParseCategoriesFromDb_fails_on_illegal_id() {
        parseCategoriesFromDb("qwe{^id^}qwe");
    }

    @Test
    public void contains_returns_true_for_the_same_category() throws Exception {
        Category category = new Category(777L, asList("qwe", "rty", "qaz"));
        assertTrue(category.contains(category));
    }

    @Test
    public void contains_returns_false_for_unrelated_categories() throws Exception {
        Category category1 = new Category(22L, asList("qwe", "rty", "qaz"));
        Category category2 = new Category(33L, asList("spam", "eggs"));
        assertFalse(category1.contains(category2));
    }

    @Test
    public void contains_returns_true_for_sub_category() throws Exception {
        Category category1 = new Category(22L, asList("qwe", "rty"));
        Category category2 = new Category(33L, asList("qwe", "rty", "qaz"));
        assertTrue(category1.contains(category2));
    }

    @Test
    public void contains_returns_true_for_sub_sub_categories() throws Exception {
        Category category1 = new Category(22L, asList("qwe", "rty"));
        Category category2 = new Category(33L, asList("qwe", "rty", "qaz", "pop"));
        Category category3 = new Category(33L, asList("qwe", "rty", "qaz", "pop", "zaz", "sas"));
        assertTrue(category1.contains(category2));
        assertTrue(category1.contains(category3));
        assertTrue(category2.contains(category3));
    }

    @Test
    public void contains_returns_false_for_parent_category() throws Exception {
        Category category1 = new Category(22L, asList("qwe", "rty"));
        Category category2 = new Category(33L, asList("qwe", "rty", "qaz"));
        assertFalse(category2.contains(category1));
    }
}
