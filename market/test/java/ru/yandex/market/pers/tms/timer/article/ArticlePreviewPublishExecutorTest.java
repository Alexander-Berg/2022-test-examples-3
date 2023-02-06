package ru.yandex.market.pers.tms.timer.article;

import org.apache.http.client.HttpClient;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.pers.grade.core.article.service.ArticleModerationService;
import ru.yandex.market.pers.grade.core.article.service.PreviewCreatorService;
import ru.yandex.market.pers.grade.core.article.service.PreviewCreatorServiceImpl;
import ru.yandex.market.pers.grade.core.mock.PersCoreMockFactory;
import ru.yandex.market.pers.tms.MockedPersTmsTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.pers.grade.client.mock.HttpClientMockHelpers.mockResponseWithFile;

public class ArticlePreviewPublishExecutorTest extends MockedPersTmsTest {
    private static final Long FAKE_USER_ID = 123L;
    private static final long ID = 1323212L;
    private static final long ARTICLE_ID = 1323212234322L;
    private static final int TEST_ARTICLES_COUNT = 10;

    @Autowired
    ArticlePreviewPublishExecutor articlePreviewPublishExecutor;
    @Autowired
    ArticleModerationService articleModerationService;
    @Autowired
    @Qualifier("previewPublishRestTemplate")
    RestTemplate restTemplate;
    @Autowired
    ComplexMonitoring complexMonitoring;
    @Autowired
    private PreviewCreatorService previewCreatorService;

    private int countWithMissingPreview() {
        return pgJdbcTemplate.queryForObject("select count(*) from article_moderation where preview_created is null", Integer.class);
    }

    @Test
    public void test() throws Exception {
        Assert.assertEquals(0, countWithMissingPreview());
        startArticleModeration(TEST_ARTICLES_COUNT);
        Assert.assertEquals(TEST_ARTICLES_COUNT, countWithMissingPreview());
        PersCoreMockFactory.goodPreviewPublishRestTemplate(restTemplate);
        articlePreviewPublishExecutor.runTmsJob();
        Assert.assertEquals(0, countWithMissingPreview());
        Integer count = pgJdbcTemplate.queryForObject("select count(*) from article_moderation where preview_created = 1", Integer.class);
        Assert.assertEquals(TEST_ARTICLES_COUNT, (int) count);
    }

    @Test
    public void testBrokenRestTemplate() throws Exception {
        Assert.assertEquals(MonitoringStatus.OK, complexMonitoring.getResult().getStatus());
        startArticleModeration(TEST_ARTICLES_COUNT);
        articlePreviewPublishExecutor.runTmsJob();
        ComplexMonitoring.Result result = complexMonitoring.getResult();
        Assert.assertEquals(MonitoringStatus.CRITICAL, result.getStatus());
        Assert.assertTrue(result.getMessage().contains("Article moderations without published preview:"));
    }

    @Test
    public void testGoodBukerAndBrokenSaasRestTemplate() throws Exception {
        startArticleModeration(TEST_ARTICLES_COUNT);
        PersCoreMockFactory.goodBukerAndBrokenSaasRestTemplate(restTemplate);
        articlePreviewPublishExecutor.runTmsJob();
        Assert.assertEquals(TEST_ARTICLES_COUNT, countWithMissingPreview());
    }

    @Test
    public void testGoodSaasAndBrokenBukerRestTemplate() throws Exception {
        startArticleModeration(TEST_ARTICLES_COUNT);
        PersCoreMockFactory.goodSaasAndBrokenBukerRestTemplate(restTemplate);
        articlePreviewPublishExecutor.runTmsJob();
        Assert.assertEquals(TEST_ARTICLES_COUNT, countWithMissingPreview());
    }

    private void startArticleModeration(int N) {
        // Ломаем RestTemplate, чтобы не начали создаваться preview
        PersCoreMockFactory.brokenRestTemplate(restTemplate);
        for (int i = 0; i < N; i++) {
            articleModerationService.startArticleModeration(i, 0, FAKE_USER_ID);
        }
    }

    @Test
    public void testCreatePreviewSaasWithAllSuccess() {
        assertTrue(((PreviewCreatorServiceImpl)previewCreatorService)
                .createPreviewSaas(buildRestTemplateWithSaasResponse("all_success.json"), ID, ARTICLE_ID));
    }

    @Test
    public void testCreatePreviewSaasWithEmptyList() {
        assertTrue(((PreviewCreatorServiceImpl)previewCreatorService)
                .createPreviewSaas(buildRestTemplateWithSaasResponse("empty_list.json"), ID, ARTICLE_ID));
    }

    @Test
    public void testCreatePreviewSaasWithAnyNotSuccess() {
        assertFalse(((PreviewCreatorServiceImpl)previewCreatorService)
                .createPreviewSaas(buildRestTemplateWithSaasResponse("any_not_success.json"), ID, ARTICLE_ID));
    }

    private RestTemplate buildRestTemplateWithSaasResponse(String fileName) {
        HttpClient httpClient = mock(HttpClient.class);
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        mockResponseWithFile(httpClient, 200, String.format("/testdata/create_article_preview/saas_response/%s", fileName));
        return restTemplate;
    }
}
