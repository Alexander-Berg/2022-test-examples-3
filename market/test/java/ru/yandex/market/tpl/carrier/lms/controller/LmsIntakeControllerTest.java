package ru.yandex.market.tpl.carrier.lms.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartner;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class LmsIntakeControllerTest extends LmsControllerTest {

    private final RunTemplateGenerator runTemplateGenerator;
    private final TestUserHelper testUserHelper;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;
    private final OrderWarehouseRepository orderWarehouseRepository;

    private OrderWarehousePartner partner;

    @BeforeEach
    void setUp() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        partner = orderWarehousePartnerRepository.saveAndFlush(
                new OrderWarehousePartner(
                        "135",
                        "ООО Пушкин"
                )
        );

        runTemplateGenerator.generate(cb -> cb
                .deliveryServiceId(9001L)
                .campaignId(company.getCampaignId())
                .externalId("abc")
        );
        runTemplateGenerator.generate(cb -> cb
                .deliveryServiceId(9001L)
                .campaignId(company.getCampaignId())
                .externalId("def"));

        OrderWarehouse warehouse = orderWarehouseRepository.findByYandexId("20").orElseThrow();
        warehouse.setPartner(partner);
        orderWarehouseRepository.saveAndFlush(warehouse);
    }

    @SneakyThrows
    @Test
    void shouldReturnIntakePage() {
        mockMvc.perform(get("/LMS/carrier/intake"))
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldReturnIntakeRunTemplateItems() {
        mockMvc.perform(get("/LMS/carrier/intake/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(4)));
    }

    @SneakyThrows
    @Test
    void shouldFilterItemsByPartner() {
        mockMvc.perform(get("/LMS/carrier/intake/items")
                .param("partner", String.valueOf(partner.getYandexId()))
        )

                .andExpect(jsonPath("$.items").value(Matchers.hasSize(2)));
    }

}
