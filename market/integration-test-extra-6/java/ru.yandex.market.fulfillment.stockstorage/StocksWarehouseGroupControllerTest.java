package ru.yandex.market.fulfillment.stockstorage;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StocksWarehouseGroupControllerTest extends AbstractContextualTest {
    /**
     * Проверяем успешное создание группы.
     */
    @Test
    @ExpectedDatabase(value = "classpath:database/expected/stocks_warehouse_group/create_group.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testCreateGroupOk() throws Exception {
        mockMvc.perform(put("/stocks-warehouse-groups")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"groupId\": 1, \"mainWarehouseId\": 10, \"warehouses\": [" +
                                "{\"vendorId\": 100, \"warehouseId\": 10}, " +
                                "{\"vendorId\": 110, \"warehouseId\": 11}, " +
                                "{\"vendorId\": 120, \"warehouseId\": 12}]}"))
                .andExpect(status().isOk());
    }

    /**
     * Проверяем ошибку создания, если < 2 складов в группе.
     */
    @Test
    void testCreateGroupFail() throws Exception {
        mockMvc.perform(put("/stocks-warehouse-groups")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"groupId\": 1, \"mainWarehouseId\": 10, " +
                                "\"warehouses\": [{\"vendorId\": 1, \"warehouseId\": 1}]}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверяем успешное удаление группы.
     */
    @Test
    @DatabaseSetup("classpath:database/expected/stocks_warehouse_group/create_group.xml")
    @ExpectedDatabase(value = "classpath:database/expected/stocks_warehouse_group/empty_group.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testDeleteGroup() throws Exception {
        mockMvc.perform(delete("/stocks-warehouse-groups/1"))
                .andExpect(status().isOk());
    }

    /**
     * Проверяем 404 для удаления несуществующей группы.
     */
    @Test
    void testDeleteNonExistingGroup() throws Exception {
        mockMvc.perform(delete("/stocks-warehouse-groups/1"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверяем корректное добавление новых складов в группу и игнорирование уже существующих там.
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_warehouse_group/group.xml")
    @ExpectedDatabase(value = "classpath:database/expected/stocks_warehouse_group/create_group.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testAddWarehouse() throws Exception {
        mockMvc.perform(post("/stocks-warehouse-groups/1/add-warehouse")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"warehouses\": [{\"vendorId\": 120, \"warehouseId\": 12}," +
                                "{\"vendorId\": 100, \"warehouseId\": 10}]}"))
                .andExpect(status().isOk());
    }

    /**
     * Проверяем, что падаем при добавлении склада в группе, который в другой группе.
     */
    @Test
    @DatabaseSetup({"classpath:database/states/stocks_warehouse_group/group.xml",
            "classpath:database/states/stocks_warehouse_group/group2.xml"})
    void testAddWarehouseFromAnotherGroupError() throws Exception {
        mockMvc.perform(post("/stocks-warehouse-groups/1/add-warehouse")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"warehouses\": [{\"vendorId\": 200, \"warehouseId\": 20}]}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверяем 404 для добавления склада в несуществующую группу.
     */
    @Test
    void testAddWarehouseToNonExistingGroup() throws Exception {
        mockMvc.perform(post("/stocks-warehouse-groups/1/add-warehouse")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"warehouses\": [{\"vendorId\": 100, \"warehouseId\": 10}]}"))
                .andExpect(status().isNotFound());
    }
}
