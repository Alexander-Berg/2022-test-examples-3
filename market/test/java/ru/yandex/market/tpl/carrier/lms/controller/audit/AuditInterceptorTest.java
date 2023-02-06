package ru.yandex.market.tpl.carrier.lms.controller.audit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.lms.controller.LmsControllerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class AuditInterceptorTest extends LmsControllerTest {

    private final TestUserHelper testUserHelper;

    private Company company;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
    }

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
                        .header(CarrierAuditTracer.DEFAULT_SOURCE_HEADER, CarrierSource.TRANSPORT_MANAGER)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("source: CRM_OPERATOR"));
    }

}
