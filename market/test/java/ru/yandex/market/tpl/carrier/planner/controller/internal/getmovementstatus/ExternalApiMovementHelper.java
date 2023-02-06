package ru.yandex.market.tpl.carrier.planner.controller.internal.getmovementstatus;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.delivery.request.GetMovementStatusHistoryRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetMovementStatusRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@RequiredArgsConstructor

@Component
public class ExternalApiMovementHelper {

    private final MockMvc mockMvc;
    private final XmlMapper xmlMapper = Jackson2ObjectMapperBuilder.xml().build();

    @SneakyThrows
    public ResultActions performGetMovementStatus(RequestWrapper<GetMovementStatusRequest> request) {
        return mockMvc.perform(post("/delivery/query-gateway/getMovementStatus")
                        .content(xmlMapper.writeValueAsString(request))
                        .contentType(MediaType.TEXT_XML_VALUE))
                .andExpect(status().isOk())
                .andExpect(xpath("//root/requestState/isError").booleanValue(false));
    }

    @SneakyThrows
    public ResultActions performGetMovementStatusHistory(RequestWrapper<GetMovementStatusHistoryRequest> request) {
        return mockMvc.perform(post("/delivery/query-gateway/getMovementStatusHistory")
                        .content(xmlMapper.writeValueAsString(request))
                        .contentType(MediaType.TEXT_XML_VALUE))
                .andExpect(status().isOk())
                .andExpect(xpath("//root/requestState/isError").booleanValue(false));
    }
}
