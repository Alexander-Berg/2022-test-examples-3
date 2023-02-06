package ru.yandex.market.ff4shops.stocks.controller;

import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.json.openapi.AbstractStocksOpenApiTest;
import ru.yandex.market.ff4shops.client.model.GetGroupsByPartnersRequest;
import ru.yandex.market.ff4shops.client.model.StocksWarehouseGroupAddRequest;
import ru.yandex.market.ff4shops.client.model.StocksWarehouseGroupRequest;
import ru.yandex.market.ff4shops.client.model.WarehousesAndGroupsRequest;
import ru.yandex.market.ff4shops.model.SeekPaging;
import ru.yandex.market.ff4shops.stocks.controller.wrapped.StocksWarehouseGroupWrappedController;
import ru.yandex.market.ff4shops.stocks.model.StocksWarehouseGroupPaging;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.ff4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.ff4shops.client.ResponseSpecBuilders.validatedWith;

/**
 * Тесты для {@link StocksWarehouseGroupWrappedController}.
 */
public class StocksWarehouseGroupControllerTest extends AbstractStocksOpenApiTest {
    /**
     * Проверяет успешное создание группы складов.
     */
    @Test
    @DbUnitDataSet(before = "../repository/StockWarehouseGroupTest.before.csv",
            after = "../repository/StockWarehouseGroupTest.insert.after.csv")
    void testCreateGroup() {
        String jsonResponse = apiClient.stocksWarehouseGroup()
                .createStocksWarehouseGroup()
                .uidQuery(1L)
                .body(new StocksWarehouseGroupRequest()
                        .name("Группа 1")
                        .warehouseIds(List.of(14L, 15L, 16L)))
                .execute(validatedWith(shouldBeCode(SC_OK)))
                .jsonPath()
                .prettify();

        JSONAssert.assertEquals(
                "{\"id\": 1, \"mainWarehouseId\": 14}",
                jsonResponse,
                JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     * Проверяет, что для группы из одного склада возвращается ошибка.
     */
    @Test
    void testCreateFail() {
        String jsonResponse = apiClient.stocksWarehouseGroup()
                .createStocksWarehouseGroup()
                .uidQuery(1L)
                .body(new StocksWarehouseGroupRequest()
                        .name("Группа 1")
                        .warehouseIds(List.of(10L)))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .jsonPath()
                .prettify();

        JSONAssert.assertEquals("[{\"subCode\": \"BAD_REQUEST\"}]",
                new JSONObject(jsonResponse).getJSONArray("errors"),
                JSONCompareMode.LENIENT);
    }

    @Test
    @DbUnitDataSet(before = "../repository/StockWarehouseGroupTest.before.csv")
    void testGetGroupById404() {
        apiClient.stocksWarehouseGroupWrapped()
                .getGroupByGroupId()
                .groupIdPath(501L)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @DbUnitDataSet(before = "StocksWarehouseGroupControllerTest.before.csv",
    after = "StocksWarehouseGroupControllerTest.after.csv")
    void testAddWarehouseToGroup() {
        apiClient.stocksWarehouseGroup().addWarehouses()
                .groupIdPath(500L)
                .body(new StocksWarehouseGroupAddRequest().warehouseIds(List.of(13L, 14L)))
                .execute(validatedWith(shouldBeCode(SC_OK)))
                .jsonPath()
                .prettify();
    }

    @Test
    @DbUnitDataSet(before = "StocksWarehouseGroupControllerTest.before.csv")
    void testAddWarehouseGroupNotFound() {
        apiClient.stocksWarehouseGroup().addWarehouses()
                .groupIdPath(503L)
                .body(new StocksWarehouseGroupAddRequest().warehouseIds(List.of(13L, 14L)))
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @DbUnitDataSet(before = "StocksWarehouseGroupControllerTest.before.csv")
    void testAddWarehouseAlreadyInGroup() {
        apiClient.stocksWarehouseGroup().addWarehouses()
                .groupIdPath(500L)
                .body(new StocksWarehouseGroupAddRequest().warehouseIds(List.of(16L)))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .jsonPath()
                .prettify();
    }

    @Test
    @DbUnitDataSet(before = "../repository/StockWarehouseGroupTest.before.csv")
    void testGetGroupById() {
        String jsonResponse = apiClient.stocksWarehouseGroupWrapped()
                .getGroupByGroupId()
                .groupIdPath(500L)
                .execute(validatedWith(shouldBeCode(SC_OK)))
                .jsonPath()
                .prettify();
        var expected = "{\n" +
                "            \"id\": 500,\n" +
                "            \"name\": \"one\",\n" +
                "            \"mainWarehouseId\": 10,\n" +
                "            \"warehouses\": [\n" +
                "                {\n" +
                "                    \"warehouseId\": 10,\n" +
                "                    \"partnerId\": 110,\n" +
                "                    \"name\": \"test\",\n" +
                "                    \"type\": \"EXPRESS\",\n" +
                "                    \"address\": \"address\",\n" +
                "                    \"settlement\": \"city\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"warehouseId\": 11,\n" +
                "                    \"partnerId\": 111,\n" +
                "                    \"name\": \"test\",\n" +
                "                    \"type\": \"FBS\",\n" +
                "                    \"address\": \"address\",\n" +
                "                    \"settlement\": \"city\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"warehouseId\": 9,\n" +
                "                    \"partnerId\": 112,\n" +
                "                    \"name\": \"test\",\n" +
                "                    \"type\": \"DBS\",\n" +
                "                    \"address\": \"address\",\n" +
                "                    \"settlement\": \"city\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }";
        JSONAssert.assertEquals(expected,
                new JSONObject(jsonResponse).getJSONObject("result"),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DbUnitDataSet(before = "../repository/StockWarehouseGroupTest.before.csv")
    void testGeGroupByPartner() {
        apiClient.stocksWarehouseGroup()
                .createStocksWarehouseGroup()
                .uidQuery(1L)
                .body(new StocksWarehouseGroupRequest()
                        .name("Группа 1")
                        .warehouseIds(List.of(14L, 15L, 16L)))
                .execute(validatedWith(shouldBeCode(SC_OK)));

        String jsonResponse = apiClient.stocksWarehouseGroup().getGroupByPartner()
                .partnerIdPath(114L)
                .execute(validatedWith(shouldBeCode(SC_OK)))
                .jsonPath()
                .prettify();

        JSONAssert.assertEquals(
                "{\"id\": 1, \"name\": \"Группа 1\", \"mainWarehouseId\": 14," +
                        "\"warehouses\": [" +
                        "            {" +
                        "                \"warehouseId\": 14," +
                        "                \"partnerId\": 114," +
                        "                \"name\": \"test\"," +
                        "                \"type\": \"EXPRESS\"," +
                        "                \"settlement\": \"city\"," +
                        "                \"address\": \"address\"" +
                        "            }," +
                        "            {" +
                        "                \"warehouseId\": 15," +
                        "                \"partnerId\": 115," +
                        "                \"name\": \"test\"," +
                        "                \"type\": \"FBS\"," +
                        "                \"settlement\": \"city\"," +
                        "                \"address\": \"address\"" +
                        "            }," +
                        "            {" +
                        "                \"warehouseId\": 16," +
                        "                \"partnerId\": 116," +
                        "                \"name\": \"test\"," +
                        "                \"type\": \"DBS\"," +
                        "                \"settlement\": \"city\"," +
                        "                \"address\": \"address\"" +
                        "            }" +
                        "]}",
                new JSONObject(jsonResponse).getJSONObject("result"),
                JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    @DbUnitDataSet(before = "../repository/StockWarehouseGroupTest.before.csv")
    void testGeGroupsByPartners() {
        String jsonResponse = apiClient.stocksWarehouseGroupWrapped().getGroupsByPartners()
                .body(new GetGroupsByPartnersRequest().partnerId(List.of(110L, 111L, 113L)))
                .execute(validatedWith(shouldBeCode(SC_OK)))
                .jsonPath()
                .prettify();

        JSONAssert.assertEquals(
                "{\"groups\": [{\"id\": 500, \"name\": \"one\", \"mainWarehouseId\": 10," +
                        "\"warehouses\": [" +
                        "            {" +
                        "                \"warehouseId\": 10," +
                        "                \"partnerId\": 110," +
                        "                \"name\": \"test\"," +
                        "                \"type\": \"EXPRESS\"," +
                        "                \"settlement\": \"city\"," +
                        "                \"address\": \"address\"" +
                        "            }," +
                        "            {" +
                        "                \"warehouseId\": 11," +
                        "                \"partnerId\": 111," +
                        "                \"name\": \"test\"," +
                        "                \"type\": \"FBS\"," +
                        "                \"settlement\": \"city\"," +
                        "                \"address\": \"address\"" +
                        "            }," +
                        "            {" +
                        "                \"warehouseId\": 9," +
                        "                \"partnerId\": 112," +
                        "                \"name\": \"test\"," +
                        "                \"type\": \"DBS\"," +
                        "                \"settlement\": \"city\"," +
                        "                \"address\": \"address\"" +
                        "            }" +
                        "]}]}",
                new JSONObject(jsonResponse).getJSONObject("result"),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testGeGroupByPartnerEmpty() {
        String jsonResponse = apiClient.stocksWarehouseGroup().getGroupByPartner()
                .partnerIdPath(999L)
                .execute(validatedWith(shouldBeCode(SC_OK)))
                .jsonPath()
                .prettify();

        assertThat(new JSONObject(jsonResponse).optJSONObject("result")).isNull();
    }

    /**
     * Проверяет поиск групп и складов. В том числе проверяет, что, несмотря на limit 3, возвращается 4 склада,
     * чтобы склады в группе не дробить на разные страницы
     */
    @ParameterizedTest
    @CsvSource({"3, true", "10, false"})
    @DbUnitDataSet(before = "../service/WarehousesServiceTest.before.csv")
    void testGetWarehousesAndGroups(int limit, boolean expectPaging) {
        String jsonResponse = apiClient.stocksWarehouseGroupWrapped()
                .warehousesAndGroups()
                .body(new WarehousesAndGroupsRequest().partnerId(List.of(110L, 111L, 112L, 113L)))
                .limitQuery(limit)
                .pageTokenQuery(SeekPaging.tokenize(new StocksWarehouseGroupPaging(0L, null)))
                .execute(validatedWith(shouldBeCode(SC_OK)))
                .jsonPath()
                .prettify();

        String nextPageToken = SeekPaging.tokenize(new StocksWarehouseGroupPaging(2L, null));
        JSONObject result = new JSONObject(jsonResponse).getJSONObject("result");

        JSONAssert.assertEquals(
                "{\"groups\": [{" +
                        "        \"id\": 1," +
                        "        \"name\": \"Группа 1\"," +
                        "        \"mainWarehouseId\": 10," +
                        "        \"warehouses\": [" +
                        "          {" +
                        "            \"warehouseId\": 10," +
                        "            \"partnerId\": 110," +
                        "            \"name\": \"test\"," +
                        "            \"type\": \"EXPRESS\"," +
                        "            \"address\": \"Москва\"," +
                        "            \"settlement\": null" +
                        "          }," +
                        "          {" +
                        "            \"warehouseId\": 11," +
                        "            \"partnerId\": 111," +
                        "            \"name\": \"test\"," +
                        "            \"type\": \"FBS\"," +
                        "            \"address\": \"Питер, Невский проспект\"," +
                        "            \"settlement\": null" +
                        "          }" +
                        "        ]" +
                        "      }," +
                        "      {" +
                        "        \"id\": 2," +
                        "        \"name\": \"Группа 2\"," +
                        "        \"mainWarehouseId\": 20," +
                        "        \"warehouses\": [" +
                        "          {" +
                        "            \"warehouseId\": 20," +
                        "            \"partnerId\": 112," +
                        "            \"name\": \"test\"," +
                        "            \"type\": \"FBS\"," +
                        "            \"address\": \"Москва\"," +
                        "            \"settlement\": null" +
                        "          }," +
                        "          {" +
                        "            \"warehouseId\": 21," +
                        "            \"partnerId\": 113," +
                        "            \"name\": \"test\"," +
                        "            \"type\": \"DBS\"," +
                        "            \"address\": \"Воронеж\"," +
                        "            \"settlement\": null" +
                        "          }" +
                        "        ]" +
                        "      }]," +
                        "    \"warehouses\": []," +
                        "    \"paging\": " +
                        (expectPaging ? " {\"nextPageToken\": \"" + nextPageToken + "\"}" : "null") +
                        "}",
                result, JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     * Проверяет поиск групп и складов. Поиск по идентификатору группы
     */
    @Test
    @DbUnitDataSet(before = "../service/WarehousesServiceTest.before.csv")
    void testGetWarehousesAndGroupsByGroupId() {
        long expectedGroupId = 2L;
        String jsonResponse = apiClient.stocksWarehouseGroupWrapped()
                .warehousesAndGroups()
                .body(new WarehousesAndGroupsRequest()
                        .partnerId(List.of(110L, 111L, 112L, 113L))
                        .groupId(expectedGroupId))
                .execute(validatedWith(shouldBeCode(SC_OK)))
                .jsonPath()
                .prettify();
        JSONObject result = new JSONObject(jsonResponse).getJSONObject("result");

        JSONAssert.assertEquals(
                "{\"groups\": [{" +
                        "        \"id\": " + expectedGroupId + "," +
                        "        \"name\": \"Группа 2\"," +
                        "        \"mainWarehouseId\": 20," +
                        "        \"warehouses\": [" +
                        "          {" +
                        "            \"warehouseId\": 20," +
                        "            \"partnerId\": 112," +
                        "            \"name\": \"test\"," +
                        "            \"type\": \"FBS\"," +
                        "            \"address\": \"Москва\"," +
                        "            \"settlement\": null" +
                        "          }," +
                        "          {" +
                        "            \"warehouseId\": 21," +
                        "            \"partnerId\": 113," +
                        "            \"name\": \"test\"," +
                        "            \"type\": \"DBS\"," +
                        "            \"address\": \"Воронеж\"," +
                        "            \"settlement\": null" +
                        "          }" +
                        "        ]" +
                        "      }]," +
                        "    \"warehouses\": []," +
                        "    \"paging\": null" +
                        "}",
                result, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DbUnitDataSet(before = "../service/WarehousesServiceTest.before.csv")
    void testWarehousesInGroup() {
        String jsonResponse = apiClient.stocksWarehouseGroupWrapped()
                .warehousesInGroup()
                .warehouseIdQuery(10, 11, 30, 999L)
                .execute(validatedWith(shouldBeCode(SC_OK)))
                .jsonPath()
                .prettify();

        JSONAssert.assertEquals(
                "{\"warehouseId\": [10, 11]}",
                new JSONObject(jsonResponse).getJSONObject("result"),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     * Проверяет успешное удаление группы складов.
     */
    @Test
    @DbUnitDataSet(before = "../repository/StockWarehouseGroupTest.before.csv",
            after = "../repository/StockWarehouseGroupTest.delete.after.csv")
    void testDeleteGroup() {
        apiClient.stocksWarehouseGroup()
                .deleteStocksWarehouseGroup()
                .uidQuery(1L)
                .groupIdPath(500)
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }
}
