package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.delivery.request.PutMovementRequest;
import ru.yandex.market.logistic.api.model.delivery.request.PutTripRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@RequiredArgsConstructor

@TestComponent
@ConditionalOnWebApplication
public class PutMovementHelper {

    private final MockMvc mockMvc;
    private final XmlMapper xmlMapper = Jackson2ObjectMapperBuilder.xml().build();

    @SneakyThrows
    public ResultActions performPutMovement(RequestWrapper<PutMovementRequest> request) {
        return mockMvc.perform(post("/delivery/query-gateway/putMovement")
                .content(xmlMapper.writeValueAsString(request))
                .contentType(MediaType.TEXT_XML_VALUE))
                .andExpect(status().isOk())
                .andExpect(xpath("//root/requestState/isError").booleanValue(false));
    }

    @SneakyThrows
    public ResultActions performPutMovementWithoutErrorCheck(RequestWrapper<PutMovementRequest> request) {
        return mockMvc.perform(post("/delivery/query-gateway/putMovement")
                        .content(xmlMapper.writeValueAsString(request))
                        .contentType(MediaType.TEXT_XML_VALUE))
                .andExpect(status().isOk());
    }

    @SneakyThrows
    public ResultActions performPutTrip(RequestWrapper<PutTripRequest> request) {
        return mockMvc.perform(post("/delivery/query-gateway/putTrip")
                        .content(xmlMapper.writeValueAsString(request))
                        .contentType(MediaType.TEXT_XML_VALUE))
                .andExpect(status().isOk());
    }
}
