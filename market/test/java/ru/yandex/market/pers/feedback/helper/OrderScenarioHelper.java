package ru.yandex.market.pers.feedback.helper;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.pers.feedback.order.api.OrderScenarioRuleCreateable;
import ru.yandex.market.pers.feedback.order.api.OrderScenarioRuleReadable;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class OrderScenarioHelper extends MockMvcAware {
    private final ObjectMapper objectMapper;

    public OrderScenarioHelper(WebApplicationContext webApplicationContext, ObjectMapper objectMapper) {
        super(webApplicationContext);
        this.objectMapper = objectMapper;
    }

    public OrderScenarioRuleReadable postOrderScenarioRule(OrderScenarioRuleCreateable createable) throws Exception {
        MvcResult result = performRequest(post("/scenario/rules")
                .content(objectMapper.writeValueAsString(createable))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), OrderScenarioRuleReadable.class);
    }

    public List<OrderScenarioRuleReadable> getOrderScenarioRules() throws Exception {
        MvcResult result = performRequest(get("/scenario/rules"))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<OrderScenarioRuleReadable>>() {});
    }

    public void putOrderScenarioRule(long id, OrderScenarioRuleCreateable body) throws Exception {
        performRequest(put("/scenario/rules/{id}", id)
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());
    }
}
