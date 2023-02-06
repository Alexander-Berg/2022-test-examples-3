package ru.yandex.market.tpl.partner.carrier.controller.partner;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.company_property.CompanyProperty;
import ru.yandex.market.tpl.carrier.core.domain.company_property.CompanyPropertyRepository;
import ru.yandex.market.tpl.carrier.core.domain.company_property.CompanyPropertyType;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class PartnerCarrierCompanyControllerTest extends BaseTplPartnerCarrierWebIntTest {

    private final CompanyPropertyRepository companyPropertyRepository;
    private final TestUserHelper testUserHelper;

    private Company company;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .login("anotherLogin@yandex.ru")
                .build()
        );

        transactionTemplate.execute(tx -> {

                    CompanyProperty<Boolean> relayRunProperty = new CompanyProperty<>();
                    relayRunProperty.init(company, CompanyPropertyType.CompanyPropertyName.RELAY_RUN_ALLOWED, "true");
                    companyPropertyRepository.save(relayRunProperty);
                    return null;
                }
        );

    }

    @SneakyThrows
    @Test
    void testProperty() {
        mockMvc.perform(get("/internal/partner/properties")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties").isMap())
                .andExpect(jsonPath("$.properties.RELAY_RUN_ALLOWED").value("true"));
    }

}