package ru.yandex.market.replenishment.autoorder.api;

import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockLogin
public class RegionControllerTest extends ControllerTest {

    @Test
    @DbUnitDataSet(before = "RegionControllerTest.before.csv")
    public void testGet() throws Exception {
        mockMvc.perform(get("/api/v2/region"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(106));
    }

}
