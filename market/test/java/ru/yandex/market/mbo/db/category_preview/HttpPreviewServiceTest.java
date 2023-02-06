package ru.yandex.market.mbo.db.category_preview;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.common.util.http.HttpClientFactory;
import ru.yandex.market.mbo.db.category_preview.indexer.HttpPreviewService;
import ru.yandex.market.mbo.db.category_preview.indexer.IndexerTypeResolver;
import ru.yandex.market.mbo.db.category_preview.indexer.PreviewUrlBuilder;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.gwt.models.category_preview.CategoryPreview;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author york
 * @since 14.04.2017
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class HttpPreviewServiceTest {

    private static final String FAIL_SET_CATEGORIES_URL = "!fail!";
    @Mock
    private PreviewUrlBuilder activeBuilder1;
    @Mock
    private PreviewUrlBuilder activeBuilder2;
    @Mock
    private PreviewUrlBuilder activeThrowingBuilder1;
    @Mock
    private PreviewUrlBuilder inactiveBuilder;
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpClientFactory httpClientFactory;
    @Mock
    private HttpResponse setCategories;
    @Mock
    private HttpResponse getCategoryStatus;
    @Mock
    private HttpResponse getGenerationStatus;
    @Mock
    private HttpGet get;
    @Mock
    private HttpGet failGet;

    @Before
    public void init() throws IOException {
        when(activeBuilder1.getIndexerType()).thenReturn("1");
        when(activeBuilder1.isActive()).thenReturn(true);

        when(activeThrowingBuilder1.getIndexerType()).thenReturn("1");
        when(activeThrowingBuilder1.isActive()).thenReturn(true);
        when(activeThrowingBuilder1.buildSetCategoriesUrl(anyCollection())).thenReturn(FAIL_SET_CATEGORIES_URL);

        when(activeBuilder2.getIndexerType()).thenReturn("2");
        when(activeBuilder2.isActive()).thenReturn(true);

        when(inactiveBuilder.getIndexerType()).thenReturn("3");
        when(inactiveBuilder.isActive()).thenReturn(false);

        doReturn(failGet).when(httpClientFactory).createGetMethod(ArgumentMatchers.eq(FAIL_SET_CATEGORIES_URL));
        doReturn(get).when(httpClientFactory).createGetMethod(
            AdditionalMatchers.not(ArgumentMatchers.eq(FAIL_SET_CATEGORIES_URL)));

        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        when(setCategories.getStatusLine()).thenReturn(statusLine);
        when(getCategoryStatus.getStatusLine()).thenReturn(statusLine);
        when(getGenerationStatus.getStatusLine()).thenReturn(statusLine);

        String setCategoriesRespStr = "{}";
        HttpEntity setCategoriesResp = mockHttpEntity(setCategoriesRespStr);
        when(setCategories.getEntity()).thenReturn(setCategoriesResp);

        String getCategoryStatusRespStr = "{\"status\": \"unknown\", \"category_id\": 123}";
        HttpEntity getCategoryStatusResp = mockHttpEntity(getCategoryStatusRespStr);
        when(getCategoryStatus.getEntity()).thenReturn(getCategoryStatusResp);

        String getGenerationStatusRespStr = "[ {\"name\": \"20161102_1836\", " +
            "\"status\": \"completed\", " +
            "\"categories\": [] " +
            "} ]";
//            "{ \"categoryId\": 12345, \"timestamp\": 543125 } ] } ]";
        HttpEntity getGenerationStatusResp = mockHttpEntity(getGenerationStatusRespStr);
        when(getGenerationStatus.getEntity()).thenReturn(getGenerationStatusResp);
    }


    @Test
    public void testPreviewUrlBuilder() throws Exception {
        PreviewUrlBuilder builder = new PreviewUrlBuilder();
        builder.setIndexerType("");
        builder.setCategoryStatusUrl("");
        builder.setGenerationStatusUrl("rerew");
        builder.setSetCategoriesUrl("aaabn");
        builder.afterPropertiesSet();

        Assert.assertFalse(builder.isActive());

        builder.setIndexerType("telecaster");
        builder.setCategoryStatusUrl("eererwr");
        builder.setGenerationStatusUrl("rerew");
        builder.setSetCategoriesUrl("aaabn");
        builder.afterPropertiesSet();

        Assert.assertTrue(builder.isActive());
    }

    @Test
    public void testHttpPreviewGetCategories() throws IOException {
        HttpPreviewService service = testGetsGoesToMasterInit(getCategoryStatus);

        service.getCategoryStatus(100L);
        verify(activeBuilder1, never()).buildGetCategoryStatusUrl(anyLong());
        verify(activeBuilder2, times(1)).buildGetCategoryStatusUrl(anyLong());
        verify(inactiveBuilder, never()).buildGetCategoryStatusUrl(anyLong());
    }

    @Test
    public void testHttpPreviewGetGenerationStatus() throws IOException {
        HttpPreviewService service = testGetsGoesToMasterInit(getGenerationStatus);
        service.getGenerationStatus("gen");
        verify(activeBuilder1, never()).buildGetGenerationStatusUrl(anyString());
        verify(activeBuilder2, times(1)).buildGetGenerationStatusUrl(anyString());
        verify(inactiveBuilder, never()).buildGetGenerationStatusUrl(anyString());
    }

    @Test
    public void testHttpPreviewSetCategories() throws IOException {
        IndexerTypeResolver indexerTypeResolver = mock(IndexerTypeResolver.class);
        when(indexerTypeResolver.getIndexerType()).thenReturn("2");
        HttpPreviewService service = initPreviewService(indexerTypeResolver,
            Arrays.asList(activeBuilder1, activeBuilder2, inactiveBuilder));

        doReturn(setCategories).when(httpClient).execute(any());

        ReflectionTestUtils.setField(service, "httpClient", httpClient);

        //request goes to all
        service.setCategories(Collections.singletonList(new CategoryPreview(100L, System.currentTimeMillis())));
        verify(indexerTypeResolver).getIndexerType();
        verify(activeBuilder1, times(1)).buildSetCategoriesUrl(anyCollection());
        verify(activeBuilder2, times(1)).buildSetCategoriesUrl(anyCollection());
        verify(inactiveBuilder, never()).buildSetCategoriesUrl(anyCollection());
    }

    @Test
    public void testHttpPreviewSetCategoriesFail() throws IOException {
        IndexerTypeResolver indexerTypeResolver = mock(IndexerTypeResolver.class);
        when(indexerTypeResolver.getIndexerType()).thenReturn("1");
        HttpPreviewService service = initPreviewService(indexerTypeResolver,
            Arrays.asList(activeThrowingBuilder1, activeBuilder2, inactiveBuilder));

        doReturn(setCategories).when(httpClient).execute(ArgumentMatchers.eq(get));
        doThrow(new OperationException("1")).when(httpClient).execute(ArgumentMatchers.eq(failGet));
        ReflectionTestUtils.setField(service, "httpClient", httpClient);

        boolean thrown = false;
        try {
            service.setCategories(Collections.singletonList(new CategoryPreview(100L, System.currentTimeMillis())));
        } catch (OperationException op) {
            thrown = true;
        }
        Assert.assertTrue(thrown);
        verify(activeThrowingBuilder1, times(1)).buildSetCategoriesUrl(anyCollection());
        verify(activeBuilder2, times(1)).buildSetCategoriesUrl(anyCollection());
        verify(inactiveBuilder, never()).buildSetCategoriesUrl(anyCollection());
    }


    @Test(expected = OperationException.class)
    public void testHttpPreviewUnknownIndexer() {
        IndexerTypeResolver indexerTypeResolver = mock(IndexerTypeResolver.class);
        when(indexerTypeResolver.getIndexerType()).thenReturn("5");

        HttpPreviewService service = initPreviewService(indexerTypeResolver,
            Arrays.asList(activeBuilder1, activeBuilder2, inactiveBuilder));

        service.getCategoryStatus(100L);
    }


    private HttpPreviewService testGetsGoesToMasterInit(HttpResponse response)
            throws IOException {
        IndexerTypeResolver indexerTypeResolver = mock(IndexerTypeResolver.class);
        when(indexerTypeResolver.getIndexerType()).thenReturn("2");
        HttpPreviewService service = initPreviewService(indexerTypeResolver,
            Arrays.asList(activeBuilder1, activeBuilder2, inactiveBuilder));

        doReturn(response).when(httpClient).execute(any());
        ReflectionTestUtils.setField(service, "httpClient", httpClient);
        return service;
    }


    private HttpPreviewService initPreviewService(IndexerTypeResolver indexerTypeResolver,
                                                  List<PreviewUrlBuilder> builders) {
        HttpPreviewService service = new HttpPreviewService();
        ReflectionTestUtils.setField(service, "indexerTypeResolver", indexerTypeResolver);
        ReflectionTestUtils.setField(service, "httpClientFactory", httpClientFactory);
        service.setUrlBuilders(builders);
        ReflectionTestUtils.setField(service, "httpClient", httpClient);
        return service;
    }

    private HttpEntity mockHttpEntity(String responseStr) throws IOException {
        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(
            new ByteArrayInputStream(responseStr.getBytes(StandardCharsets.UTF_8)));
        return entity;
    }
}
