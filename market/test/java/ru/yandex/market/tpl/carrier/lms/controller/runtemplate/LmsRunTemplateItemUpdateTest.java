package ru.yandex.market.tpl.carrier.lms.controller.runtemplate;

import java.time.DayOfWeek;

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
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateItem;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.lms.controller.LmsControllerTest;
import ru.yandex.market.tpl.carrier.planner.lms.runtemplate.item.LmsRunTemplateItemUpdateDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class LmsRunTemplateItemUpdateTest extends LmsControllerTest {
    private final TestUserHelper testUserHelper;
    private final RunTemplateGenerator runTemplateGenerator;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    private final RunTemplateRepository runTemplateRepository;

    private Company company;
    private RunTemplate runTemplate;

    private RunTemplateItem runTemplateItem;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        transactionTemplate.execute(tc -> {
            runTemplate = runTemplateGenerator.generate(cb -> {
                cb.campaignId(company.getCampaignId());
            });
            runTemplateItem = runTemplate.streamItems().findFirst().orElseThrow();
            return null;
        });
    }

    @SneakyThrows
    @Test
    void updateDaysOfWeek() {
        LmsRunTemplateItemUpdateDto updateDto = new LmsRunTemplateItemUpdateDto();
        updateDto.setOrderNumber(runTemplateItem.getOrderNumber());
        updateDto.setMonday(true);
        updateDto.setTuesday(true);
        updateDto.setWednesday(false);
        updateDto.setThursday(true);
        updateDto.setFriday(true);
        updateDto.setSaturday(true);
        updateDto.setSunday(true);

        mockMvc.perform(put("/LMS/carrier/run-templates/{id}/items/{itemId}", runTemplate.getId(), runTemplateItem.getId())
                .content(objectMapper.writeValueAsString(updateDto))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            runTemplate = runTemplateRepository.findByIdOrThrow(runTemplate.getId());
            runTemplateItem = runTemplate.streamItems().filterBy(RunTemplateItem::getId, runTemplateItem.getId()).findAny().orElseThrow();

            Assertions.assertThat(runTemplateItem.getDaysOfWeek()).containsExactlyInAnyOrder(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
            );
            return null;
        });

    }

}
