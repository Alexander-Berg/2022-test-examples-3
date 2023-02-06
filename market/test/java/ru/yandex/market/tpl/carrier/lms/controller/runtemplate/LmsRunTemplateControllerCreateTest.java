package ru.yandex.market.tpl.carrier.lms.controller.runtemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.lms.controller.LmsControllerTest;
import ru.yandex.market.tpl.carrier.planner.lms.runtemplate.LmsRunTemplateCreateDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LmsRunTemplateControllerCreateTest extends LmsControllerTest {

    private final TestUserHelper testUserHelper;
    private final ObjectMapper objectMapper;

    private Company company;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .campaignId(1234L)
                .build());
    }


    @SneakyThrows
    @Test
    void shouldCreateRunTemplate() {
        mockMvc.perform(post("/LMS/carrier/run-templates")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(LmsRunTemplateCreateDto.builder()
                        .campaignId(company.getCampaignId())
                        .deliveryServiceId(123L)
                        .externalId("externalId")
                        .build()
                ))
        )
                .andExpect(status().isOk());
    }
}
