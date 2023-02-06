package ru.yandex.market.tpl.billing.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.dto.EnvironmentDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты для {@link EnvironmentController}
 */
public class EnvironmentControllerTest extends AbstractFunctionalTest {

    @Autowired
    private MockMvc restMockMvc;

    @Test
    @DbUnitDataSet(
            before = "/controller/environments/environmentsBefore.csv",
            after = "/controller/environments/environmentsAfter.csv")
    void updateEnvVariableTest() throws Exception {

        EnvironmentDto requestBody = new EnvironmentDto();
        requestBody.setKey("key-3");
        requestBody.setValue("value3");

        restMockMvc.perform(post("/environment")
                        .content(asJsonString(requestBody))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = "/controller/environments/environmentsBefore.csv",
            after = "/controller/environments/environmentNotFoundAfter.csv")
    void updateEnvVariableDoesNotExistTest() throws Exception {

        EnvironmentDto requestBody = new EnvironmentDto();
        requestBody.setKey("k-3");
        requestBody.setValue("value3");

        restMockMvc.perform(
                post("/environment")
                        .content(asJsonString(requestBody))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "/controller/environments/environmentsBefore.csv")
    void getSpecifiedEnvironmentTest() throws Exception {
        restMockMvc.perform(
                get("/environment")
                        .param("key", "key-3")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variable").value("val-3"))
                .andExpect(jsonPath("$.isEnvironmentFound").value(true));
    }

    @Test
    @DbUnitDataSet(before = "/controller/environments/environmentsBefore.csv")
    void getSpecifiedEnvironmentEmptyTest() throws Exception {
        restMockMvc.perform(
                get("/environment")
                        .param("key", "k-3")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variable").value(""))
                .andExpect(jsonPath("$.isEnvironmentFound").value(false));
    }
}
