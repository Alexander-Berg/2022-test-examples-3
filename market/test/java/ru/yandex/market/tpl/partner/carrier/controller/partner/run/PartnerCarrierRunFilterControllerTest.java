package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.carrier.core.db.QueryCountAssertions;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportRepository;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.transport.PartnerCarrierTransportCreateDto;
import ru.yandex.market.tpl.partner.carrier.model.transport.PartnerCarrierTransportFilterDto;
import ru.yandex.market.tpl.partner.carrier.model.transport.PartnerCarrierTransportOptionDto;
import ru.yandex.market.tpl.partner.carrier.model.user.partner.PartnerCarrierUserFilterDto;
import ru.yandex.market.tpl.partner.carrier.model.user.partner.PartnerCarrierUserOptionDto;
import ru.yandex.market.tpl.partner.carrier.service.user.transport.PartnerCarrierTransportService;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunFilterControllerTest extends BaseTplPartnerCarrierWebIntTest {
    private static final long SORTING_CENTER_ID = 47819L;
    public static final int PALLETS_CAPACITY = 123;

    private final DsRepository dsRepository;
    private final TransportRepository transportRepository;

    private final TestUserHelper testUserHelper;
    private final RunGenerator manualRunService;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final PartnerCarrierTransportService partnerCarrierTransportService;
    private final UserCommandService userCommandService;

    private final ObjectMapper tplObjectMapper;

    private Company company;
    private Run run;
    private Run run2;
    private Transport transport;
    private Transport transport2;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .login("anotherLogin@yandex.ru")
                .build()
        );

        user1 = testUserHelper.findOrCreateUser(UID);

        transport = transportRepository.findByIdOrThrow(partnerCarrierTransportService.create(
                PartnerCarrierTransportCreateDto.builder()
                        .name("Газель")
                        .capacity(new BigDecimal("1.23"))
                        .palletsCapacity(PALLETS_CAPACITY)
                        .brand("Газель")
                        .model("Газель Некст")
                        .number("а123мр")
                        .build(), company.getId()
        ).getId());

        transport2 = transportRepository.findByIdOrThrow(partnerCarrierTransportService.create(
                PartnerCarrierTransportCreateDto.builder()
                        .name("Газель2")
                        .capacity(new BigDecimal("1.23"))
                        .palletsCapacity(PALLETS_CAPACITY)
                        .brand("Газель2")
                        .model("Газель Некст2")
                        .number("а124мр")
                        .build(), company.getId()
        ).getId());

        Long deliveryServiceId = dsRepository.findByCompaniesId(company.getId()).iterator().next().getId();

        OrderWarehouse warehouseTo = orderWarehouseGenerator.generateWarehouse();

        run = manualRunService.generate(b -> b
                .externalId("asd")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("123")
                                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                                .orderWarehouseTo(warehouseTo)
                                                .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .pallets(64)
                                                .build(),
                                        1,
                                        null,
                                        null
                                ),
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("345")
                                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                                .orderWarehouseTo(warehouseTo)
                                                .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 2, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .pallets(64)
                                                .build(),
                                        2,
                                        null,
                                        null
                                )
                        )
                )
        );

        run2 = manualRunService.generate(b -> b
                .externalId("def")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("234")
                                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                                .orderWarehouseTo(warehouseTo)
                                                .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .pallets(125)
                                                .build(),
                                        1,
                                        null,
                                        null
                                ),
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("456")
                                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                                .orderWarehouseTo(warehouseTo)
                                                .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 2, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .pallets(125)
                                                .build(),
                                        2,
                                        null,
                                        null
                                )
                        )
                )
        );
    }

    @SneakyThrows
    @Test
    void shouldReturnDrivers() {
        user2 = testUserHelper.findOrCreateUser(ANOTHER_UID, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE);
        var user3 = testUserHelper.findOrCreateUser(ANOTHER_UID_2, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE_2);

        var driversContent = QueryCountAssertions.assertQueryCountTotalEqual(4, () ->
                mockMvc.perform(get("/internal/partner/runs/{id}/drivers", run.getId())
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        ).andExpect(status().isOk())
                        .andExpect(jsonPath("$.options").isArray())
                        .andExpect(jsonPath("$.options").value(Matchers.hasSize(3)))
                        .andReturn()
                        .getResponse().getContentAsString()
        );

        PartnerCarrierUserFilterDto filter = tplObjectMapper.readValue(driversContent,
                PartnerCarrierUserFilterDto.class);
        Assertions.assertThat(filter).isNotNull();

        List<PartnerCarrierUserOptionDto> options = filter.getOptions();
        Assertions.assertThat(options).isNotNull();
        Assertions.assertThat(options).hasSize(3);

        var option = options.stream().filter(op -> op.getId() == user1.getId()).findFirst().orElseThrow();
        Assertions.assertThat(option.getId()).isEqualTo(user1.getId());
        Assertions.assertThat(option.getName()).isEqualTo(user1.getName());
        Assertions.assertThat(option.getUid()).isEqualTo(user1.getUid());
        Assertions.assertThat(option.isHasPassportData()).isEqualTo(false);
    }

    @SneakyThrows
    @Test
    void shouldNotReturnBlacklistedDriver() {
        user2 = testUserHelper.findOrCreateUser(ANOTHER_UID, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE);
        var driversContent = mockMvc.perform(get("/internal/partner/runs/{id}/drivers", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.options").value(Matchers.hasSize(2)))
                .andReturn()
                .getResponse().getContentAsString();

        userCommandService.markBlackListed(new UserCommand.MarkBlackListed(user2.getId(), null));

        driversContent = mockMvc.perform(get("/internal/partner/runs/{id}/drivers", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.options").value(Matchers.hasSize(1)))
                .andReturn()
                .getResponse().getContentAsString();

        PartnerCarrierUserFilterDto filter = tplObjectMapper.readValue(driversContent,
                PartnerCarrierUserFilterDto.class);
        Assertions.assertThat(filter).isNotNull();

        List<PartnerCarrierUserOptionDto> options = filter.getOptions();
        Assertions.assertThat(options).isNotNull();
        Assertions.assertThat(options).hasSize(1);

        PartnerCarrierUserOptionDto option = options.get(0);
        Assertions.assertThat(option.getId()).isEqualTo(user1.getId());
        Assertions.assertThat(option.getName()).isEqualTo(user1.getName());
        Assertions.assertThat(option.getUid()).isEqualTo(user1.getUid());
    }

    @SneakyThrows
    @Test
    void shouldReturnTransports() {
        var transportsContent = QueryCountAssertions.assertQueryCountTotalEqual(7, () -> mockMvc.perform(get("/internal/partner/runs/{id}/transports", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.options").value(Matchers.hasSize(2)))
                .andReturn()
                .getResponse().getContentAsString()
        );

        PartnerCarrierTransportFilterDto filter = tplObjectMapper.readValue(transportsContent,
                PartnerCarrierTransportFilterDto.class);
        Assertions.assertThat(filter).isNotNull();

        List<PartnerCarrierTransportOptionDto> options = filter.getOptions();
        Assertions.assertThat(options).isNotNull();
        Assertions.assertThat(options).hasSize(2);

        PartnerCarrierTransportOptionDto option = options.get(0);
        Assertions.assertThat(option.getId()).isEqualTo(transport.getId());
        Assertions.assertThat(option.getName()).isEqualTo(transport.getName());
        Assertions.assertThat(option.getPalletsCapacity()).isEqualTo(PALLETS_CAPACITY);
    }

    @SneakyThrows
    @Test
    void shouldReturnTransports2() {
        mockMvc.perform(get("/internal/partner/runs/{id}/transports", run2.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.options").value(Matchers.empty()));
    }

    @SneakyThrows
    @Test
    void shouldNotReturnDeletedTransports() {
        mockMvc.perform(delete("/internal/partner/transports/{transportId}", transport.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk());

        mockMvc.perform(get("/internal/partner/runs/{id}/transports", run2.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.options").isArray())
                .andExpect(jsonPath("$.options").value(Matchers.empty()));

    }

    @SneakyThrows
    @Test
    void shouldReturnFiltersMap() {
        mockMvc.perform(get("/internal/partner/runs/typeSubtypeFilter")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk());
    }
}
