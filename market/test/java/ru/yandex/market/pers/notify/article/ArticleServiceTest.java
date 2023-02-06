package ru.yandex.market.pers.notify.article;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author misterku
 */
public class ArticleServiceTest {

    private ArticleService articleService;

    @BeforeEach
    public void setUp() {
        articleService = new ArticleService();
    }

    @Test
    public void simpleParsing() {
        articleService.setPath(getClass().getResource("/cmspages/file1.json").getPath());
        List<Article> articles = articleService.readAllArticles();
        assertEquals(2, articles.size());
        assertTrue(articleService.readApplicableArticles(article -> false).isEmpty());
    }

    @Test
    public void badFileFormat() {
        articleService.setPath(getClass().getResource("/cmspages/bad.json").getPath());
        List<Article> articles = articleService.readAllArticles();
        assertNull(articles);
    }

    @Test
    public void goodFileWithDates() {
        articleService.setPath(getClass().getResource("/cmspages/good.json").getPath());
        List<Article> articles = articleService.readApplicableArticles(article -> true);
        assertEquals(2, articles.size());
    }
}
