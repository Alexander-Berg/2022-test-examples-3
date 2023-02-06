package ru.yandex.market.pers.feedback.helper;

import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.pers.feedback.order.api.OrderGradesReadable;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class OrderGradesHelper extends MockMvcAware {
    private final ObjectMapper objectMapper;

    public OrderGradesHelper(WebApplicationContext webApplicationContext, ObjectMapper objectMapper) {
        super(webApplicationContext);
        this.objectMapper = objectMapper;
    }

    public OrderGradesReadable getOrderGrades(List<Long> orderIds) throws Exception {
        String orderGradesReadableResponse = performRequest(get("/grades")
                .param("clientId", "1")
                .param("orderIds", orderIds.stream().map(String::valueOf).toArray(String[]::new)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        return objectMapper.readValue(orderGradesReadableResponse, OrderGradesReadable.class);
    }
}
