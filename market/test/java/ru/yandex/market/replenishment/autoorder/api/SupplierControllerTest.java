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
public class SupplierControllerTest extends ControllerTest {

    @Test
    @DbUnitDataSet(before = "SupplierControllerTest.before.csv")
    public void testGetSingleById() throws Exception {
        mockMvc.perform(get("/suppliers/1337"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1337))
            .andExpect(jsonPath("$.name").value("supp"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerTest.before.csv")
    public void testGetMultipleByIds() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers?ids=1337&ids=420"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[?(@.id==1337)].name").value("supp"))
            .andExpect(jsonPath("$[?(@.id==420)].name").value("blaze_it"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerTest.before.csv")
    public void testFakeTenderSupplierShouldNotBeFoundByName() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/search?name=Fake"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerTest.before.csv")
    public void testFakeTenderSupplierShouldNotBeFoundByEmptySearch() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/search"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].id").value(420))
            .andExpect(jsonPath("$[1].id").value(112))
            .andExpect(jsonPath("$[2].id").value(1337));
    }
}
