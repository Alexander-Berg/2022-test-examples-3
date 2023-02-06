package ru.yandex.market.ff.controller.api;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.util.FileContentUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SupplierInfoControllerTest extends MvcIntegrationTest {

    @Test
    @DatabaseSetup("classpath:controller/real_supplier_info/by_id/before.xml")
    public void findSupplierInfosByIdGroupById() throws Exception {
        String response = FileContentUtils.getFileContent(
                "controller/real_supplier_info/by_id/response.json");

        mockMvc.perform(get("/supplier-info/group-by-id/3")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/real_supplier_info/by_ids/before.xml")
    public void testFindRealSupplierInfoByIds() throws Exception {
        String response = FileContentUtils.getFileContent(
                "controller/real_supplier_info/by_ids/response.json");

        mockMvc.perform(get("/supplier-info/aggregated-by-first-name")
                .param("supplierId", "b")
                .param("supplierId", "a")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/real_supplier_info/group-by-id/before.xml")
    public void testFindSupplierInfosByNameSubstringGroupById() throws Exception {
        String response = FileContentUtils.getFileContent(
                "controller/real_supplier_info/group-by-id/response.json");

        mockMvc.perform(post("/supplier-info/group-by-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierNameSubstr\":\"supplier\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/real_supplier_info/group-by-id/before.xml")
    public void testFindSupplierInfosByNameSubstringGroupByNameAndId() throws Exception {
        String response = FileContentUtils.getFileContent(
                "controller/real_supplier_info/group-by-id/response-by-name-and-id.json");

        mockMvc.perform(post("/supplier-info/group-by-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierNameSubstr\":\"печаль\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

}
