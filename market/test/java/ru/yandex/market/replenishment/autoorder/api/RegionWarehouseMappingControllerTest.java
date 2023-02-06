package ru.yandex.market.replenishment.autoorder.api;

import org.junit.Test;

import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockLogin
public class RegionWarehouseMappingControllerTest extends ControllerTest {

    @Test
    public void testGetAll() throws Exception {
        mockMvc.perform(get("/api/v1/region-warehouse-mapping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(35))
                .andExpect(jsonPath("$[0].warehouseId").value(147L))
                .andExpect(jsonPath("$[0].regionId").value(959L))
                .andExpect(jsonPath("$[1].warehouseId").value(147L))
                .andExpect(jsonPath("$[1].regionId").value(977L))
                .andExpect(jsonPath("$[2].warehouseId").value(147L))
                .andExpect(jsonPath("$[2].regionId").value(10946L));
    }
}
