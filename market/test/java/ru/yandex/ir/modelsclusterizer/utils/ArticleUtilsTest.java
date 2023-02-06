package ru.yandex.ir.modelsclusterizer.utils;

import org.junit.Test;
import ru.yandex.ir.dao.ArticleMachineDaoXml;
import ru.yandex.ir.modelsclusterizer.be.Article;
import ru.yandex.utils.string.aho.Vertex;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * todo описать предназначение.
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class ArticleUtilsTest {
    @Test
    public void testExtractArticle() {

        ArticleMachineDaoXml atd = new ArticleMachineDaoXml();
        atd.setArticleTemplateFile("models-clusterizer-common/article_templates.xml");
        atd.reload();
        Vertex<Integer> articleMachine = atd.getArticleMachine();
        checkArticles(articleMachine,
            new String[]{"Тушь объемная для ресниц MAXI VITAMIN GT860 1шт", ""},
            new String[]{"GT860"}
        );
        checkArticles(articleMachine,
            new String[]{"", "Тушь объемная для ресниц MAXI VITAMIN 1шт"},
            new String[]{}
        );
        checkArticles(articleMachine,
            new String[]{"Подвязка SENTIMENT Charmante CLZ 880805", ""},
            new String[]{"880805"}
        );
        checkArticles(articleMachine,
            new String[]{"Лампа светодиодная G13 18W 4000K трубчатая матовая", ""},
            new String[]{}
        );
    }

    private void checkArticles(Vertex<Integer> articleMachine, String[] pseudoOffer, String[] result) {
        Set<String> articles = ArticleUtils.extractArticles(articleMachine, pseudoOffer[0], pseudoOffer[1])
            .stream().map(Article::getName).collect(Collectors.toSet());
        Set<String> resultSet = Arrays.stream(result).collect(Collectors.toSet());
        assertEquals(articles, resultSet);
    }

    @Test
    public void testSurroundByWhitespaceOrPunct() {
        String str = "18W 4000K. Лампа";
        assertEquals(ArticleUtils.surroundByWhitespaceOrPunct(str, 0, 0), false);
        assertEquals(ArticleUtils.surroundByWhitespaceOrPunct(str, 0, 3), true);
        assertEquals(ArticleUtils.surroundByWhitespaceOrPunct(str, 0, 4), false);
        assertEquals(ArticleUtils.surroundByWhitespaceOrPunct(str, 0, str.length()), true);
        assertEquals(ArticleUtils.surroundByWhitespaceOrPunct(str, 4, 9), true);
    }

    @Test
    public void testContainsYearToken() {
        assertFalse(ArticleUtils.containsYearToken("2013"));
        assertFalse(ArticleUtils.containsYearToken("2014"));
        assertFalse(ArticleUtils.containsYearToken("2015"));
        assertFalse(ArticleUtils.containsYearToken("2016"));
        assertFalse(ArticleUtils.containsYearToken("2017"));
        assertTrue(ArticleUtils.containsYearToken("2018"));
        assertTrue(ArticleUtils.containsYearToken("2019"));
        assertTrue(ArticleUtils.containsYearToken("2020"));
        assertTrue(ArticleUtils.containsYearToken("2021"));
        assertTrue(ArticleUtils.containsYearToken("2022"));
        assertFalse(ArticleUtils.containsYearToken("2023"));

        assertTrue(ArticleUtils.containsYearToken("2012", "2012"));
        assertTrue(ArticleUtils.containsYearToken(" 2012", "2012"));
        assertTrue(ArticleUtils.containsYearToken("2012 ", "2012"));
        assertTrue(ArticleUtils.containsYearToken(" 2012 ", "2012"));
        assertTrue(ArticleUtils.containsYearToken("коллекция 2012", "2012"));
        assertTrue(ArticleUtils.containsYearToken("2012 год", "2012"));
        assertTrue(ArticleUtils.containsYearToken("коллекция2012", "2012"));
        assertTrue(ArticleUtils.containsYearToken("2012год", "2012"));
        assertTrue(ArticleUtils.containsYearToken("2011-2012", "2012"));
        assertTrue(ArticleUtils.containsYearToken("2012-2013", "2012"));

        assertFalse(ArticleUtils.containsYearToken("", "2012"));
        assertFalse(ArticleUtils.containsYearToken("2", "2012"));
        assertFalse(ArticleUtils.containsYearToken("20", "2012"));
        assertFalse(ArticleUtils.containsYearToken("201", "2012"));
        assertFalse(ArticleUtils.containsYearToken("12012", "2012"));
        assertFalse(ArticleUtils.containsYearToken("20123", "2012"));
        assertFalse(ArticleUtils.containsYearToken("120123", "2012"));
    }

    @Test
    public void testGetArticlePattern() {
        //assertEquals(Arrays.asList("aaa", "AAA", " ", "111"), ArticleUtils.getArticlePattern("abcABC/001"));
        //assertEquals(Arrays.asList(" ", "aaa", "AAA", " ", "111"), ArticleUtils.getArticlePattern(" abcABC/001"));
        //assertEquals(Arrays.asList(" ", "aaa", "AAA", " ", "111"), ArticleUtils.getArticlePattern(" abcABC / 001"));
        assertEquals(Arrays.asList("aaa", "AAA"), ArticleUtils.getArticlePattern("abcABC"));
        //assertEquals(Arrays.asList("aaa", " ", "AAA"), ArticleUtils.getArticlePattern("abc-ABC"));
        //assertEquals(Arrays.asList("aaa", " ", "AAA"), ArticleUtils.getArticlePattern("abc.ABC"));
        //assertEquals(Arrays.asList("aaa", " ", "AAA"), ArticleUtils.getArticlePattern("abc:ABC"));
        //assertEquals(Arrays.asList("aaa", " ", "AAA"), ArticleUtils.getArticlePattern("abc, ABC"));
        //assertEquals(Arrays.asList(" ", "aaa", " ", "AAA", " "), ArticleUtils.getArticlePattern(" abc, ABC "));
        //assertEquals(Arrays.asList(" ", "aaa", " ", "AAA", " "), ArticleUtils.getArticlePattern(", abc, ABC, "));
    }

    @Test
    public void testGetTokenPattern() {
        assertEquals("", ArticleUtils.getTokenPattern('X', 0));
        assertEquals("X", ArticleUtils.getTokenPattern('X', 1));
        assertEquals("XX", ArticleUtils.getTokenPattern('X', 2));
        assertEquals("XXX", ArticleUtils.getTokenPattern('X', 3));

        assertEquals("", ArticleUtils.getTokenPattern('x', 0));
        assertEquals("x", ArticleUtils.getTokenPattern('x', 1));
        assertEquals("xx", ArticleUtils.getTokenPattern('x', 2));
        assertEquals("xxx", ArticleUtils.getTokenPattern('x', 3));

        assertEquals("", ArticleUtils.getTokenPattern('4', 0));
        assertEquals("4", ArticleUtils.getTokenPattern('4', 1));
        assertEquals("44", ArticleUtils.getTokenPattern('4', 2));
        assertEquals("444", ArticleUtils.getTokenPattern('4', 3));

        assertEquals("", ArticleUtils.getTokenPattern('X', 0));
        assertEquals("", ArticleUtils.getTokenPattern('X', 0));
        assertEquals("X", ArticleUtils.getTokenPattern('X', 1));
        assertEquals("X", ArticleUtils.getTokenPattern('X', 1));
        assertEquals("XX", ArticleUtils.getTokenPattern('X', 2));
        assertEquals("XX", ArticleUtils.getTokenPattern('X', 2));
    }

    @Test
    public void testNormalizeArticle() {
        assertEquals("", ArticleUtils.normalizeArticle("/_ .\\()"));
        assertEquals("a b", ArticleUtils.normalizeArticle("a/_ .\\()b"));
        assertEquals("a b c d e f g h", ArticleUtils.normalizeArticle("a/b_c d.e\\f(g)h"));

        assertEquals("zaxscd zaxs CDVF 0101 02", ArticleUtils.normalizeArticle("zaxscd-zaxs/CDVF-0101:02"));
        assertEquals("zaxscd zaxs CDVF 0101 02", ArticleUtils.normalizeArticle("zaxscd - zaxs / CDVF - 0101 : 02"));
        assertEquals("zaxscd zaxs CDVF 0101", ArticleUtils.normalizeArticle("zaxscd zaxs CDVF 0101"));
    }
}
