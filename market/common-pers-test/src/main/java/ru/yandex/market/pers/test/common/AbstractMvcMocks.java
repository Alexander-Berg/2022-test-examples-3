package ru.yandex.market.pers.test.common;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.util.ExecUtils;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 15.08.2019
 */
public abstract class AbstractMvcMocks {
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("mockMvc")
    protected MockMvc mockMvc;

    protected String invokeAndGet(MockHttpServletRequestBuilder requestBuilder, ResultMatcher expected) {
        return invokeMvc(mockMvc, requestBuilder, expected);
    }

    protected String invokeAndRetrieveResponse(MockHttpServletRequestBuilder requestBuilder, ResultMatcher expected) {
        return invokeMvc(mockMvc, requestBuilder, expected);
    }

    public static String invokeMvc(MockMvc mockMvc,
                                   MockHttpServletRequestBuilder requestBuilder,
                                   ResultMatcher expected) {
        try {
            return mockMvc.perform(requestBuilder
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(expected)
                .andReturn().getResponse().getContentAsString();
        } catch (Exception e) {
            throw ExecUtils.silentError(e, "Failed mvcMock call");
        }
    }

    protected <T> T parseValue(String content, TypeReference<T> valueTypeRef) {
        try {
            return objectMapper.readValue(content, valueTypeRef);
        } catch (IOException e) {
            throw ExecUtils.silentError(e);
        }
    }

    protected <T> T parseValue(String content, Class<T> type) {
        try {
            return objectMapper.readValue(content, type);
        } catch (IOException e) {
            throw ExecUtils.silentError(e);
        }
    }

    public static <T, R> String[] toArrayStr(Collection<T> items) {
        return toArrayStr(items, x->x);
    }

    public static <T, R> String[] toArrayStr(Collection<T> items, Function<T, R> mapping) {
        if (items == null || items.isEmpty()) {
            // totally hacked
            return new String[]{null};
        }
        return items.stream().map(mapping).map(String::valueOf).toArray(String[]::new);
    }

}
