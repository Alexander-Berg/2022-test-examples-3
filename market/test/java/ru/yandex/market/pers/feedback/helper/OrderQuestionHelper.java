package ru.yandex.market.pers.feedback.helper;

import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.pers.feedback.order.api.OrderFeedbackQuestion;
import ru.yandex.market.pers.feedback.order.api.OrderQuestionCreatable;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class OrderQuestionHelper extends MockMvcAware {
    private final ObjectMapper objectMapper;

    private OrderQuestionHelper(WebApplicationContext webApplicationContext, ObjectMapper objectMapper) {
        super(webApplicationContext);
        this.objectMapper = objectMapper;
    }

    public List<OrderFeedbackQuestion> getQuestions() throws Exception {
        String contentAsString = performRequest(get("/questions"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(contentAsString, new TypeReference<List<OrderFeedbackQuestion>>() {
        });
    }

    @Nonnull
    public OrderFeedbackQuestion putQuestion(@Nonnull OrderQuestionCreatable questionCreatable) throws Exception {
        String contentAsString = performRequest(put("/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(questionCreatable)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(contentAsString, OrderFeedbackQuestion.class);
    }

    public void deleteQuestion(long id) throws Exception {
        performRequest(delete("/questions/{id}", id))
                .andExpect(status().isOk());
    }

    public void updateQuestion(long id, OrderQuestionCreatable question) throws Exception {
        performRequest(put("/questions/{id}", id)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(question)))
                .andExpect(status().isOk());
    }
}
