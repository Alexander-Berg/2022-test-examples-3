package ru.yandex.market.tpl.carrier.lms.controller.runtemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplate;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.lms.controller.LmsControllerTest;
import ru.yandex.market.tpl.carrier.planner.lms.runtemplate.LmsRunTemplateUpdateDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LmsRunTemplateUpdateTest extends LmsControllerTest {

    private final TestUserHelper testUserHelper;
    private final RunTemplateGenerator runTemplateGenerator;
    private final RunTemplateRepository runTemplateRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    private Company company;
    private Company company2;
    private RunTemplate runTemplate;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        company2 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName("Другая")
                .campaignId(228L)
                .login("another-login@yandex.ru")
                .build());

        runTemplate = runTemplateGenerator.generate(cb -> {
            cb.campaignId(company.getCampaignId());
        });
    }

    @SneakyThrows
    @Test
    void shouldUpdateRunTemplate() {
        mockMvc.perform(put("/LMS/carrier/run-templates/{id}", runTemplate.getId())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(new LmsRunTemplateUpdateDto(
                        runTemplate.getExternalId(),
                        company2.getId()
                )))
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.item.values.companyId").value(company2.getId()));

        runTemplate = runTemplateRepository.findByIdOrThrow(runTemplate.getId());
        Assertions.assertThat(runTemplate.getCompany().getId())
                .isEqualTo(company2.getId());
    }
}
