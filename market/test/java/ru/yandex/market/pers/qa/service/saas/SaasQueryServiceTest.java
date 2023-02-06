package ru.yandex.market.pers.qa.service.saas;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pers.qa.PersQATest;
import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.client.model.SortField;
import ru.yandex.market.pers.qa.mock.SaasSearchMockUtils;
import ru.yandex.market.pers.qa.model.PageFilter;
import ru.yandex.market.pers.qa.model.ResultChunk;
import ru.yandex.market.pers.qa.model.ResultLimit;
import ru.yandex.market.pers.qa.model.Sort;
import ru.yandex.market.pers.qa.model.saas.SaasQuestionFilter;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;
import ru.yandex.market.util.ListUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pers.qa.model.QaEntityType.QUESTION;
import static ru.yandex.market.pers.qa.model.saas.SaasQaSearchAttribute.AUTHOR_ID;
import static ru.yandex.market.pers.qa.model.saas.SaasQaSearchAttribute.MODEL_ID;
import static ru.yandex.market.pers.qa.model.saas.SaasQaSearchAttribute.TYPE;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 26.12.2018
 */
public class SaasQueryServiceTest extends PersQATest {
    @Autowired
    private SaasQueryService saasQueryService;

    @Autowired
    @Qualifier("saasHttpClient")
    protected HttpClient saasHttpClientMock;

    @BeforeEach
    void init() {
        PersQaServiceMockFactory.resetMocks();
    }

    @Test
    void checkSaasIdList() {

        ArgumentMatcher<HttpUriRequest> matcher = HttpClientMockUtils.and(
            SaasSearchMockUtils.withSearchAttribute("url", "question-1504522"),
            SaasSearchMockUtils.withSearchAttribute("url", "question-1504523")
        );

        HttpClientMockUtils.mockResponseWithFile(
            saasHttpClientMock,
            "/saas/saas_response_questions_by_url.json",
            matcher);

        List<Long> result = saasQueryService.findExisting(QUESTION, Arrays.asList(1504522L, 1504523L));

        assertEquals(1, result.size());
        assertEquals(1504522, result.get(0).longValue());
    }

    @Test
    void checkModelQuestionsList() {
        final long modelId = 19295879561L;

        SaasQuestionFilter filter = new SaasQuestionFilter()
            .modelId(modelId);

        final long pageSize = 3;
        final long firstPage = 1;
        final long secondPage = 2;

        mockSaasModelIdCall(modelId, firstPage, pageSize, "/saas/saas_questions_by_model_19295879561.json");

        ResultChunk<Long> resultIds = saasQueryService.getQuestionIds(
            filter,
            Sort.desc(SortField.ID),
            PageFilter.firstPage(pageSize)
        );

        assertEquals(5, resultIds.getTotalCount());
        assertArrayEquals(new long[]{1413834L, 1409252L, 1408851L},
            ListUtils.toLongArray(resultIds.getData()));
    }

    @Test
    void checkModelQuestionsListPage2() {
        final long modelId = 19295879561L;

        SaasQuestionFilter filter = new SaasQuestionFilter()
            .modelId(modelId);

        final long pageSize = 3;
        final long firstPage = 1;
        final long secondPage = 2;

        mockSaasModelIdCall(modelId, secondPage, pageSize, "/saas/saas_questions_by_model_19295879561_page_2.json");

        ResultChunk<Long> resultIds = saasQueryService.getQuestionIds(
            filter,
            Sort.desc(SortField.ID),
            ResultLimit.page(secondPage, pageSize)
        );

        assertEquals(5, resultIds.getTotalCount());
        assertArrayEquals(new long[]{1307345L, 1303587L},
            ListUtils.toLongArray(resultIds.getData()));
    }

    @Test
    void checkModelQuestionsListShifted() {
        final long modelId = 19295879561L;

        SaasQuestionFilter filter = new SaasQuestionFilter()
            .modelId(modelId);

        final long pageSize = 3;
        final long firstPage = 1;
        final long secondPage = 2;

        mockSaasModelIdCall(modelId, firstPage, pageSize, "/saas/saas_questions_by_model_19295879561.json");
        mockSaasModelIdCall(modelId, secondPage, pageSize, "/saas/saas_questions_by_model_19295879561_page_2.json");

        ResultChunk<Long> resultIds = saasQueryService.getQuestionIds(
            filter,
            Sort.desc(SortField.ID),
            new ResultLimit(1L, pageSize)
        );

        assertEquals(5, resultIds.getTotalCount());
        assertArrayEquals(new long[]{1409252L, 1408851L, 1307345L},
            ListUtils.toLongArray(resultIds.getData()));
    }


    @Test
    void checkModelQuestionsListShiftedAnother() {
        final long modelId = 19295879561L;

        SaasQuestionFilter filter = new SaasQuestionFilter()
            .modelId(modelId);

        final long pageSize = 3;
        final long firstPage = 1;
        final long secondPage = 2;

        mockSaasModelIdCall(modelId, firstPage, pageSize, "/saas/saas_questions_by_model_19295879561.json");
        mockSaasModelIdCall(modelId, secondPage, pageSize, "/saas/saas_questions_by_model_19295879561_page_2.json");

        ResultChunk<Long> resultIds = saasQueryService.getQuestionIds(
            filter,
            Sort.desc(SortField.ID),
            new ResultLimit(2L, pageSize)
        );

        assertEquals(5, resultIds.getTotalCount());
        assertArrayEquals(new long[]{1408851L, 1307345L, 1303587L},
            ListUtils.toLongArray(resultIds.getData()));
    }

