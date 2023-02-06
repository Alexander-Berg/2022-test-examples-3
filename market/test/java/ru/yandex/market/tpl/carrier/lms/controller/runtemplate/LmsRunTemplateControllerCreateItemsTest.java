package ru.yandex.market.tpl.carrier.lms.controller.runtemplate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplate;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.RunTemplateCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.lms.controller.LmsControllerTest;
import ru.yandex.market.tpl.carrier.planner.lms.runtemplate.item.LmsRunTemplateItemCreateDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class LmsRunTemplateControllerCreateItemsTest extends LmsControllerTest {

    private final RunTemplateCommandService runTemplateCommandService;
    private final RunTemplateRepository runTemplateRepository;
    private final ObjectMapper tplObjectMapper;
    private final TestUserHelper testUserHelper;
    private final LMSClient lmsClient;
    private final TransactionTemplate transactionTemplate;

    private Company company;
    private RunTemplate runTemplate;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        runTemplate = runTemplateCommandService.create(RunTemplateCommand.Create.builder()
                .campaignId(company.getCampaignId())
                .externalId("name")
                .items(List.of())
                .build());

        Mockito.when(lmsClient.getPartner(123L))
                .thenReturn(Optional.of(PartnerResponse.newBuilder()
                        .name("DropShip123")
                        .build()));

        Mockito.when(lmsClient.getPartner(345L))
                .thenReturn(Optional.of(PartnerResponse.newBuilder()
                        .name("SC345")
                        .build()));
        Mockito.when(lmsClient.getLogisticsPoints(MockitoHamcrest.argThat(Matchers.hasProperty("partnerIds", Matchers.contains(123L)))))
                .thenReturn(List.of(LogisticsPointResponse.newBuilder()
                        .id(10123L)
                        .externalId("123")
                        .partnerId(123L)
                        .address(Address.newBuilder()
                                .country("Россия")
                                .region("Пенза")
                                .settlement("Пенза")
                                .street("Пушкина")
                                .house("Колотушкина")
                                .addressString("ул. Пушкина, д. Колотушкина")
                                .latitude(BigDecimal.valueOf(55.745857))
                                .longitude(BigDecimal.valueOf(37.452216))
                                .exactLocationId(123)
                                .build())
                        .locationZoneId(120564L)
                        .phones(Set.of(new Phone("+790000000", null, null, PhoneType.PRIMARY)))
                        .contact(new Contact("Александр", "Пушкин", "Сергеевич"))
                        .build()));

        Mockito.when(lmsClient.getLogisticsPoints(MockitoHamcrest.argThat(Matchers.hasProperty("partnerIds", Matchers.contains(345L)))))
                .thenReturn(List.of(LogisticsPointResponse.newBuilder()
                        .id(10345L)
                        .externalId("345")
                        .partnerId(345L)
                        .address(Address.newBuilder()
                                .country("Россия")
                                .region("Тверь")
                                .settlement("Тверь")
                                .street("Ленина")
                                .house("1")
                                .addressString("ул. Ленина, д. 1")
                                .latitude(BigDecimal.valueOf(55.745857))
                                .longitude(BigDecimal.valueOf(37.452216))
                                .exactLocationId(123)
                                .build())
                        .locationZoneId(120564L)
                        .phones(Set.of(new Phone("+790000000", null, null, PhoneType.PRIMARY)))
                        .contact(new Contact("Василий", "Хвост", "Семенович"))
                        .build()));
    }

    @SneakyThrows
    @Test
    void shouldCreateRunTemplateItem() {


        mockMvc.perform(post("/LMS/carrier/run-templates/{templateId}/items", runTemplate.getId())
                .content(tplObjectMapper.writeValueAsString(LmsRunTemplateItemCreateDto.builder()
                        .orderNumber(1)
                        .partnerFrom(123L)
                        .partnerTo(345L)
                        .ignoreVolume(true)
                        .monday(false)
                        .tuesday(false)
                        .wednesday(false)
                        .thursday(false)
                        .friday(false)
                        .saturday(true)
                        .sunday(false)
                        .build()))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            runTemplate = runTemplateRepository.findByIdAndDeletedFalseOrThrow(runTemplate.getId());
            Assertions.assertThat(runTemplate.streamItems().findFirst().orElseThrow().getDaysOfWeek()).containsExactly(DayOfWeek.SATURDAY);
            return null;
        });
    }
}
