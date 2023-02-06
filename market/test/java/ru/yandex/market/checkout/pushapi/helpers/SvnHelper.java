package ru.yandex.market.checkout.pushapi.helpers;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.pushapi.client.entity.StocksRequest;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebTestHelper
public class SvnHelper extends MockMvcAware {

    public SvnHelper(MockMvc mockMvc, PushApiTestSerializationService testSerializationService) {
        super(mockMvc, testSerializationService);
    }

    private MockHttpServletRequestBuilder postXml(Object body, String uri, Object... uriVars) {
        String xmlBody = testSerializationService.serialize(body);
        return post(uri, uriVars)
                .contentType(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .content(xmlBody)
                .characterEncoding(StandardCharsets.UTF_8.name());
    }

    private MockHttpServletRequestBuilder postJson(Object body, String uri, Object... uriVars) {
        try {
            String json = new ObjectMapper().writeValueAsString(body);
            return post(uri, uriVars)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(json)
                    .characterEncoding(StandardCharsets.UTF_8.name());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ResultActions performQueryStocksXml(long shopId, StocksRequest request) throws Exception {
        return mockMvc.perform(postXml(request, "/svn-shop/{shopId}/stocks", shopId))
                .andDo(log())
                .andExpect(status().isOk());
    }

    public ResultActions performQueryStocksXml(long shopId,
                                               StocksRequest request,
                                               String authToken) throws Exception {
        return mockMvc.perform(postXml(request, "/svn-shop/{shopId}/stocks", shopId)
                        .param("auth-token", authToken))
                .andDo(log())
                .andExpect(status().isOk());
    }

    public ResultActions performQueryStocksJson(long shopId, StocksRequest request) throws Exception {
        return mockMvc.perform(postJson(request, "/svn-shop/{shopId}/stocks", shopId))
                .andDo(log())
                .andExpect(status().isOk());
    }

    public ResultActions performQueryStocksJson(long shopId,
                                                StocksRequest request,
                                                String authToken) throws Exception {
        return mockMvc.perform(postJson(request, "/svn-shop/{shopId}/stocks", shopId)
                        .param("auth-token", authToken))
                .andDo(log())
                .andExpect(status().isOk());
    }
}
