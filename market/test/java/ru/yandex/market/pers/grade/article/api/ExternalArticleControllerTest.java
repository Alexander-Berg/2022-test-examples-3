package ru.yandex.market.pers.grade.article.api;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.db.DbUtil;
import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.core.article.model.ArticleModState;
import ru.yandex.market.pers.grade.core.article.model.ArticleModerationResult;
import ru.yandex.market.pers.grade.core.article.model.ArticleState;
import ru.yandex.market.pers.grade.core.mock.PersCoreMockFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("production")
public class ExternalArticleControllerTest extends MockedPersGradeTest {
    protected static long FAKE_USER = -123456789;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    @Qualifier("previewPublishRestTemplate")
    RestTemplate restTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void getStatusesBadRequest() throws Exception {
        String response = mockMvc.perform(get("/api/article/moderation"))
            .andExpect(status().is4xxClientError())
            .andReturn().getResponse().getContentAsString();
        String expected = "Required long[] parameter 'articleId' is not present";
        Assert.assertTrue(expected, response.contains(expected));
    }

    @Test
    public void getStatusesBadRequestTooManyArticleIds() throws Exception {
        String articleIdsString = String.join("&", Collections.nCopies(101, "articleId=1"));
        String response = mockMvc.perform(get("/api/article/moderation?" + articleIdsString))
            .andDo(print())
            .andExpect(status().is4xxClientError())
            .andReturn().getResponse().getContentAsString();
        String expected = "Too many articleIds";
        Assert.assertTrue(expected, response.contains(expected));
    }

    @Test
    public void getStatusesOnEmptyModeration() throws Exception {
        int articleId = 1;
        int fakeArticleId = 100500;
        List<ArticleModerationResult> result = objectMapper.readValue(
            mockMvc.perform(get("/api/article/moderation?articleId=" + articleId + "&articleId=" + fakeArticleId))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<List<ArticleModerationResult>>() {
            });
        Assert.assertEquals(result.size(), 0);
    }

    @Test
    public void startAndStopArticleModeration() throws Exception {
        int articleId = 1;
        int revision = 0;
        PersCoreMockFactory.brokenRestTemplate(restTemplate);
        List<Integer> states = pgJdbcTemplate.queryForList("select state from article_moderation where article_id = ? and revision = ?", Integer.class, articleId, revision);
        Assert.assertTrue("Must be empty", states.isEmpty());
        List<ArticleModerationResult> result = objectMapper.readValue(
            mockMvc.perform(get("/api/article/moderation?articleId=" + articleId))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<List<ArticleModerationResult>>() {
            });
        Assert.assertEquals(0, result.size());
        mockMvc.perform(post("/api/article/moderation?articleId=" + articleId + "&revision=" + revision))
            .andDo(print())
            .andExpect(status().is2xxSuccessful());
        List<Pair<Integer, Integer>> statesAndPreviewPublish = pgJdbcTemplate.query(
            "select state, preview_created from article_moderation where article_id = ? and revision = ?",
            (rs, rowNum) -> Pair.of(rs.getInt(1), DbUtil.getInteger(rs, 2)),
            articleId, revision);
        Assert.assertEquals("One element expected", 1, statesAndPreviewPublish.size());
        Assert.assertEquals(ArticleState.ACTUAL.getValue(), (int) statesAndPreviewPublish.get(0).getLeft());
        Assert.assertNull(statesAndPreviewPublish.get(0).getRight());
        result = objectMapper.readValue(
            mockMvc.perform(get("/api/article/moderation?articleId=" + articleId))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<List<ArticleModerationResult>>() {
            });
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(articleId, result.get(0).getArticleId());

        mockMvc.perform(put("/api/article/moderation/stop?articleId=" + articleId))
            .andDo(print())
            .andExpect(status().is2xxSuccessful());
        states = pgJdbcTemplate.queryForList("select state from article_moderation where article_id = ? and revision = ?", Integer.class, articleId, revision);
        Assert.assertEquals("One element expected", 1, states.size());
        Assert.assertEquals(ArticleState.DELETED.getValue(), (int) states.get(0));
    }

    @Test
    public void startModerationWithSuccessPreviewCreation() throws Exception {
        int articleId = 1;
        int revision = 0;
        mockMvc.perform(post("/api/article/moderation?articleId=" + articleId + "&revision=" + revision))
            .andDo(print())
            .andExpect(status().is2xxSuccessful());
        List<Pair<Integer, Integer>> statesAndPreviewPublish = pgJdbcTemplate.query(
            "select state, preview_created from article_moderation where article_id = ? and revision = ?",
            (rs, rowNum) -> Pair.of(rs.getInt(1), DbUtil.getInteger(rs, 2)),
            articleId, revision);
        Assert.assertEquals("One element expected", 1, statesAndPreviewPublish.size());
        Assert.assertEquals(ArticleState.ACTUAL.getValue(), (int) statesAndPreviewPublish.get(0).getLeft());
        Assert.assertEquals(1, (int) statesAndPreviewPublish.get(0).getRight());
    }

    @Test
    public void retryOnStartModeration() throws Exception {
        int articleId = 1;
        int fakeArticleId = 100500;
        int revision = 0;
        List<ArticleModerationResult> result = objectMapper.readValue(
            mockMvc.perform(get("/api/article/moderation?articleId=" + articleId + "&articleId=" + fakeArticleId))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<List<ArticleModerationResult>>() {
            });
        Assert.assertTrue("Must be empty", result.isEmpty());
        mockMvc.perform(post("/api/article/moderation?articleId=" + articleId + "&revision=" + revision))
            .andExpect(status().is2xxSuccessful());
        result = objectMapper.readValue(
            mockMvc.perform(get("/api/article/moderation?articleId=" + articleId + "&articleId=" + fakeArticleId))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<List<ArticleModerationResult>>() {
            });
        Assert.assertEquals("One element expected", 1, result.size());
        Assert.assertEquals(ArticleModState.NEW.getValue(), result.get(0).getArticleModState().getValue());
        mockMvc.perform(post("/api/article/moderation?articleId=" + articleId + "&revision=" + revision))
            .andDo(print())
            .andExpect(status().is2xxSuccessful());
        result = objectMapper.readValue(
            mockMvc.perform(get("/api/article/moderation?articleId=" + articleId + "&articleId=" + fakeArticleId))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<List<ArticleModerationResult>>() {
            });
        Assert.assertEquals("One element expected", 1, result.size());
        Assert.assertEquals(ArticleModState.NEW.getValue(), result.get(0).getArticleModState().getValue());
    }

    @Test
    public void startModerationWithRevisionLessThanAlreadyOnModeration() throws Exception {
        int articleId = 1;
        int revision1 = 2;
        int revision2 = 1;
        mockMvc.perform(
            post("/api/article/moderation")
                .param("articleId", String.valueOf(articleId))
                .param("revision", String.valueOf(revision1)))
            .andExpect(status().is2xxSuccessful());
        String response = mockMvc.perform(
            post("/api/article/moderation")
                .param("articleId", String.valueOf(articleId))
                .param("revision", String.valueOf(revision2)))
            .andExpect(status().is4xxClientError())
            .andReturn().getResponse().getContentAsString();
        Assert.assertTrue(response.contains(String.format("Illegal revision %d. Article with revision %d already on moderation", revision2, revision1)));
    }

}
