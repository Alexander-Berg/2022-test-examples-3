package ru.yandex.market.markup2.utils.report;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.utils.cards.CardType;
import ru.yandex.market.markup2.utils.top.QueriesType;
import ru.yandex.market.markup2.utils.top.SearchQueryInfo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author inenakhov
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ReportServiceTest {
    private static final long CATEGORY_ID = 13475138L;
    private static final String CATEGORY_NAME = "Хранение грудного молока";
    private static final String QUERY = "query";
    private static final String GLFILTER = "13490657%3A13490658";
    private static final int PAGE = 4;
    private static final int SERP_SIZE = 12;

    private ReportService reportService;
    private RemoteReportServiceMock remoteReportServiceMock = new RemoteReportServiceMock();

    @Before
    public void setup() throws Exception {
        reportService = spy(new ReportService());
        reportService.setReportSearchUrl("http://host");
        reportService.afterPropertiesSet();
    }

    @Test
    public void testGetSerp() throws Exception {
        doReturn(remoteReportServiceMock.getSerp()).when(reportService).requestRemoteService(any());

        SearchQueryInfo searchQueryInfo = new SearchQueryInfo(QUERY, CATEGORY_ID, QueriesType.SEARCH);
        Serp serpSearch = reportService.getSerp(searchQueryInfo, SERP_SIZE, false);
        assertEquals(serpSearch.getCategoryId(), CATEGORY_ID);
        assertEquals(CATEGORY_NAME, serpSearch.getCategoryName());
        assertEquals(4, serpSearch.getCards().size());
        assertEquals(1, serpSearch.getOffersIds().size());

        SearchQueryInfo filterSearchQueryInfo = new SearchQueryInfo(QUERY, CATEGORY_ID, GLFILTER, QueriesType.FILTERS);
        Serp filtersSerp  = reportService.getSerp(filterSearchQueryInfo, SERP_SIZE, false);
        assertEquals(filtersSerp.getCategoryId(), CATEGORY_ID);
        assertEquals(filtersSerp.getCategoryName(), CATEGORY_NAME);

        SearchQueryInfo pageSearchQueryInfo = new SearchQueryInfo(CATEGORY_ID, PAGE, QueriesType.CATEGORY_PAGES);
        Serp pageSerp = reportService.getSerp(pageSearchQueryInfo, SERP_SIZE, false);
        assertEquals(pageSerp.getCategoryId(), CATEGORY_ID);
        assertEquals(pageSerp.getCategoryName(), CATEGORY_NAME);

        ReportCard reportCard = serpSearch.getCards().get(0);
        assertEquals(CardType.MODEL, reportCard.getType());
        assertEquals(CATEGORY_ID, reportCard.getCategoryId());
        assertEquals(CATEGORY_NAME, reportCard.getCategoryName());
        assertEquals("Philips AVENT Контейнеры 180 мл (SCF618)", reportCard.getTitle());
        assertEquals("контейнер 180 мл, не содержит бисфенол А, особенности: отметка о содержимом, " +
                         "подходит для СВЧ, можно мыть в посудомоечной машине", reportCard.getDescription());
        assertEquals("https://avatars.mds.yandex.net/get-mpic/200316/img_id2561870271571394815/orig",
                     reportCard.getImageUrl());
        assertEquals(Long.valueOf(1722190264), reportCard.getId());

        assertTrue(serpSearch.getOffersIds().contains("f94198526596d9d6eeae184b1dea696b"));
    }

    @Test
    public void testFindWithPics() throws Exception {
        doReturn(remoteReportServiceMock.getFindModels()).when(reportService).requestRemoteService(any());

        List<ReportCard> reportCards = reportService.findWithPics(Lists.newArrayList(1L, 2L));

        assertEquals(2, reportCards.size());
        ReportCard reportCard = reportCards.get(0);
        assertEquals(CardType.MODEL, reportCard.getType());
        assertEquals(91491, reportCard.getCategoryId());
        assertEquals("Мобильные телефоны", reportCard.getCategoryName());
        assertEquals("Apple iPhone 5S 16Gb", reportCard.getTitle());
        assertEquals("GSM, LTE, смартфон, iOS 7, вес 112 г, ШхВхТ 58.6x123.8x7.6 мм, экран 4\", 1136x640, " +
                         "Bluetooth, Wi-Fi, GPS, ГЛОНАСС, фотокамера 8 МП, память 16 Гб, аккумулятор 1560 мА⋅ч",
                     reportCard.getDescription());
        assertEquals("https://avatars.mds.yandex.net/get-mpic/96484/img_id9003833516339462315/orig",
                     reportCard.getImageUrl());
        assertEquals(Long.valueOf(10495456), reportCard.getId());


        //Test batches
        List<Long> modelIds = LongStream.rangeClosed(0, 100).boxed().collect(Collectors.toList());
        reportService.findWithPics(modelIds);
        verify(reportService, atLeast(2)).requestRemoteService(any());
    }

    /**
     * Этот тест использует файлик "report_find_model_response_no_pics.json", который есть производная
     * файлика "report_find_model_response.json". Из него оставлена только первая сущность "Apple iPhone 5S 16Gb",
     * которая далее откопирована трижды.
     * 1. (id 104954561) "оригинальный"  объект
     * 2. (id 104954562) "оригинальный", где каждый "url" = "" и нет "thumbnails"
     * 3. (id 104954563) "оригинальный", где нет "pictures" и нет "thumbnails"
     * 4. (id 104954564) "оригинальный", где каждый "pictures" = ""
     * и нет ключа "thumbnails"
     * 5. (id 104954565) "оригинальный", где первая pictures пустая и нет "thumbnails" (пустой массив)
     * 6. (id 104954566) "оригинальный", где первая pictures пустая и нет "thumbnails" (пустой url)
     * 7. (id 104954566) "оригинальный", где первая pictures пустая и нет "thumbnails" (нет url)
     * 8. (id 104954565) "оригинальный", где нет original, но есть thumbnails
     * 9. (id 104954566) "оригинальный", где нет original, первый thumbnails битый, а второй нормальный.
     * 10.(id 104954567) "оригинальный", нет original, все thumbnails начинаются не с
     *      //avatars.mds.yandex.net кроме одного
     *
     * @throws Exception
     */
    @Test
    public void testFindWithPicsNoPiscExpected() throws Exception {
        doReturn(remoteReportServiceMock.getFindModelsNoPics()).when(reportService).requestRemoteService(any());

        List<ReportCard> reportCards = reportService.findWithPics(Lists.newArrayList(1L, 2L));

        assertEquals(4, reportCards.size());
        assertTrue(104954561L == reportCards.get(0).getId());
        assertEquals("https://avatars.mds.yandex.net/get-mpic/96484/img_id9003833516339462315/orig",
                     reportCards.get(0).getImageUrl());

        assertTrue(104954568L == reportCards.get(1).getId());
        assertEquals("https://avatars.mds.yandex.net/get-mpic/96484/img_id9003833516339462315/orig",
                     reportCards.get(1).getImageUrl());

        assertTrue(104954569L == reportCards.get(2).getId());
        assertEquals("https://avatars.mds.yandex.net/get-mpic/96484/img_id9003833516339462315/orig",
                     reportCards.get(2).getImageUrl());

        assertTrue(104954570L == reportCards.get(3).getId());
        assertEquals("https://avatars.mds.yandex.net/get-mpic/96484/img_id9003833516339462315/orig",
                     reportCards.get(3).getImageUrl());
    }

    @Test
    public void testFindPicUrls() throws Exception {
        doReturn(remoteReportServiceMock.getFindPicUrls()).when(reportService).requestRemoteService(any());

        Map<String, String> picUrls = reportService.findPicUrls(Lists.newArrayList("Qm0zAN0Bb-o60xLK8wAIFg",
                                                                                   "Aqegwl1UzCZEhr_SG4c-MA",
                                                                                   "mc6zdPq7OJ8sXolAKM3s-A",
                                                                                   "1"));

        //all pictures from thumbnails
        assertEquals(2, picUrls.size());
        assertEquals("https://avatars.mds.yandex.net/get-marketpic/406938/market_YrQdcP2u0pzoxFAgUJqSzg/orig",
                     picUrls.get("Qm0zAN0Bb-o60xLK8wAIFg"));
        assertEquals("https://avatars.mds.yandex.net/get-marketpic/174398/market_Rc9In7nG7geyn8xNcTcgww/orig",
                     picUrls.get("Aqegwl1UzCZEhr_SG4c-MA"));

        //Test batches
        List<Long> modelIds = LongStream.rangeClosed(0, 100).boxed().collect(Collectors.toList());
        reportService.findWithPics(modelIds);
        verify(reportService, atLeast(2)).requestRemoteService(any());
    }
}
