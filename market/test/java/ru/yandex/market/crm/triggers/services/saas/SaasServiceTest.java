package ru.yandex.market.crm.triggers.services.saas;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SaasServiceTest {

    private SaasService service;

    @Mock
    private SaasClient client;

    @Before
    public void before() {
        service = new SaasService(client);
    }

    @Test
    public void testNoReviewers() {
        mockClientResponse("id", 0, emptyList());
        Set<Long> result = service.getModelAllReviewersUids("id");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSinglePageRequest() {
        List<Long> expectedUids = asList(1L, 2L, 3L);
        mockClientResponse("id", 0, expectedUids);
        Set<Long> result = service.getModelAllReviewersUids("id");
        assertEquals(new HashSet<>(expectedUids), result);
    }

    /**
     * Проверяем случай большого количества отзывов о модели
     */
    @Test
    public void testTwoPageRequest() {
        List<Long> expectedUids =
                LongStream.range(0, SaasService.ONE_PAGE_RESULT_SIZE).boxed().collect(Collectors.toList());
        mockClientResponse("id", 0, expectedUids);
        mockClientResponse("id", 1, singletonList(501L));

        Set<Long> result = service.getModelAllReviewersUids("id");
        Set<Long> expected = new HashSet<>(expectedUids);
        expected.add(501L);

        verify(client, times(2)).getModelReviewersUids(eq("id"), anyInt(),
                Matchers.eq(SaasService.ONE_PAGE_RESULT_SIZE));
        assertEquals(expected, result);
    }

    private void mockClientResponse(String modelId, int page, List<Long> reviewerUids) {
        LongArraySet response = new LongArraySet(reviewerUids);
        when(client.getModelReviewersUids(modelId, page, SaasService.ONE_PAGE_RESULT_SIZE)).thenReturn(response);
    }
}