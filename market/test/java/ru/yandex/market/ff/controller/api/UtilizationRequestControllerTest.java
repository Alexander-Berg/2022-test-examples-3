package ru.yandex.market.ff.controller.api;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.util.FileContentUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UtilizationRequestControllerTest extends MvcIntegrationTest {

    @Test
    @DatabaseSetup("classpath:controller/request-api/find-utilization-transfer-items-count.xml")
    void findUtilizationTransferItemsCountAndNoResultWithWrongStatuses() throws Exception {
        String request = FileContentUtils.getFileContent(
                "controller/request-api/find-utilization-transfer-items-count/no-result/request.json");
        String response = FileContentUtils.getFileContent(
                "controller/request-api/find-utilization-transfer-items-count/no-result/response.json");

        mockMvc.perform(
                        post("/utilization/get-utilization-items-count")
                                .contentType(ContentType.APPLICATION_JSON.getMimeType())
                                .content(request)
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/find-utilization-transfer-items-count.xml")
    void findUtilizationTransferItemsCountAndCorrectAggregationAndFilter() throws Exception {
        String request = FileContentUtils.getFileContent(
                "controller/request-api/find-utilization-transfer-items-count/correct-sum/request.json");
        String response = FileContentUtils.getFileContent(
                "controller/request-api/find-utilization-transfer-items-count/correct-sum/response.json");

        mockMvc.perform(
                        post("/utilization/get-utilization-items-count")
                                .contentType(ContentType.APPLICATION_JSON.getMimeType())
                                .content(request)
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/find-utilization-transfer-items-count.xml")
    void findUtilizationTransferItemsCountMultipleWarehousesAndCorrectAggregation() throws Exception {
        String correctWarehouseAggregationPath =
                "controller/request-api/find-utilization-transfer-items-count/correct-warehouse-aggregation/";
        String request = FileContentUtils.getFileContent(
                correctWarehouseAggregationPath + "request.json");
        String response = FileContentUtils.getFileContent(
                correctWarehouseAggregationPath + "response.json");

        mockMvc.perform(
                        post("/utilization/get-utilization-items-count")
                                .contentType(ContentType.APPLICATION_JSON.getMimeType())
                                .content(request)
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }
}
