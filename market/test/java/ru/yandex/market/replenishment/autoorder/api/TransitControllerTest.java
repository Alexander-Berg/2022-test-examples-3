package ru.yandex.market.replenishment.autoorder.api;

import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockLogin
public class TransitControllerTest extends ControllerTest {

    @Test
    @DbUnitDataSet(before = "TransitControllerTest_testUpdateRelevance.before.csv",
            after = "TransitControllerTest_testUpdateRelevance.after.csv")
    public void testUpdateRelevance_actual() throws Exception {
        mockMvc.perform(put("/api/v1/transits/1")
                .contentType(APPLICATION_JSON_UTF8)
                .content("true")).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TransitControllerTest_testUpdateRelevance.after.csv",
            after = "TransitControllerTest_testUpdateRelevance.before.csv")
    public void testUpdateRelevance_notActual() throws Exception {
        mockMvc.perform(put("/api/v1/transits/1")
                .contentType(APPLICATION_JSON_UTF8)
                .content("false")).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TransitControllerTest_testGetTransits.before.csv")
    public void testGetTransits() throws Exception {
        mockMvc.perform(get("/api/v1/transits?warehouseId=147&msku=100")
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].msku").value(100))
                .andExpect(jsonPath("$[0].warehouseId").value(147))
                .andExpect(jsonPath("$[0].special").value(false));
    }

    @Test
    @DbUnitDataSet(before = "TransitControllerTest_testGetTransits_specific.before.csv")
    public void testGetTransitsSpecific() throws Exception {
        mockMvc.perform(get("/api/v1/transits?warehouseId=171&msku=200")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].msku").value(200))
            .andExpect(jsonPath("$[0].warehouseId").value(171))
            .andExpect(jsonPath("$[0].special").value(true));
    }

    @Test
    @DbUnitDataSet(before = "TransitControllerTest_testGetTransits.before.csv")
    public void testGetTransitsResult() throws Exception {
        mockMvc.perform(get("/api/v1/transits/result?recommendationId=1003")
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].msku").value(200))
                .andExpect(jsonPath("$[0].warehouseId").value(171))
                .andExpect(jsonPath("$[1].msku").value(200))
                .andExpect(jsonPath("$[1].warehouseId").value(171));
    }
}
