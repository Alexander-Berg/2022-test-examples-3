package ru.yandex.market.tpl.carrier.driver.controller.audit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class AuditInterceptorTest  extends BaseDriverApiIntTest {

    private final TestUserHelper testUserHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private User user;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.USER_DRAFT_ENABLED, true);
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        user = testUserHelper.findOrCreateUser(UserUtil.TAXI_ID, UID);
    }

    @Test
    @SneakyThrows
    void shouldInterceptLoginForAllEndpoints() {
        mockMvc.perform(post("/internal/audit/login")
                        .header(ApiParams.TAXI_PARK_ID_HEADER, user.getTaxiId().split("_")[0])
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, user.getTaxiId().split("_")[1])
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("login: Комаров Пашка"));
        mockMvc.perform(post("/internal/audit/login")
                        .header(CarrierAuditTracer.DEFAULT_LOGIN_HEADER, "belkinmike")
                        .header(ApiParams.TAXI_PARK_ID_HEADER, user.getTaxiId().split("_")[0])
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, user.getTaxiId().split("_")[1])
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("login: belkinmike"));
        mockMvc.perform(get("/internal/audit/source")
                        .header(CarrierAuditTracer.DEFAULT_SOURCE_HEADER, CarrierSource.TRANSPORT_MANAGER)
                        .header(ApiParams.TAXI_PARK_ID_HEADER, user.getTaxiId().split("_")[0])
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, user.getTaxiId().split("_")[1])
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("source: COURIER"));
    }

}
