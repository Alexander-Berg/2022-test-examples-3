package ru.yandex.market.tpl.partner.carrier.controller.partner.filters;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.core.domain.company.Company.DEFAULT_COMPANY_NAME;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PartnerCarrierFiltersControllerIntTest extends BaseTplPartnerCarrierWebIntTest {

    private final TestUserHelper testUserHelper;

    private Company company;
    private User user;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(DEFAULT_COMPANY_NAME);
        user = testUserHelper.findOrCreateUser(UID);
    }

    @SneakyThrows
    @Test
    void shouldGetFilters() {
        mockMvc.perform(
                get("/internal/partner/filters")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldGetFiltersCourier() {
        mockMvc.perform(
                get("/internal/partner/filters/courier")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.options").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.options[0].id").value(Matchers.equalTo(user.getId().intValue())))
                .andExpect(jsonPath("$.options[0].uid").value(Matchers.equalTo((int) user.getUid())))
                .andExpect(jsonPath("$.options[0].name").value(Matchers.equalTo(user.getName())))
        ;
    }

    @SneakyThrows
    @Test
    void shouldGetFiltersByPhone() {

        mockMvc.perform(
                        get("/internal/partner/filters/courier")
                                .param("phone", "223")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options").value(Matchers.hasSize(1)));

        mockMvc.perform(
                        get("/internal/partner/filters/courier")
                                .param("phone", "999")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options").value(Matchers.hasSize(0)));
    }

    @SneakyThrows
    @Test
    void shouldGetFiltersByName() {

        mockMvc.perform(
                        get("/internal/partner/filters/courier")
                                .param("name", "Комаров")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options").value(Matchers.hasSize(1)));

        mockMvc.perform(
                        get("/internal/partner/filters/courier")
                                .param("name", "Иванов")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options").value(Matchers.hasSize(0)));
    }

    @SneakyThrows
    @Test
    void shouldGetTransportTypes() {
        mockMvc.perform(
                get("/internal/partner/filters/transportTypes")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
        ;
    }
}
