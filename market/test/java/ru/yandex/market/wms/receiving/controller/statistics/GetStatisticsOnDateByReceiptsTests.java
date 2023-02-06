package ru.yandex.market.wms.receiving.controller.statistics;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class GetStatisticsOnDateByReceiptsTests extends ReceivingIntegrationTest {

    @Test
    @DatabaseSetup("/cancel-inbound/before/common.xml")
    public void test1() {
        assertRequest(
                mockMvc,
                get("/statistics/2021-05-20/receipts"),
                status().isOk(),
                "controller/statistics/test1/response.json"
        );
    }

    private static void assertRequest(MockMvc mockMvc,
                                      MockHttpServletRequestBuilder requestBuilder,
                                      ResultMatcher status,
                                      String responseFile) {
        assertRequest(mockMvc, requestBuilder, status, responseFile, null);
    }

    @SneakyThrows
    private static void assertRequest(MockMvc mockMvc,
                                      MockHttpServletRequestBuilder requestBuilder,
                                      ResultMatcher status,
                                      String responseFile,
                                      String requestFile) {
        requestBuilder.contentType(MediaType.APPLICATION_JSON);
        if (requestFile != null) {
            requestBuilder.content(getFileContent(requestFile));
        }
        ResultActions result = mockMvc.perform(requestBuilder)
                .andExpect(status);
        if (responseFile != null) {
            String response = getFileContent(responseFile);
            result.andExpect(content().json(response, false));
        }
    }
}
