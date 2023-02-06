package ru.yandex.market.tpl.carrier.lms.controller.runtemplate;

import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateItem;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.NewRunTemplateItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.RunTemplateCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.lms.controller.LmsControllerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class LmsRunTemplateControllerDeleteItemsTest extends LmsControllerTest {

    private final RunTemplateCommandService runTemplateCommandService;
    private final TestUserHelper testUserHelper;

    @SneakyThrows
    @Test
    void shouldDeleteRunTemplateItem() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        var runTemplate = runTemplateCommandService.create(RunTemplateCommand.Create.builder()
                .externalId("runTemplate1")
                .campaignId(company.getCampaignId())
                .deliveryServiceId(234L)
                .items(List.of(
                        new NewRunTemplateItemData(
                                "123",
                                "456",
                                1,
                                Set.of(),
                                false,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                ))
                .build()
        );

        RunTemplateItem runTemplateItem = runTemplate.streamItems().findFirst().orElseThrow();

        mockMvc.perform(delete("/LMS/carrier/run-templates/{templateId}/items/{templateItemId}",
                runTemplate.getId(), runTemplateItem.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/LMS/carrier/run-templates/{templateId}/items", runTemplate.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.empty()));
    }
}
