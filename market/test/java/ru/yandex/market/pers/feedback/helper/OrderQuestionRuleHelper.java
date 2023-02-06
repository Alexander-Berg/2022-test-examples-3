package ru.yandex.market.pers.feedback.helper;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletPurpose;
import ru.yandex.market.pers.feedback.builder.OrderQuestionRuleCreatableBuilder;
import ru.yandex.market.pers.feedback.order.api.DeliveredOnTime;
import ru.yandex.market.pers.feedback.order.api.FeedbackType;
import ru.yandex.market.pers.feedback.order.api.OrderFeedbackQuestion;
import ru.yandex.market.pers.feedback.order.api.OrderQuestionRuleCreatable;
import ru.yandex.market.pers.feedback.order.api.OrderQuestionRuleReadable;
import ru.yandex.market.pers.feedback.order.api.QuestionCategory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class OrderQuestionRuleHelper extends MockMvcAware {
    private final ObjectMapper objectMapper;

    public OrderQuestionRuleHelper(WebApplicationContext webApplicationContext,
                                   ObjectMapper objectMapper) {
        super(webApplicationContext);
        this.objectMapper = objectMapper;
    }

    public OrderQuestionRuleReadable postQuestionRule(OrderQuestionRuleCreatable questionRuleCreatable) throws Exception {
        String response = performRequest(post("/questions/rules")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsBytes(questionRuleCreatable))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, OrderQuestionRuleReadable.class);
    }

    public List<OrderQuestionRuleReadable> getQuestionRules() throws Exception {
        String content = performRequest(get("/questions/rules"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(content, new TypeReference<List<OrderQuestionRuleReadable>>() {});
    }

    public void deleteQuestionRule(long id) throws Exception {
        performRequest(delete("/questions/rules/{id}", id))
                .andExpect(status().isOk());
    }

    public void putQuestionRule(long id, OrderQuestionRuleCreatable questionRule) throws Exception {
        performRequest(put("/questions/rules/{id}", id)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(questionRule)))
                .andExpect(status().isOk());
    }

    public void addRuleWithFeedbackType(OrderFeedbackQuestion question, FeedbackType type) throws Exception {
        postQuestionRule(getRuleBuilder(question)
            .withFeedbackType(type.value())
            .build()
        );
    }

    public void addRuleWithDeliveredOnTime(OrderFeedbackQuestion question, DeliveredOnTime dot) throws Exception {
        postQuestionRule(getRuleBuilder(question)
            .withDeliveredOnTime(dot.value())
            .build()
        );
    }

    public void addRuleWithCategory(OrderFeedbackQuestion question, QuestionCategory category) throws Exception {
        postQuestionRule(getRuleBuilder(question)
            .withCategory(category.value())
            .build()
        );
    }

    public void addRuleWithOutletPurpose(OrderFeedbackQuestion question, OutletPurpose purpose) throws Exception {
        postQuestionRule(getRuleBuilder(question)
            .withOutletPurpose(purpose.getId())
            .build()
        );
    }

    private OrderQuestionRuleCreatableBuilder getRuleBuilder(OrderFeedbackQuestion question) {
        return new OrderQuestionRuleCreatableBuilder()
            .withQuestionId(question.getId())
            .withDeliveryType(-1)
            .withOutletPurpose(-1)
            .withPaymentMethod(-1)
            .withPaymentType(-1)
            .withShowGrade(-1)
            .withCategory(QuestionCategory.UNKNOWN.value())
            .withDeliveredOnTime(DeliveredOnTime.UNKNOWN.value())
            .withFeedbackType(FeedbackType.UNKNOWN.value());
    }

}
