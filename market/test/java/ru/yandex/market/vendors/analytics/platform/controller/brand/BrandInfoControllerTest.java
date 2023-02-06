package ru.yandex.market.vendors.analytics.platform.controller.brand;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.model.widget.Measure;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * Functional tests for {@link BrandInfoController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "BrandInfoControllerTest.csv")
@ClickhouseDbUnitDataSet(before = "BrandInfoControllerTest.ch.csv")
class BrandInfoControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Получить информацию по брендам по переданным идентификаторам брендов")
    void getBrands() {
        String actualResponse = getBrandInfos(List.of(1L, 3L, 4L));
        String expectedResponse = ""
                + "[  \n"
                + "   {  \n"
                + "      \"brandId\":1,\n"
                + "      \"brandName\":\"Apple\"\n"
                + "   },\n"
                + "   {  \n"
                + "      \"brandId\":3,\n"
                + "      \"brandName\":\"Naike\"\n"
                + "   },\n"
                + "   {  \n"
                + "      \"brandId\":4,\n"
                + "      \"brandName\":\"Kuma\"\n"
                + "   }\n"
                + "]";
        JsonTestUtil.assertEquals(
                expectedResponse,
                actualResponse
        );
    }

    @Test
    @DisplayName("Поиск брендов с фильтрами")
    void searchBrandWithFilters() {
        String actual = searchBrandsWithFilters(
                "a",
                Measure.MONEY
        );
        String expected = "{\n"
                + "  \"paging\": {\n"
                + "    \"pageNumber\": 0,\n"
                + "    \"pageSize\": 4,\n"
                + "    \"totalPages\": 1,\n"
                + "    \"totalElements\": 1\n"
                + "  },\n"
                + "  \"brands\": [\n"
                + "    {\n"
                + "      \"brandId\": 2,\n"
                + "      \"brandName\": \"Samsung\"\n"
                + "    }\n"
                + "  ]\n"
                + "} ";
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Поиск брендов без фильтров")
    void searchBrandWithoutFilters() {
        String actual = searchBrandsWithFilters(
                Measure.SEARCH_COUNT
        );
        String expected = "{\n"
                + "  \"paging\": {\n"
                + "    \"pageNumber\": 0,\n"
                + "    \"pageSize\": 4,\n"
                + "    \"totalPages\": 1,\n"
                + "    \"totalElements\": 3\n"
                + "  },\n"
                + "  \"brands\": [\n"
                + "    {\n"
                + "      \"brandId\": 5,\n"
                + "      \"brandName\": \"Foo\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"brandId\": 4,\n"
                + "      \"brandName\": \"Kuma\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"brandId\": 3,\n"
                + "      \"brandName\": \"Naike\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        JsonTestUtil.assertEquals(expected, actual);
    }

    private String searchBrandsWithFilters(
            Measure measure
    ) {
        return searchBrandsWithFilters(null, measure);
    }

    private String searchBrandsWithFilters(
            String partName,
            Measure measure
    ) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/brands/search/filters")
                .queryParam("hid", 100)
                .queryParam("partName", partName)
                .queryParam("measure", measure)
                .queryParam("pageSize", 4)
                .queryParam("pageNumber", 0)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

    private String brandsUrl(Collection<Long> ids) {
        var idsParam = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/brands")
                .queryParam("id", idsParam)
                .toUriString();
    }

    private String getBrandInfos(Collection<Long> ids) {
        var url = brandsUrl(ids);
        return FunctionalTestHelper.get(url).getBody();
    }
}
