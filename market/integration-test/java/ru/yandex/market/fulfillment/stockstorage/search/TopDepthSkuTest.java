package ru.yandex.market.fulfillment.stockstorage.search;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchRestClient.SEARCH;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchRestClient.TOP_DEPTH_SKU;

public class TopDepthSkuTest extends AbstractContextualTest {

    /**
     * Проверка того что выборка по глубине стока происходит из реплики
     */
    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
            value = "classpath:database/states/sku/search/top_depth_sku_db_master.xml")
    @DatabaseSetup(connection = "replicaDbUnitDatabaseConnection",
            value = "classpath:database/states/sku/search/top_depth_sku_db_replica.xml")
    public void replicaReadTest() throws Exception {
        String actualJson = request("1", "FIT", "1");

        softly
                .assertThat(actualJson)
                .is(jsonMatching(extractFileContent("requests/sku/search/topDepth/replica_read.json")));
    }

    /**
     * Проверка на то что контроллер будет отвечать ошибкой,
     * если не передан обязательный параметр warehouseId
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/top_depth_sku_db.xml")
    public void getTopOneSkuWithoutWarehousreId() throws Exception {
        String actualJson = mockMvc.perform(get(SEARCH + TOP_DEPTH_SKU)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .param("limit", "1")
                .param("stockType", "FIT"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly
                .assertThat(actualJson)
                .contains("Required request parameter 'warehouseId' for method parameter type int is not present");
    }

    /**
     * Валидация запроса на 1 SKU.
     * Запрошен 1 SKU c FIT стоком.
     * Вернется SKU c самым большим стоком
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/top_depth_sku_db.xml")
    public void getTopOneSku() throws Exception {
        String actualJson = request("1", "FIT", "1");

        softly
                .assertThat(actualJson)
                .is(jsonMatching(extractFileContent("requests/sku/search/topDepth/single_fit.json")));
    }

    /**
     * Валидация запроса на пустой БД.
     * Запрошен 1 SKU c FIT стоком.
     * Вернется объект с данными limit и stocktype но без стоков
     */
    @Test
    public void checkEmptyDB() throws Exception {
        String actualJson = request("1", "FIT", "1");

        softly
                .assertThat(actualJson)
                .is(jsonMatching("{  \"limit\": 1, \"stockType\": 10, \"stocks\": []}"));
    }

    /**
     * Валидация стандартного запроса без праметров
     * Вернется объект с данными limit и stocktype и 10 FIT элементами
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/top_depth_sku_db.xml")
    public void checkDefaultValues() throws Exception {
        String actualJson = request(null, null, "1");

        softly
                .assertThat(actualJson)
                .is(jsonMatchingWithoutOrder(extractFileContent("requests/sku/search/topDepth/ten_fit.json")));
    }

    /**
     * Валидация стандартного запроса c limit > 100 - должен упасть с ошибкой
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/top_depth_sku_db.xml")
    public void checkTooBigLimit() throws Exception {
        String actualJson = mockMvc.perform(get(SEARCH + TOP_DEPTH_SKU)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .param("limit", "101")
                .param("stockType", "FIT")
                .param("warehouseId", "1"))
                .andExpect(status().is4xxClientError())
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly
                .assertThat(actualJson)
                .contains("Limit is too large. Max value is 100");
    }

    /**
     * Валидация стандартного запроса, где указан только warehouseId=2.
     * Вернется объект с данными limit и stocktype и 3 FIT элементами
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/top_depth_sku_db.xml")
    public void checkDefaultValuesWithWarehouseIdOnly() throws Exception {
        String actualJson = request(null, null, "2");
        System.err.println(actualJson);

        softly
                .assertThat(actualJson)
                .is(jsonMatching(extractFileContent("requests/sku/search/topDepth/fits_of_second_warehouse.json")));
    }

    /**
     * Валидация стандартного запроса на 10 SKU с FIT стоком для warehouseId.
     * Запрошен 10 SKU c FIT стоком и warehouseId=2.
     * Вернется объект с данными limit и stocktype и 3 FIT элементами
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/top_depth_sku_db.xml")
    public void check10FitWithWarehouseId() throws Exception {
        String actualJson = request("10", "FIT", "2");

        softly
                .assertThat(actualJson)
                .is(jsonMatching(extractFileContent("requests/sku/search/topDepth/fits_of_second_warehouse.json")));
    }

    /**
     * Валидация стандартного запроса на 10 SKU с FIT стоком для несуществующего warehouseId.
     * Запрошен 10 SKU c FIT стоком и warehouseId=47.
     * Вернется объект с данными limit и stocktype и 3 FIT элементами
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/top_depth_sku_db.xml")
    public void check10FitWithUnknownWarehouseId() throws Exception {
        String actualJson = request("10", "FIT", "47");

        softly
                .assertThat(actualJson)
                .is(jsonMatching("{  \"limit\": 10, \"stockType\": 10, \"stocks\": []}"));
    }

    private String request(String limit, String stockType, String warehouseId) throws Exception {
        return mockMvc.perform(get(SEARCH + TOP_DEPTH_SKU)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .param("limit", limit)
                .param("warehouseId", warehouseId)
                .param("stockType", stockType))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String request(String limit, String stockType) throws Exception {
        return request(limit, stockType, null);
    }

}
