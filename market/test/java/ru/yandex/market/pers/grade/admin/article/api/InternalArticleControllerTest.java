package ru.yandex.market.pers.grade.admin.article.api;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.grade.admin.MockedPersGradeAdminTest;
import ru.yandex.market.pers.grade.admin.article.api.dto.Article;
import ru.yandex.market.pers.grade.admin.article.api.dto.Count;
import ru.yandex.market.pers.grade.core.article.model.ArticleModState;
import ru.yandex.market.pers.grade.core.article.model.ArticleModerationResult;
import ru.yandex.market.pers.grade.core.article.service.ArticleModerationService;
import ru.yandex.market.pers.grade.core.mock.PersCoreMockFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class InternalArticleControllerTest extends MockedPersGradeAdminTest {

    private static final long FAKE_USER_ID = 123L;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ArticleModerationService articleModerationService;
    @Autowired
    @Qualifier("mboCardRestTemplate")
    RestTemplate mboCardRestTemplate;
    @Autowired
    @Qualifier("previewPublishRestTemplate")
    RestTemplate previewPublishRestTemplate;

    @Test
    public void testGetReasons() throws Exception {
        mvc.perform(get("/api/article/moderation/reasons"))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithUserDetails(userDetailsServiceBeanName = "persDebugUserDetailsService", value = "spbtester")
    public void testNoArticlesForModeration() throws Exception {
        List<Article> result = objectMapper.readValue(
            mvc.perform(get("/api/article/moderation?_user_id=" + FAKE_MODERATOR_ID)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andReturn().getResponse().getContentAsString(), new TypeReference<List<Article>>() {
        });
        Assert.assertTrue("Must be empty", result.isEmpty());
    }

    @Test
    @WithUserDetails(userDetailsServiceBeanName = "persDebugUserDetailsService", value = "spbtester")
    public void testGetArticlesForModeration() throws Exception {
        int articleId = 1;
        int revision = 1;
        articleModerationService.startArticleModeration(articleId, revision, FAKE_USER_ID);
        List<Article> result = objectMapper.readValue(
            mvc.perform(get("/api/article/moderation?_user_id=" + FAKE_MODERATOR_ID)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andReturn().getResponse().getContentAsString(), new TypeReference<List<Article>>() {
        });
        Assert.assertEquals("Size must be equal 1", 1, result.size());
        Assert.assertEquals(articleId, result.get(0).getArticleId());
        Assert.assertEquals(revision, result.get(0).getRevision());
        Assert.assertTrue(result.get(0).getUrl().endsWith(String.format("?content-preview=once&articleId=%d&revision=%d", articleId, revision)));
        result = objectMapper.readValue(
            mvc.perform(get("/api/article/moderation?_user_id=" + FAKE_MODERATOR_ID)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andReturn().getResponse().getContentAsString(), new TypeReference<List<Article>>() {
        });
        Assert.assertTrue("Must be empty", result.isEmpty());
    }

    @Test
    @WithUserDetails(userDetailsServiceBeanName = "persDebugUserDetailsService", value = "spbtester")
    public void testSuccessModerationArticleButNotPublished() throws Exception {
        long articleId = 1;
        int revision = 1;
        PersCoreMockFactory.brokenRestTemplate(mboCardRestTemplate);
        articleModerationService.startArticleModeration(articleId, revision, FAKE_USER_ID);
        List<Article> result = objectMapper.readValue(
            mvc.perform(get("/api/article/moderation?_user_id=" + FAKE_MODERATOR_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), new TypeReference<List<Article>>() {
            });
        Assert.assertEquals("Size must be equal 1", 1, result.size());
        Article article = result.get(0);
        String moderateUrl = String.format("/api/article/moderate?_user_id=%s&id=%d&articleId=%d&revision=%d&state=%d",
            FAKE_MODERATOR_ID, article.getId(), article.getArticleId(), article.getRevision(), ArticleModState.APPROVED.getValue());
        mvc.perform(post(moderateUrl))
            .andExpect(status().is2xxSuccessful());
        List<ArticleModerationResult> moderationResults = articleModerationService.getArticleModerationStatuses(Collections.singletonList(articleId));
        Assert.assertEquals("Size must be equal 1", 1, moderationResults.size());
        ArticleModerationResult articleModerationResult = moderationResults.get(0);
        Assert.assertEquals(article.getId(), articleModerationResult.getId());
        Assert.assertEquals(articleId, articleModerationResult.getArticleId());
        Assert.assertEquals(revision, articleModerationResult.getRevision());
        Assert.assertEquals(ArticleModState.APPROVED, articleModerationResult.getArticleModState());
        Assert.assertNull(articleModerationResult.getReason());
        Assert.assertNull(articleModerationResult.getRecommendation());
        Long published = pgJdbcTemplate.queryForObject("select published from article_published where article_moderation_id = ?", Long.class, article.getId());
        Assert.assertNull(published);
    }

    @Test
    @WithUserDetails(userDetailsServiceBeanName = "persDebugUserDetailsService", value = "spbtester")
    public void testRejectedModerationArticle() throws Exception {
        long articleId = 1;
        int revision = 1;
        int reasonId = 0;
        List<Pair<String, String>> expectedRejectedReasons = pgJdbcTemplate.query(
            "select name, recommendation from article_moderation_reason where id = ?",
            (rs, rowNum) -> Pair.of(rs.getString("name"), rs.getString("recommendation")),
            reasonId);
        String reason = expectedRejectedReasons.get(0).getLeft();
        String recommendation = expectedRejectedReasons.get(0).getRight();
        articleModerationService.startArticleModeration(articleId, revision, FAKE_USER_ID);
        List<Article> result = objectMapper.readValue(
            mvc.perform(get("/api/article/moderation?_user_id=" + FAKE_MODERATOR_ID)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), new TypeReference<List<Article>>() {
            });
        Assert.assertEquals("Size must be equal 1", 1, result.size());
        Article article = result.get(0);
        String moderateUrl = String.format("/api/article/moderate?_user_id=%s&id=%d&articleId=%d&revision=%d&state=%d&reason_id=%d",
            FAKE_MODERATOR_ID, article.getId(), article.getArticleId(), article.getRevision(), ArticleModState.REJECTED.getValue(), reasonId);
        mvc.perform(post(moderateUrl))
            .andExpect(status().is2xxSuccessful());
        List<ArticleModerationResult> moderationResults = articleModerationService.getArticleModerationStatuses(Collections.singletonList(articleId));
        Assert.assertEquals("Size must be equal 1", 1, moderationResults.size());
        ArticleModerationResult articleModerationResult = moderationResults.get(0);
        Assert.assertEquals(article.getId(), articleModerationResult.getId());
        Assert.assertEquals(articleId, articleModerationResult.getArticleId());
        Assert.assertEquals(revision, articleModerationResult.getRevision());
        Assert.assertEquals(ArticleModState.REJECTED, articleModerationResult.getArticleModState());
        Assert.assertEquals(reason, articleModerationResult.getReason());
        Assert.assertEquals(recommendation, articleModerationResult.getRecommendation());
        long exist = pgJdbcTemplate.queryForObject("select count(*) from article_published where article_moderation_id = ?", Long.class, article.getId());
        Assert.assertEquals("There is row in article_published for this article", 0, exist);
    }

    @Test
    public void testCount() throws Exception {
        Count result = getCount();
        Assert.assertEquals(0, result.getCount());

        int revision1 = 1;
        int articleId1 = 1;
        PersCoreMockFactory.brokenRestTemplate(previewPublishRestTemplate);
        articleModerationService.startArticleModeration(articleId1, revision1, FAKE_USER_ID);

        result = getCount();
        Assert.assertEquals(0, result.getCount());

        int revision2 = 1;
        int articleId2 = 2;
        PersCoreMockFactory.goodPreviewPublishRestTemplate(previewPublishRestTemplate);
        articleModerationService.startArticleModeration(articleId2, revision2, FAKE_USER_ID);

        result = getCount();
        Assert.assertEquals(1, result.getCount());
    }

    private Count getCount() throws Exception {
        return objectMapper.readValue(
            mvc.perform(get("/api/article/moderation/count")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), Count.class);
    }

}
