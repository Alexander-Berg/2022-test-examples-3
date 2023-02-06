package ru.yandex.market.replenishment.autoorder.api;

import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockLogin
public class InterWarehouseMovementControllerTest extends ControllerTest {

    private final static String PREFIX = "/api/v1/movements/inter-warehouse";

    @Test
    @DbUnitDataSet(before = "InterWarehouseMovementControllerTest.getAll.before.csv")
    public void testGetByIds() throws Exception {
        mockMvc.perform(get(PREFIX + "?ids=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseMovementControllerTest.getAll.before.csv")
    public void testGetByWarehouseFrom() throws Exception {
        mockMvc.perform(get(PREFIX + "?warehouseIdFrom=145"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.id==1)].id", hasSize(1)))
                .andExpect(jsonPath("$[?(@.id==2)].id", hasSize(1)));
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseMovementControllerTest.getAll.before.csv")
    public void testGetByWarehouseTo() throws Exception {
        mockMvc.perform(get(PREFIX + "?warehouseIdTo=145"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(3));
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseMovementControllerTest.getAll.before.csv")
    public void testGetByExportedFrom() throws Exception {
        mockMvc.perform(get(PREFIX + "?exportedDateFrom=2020-03-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseMovementControllerTest.getAll.before.csv")
    public void testGetByExportedTo() throws Exception {
        mockMvc.perform(get(PREFIX + "?exportedDateTo=2020-03-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseMovementControllerTest.getAll.before.csv")
    public void testGetAll() throws Exception{
        mockMvc.perform(get(PREFIX))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.id==1)].id", hasSize(1)))
                .andExpect(jsonPath("$[?(@.id==2)].id", hasSize(1)))
                .andExpect(jsonPath("$[?(@.id==3)].id", hasSize(1)));
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseMovementControllerTest.getStats.before.csv")
    public void testStats() throws Exception{
        mockMvc.perform(get(PREFIX + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value("2"));
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseMovementControllerTest.getStats.before.csv")
    public void testStatsFilterDateFrom() throws Exception{
        mockMvc.perform(get(PREFIX + "/stats?exportedDateFrom=2021-03-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value("2"));
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseMovementControllerTest.getStats.before.csv")
    public void testStatsFilterDateTo() throws Exception{
        mockMvc.perform(get(PREFIX + "/stats?exportedDateTo=2021-03-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value("1"));
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseMovementControllerTest.getStats.before.csv")
    public void testStatsFilterWarehouseFrom() throws Exception {
        mockMvc.perform(get(PREFIX + "/stats?warehouseIdFrom=147"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value("1"));
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseMovementControllerTest.getStats.before.csv")
    public void testStatsFilterWarehouseTo() throws Exception{
        mockMvc.perform(get(PREFIX + "/stats?warehouseIdTo=145"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value("2"));
    }
}
