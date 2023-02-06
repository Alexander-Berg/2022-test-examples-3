package ru.yandex.market.tpl.api.controller.api.locker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import ru.yandex.market.tpl.api.model.task.locker.delivery.LockerDeliveryCancelRequestDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.tpl.api.BaseApiIntTest.AUTH_HEADER_VALUE;

@TestComponent
@ConditionalOnWebApplication
public class LockerDeliveryTaskControllerRequests {

    @Autowired
    private ObjectMapper tplObjectMapper;
    @Autowired
    private MockMvc mockMvc;

    @SneakyThrows
    public ResultActions performCancelRequest(long taskId, LockerDeliveryCancelRequestDto request) {
        return mockMvc.perform(post("/api/tasks/locker-delivery/{taskId}/cancel", taskId)
            .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(tplObjectMapper.writeValueAsString(request))
        ).andDo(MockMvcResultHandlers.print());
    }

    @SneakyThrows
    public ResultActions performFinishBySupport(long taskId) {
        return mockMvc.perform(post("/api/tasks/locker-delivery/{taskId}/finish-by-support", taskId)
            .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andDo(MockMvcResultHandlers.print());
    }

    @SneakyThrows
    public LockerDeliveryTaskDto serialize(ResultActions actions) {
        return tplObjectMapper.readValue(actions.andReturn().getResponse().getContentAsString(), LockerDeliveryTaskDto.class);
    }

    public ResultActions performReopen(long taskId) throws Exception {
        return mockMvc.perform(post("/api/tasks/locker-delivery/{taskId}/reopen", taskId)
            .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
        );
    }
}
