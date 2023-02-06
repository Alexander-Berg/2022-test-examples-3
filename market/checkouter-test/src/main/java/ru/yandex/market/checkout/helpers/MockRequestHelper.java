package ru.yandex.market.checkout.helpers;

import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.WebTestHelper;

import static org.apache.http.HttpStatus.SC_OK;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.util.GenericMockHelper.withUserRole;

@WebTestHelper
public class MockRequestHelper {

    @Autowired
    protected MockMvc mockMvc;

    @Resource(name = "checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapperPrototype;

    public <T> T request(RequestBuilder requestBuilder, Class<? extends T> returnType, ResultMatcher... matchers)
            throws Exception {
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        for (ResultMatcher matcher : matchers) {
            resultActions.andExpect(matcher);
        }

        return objectMapperPrototype.readValue(
                resultActions.andReturn().getResponse().getContentAsString(), returnType);
    }

    public <T> T request(RequestBuilder requestBuilder, Class<? extends T> returnType) throws Exception {
        return request(requestBuilder, returnType, status().is(SC_OK));
    }

    public <T> T get(Class<? extends T> returnType, Order order, String path, Object... vars) throws Exception {
        return objectMapperPrototype.readValue(
                mockMvc.perform(withUserRole(MockMvcRequestBuilders.get(path, vars), order))
                        .andReturn().getResponse().getContentAsString(), returnType);
    }

    public <R, T> R post(Class<? extends R> returnType, Order order, T content, String path, Object... vars)
            throws Exception {
        return objectMapperPrototype.readValue(
                mockMvc.perform(withUserRole(MockMvcRequestBuilders.post(path, vars)
                        .content(objectMapperPrototype.writeValueAsString(content)), order))
                        .andReturn().getResponse().getContentAsString(), returnType);
    }

    public <T> MvcResult post(Order order, T content, String path, Object... vars) throws Exception {
        return mockMvc.perform(withUserRole(MockMvcRequestBuilders.post(path, vars)
                .content(objectMapperPrototype.writeValueAsString(content)), order))
                .andReturn();
    }

    public <R, T> R patch(Class<? extends R> returnType, Order order, T content, String path, Object... vars)
            throws Exception {
        return objectMapperPrototype.readValue(
                mockMvc.perform(withUserRole(MockMvcRequestBuilders.patch(path, vars)
                        .content(objectMapperPrototype.writeValueAsString(content)), order))
                        .andReturn().getResponse().getContentAsString(), returnType);
    }

    public <T> void put(Order order, T content, String path, Object... vars) throws Exception {
        mockMvc.perform(withUserRole(MockMvcRequestBuilders.post(path, vars)
                .content(objectMapperPrototype.writeValueAsString(content)), order));
    }
}
