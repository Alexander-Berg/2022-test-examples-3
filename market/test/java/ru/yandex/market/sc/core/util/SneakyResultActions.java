package ru.yandex.market.sc.core.util;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@RequiredArgsConstructor
public class SneakyResultActions implements ResultActions {

    public static final ObjectMapper OBJECT_MAPPER = createJsonMapper();
    private final ResultActions resultActions;

    @SneakyThrows
    @Override
    public SneakyResultActions andExpect(ResultMatcher matcher) {
        return new SneakyResultActions(resultActions.andExpect(matcher));
    }

    @SneakyThrows
    @Override
    public SneakyResultActions andDo(ResultHandler handler) {
        return new SneakyResultActions(resultActions.andDo(handler));
    }

    @Override
    public MvcResult andReturn() {
        return resultActions.andReturn();
    }

    @SneakyThrows
    public String getResponseAsString() {
        return resultActions.andReturn().getResponse().getContentAsString();
    }

    @SneakyThrows
    public <R> R getResponseAsClass(Class<R> clazz) {
        return OBJECT_MAPPER.readValue(getResponseAsString(), clazz);
    }

    private static ObjectMapper createJsonMapper() {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setSerializationInclusion(NON_NULL);

        return objectMapper;
    }

}