    @Test
    void checkModelQuestionsCount() {
        final long modelId = 19295879561L;

        SaasQuestionFilter filter = new SaasQuestionFilter()
            .modelId(modelId);
        PageFilter pageFilter = PageFilter.firstPage(1);

        mockSaasModelIdCall(modelId,
            pageFilter.getPage(),
            pageFilter.getSize(),
            "/saas/saas_count.json",
            x -> x.replace("_TOTAL_DOCS_", "3")
                .replace("_DOCS_", "3")
        );

        long count = saasQueryService.getQuestionsCount(filter);
        assertEquals(3, count);
    }

    @Test
    void checkModelQuestionsCountBad1() {
        final long modelId = 19295879561L;

        SaasQuestionFilter filter = new SaasQuestionFilter()
            .modelId(modelId);
        PageFilter pageFilter = PageFilter.firstPage(1);

        mockSaasModelIdCall(modelId,
            pageFilter.getPage(),
            pageFilter.getSize(),
            "/saas/saas_count.json",
            x -> x.replace("_TOTAL_DOCS_", "4")
                .replace("_DOCS_", "3")
        );

        long count = saasQueryService.getQuestionsCount(filter);
        assertEquals(3, count);
    }

    @Test
    void checkModelQuestionsExceptUidPage2() {
        final long modelId = 19295879561L;
        final long uid = 1234;

        SaasQuestionFilter filter = new SaasQuestionFilter()
            .modelId(modelId)
            .exceptUid(uid);

        final long pageSize = 3;
        final long page = 2;

        HttpClientMockUtils.mockResponse(
            saasHttpClientMock,
            req -> SaasSearchMockUtils.class
                .getResourceAsStream("/saas/saas_questions_by_model_19295879561_page_2.json"),
            HttpClientMockUtils.and(
                SaasSearchMockUtils.withSearchAttribute(MODEL_ID.getName(), String.valueOf(modelId)),
                SaasSearchMockUtils.withSearchAttribute(TYPE.getName(), String.valueOf(QUESTION.getValue())),
                SaasSearchMockUtils.withSearchExcludeAttribute(AUTHOR_ID.getName(), String.valueOf(uid)),
                SaasSearchMockUtils.pageFilter(page, pageSize)
            ));

        ResultChunk<Long> resultIds = saasQueryService.getQuestionIds(
            filter,
            Sort.desc(SortField.ID),
            ResultLimit.page(page, pageSize)
        );

        assertEquals(5, resultIds.getTotalCount());
        assertArrayEquals(new long[]{1307345L, 1303587L},
            ListUtils.toLongArray(resultIds.getData()));
    }

    @Test
    public void testPageCorrection() {
        // check works well on 1-st page
        assertEquals(0, SaasQueryService.getCorrectedResultCount(0, 10, 3, 0));
        assertEquals(1, SaasQueryService.getCorrectedResultCount(0, 10, 3, 1));
        assertEquals(3, SaasQueryService.getCorrectedResultCount(0, 10, 3, 3));
        assertEquals(8, SaasQueryService.getCorrectedResultCount(0, 10, 3, 8));
        assertEquals(10, SaasQueryService.getCorrectedResultCount(0, 10, 3, 10));
        assertEquals(30, SaasQueryService.getCorrectedResultCount(0, 10, 30, 10));
        assertEquals(9, SaasQueryService.getCorrectedResultCount(0, 10, 30, 9));

        // example why can't use this correction when there are more then one page. 15 results, counter = 30
        assertEquals(30, SaasQueryService.getCorrectedResultCount(0, 10, 30, 10));
        assertEquals(30, SaasQueryService.getCorrectedResultCount(1, 10, 30, 5));
        assertEquals(30, SaasQueryService.getCorrectedResultCount(2, 10, 30, 0));
        assertEquals(30, SaasQueryService.getCorrectedResultCountAnyPage(0, 10, 30, 10));
        assertEquals(15, SaasQueryService.getCorrectedResultCountAnyPage(1, 10, 30, 5));
        assertEquals(20, SaasQueryService.getCorrectedResultCountAnyPage(2, 10, 30, 0));

        // found 5 elements on 2-nd page, expected 8 elements at all. Actual visible elements 15
        assertEquals(8, SaasQueryService.getCorrectedResultCount(1, 10, 8, 5));
        assertEquals(15, SaasQueryService.getCorrectedResultCountAnyPage(1, 10, 8, 5));
    }

    private void mockSaasModelIdCall(long modelId, long page, long pageSize, String path) {
        mockSaasModelIdCall(modelId, page, pageSize, path, null);
    }

    private void mockSaasModelIdCall(long modelId,
                                     long page,
                                     long pageSize,
                                     String path,
                                     Function<String, String> correctFun) {
        HttpClientMockUtils.mockResponse(
            saasHttpClientMock,
            req -> {
                InputStream stream = HttpClientMockUtils.class.getResourceAsStream(path);
                if (correctFun == null) {
                    return stream;
                }

                return new ByteArrayInputStream(correctFun.apply(IOUtils.toString(stream)).getBytes());
            },
            HttpClientMockUtils.and(
                SaasSearchMockUtils.withSearchAttribute(MODEL_ID.getName(), String.valueOf(modelId)),
                SaasSearchMockUtils.pageFilter(page, pageSize)
            ));
    }
}
