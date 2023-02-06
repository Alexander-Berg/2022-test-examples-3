package ru.yandex.market.partner.mvc.controller.category;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.FacetByGroupAttribute;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchFacetResult;
import ru.yandex.market.partner.mvc.controller.datacamp.dto.SearchOffersParamsDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

/**
 * Тест для {@link CategoryController}.
 */
@DbUnitDataSet(before = "before.csv")
class CategoryControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private SaasService saasService;

    @BeforeEach
    void initSaas() {
        given(saasService.searchByGroupFacetsWithFilters(any(), any())).willReturn(
                SaasSearchFacetResult.builder()
                        .setResultFacetsMap(Map.of(FacetByGroupAttribute.MARKET_CATEGORY_ID,
                                Map.of("111", 11, "222", 3, "333", 9, "444", 1)))
                        .build()

        );
    }


    @Test
    @DisplayName("Запрос к ресурсу {@code GET /categories} возвращает корректный результат")
    void testGetAllCategories() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategories.expected.json");
    }

    @Test
    @DisplayName("Запрос всех категорий по бизнесу")
    void testGetAllCategoriesByBusiness() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        var httpEntity = new HttpEntity<>(new RequestCategorySuggestDTO(), headers);

        ResponseEntity<String> response = FunctionalTestHelper.post(baseUrl + "businesses/123/categories?uid=100500",
                httpEntity);
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategoriesByBusiness.expected.json");
    }

    @Test
    @DisplayName("Проверяет, что поведение при accept_good_content=false такое же, как и без параметра")
    void testNotAcceptGoodContent() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&accept_good_content=false");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategories.expected.json");
    }

    @Test
    @DisplayName("Проверяет фильтрацию по accept_good_content")
    void testAcceptGoodContent() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&accept_good_content=true");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategoriesGoodContent.expected.json");
    }

    @Test
    @DisplayName("Проверяет фильтрацию по типу категории")
    void testGetCategoriesWithFilterByOutputType() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&output_type=GURU,GURULIGHT");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategoriesTypes.expected.json");
    }

    @Test
    @DisplayName("Проверяет фильтрацию по списку категорий")
    void testGetByCategoryIds() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&category_ids=555,777");
        JsonTestUtil.assertEquals(response, getClass(), "testGetCategoriesByIds.expected.json");
    }


    @Test
    @DisplayName("Проверяет фильтрацию по имени при запросе категорий")
    void testGetCategoriesWithFilterByName() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&name=2");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategoriesName.expected.json");
    }

    @Test
    @DbUnitDataSet(before = "nonLeaf.before.csv")
    @DisplayName("Проверяет фильтрацию по имени при запросе категорий c поиском по родительской категории")
    void testGetCategoriesWithFilterByNameWithParent() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&name=Одежда");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategoriesNameNonLeaf.expected.json");
    }

    @Test
    @DisplayName("Проверяет несколько фильтров")
    void testAllFilter() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl +
                        "/categories?uid=100500&output_type=GURU&type=LEAF&name=3&accept_good_content=true");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategoriesManyFilters.expected.json");
    }

    @Test
    @DisplayName("Проверяет пагинацию c параметрами")
    void testParametrizedPaging() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl +
                        "/categories?uid=100500&page=2&pageSize=4&accept_good_content=true");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategoriesPaging.expected.json");
    }

    @Test
    @DisplayName("Проверяет пагинацию c параметрами по бизнесу 1 страница")
    void testParametrizedPagingByBusinessFirstPage() {
        RequestCategorySuggestDTO request = new RequestCategorySuggestDTO();
        request.setAcceptGoodContent(true);
        SearchOffersParamsDTO params = new SearchOffersParamsDTO();
        params.setShopId(100L);
        params.setSupplyPlans(Set.of("WILL_SUPPLY"));
        params.setCategoryIds(Set.of(123L, 456L));
        params.setResultContentStatuses(Set.of("HAS_CARD_MARKET"));
        request.setSearchOffersParams(params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        var httpEntity = new HttpEntity<>(request, headers);
        ResponseEntity<String> response =
                FunctionalTestHelper.post(baseUrl +
                        "businesses/123/categories?uid=100500&page=1&pageSize=2", httpEntity);
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategoriesPagingByBusiness.expected.json");
    }

    @Test
    @DisplayName("Проверяет пагинацию c параметрами по бизнесу 2 страница")
    void testParametrizedPagingBusinessSecondPage() {
        RequestCategorySuggestDTO request = new RequestCategorySuggestDTO();
        request.setAcceptGoodContent(true);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        var httpEntity = new HttpEntity<>(request, headers);
        ResponseEntity<String> response =
                FunctionalTestHelper.post(baseUrl +
                        "businesses/123/categories?uid=100500&page=2&pageSize=2", httpEntity);
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategoriesPagingByBusiness2.expected.json");
    }

    @Test
    @DisplayName("Проверяет пагинацию без параметров")
    void testPagingWithoutParametrs() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl +
                        "/categories?uid=100500&page=&pageSize=&accept_good_content=true");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategoriesWoPaging.expected.json");
    }

    @Test
    @DisplayName("Проверяет пагинацию, когда размер списка после фильтрации = 0")
    void testZeroPageSizePaging() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl +
                        "/categories?uid=100500&page=2&output_type=MIXED&accept_good_content=true");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategoriesEmpty.expected.json");
    }

    @Test
    @DisplayName("Проверяет, что поведение при accept_white_content=false такое же, как и без параметра")
    void testNotAcceptWhiteContent() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&accept_white_content=false");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategories.expected.json");
    }

    @Test
    @DisplayName("Проверяет фильтрацию по accept_white_content")
    void testAcceptWhiteContent() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&accept_white_content=true");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllCategoriesWhiteContent.expected.json");
    }

    @Test
    @DisplayName("Проверяет фильтрацию по замороженным категориям")
    void testPartnerFreezeContentFilter() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&freeze_partner_content=WARN_FREEZE" +
                        "&&freeze_partner_content=FULL_FREEZE");
        JsonTestUtil.assertEquals(response, getClass(), "testGetAllWithWarnFreeze.expected.json");
    }
}
