package ru.yandex.market.vendors.analytics.platform.controller.model;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Disabled;
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
 * Functional tests for {@link ModelInfoController}.
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "ModelInfoControllerTest.before.csv")
@ClickhouseDbUnitDataSet(before = "ModelInfoControllerTest.ch.before.csv")
public class ModelInfoControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Получить информацию по моделям по переданным идентификаторам")
    void getModels() {
        String actualResponse = getModelInfos(List.of(1L, 5L));
        String expectedResponse = "[\n"
                + "  {\n"
                + "    \"id\": 1,\n"
                + "    \"categoryId\": 91491,\n"
                + "    \"brandId\": 153043,\n"
                + "    \"name\": \"Apple iphone 5S\"\n"
                + "  },\n"
                + "  {\n"
                + "    \"id\": 5,\n"
                + "    \"categoryId\": 91492,\n"
                + "    \"brandId\": 153043,\n"
                + "    \"name\": \"Apple ipad pro\"\n"
                + "  }\n"
                + "]";
        JsonTestUtil.assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DisplayName("Поиск моделей за всё время: у пользователя нет категорий")
    void searchModelsNothingFound() {
        String actual = searchPagedModels(1002L, "ip", 1, 2);
        var expected = ""
                + "{  \n"
                + "   \"paging\":{  \n"
                + "      \"pageNumber\":1,\n"
                + "      \"pageSize\":2,\n"
                + "      \"totalPages\":0,\n"
                + "      \"totalElements\":0\n"
                + "   },\n"
                + "   \"models\":[  \n"
                + "   ]\n"
                + "}";
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @Disabled
    @DisplayName("Поиск моделей за все время (с пагинацией)")
    void searchModels() {
        var response = searchPagedModels(1001L, "ip", 2, 2);
        var expectedResponse = loadFromFile("ModelInfoController.searchModels.response.json");
        JsonTestUtil.assertEquals(expectedResponse, response);
    }

    @Test
    @Disabled
    @DisplayName("Поиск моделей за конкретный период")
    void searchModelsForPeriodSales() {
        String actual = searchModelsWithParams(
                91491L,
                "Ip",
                Measure.COUNT,
                4,
                0
        );
        String expected = loadFromFile("ModelInfoController.searchModelsForPeriodSales.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @Disabled
    @DisplayName("Поиск моделей за конкретный период по поисковыми интересами")
    void searchModelsWithParams() {
        String actual = searchModelsWithParams(
                91491L,
                "Ip",
                Measure.SEARCH_COUNT,
                7,
                0
        );
        String expected = loadFromFile("ModelInfoController.searchModelsForPeriodSearches.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    private String searchModelsUrl(long userId, String partName) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/models/search/v2")
                .queryParam("userId", userId)
                .queryParam("partName", partName)
                .toUriString();
    }

    private String searchPagedModels(long userId, String partName, int pageNumber, int pageSize) {
        var url = UriComponentsBuilder.fromUriString(searchModelsUrl(userId, partName))
                .queryParam("pageNumber", pageNumber)
                .queryParam("pageSize", pageSize)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

    private String searchModelsWithParams(
            long categoryId,
            String partName,
            Measure measure,
            int pageSize,
            int pageNumber
    ) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/models/search/interval")
                .queryParam("hid", categoryId)
                .queryParam("partName", partName)
                .queryParam("measure", measure)
                .queryParam("pageSize", pageSize)
                .queryParam("pageNumber", pageNumber)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

    private String modelsUrl(Collection<Long> ids) {
        var idsParam = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/models")
                .queryParam("id", idsParam)
                .toUriString();
    }

    private String getModelInfos(Collection<Long> ids) {
        var url = modelsUrl(ids);
        return FunctionalTestHelper.get(url).getBody();
    }
}
