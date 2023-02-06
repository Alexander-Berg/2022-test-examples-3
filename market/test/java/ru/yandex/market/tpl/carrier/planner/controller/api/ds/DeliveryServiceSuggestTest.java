package ru.yandex.market.tpl.carrier.planner.controller.api.ds;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.planner.service.api.ds.DeliveryServiceSpecification.MK_VIRTUAL_INTER_WAREHOUSE_CARRIER;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DeliveryServiceSuggestTest extends BasePlannerWebTest {

    private final TestUserHelper testUserHelper;
    private final DsRepository dsRepository;

    @BeforeEach
    void setup() {
        testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName("Another company")
                .campaignId(1001L)
                .deliveryServiceIds(Set.of(MK_VIRTUAL_INTER_WAREHOUSE_CARRIER))
                .login("login-1")
                .build());
        testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName("Another other company")
                .campaignId(1002L)
                .deliveryServiceIds(Set.of(125L))
                .login("login-2")
                .build());
    }

    @SneakyThrows
    @Test
    void getAllWithoutFakes() {
        mockMvc.perform(get("/internal/delivery-services/suggest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));
    }

    @SneakyThrows
    @Test
    void getAllWithFakes() {
        mockMvc.perform(get("/internal/delivery-services/suggest?includeFakes=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(3)));
    }

    @SneakyThrows
    @Test
    void getBySubstring() {
        DeliveryService ds = dsRepository.findByIdOrThrow(125L);
        ds.setName("МоноПОЛИЯ");
        dsRepository.save(ds);

        mockMvc.perform(get("/internal/delivery-services/suggest?substring=поли"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("МоноПОЛИЯ"));
    }

}
