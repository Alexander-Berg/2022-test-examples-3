package ru.yandex.market.pers.feedback.helper;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.pers.feedback.builder.OrderCreatableBuilder;
import ru.yandex.market.pers.feedback.order.api.FeedbackType;
import ru.yandex.market.pers.feedback.order.api.OrderCreatable;
import ru.yandex.market.pers.feedback.order.api.OrderCreateReadable;
import ru.yandex.market.pers.feedback.order.api.OrderReadable;
import ru.yandex.market.pers.feedback.order.model.SecurityData;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class OrderFeedbackHelper extends MockMvcAware {
    private final ObjectMapper objectMapper;

    private OrderFeedbackHelper(WebApplicationContext webApplicationContext, ObjectMapper objectMapper) {
        super(webApplicationContext);
        this.objectMapper = objectMapper;
    }

    public OrderReadable getOrderFeedback(long orderId, int clientId) throws Exception {
        String orderReadableResponse = getOrderFeedback(orderId, clientId, status().isOk(), null)
            .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(orderReadableResponse, OrderReadable.class);
    }

    public ResultActions getOrderFeedback(long orderId, int clientId,
                                          ResultMatcher status, ResultMatcher reason) throws Exception {
        ResultActions result = mockMvc.perform(get("/orders/" + orderId + "/feedback")
            .param("clientId", String.valueOf(clientId))).andDo(log());
        if (status != null) {
            result.andExpect(status);
        }
        if (reason != null) {
            result.andExpect(reason);
        }
        return result;
    }

    public OrderCreateReadable putOrderFeedback(long orderId, int clientId, OrderCreatable orderCreatable,
                                                FeedbackType feedbackType) throws Exception {
        String result = putOrderFeedbackForActions(orderId, clientId, orderCreatable, feedbackType)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(result, OrderCreateReadable.class);
    }

    @NotNull
    public ResultActions putOrderFeedbackForActions(long orderId, int clientId, OrderCreatable orderCreatable,
                                                    FeedbackType feedbackType) throws Exception {
        MockHttpServletRequestBuilder req = put("/orders/{orderId}/feedback", orderId)
            .param("clientId", String.valueOf(clientId))
            .content(objectMapper.writeValueAsBytes(orderCreatable))
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding(StandardCharsets.UTF_8.name());
        if (feedbackType != null) {
            req.param("feedbackType", feedbackType.name());
        }
        return mockMvc.perform(req).andDo(log());
    }

    @NotNull
    public ResultActions putOrderFeedbackWithIp(long orderId, int clientId, String ip, Integer port) throws Exception {
        OrderCreatable feedback = OrderCreatableBuilder.builder()
            .withGrade(1)
            .withCallbackRequired(false)
            .withReviewDenied(false)
            .withComment("")
            .withNpsGrade(5)
            .build();

        MockHttpServletRequestBuilder req = put("/orders/{orderId}/feedback", orderId)
            .param("clientId", String.valueOf(clientId))
            .param("feedbackType", FeedbackType.ORDER.name())
            .header(SecurityData.HEADER_X_REAL_IP, ip)
            .content(objectMapper.writeValueAsBytes(feedback))
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding(StandardCharsets.UTF_8.name());

        if(port != null) {
            req.header(SecurityData.HEADER_X_SOURCE_PORT, String.valueOf(port));
        }

        return mockMvc.perform(req).andDo(log());
    }

    @NotNull
    public ResultActions putRawOrderFeedbackForActions(long orderId, int clientId, String rawOrderCreatable,
                                                       FeedbackType feedbackType) throws Exception {
        MockHttpServletRequestBuilder req = put("/orders/{orderId}/feedback", orderId)
            .param("clientId", String.valueOf(clientId))
            .content(rawOrderCreatable)
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding(StandardCharsets.UTF_8.name());
        if (feedbackType != null) {
            req.param("feedbackType", feedbackType.name());
        }
        return mockMvc.perform(req).andDo(log());
    }
}
