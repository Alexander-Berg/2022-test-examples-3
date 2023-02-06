package ru.yandex.market.checkout.helpers.utils;

import java.lang.reflect.Type;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class MockMvcAware {

    protected final TestSerializationService testSerializationService;
    private final WebApplicationContext webApplicationContext;
    @Autowired
    protected MockMvc mockMvc;

    public MockMvcAware(WebApplicationContext webApplicationContext,
                        TestSerializationService testSerializationService) {
        this.webApplicationContext = webApplicationContext;
        this.testSerializationService = testSerializationService;
    }

    protected <T> T performApiRequest(MockHttpServletRequestBuilder builder, Class<T> clazz) throws Exception {
        return performApiRequest(builder, clazz, null);
    }

    protected <T> T performApiRequest(MockHttpServletRequestBuilder builder, Class<T> clazz,
                                      ResultActionsContainer container) throws Exception {
        MockHttpServletResponse response = performAndGetResponse(builder, container);

        if (response.getStatus() == HttpServletResponse.SC_OK) {
            return testSerializationService.deserializeCheckouterObject(response.getContentAsString(), clazz);
        } else {
            return null;
        }
    }

    protected <T> T performApiRequest(MockHttpServletRequestBuilder builder, Type type, Class contextClass,
                                      ResultActionsContainer container) throws Exception {
        MockHttpServletResponse responsse = performAndGetResponse(builder, container);

        if (responsse.getStatus() == HttpServletResponse.SC_OK) {
            return testSerializationService.deserializeCheckouterObject(responsse.getContentAsString(), type,
                    contextClass);
        } else {
            return null;
        }
    }

    private MockHttpServletResponse performAndGetResponse(MockHttpServletRequestBuilder builder,
                                                          ResultActionsContainer container) throws Exception {
        ResultActions resultActions = performApiRequest(builder);
        if (container != null) {
            container.propagateResultActions(resultActions);
        } else {
            resultActions.andExpect(status().isOk());
        }
        return resultActions
                .andReturn()
                .getResponse();
    }

    protected ResultActions performApiRequest(MockHttpServletRequestBuilder builder) throws Exception {
        return mockMvc.perform(builder)
                .andDo(log());
    }
}
