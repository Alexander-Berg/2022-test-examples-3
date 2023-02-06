package ru.yandex.market.tpl.carrier.planner.controller.audit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class AuditInterceptorTest extends BasePlannerWebTest {

    @Test
    @SneakyThrows
    void shouldInterceptLoginAndSourceForAllButDsApiAndManualEndpoints() {
        mockMvc.perform(post("/internal/audit/login")
                        .header(CarrierAuditTracer.DEFAULT_LOGIN_HEADER, "belkinmike"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("login: belkinmike"));
        mockMvc.perform(get("/internal/audit/source")
                        .header(CarrierAuditTracer.DEFAULT_SOURCE_HEADER, CarrierSource.TRANSPORT_MANAGER))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("source: TRANSPORT_MANAGER"));
    }

    @Test
    @SneakyThrows
    void shouldInterceptLoginForManualEndpoints() {
        mockMvc.perform(post("/manual/audit/login")
                        .header(CarrierAuditTracer.DEFAULT_LOGIN_HEADER, "belkinmike"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("login: belkinmike"));
        mockMvc.perform(get("/manual/audit/source")
                        .header(CarrierAuditTracer.DEFAULT_SOURCE_HEADER, CarrierSource.TRANSPORT_MANAGER))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("source: MANUAL"));
    }

    @Test
    @SneakyThrows
    void shouldInterceptLoginForDsApiEndpoints() {
        mockMvc.perform(post("/delivery/query-gateway/audit/login")
                        .header(CarrierAuditTracer.DEFAULT_LOGIN_HEADER, "belkinmike"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("login: belkinmike"));
        mockMvc.perform(get("/delivery/query-gateway/audit/source")
                        .header(CarrierAuditTracer.DEFAULT_SOURCE_HEADER, CarrierSource.OPERATOR))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("source: TRANSPORT_MANAGER"));
    }
}
