package ru.yandex.market.pers.history.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;

import ru.yandex.market.pers.history.MockedDbTest;
import ru.yandex.market.pers.views.ArticleItem;
import ru.yandex.market.pers.views.ArticleStatResponse;
import ru.yandex.market.pers.views.AuthorViewsStatsResponse;
import ru.yandex.market.saas.search.SaasKvSearchRequest;
import ru.yandex.market.saas.search.SaasKvSearchService;
import ru.yandex.market.saas.search.response.SaasSearchResponse;
import ru.yandex.market.util.FormatUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ViewsControllerTest extends MockedDbTest {

    private static final Long ARTICLE_ID_1 = 12345L;
    private static final Long ARTICLE_ID_2 = 123456L;
    private static final Long ARTICLE_ID_3 = 1234567L;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SaasKvSearchService authorKvSearchService;

    @Test
    public void testSingleArticle() throws Exception {
        assertGetResponse(ARTICLE_ID_1, 0);

        updateAndCheckStat(ARTICLE_ID_1, 1);
        assertGetResponse(ARTICLE_ID_1, 1);

        updateAndCheckStat(ARTICLE_ID_1, 2);
        assertGetResponse(ARTICLE_ID_1, 2);
    }

    @Test
    public void testManyArticles() throws Exception {
        assertGetResponse(
            new ArticleItem(ARTICLE_ID_1, 0L),
            new ArticleItem(ARTICLE_ID_2, 0L),
            new ArticleItem(ARTICLE_ID_3, 0L)
        );

        updateAndCheckStat(ARTICLE_ID_1, 1);

        assertGetResponse(
            new ArticleItem(ARTICLE_ID_1, 1L),
            new ArticleItem(ARTICLE_ID_2, 0L),
            new ArticleItem(ARTICLE_ID_3, 0L)
        );

        updateAndCheckStat(ARTICLE_ID_1, 2);
        updateAndCheckStat(ARTICLE_ID_2, 1);

        assertGetResponse(
            new ArticleItem(ARTICLE_ID_1, 2L),
            new ArticleItem(ARTICLE_ID_2, 1L),
            new ArticleItem(ARTICLE_ID_3, 0L)
        );
    }

    @Test
    public void testArticleUpdateStat() throws Exception {
        updateAndCheckStat(ARTICLE_ID_1, 1);
        updateAndCheckStat(ARTICLE_ID_1, 2);
        updateAndCheckStat(ARTICLE_ID_1, 3);
    }

    @Test
    public void testAuthorControllerSimple() throws Exception {
        checkAuthorStats("/data/saas_kv_simple_response.json",
            new AuthorViewsStatsResponse(
                42, 42,
                0, 0
            ));
    }

    @Test
    public void testAuthorControllerFull() throws Exception {
        checkAuthorStats("/data/saas_kv_full_response.json",
            new AuthorViewsStatsResponse(
                452, 3,
                555, 3
            ));
    }

    @Test
    public void testAuthorControllerEmpty() throws Exception {
        checkAuthorStats("/data/saas_kv_empty_response.json",
            new AuthorViewsStatsResponse(
                0, 0,
                0, 0
            ));
    }

    private void checkAuthorStats(String path, AuthorViewsStatsResponse expected) throws Exception {
        SaasSearchResponse preparedResponse = FormatUtils.fromJson(
            IOUtils.toString(getClass().getResourceAsStream(path)),
            SaasSearchResponse.class);
        when(authorKvSearchService.search(any(SaasKvSearchRequest.class))).thenReturn(preparedResponse);

        long authorId = 3141;

        String response = mockMvc.perform(get(String.format("/views/author/UID/%s", authorId)))
            .andDo(print())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        AuthorViewsStatsResponse responseData = FormatUtils.fromJson(response, AuthorViewsStatsResponse.class);
        assertEquals(expected.getViewsCount(), responseData.getViewsCount());
        assertEquals(expected.getDeltaViewsCount(), responseData.getDeltaViewsCount());
        assertEquals(expected.getLikesCount(), responseData.getLikesCount());
        assertEquals(expected.getDeltaLikesCount(), responseData.getDeltaLikesCount());
    }

    private void assertGetResponse(ArticleItem... articles) throws Exception {
        final String[] ids = Stream.of(articles).map(it -> String.valueOf(it.getId())).toArray(String[]::new);
        assertResponse(
            get("/views/article")
                .param("id", ids),
            getStatResponse(articles));
    }

    private void assertGetResponse(long articleId, long count) throws Exception {
        assertGetResponse(new ArticleItem(articleId, count));
    }

    private void updateAndCheckStat(long articleId, long expectedCount) throws Exception {
        mockMvc.perform(post(String.format("/views/article/%s", articleId)))
            .andDo(print())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(content().json(getUpdateResponse(articleId, expectedCount)))
            .andExpect(status().isOk());
    }

    private void assertResponse(RequestBuilder requestBuilder, String response) throws Exception {
        mockMvc.perform(requestBuilder)
            .andDo(print())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(content().json(response))
            .andExpect(status().isOk());
    }

    private static String getStatResponse(ArticleItem... articles) throws JsonProcessingException {
        List<ArticleItem> list = new ArrayList<>();
        Collections.addAll(list, articles);
        return objectMapper.writeValueAsString(new ArticleStatResponse(list));
    }

    private static String getUpdateResponse(long articleId, long count) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new ArticleItem(articleId, count));
    }
}
