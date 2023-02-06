package ru.yandex.market.tsup.audit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.core.domain.base.Source;
import ru.yandex.market.tsup.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AuditInterceptorTest extends AbstractContextualTest {

    @Test
    @SneakyThrows
    void shouldInterceptLoginForAllEndpoints() {
        mockMvc.perform(post("/internal/audit/login")
                        .header(CarrierAuditTracer.DEFAULT_LOGIN_HEADER, "belkinmike")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("login: belkinmike"));

        mockMvc.perform(get("/internal/audit/source")
                        .header(CarrierAuditTracer.DEFAULT_SOURCE_HEADER, Source.TRANSPORT_MANAGER)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("source: OPERATOR"));
    }

}
