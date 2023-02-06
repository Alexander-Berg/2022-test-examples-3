package ru.yandex.market.pers.feedback.helper;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.pers.feedback.order.api.TakeoutDataWrapper;
import ru.yandex.market.pers.service.common.dto.TakeoutStatusDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class TakeoutHelper extends MockMvcAware {

    private final ObjectMapper objectMapper;

    public TakeoutHelper(WebApplicationContext webApplicationContext, ObjectMapper objectMapper) {
        super(webApplicationContext);
        this.objectMapper = objectMapper;
    }

    public String getFeedbackType() {
        return "feedback";
    }

    public TakeoutDataWrapper getData(long userId) throws Exception {
        String takeoutDataResponse = performRequest(get("/takeout")
                .param("uid", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(takeoutDataResponse, TakeoutDataWrapper.class);
    }

    public TakeoutStatusDto getStatus(long userId) throws Exception {
        String takeoutStatusResponse = performRequest(get("/takeout/status")
                .param("uid", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(takeoutStatusResponse, TakeoutStatusDto.class);
    }

    public void deleteHardFeedback(long userId, Set<String> types, long deleteBefore) throws Exception {
        performRequest(post("/takeout/delete/hard")
                .param("uid", String.valueOf(userId))
                .param("types", types.toArray(String[]::new))
                .param("time", String.valueOf(deleteBefore)))
                .andExpect(status().isOk());
    }
}
