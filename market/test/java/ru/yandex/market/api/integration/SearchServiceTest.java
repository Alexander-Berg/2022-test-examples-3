package ru.yandex.market.api.integration;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.Category;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.CategoryV2;
import ru.yandex.market.api.domain.v2.ResultFieldV2;
import ru.yandex.market.api.domain.v2.SearchPageInfo;
import ru.yandex.market.api.domain.v2.redirect.parameters.SearchQuery;
import ru.yandex.market.api.internal.report.InternalSearchResults;
import ru.yandex.market.api.internal.report.SearchForm;
import ru.yandex.market.api.model.UniversalModelSort;
import ru.yandex.market.api.search.SearchService;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * Created by vivg on 06.10.16.
 */
public class SearchServiceTest extends BaseTest {

    @Inject
    ReportTestClient reportTestClient;

    @Inject
    SearchService searchService;

    /**
     * Тестируем, что категории отдаем в порядке сооветвутствующем полю defaultOrder
     */
    @Test
    public void testOrderSearchCategories() {
        SearchForm.Builder s = SearchForm.builder()
            .setQuery(SearchQuery.text("iphone"))
            .setGenericParams(genericParams)
            .setPageInfo(new PageInfo(1, 10));

        reportTestClient.searchV2("iphone", "search_iphone_found_categories.json");

        InternalSearchResults result = Futures.waitAndGet(searchService.searchV2(
                Collections.singleton(ResultFieldV2.FOUND_CATEGORIES), s));

        List<? extends Category> categories = result.getCategories();

        Assert.assertEquals(TEST_ORDER_SEARCH_CATEGORIES_HIDS.length, categories.size());

        for (int i = 0; i < TEST_ORDER_SEARCH_CATEGORIES_HIDS.length; ++i) {
            Assert.assertEquals(TEST_ORDER_SEARCH_CATEGORIES_HIDS[i], categories.get(i).getId());
        }
    }

    /**
     * Тестируем, что отадем кол-во найденных моделей и офферов при фильтрации
     */
    @Test
    public void testPageInfoWithModelAndOfferCountWhenFilterV2() {
        reportTestClient.categorySearch(IntLists.singleton(91013), "filter_91013_total_2925.json");

        ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
            .put("7893318", "152955")
            .build();

        SearchForm.Builder sb = SearchForm.builder()
            .setCategories(IntLists.singleton(91013))
            .setFilterParams(params)
            .setSort(UniversalModelSort.POPULARITY_SORT)
            .setPageInfo(PageInfo.DEFAULT)
            .setGenericParams(genericParams);

        InternalSearchResults result = Futures.waitAndGet(
            searchService.filterV2(
                sb,
                Collections.emptyList()
            )
        );

        SearchPageInfo pageInfo = (SearchPageInfo) result.getItems().getPageInfo();

        Assert.assertEquals(pageInfo.getTotalItems(), 2925);
    }


    /**
     * Тестируем, что отадем кол-во найденных моделей и офферов при поиске
     */
    @Test
    public void testPageInfoWithModelAndOfferCountWhenSearchV2() {
        SearchForm.Builder s = SearchForm.builder()
            .setQuery(SearchQuery.text("часы"))
            .setPageInfo(PageInfo.DEFAULT)
            .setGenericParams(genericParams);

        reportTestClient.searchV2("часы", "search_watch_total_109597.json");

        InternalSearchResults result = Futures.waitAndGet(searchService.searchV2(
            Collections.emptyList(), s));
        SearchPageInfo pageInfo = (SearchPageInfo) result.getItems().getPageInfo();

        Assert.assertEquals(pageInfo.getTotalItems(), 109597);
    }

    /**
     * Тестируем, что отдаем в имени категории для поиска "длинной имя" из uniqName параметра в репорте
      */
    @Test
    public void testSearchCategoriesName() {
        // настройка системы
        // вызов системы
        SearchForm.Builder s = SearchForm.builder()
                .setQuery(SearchQuery.text("странное красное платье"))
                .setGenericParams(genericParams)
                .setPageInfo(PageInfo.DEFAULT);

        reportTestClient.searchV2("странное красное платье", "search_red_dress_category_name.json");

        InternalSearchResults searchResults = Futures.waitAndGet(
                searchService.searchV2(Collections.singleton(ResultFieldV2.FOUND_CATEGORIES), s));
        // проверка утверждений
        List<? extends Category> categories = searchResults.getCategories();
        Assert.assertEquals(categories.size(), 1);
        CategoryV2 category = (CategoryV2) categories.get(0);
        Assert.assertEquals("Книги по карьере", category.getName());
    }

    private static final int [] TEST_ORDER_SEARCH_CATEGORIES_HIDS = new int[]{
            91491,
            91498,
            459013,
            91503,
            91072,
            10834023,
            10382050,
            90555,
            2724669,
            191219,
            91499,
            91502,
            90409,
            90881,
            10498025,
            6126496,
            8353924,
            12429672,
            91112,
            91074,
            10983253,
            989023,
            6856242,
            12410815,
            7962992,
            91769,
            2662954,
            288003,
            90619,
            91117,
            6374342,
            469108,
            90472,
            4317343,
            12410435,
            91248,
            723088,
            10682592,
            3465066,
            90551,
            91530,
            91073,
            90404,
            90613,
            763070,
            91079,
            10484476,
            90539,
            6516122,
            7812201,
            13858284,
            6144238,
            6278685,
            6122737,
            91574,
            119851,
            90713,
            91104,
            91484,
            4684839,
            12367773,
            12557426,
            90725,
            90741,
            91037,
            191214,
            818863,
            4951525,
            5081621,
            7339056,
            10833347,
            12894143,
            12501736,
            12494574,
            11158645,
            12807782,
            278370,
            2501051,
            91700,
            1009492,
            12341944,
            90627,
            6498837,
            90462,
            90567,
            240865,
            90544,
            6430985,
            10682647,
            91630,
            10833344,
            10972670,
            922144,
            477439,
            278423,
            434515,
            6126526,
            91800,
            91039,
            6430983,
            7812170,
            10818844,
            5048602,
            91027,
            12888025,
            90787,
            6374343,
            91303,
            91034,
            723087,
            8351842,
            488061,
            7076558,
            90610,
            90538,
            5090309,
            90751,
            984941,
            7812177,
            91774,
            90615,
            91249,
            233108,
            91709,
            91500,
            90958,
            6847580,
            947109,
            966823,
            6144280,
            91078,
            14223539,
            1009489,
            91198,
            90594,
            91013,
            10683227,
            91293,
            91304,
            638257,
            90747,
            90483,
            91259,
            91033,
            13199616,
            7791092,
            5081622,
            6038878,
            91516,
            6169283,
            90946,
            90451,
            90560,
            186461,
            4981541,
            90635,
            91107,
            90548,
            91664,
            10682618,
            10785221,
            10682610,
            10682597,
            7940846,
            91076,
            91522,
            4684840,
            91464,
            4165204,
            90565,
            226666,
            431294,
            7812062,
            7812196,
            91520,
            7811902,
            7812151,
            7811940,
            7812200,
            7812199,
            7869391,
            10682526
    };
}
