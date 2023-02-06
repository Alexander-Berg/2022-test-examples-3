package ru.yandex.market.helpers;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.shopadminstub.model.StocksRequest;
import ru.yandex.market.util.TestSerializationService;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebTestHelper
public class SvnHelper extends MockMvcAware {

    public SvnHelper(MockMvc mockMvc, TestSerializationService testSerializationService) {
        super(mockMvc, testSerializationService);
    }

    public ResultActions perfromQueryStocksXml(long shopId, StocksRequest request) throws Exception {
        return mockMvc.perform(post("/svn-shop/{shopId}/stocks", shopId)
                .contentType(MediaType.APPLICATION_XML)
                .content(testSerializationService.serializeXml(request))
                .characterEncoding(StandardCharsets.UTF_8.name()))
                .andDo(log())
                .andExpect(status().isOk());
    }

    public ResultActions perfromQueryStocksXml(long shopId,
                                               StocksRequest request,
                                               String authToken) throws Exception {
        return mockMvc.perform(post("/svn-shop/{shopId}/stocks", shopId)
                .param("auth-token", authToken)
                .contentType(MediaType.APPLICATION_XML)
                .content(testSerializationService.serializeXml(request))
                .characterEncoding(StandardCharsets.UTF_8.name()))
                .andDo(log())
                .andExpect(status().isOk());
    }

    public ResultActions performQueryStocksJson(long shopId, StocksRequest request) throws Exception {
        return mockMvc.perform(post("/svn-shop/{shopId}/stocks", shopId)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeJson(request)))
                .andDo(log())
                .andExpect(status().isOk());
    }

    public ResultActions performQueryStocksJson(long shopId,
                                                StocksRequest request,
                                                String authToken) throws Exception {
        return mockMvc.perform(post("/svn-shop/{shopId}/stocks", shopId)
                .param("auth-token", authToken)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeJson(request)))
                .andDo(log())
                .andExpect(status().isOk());
    }
}
