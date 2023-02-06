package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.carrier.core.db.QueryCountAssertions;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementType;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunSubtype;
import ru.yandex.market.tpl.carrier.core.domain.run.RunType;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartner;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.run.RunStatus;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.PartnerRunDto;
import ru.yandex.market.tpl.partner.carrier.service.run.PartnerCarrierRunService;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PartnerCarrierRunControllerV2Test extends BaseTplPartnerCarrierWebIntTest {
    private static final long SORTING_CENTER_ID = 47819L;
    private static final long CAMPAIGN_ID_1 = 123456L;
    private static final long CAMPAIGN_ID_2 = 456789L;
    private static final String RUN_NAME = "Маршрут 1";
    private static final String MOVEMENT_EXTERNAL_ID = "123";
    private static final String MOVEMENT_EXTERNAL_ID2 = "3456";

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final DsRepository dsRepository;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;
    private final TestUserHelper testUserHelper;
    private final RunGenerator manualRunService;
    private final RunCommandService runCommandService;
    private final PartnerCarrierRunService partnerCarrierRunService;
    private final ObjectMapper tplObjectMapper;
    private final DutyGenerator dutyGenerator;
    private final RunRepository runRepository;

    private Company company;
    private Company company2;

    private User user;
    private User user2;
    private Transport transport;
    private Transport transport2;
    private Transport anotherTransport;

    private Run run;
    private Run run2;
    private Run run3;
    private Run run4;
    private Run run5;
    private Run run6;
    private Run run7;
    private Run dutyRun;

    private Duty duty;

    private OrderWarehousePartner partnerFromOdd;
    private OrderWarehousePartner partnerFromEven;

    private OrderWarehouse warehouseFrom1;
    private OrderWarehouse warehouseFrom2;
    private OrderWarehouse warehouseFrom3;
    private OrderWarehouse warehouseFrom4;
    private OrderWarehouse warehouseFrom5;
    private OrderWarehouse warehouseFrom6;
    private OrderWarehouse warehouseTo1;
    private OrderWarehouse warehouseTo2;
    private OrderWarehouse warehouseTo3;

    //For Msk Timezone WH (7:00 UTC = 10:00 Msk)
    private Instant expectedArrivalDateFrom1 = ZonedDateTime.of(2021, 8, 4, 7, 00, 0, 0, ZoneOffset.UTC).toInstant();
    private Instant expectedArrivalDateTo1 = ZonedDateTime.of(2021, 8, 4, 11, 59, 59, 0, ZoneOffset.UTC).toInstant();

    private Instant expectedTenYearsAgoArrivalDateFrom1 =
            ZonedDateTime.of(2011, 8, 4, 7, 0, 0, 0, ZoneOffset.UTC).toInstant();
    private Instant expectedTenYearsAgoArrivalDateTo1 =
            ZonedDateTime.of(2011, 8, 4, 11, 59, 59, 0, ZoneOffset.UTC).toInstant();


    //For Ykb Timezone WH (4:00 UTC = 9:00 Ykb)
    private Instant expectedArrivalDateFrom2 = ZonedDateTime.of(2021, 8, 4, 4, 0, 0, 0, ZoneOffset.UTC).toInstant();
    private Instant expectedArrivalDateTo2 = ZonedDateTime.of(2021, 8, 4, 11, 59, 59, 0, ZoneOffset.UTC).toInstant();

    private LocalDate today;
    private LocalDate tenYearsAgo;
    private LocalDate tomorrow;
    private Long deliveryServiceId;

    private final TestableClock clock;

    private void setUpWarehouse(OrderWarehouse wh,
                                OrderWarehousePartner partner,
                                Integer regionId,
                                String timezone) {
        wh.setPartner(partner);
        wh.setRegionId(regionId);
        wh.setTimezone(timezone);
    }

    @BeforeEach
    void setUp() {
        Long deliveryServiceId2 = 1113L;
        today = LocalDate.of(2021, 8, 4);
        tomorrow = today.plusDays(1);
        tenYearsAgo = today.minusYears(10);

        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .login("abc@yandex.ru")
                .campaignId(CAMPAIGN_ID_1)
                .build());
        company2 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME + ".2")
                .login("def@yandex.ru")
                .campaignId(CAMPAIGN_ID_2)
                .deliveryServiceIds(Set.of(deliveryServiceId2))
                .build());

        user = testUserHelper.findOrCreateUser(UID);
        user2 = testUserHelper.findOrCreateUser(3L, Company.DEFAULT_COMPANY_NAME, "+79992228833");
        transport = testUserHelper.findOrCreateTransport();
        transport2 = testUserHelper.findOrCreateTransport("transport2", Company.DEFAULT_COMPANY_NAME);
        anotherTransport = testUserHelper.findOrCreateTransport("ANOTHER_TRANSPORT", Company.DEFAULT_COMPANY_NAME);

        partnerFromOdd = orderWarehousePartnerRepository.save(new OrderWarehousePartner("123", "Odd partner"));
        partnerFromEven = orderWarehousePartnerRepository.save(new OrderWarehousePartner("234", "Even partner"));
        warehouseFrom1 = orderWarehouseGenerator.generateWarehouse("Питер",
                ow -> setUpWarehouse(ow, partnerFromOdd, 1, "Europe/Moscow"));
        warehouseFrom2 = orderWarehouseGenerator.generateWarehouse("Москва",
                ow -> setUpWarehouse(ow, partnerFromEven, 1, "Europe/Moscow"));
        warehouseFrom3 = orderWarehouseGenerator.generateWarehouse("Бологое",
                ow -> setUpWarehouse(ow, partnerFromOdd, 1, "Asia/Yekaterinburg"));
        warehouseFrom4 = orderWarehouseGenerator.generateWarehouse("Питер",
                ow -> setUpWarehouse(ow, partnerFromEven, 1, "Asia/Yekaterinburg"));
        warehouseFrom5 = orderWarehouseGenerator.generateWarehouse("Питер",
                ow -> setUpWarehouse(ow, partnerFromOdd, 1, "Europe/Moscow"));
        warehouseFrom6 = orderWarehouseGenerator.generateWarehouse("Бологое",
                ow -> setUpWarehouse(ow,partnerFromEven, 1, "Europe/Moscow"));
        warehouseTo1 = orderWarehouseGenerator.generateWarehouse("Бологое",
                ow -> {});
        warehouseTo2 = orderWarehouseGenerator.generateWarehouse("Москва",
                ow -> {});
        warehouseTo3 = orderWarehouseGenerator.generateWarehouse("Москва",
                ow -> {});


        deliveryServiceId = dsRepository.findByCompaniesId(company.getId()).iterator().next().getId();

        /*
            ASSIGNED
            2 pallets
            0.5 volume
         */
        run = manualRunService.generate(b -> b
                .externalId("asd1")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(today)
                .name(RUN_NAME)
                .runSubtype(RunSubtype.MAIN)
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId(MOVEMENT_EXTERNAL_ID)
                                                .pallets(2)
                                                .volume(BigDecimal.valueOf(0.25))
                                                .orderWarehouse(warehouseFrom1)
                                                .orderWarehouseTo(warehouseTo1)
                                                .outboundArrivalTime(expectedArrivalDateFrom1)
                                                .deliveryIntervalFrom(expectedArrivalDateFrom1)
                                                .deliveryIntervalTo(expectedArrivalDateTo1)
                                                .type(MovementType.LINEHAUL)
                                                .build(),
                                        1,
                                        null,
                                        null
                                ),
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId(MOVEMENT_EXTERNAL_ID2)
                                                .pallets(2)
                                                .volume(BigDecimal.valueOf(0.25))
                                                .orderWarehouse(warehouseFrom2)
                                                .orderWarehouseTo(warehouseTo1)
                                                .outboundArrivalTime(expectedArrivalDateFrom1)
                                                .deliveryIntervalFrom(expectedArrivalDateFrom1)
                                                .deliveryIntervalTo(expectedArrivalDateTo1)
                                                .type(MovementType.LINEHAUL)
                                                .build(),
                                        2,
                                        null,
                                        null
                                )
                        )
                )
        );

        partnerCarrierRunService.assignTransportToRun(run.getId(), transport.getId(), company.getId());
        partnerCarrierRunService.assignCourierToRun(run.getId(), user.getId(), company.getId());
        var us = runRepository.findById(run.getId()).orElseThrow().getFirstAssignedShift();
        testUserHelper.openShift(user, us.getId());

        /*
            COMPLETED
            3 pallets
            1. volume
         */
        run2 = manualRunService.generate(b -> b.externalId("asd2")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(today)
                .name("Маршрут 2")
                .clearItems()
                .runSubtype(RunSubtype.SUPPLEMENTARY_1)
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("4567")
                                                .pallets(3)
                                                .volume(BigDecimal.valueOf(0.25))
                                                .orderWarehouse(warehouseFrom3)
                                                .orderWarehouseTo(warehouseTo2)
                                                .outboundArrivalTime(expectedArrivalDateFrom2)
                                                .deliveryIntervalFrom(expectedArrivalDateFrom2)
                                                .deliveryIntervalTo(expectedArrivalDateTo2)
                                                .type(MovementType.LINEHAUL)
                                                .build(),
                                        1,
                                        null,
                                        null
                                ),
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("5678")
                                                .pallets(3)
                                                .volume(BigDecimal.valueOf(0.75))
                                                .orderWarehouse(warehouseFrom4)
                                                .orderWarehouseTo(warehouseTo2)
                                                .outboundArrivalTime(expectedArrivalDateFrom2)
                                                .deliveryIntervalFrom(expectedArrivalDateFrom2)
                                                .deliveryIntervalTo(expectedArrivalDateTo2)
                                                .type(MovementType.LINEHAUL)
                                                .build(),
                                        2,
                                        null,
                                        null
                                )
                        )
                )
        );
        partnerCarrierRunService.assignTransportToRun(run2.getId(), transport2.getId(), company.getId());
        partnerCarrierRunService.assignCourierToRun(run2.getId(), user2.getId(), company.getId());
        runCommandService.complete(new RunCommand.Complete(run2.getId()));

        /*
            other company!!!
            CREATED
            7 pallets
            1.5 volume
        */
        run3 = manualRunService.generate(b -> b
                .externalId("asd3")
                .deliveryServiceId(deliveryServiceId2)
                .campaignId(company2.getCampaignId())
                .runDate(today)
                .runSubtype(RunSubtype.UNSCHEDULED)
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("6789")
                                                .pallets(7)
                                                .volume(BigDecimal.valueOf(0.75))
                                                .orderWarehouse(warehouseFrom5)
                                                .orderWarehouseTo(warehouseTo3)
                                                .type(MovementType.LINEHAUL)
                                                .build(),
                                        1,
                                        null,
                                        null
                                ),
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("7890")
                                                .pallets(7)
                                                .volume(BigDecimal.valueOf(0.75))
                                                .orderWarehouse(warehouseFrom6)
                                                .orderWarehouseTo(warehouseTo3)
                                                .type(MovementType.LINEHAUL)
                                                .build(),
                                        2,
                                        null,
                                        null
                                )
                        )
                )
        );
    }

    void generateRun4() {
        /*
            10 pallets
            4. volume
         */
        run4 = manualRunService.generate(b -> b
                        .externalId("asd4")
                        .deliveryServiceId(deliveryServiceId)
                        .campaignId(company.getCampaignId())
                        .runDate(tenYearsAgo)
                        .name("Some name")
                        .clearItems()
                        .items(
                                List.of(
                                        new RunGenerator.RunItemGenerateParam(
                                                MovementCommand.Create.builder()
                                                        .externalId("67891")
                                                        .pallets(10)
                                                        .volume(BigDecimal.valueOf(2.))
                                                        .orderWarehouse(warehouseFrom5)
                                                        .orderWarehouseTo(warehouseTo3)
                                                        .deliveryIntervalFrom(expectedTenYearsAgoArrivalDateFrom1)
                                                        .deliveryIntervalTo(expectedTenYearsAgoArrivalDateTo1)
                                                        .build(),
                                                1,
                                                null,
                                                null
                                        ),
                                        new RunGenerator.RunItemGenerateParam(
                                                MovementCommand.Create.builder()
                                                        .externalId("78901")
                                                        .pallets(10)
                                                        .volume(BigDecimal.valueOf(2.))
                                                        .orderWarehouse(warehouseFrom6)
                                                        .orderWarehouseTo(warehouseTo3)
                                                        .deliveryIntervalFrom(expectedTenYearsAgoArrivalDateFrom1)
                                                        .deliveryIntervalTo(expectedTenYearsAgoArrivalDateTo1)
                                                        .build(),
                                                2,
                                                null,
                                                null
                                        )
                                )
                        )
        );
    }

    void generateRun5() {
        /*
            5 pallets
            2. volume
         */
        run5 = manualRunService.generate(b -> b
                .externalId("asd5")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(today)
                .name("Some name 5")
                .runSubtype(RunSubtype.UNSCHEDULED)
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("678912")
                                                .pallets(5)
                                                .volume(BigDecimal.valueOf(1.))
                                                .orderWarehouse(warehouseFrom1)
                                                .orderWarehouseTo(warehouseTo1)
                                                .build(),
                                        1,
                                        null,
                                        null
                                ),
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("789013")
                                                .pallets(5)
                                                .volume(BigDecimal.valueOf(1.))
                                                .orderWarehouse(warehouseFrom4)
                                                .orderWarehouseTo(warehouseTo1)
                                                .build(),
                                        2,
                                        null,
                                        null
                                )
                        )
                )
        );
    }

    void generateRun6() {
        run6 = manualRunService.generate(b -> b
                .externalId("asd6")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(today)
                .name("Some name 6")
                .runSubtype(null)
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("890123")
                                                .type(MovementType.INTERWAREHOUSE)
                                                .orderWarehouse(warehouseFrom1)
                                                .orderWarehouseTo(warehouseTo1)
                                                .build(),
                                        1,
                                        null,
                                        null
                                )
                        )
                )
        );
    }

    void generateRunWithAssigned() {
        var runWithShift = manualRunService.generate(b -> b
                .externalId("asd7")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(today)
                .name("Some name 7")
                .runSubtype(null)
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("890124")
                                                .type(MovementType.INTERWAREHOUSE)
                                                .orderWarehouse(warehouseFrom1)
                                                .orderWarehouseTo(warehouseTo1)
                                                .build(),
                                        1,
                                        null,
                                        null
                                )
                        )
                )
        );

        partnerCarrierRunService.assignTransportToRun(runWithShift.getId(), transport2.getId(), company.getId());
        partnerCarrierRunService.assignCourierToRun(runWithShift.getId(), user2.getId(), company.getId());
        var us = runRepository.findById(runWithShift.getId()).orElseThrow().getFirstAssignedShift();
        testUserHelper.openShift(user2, us.getId());

    }

    void generateRunNeededWarning() {
        clock.setFixed(Instant.parse("2021-08-04T07:00:00Z"), ZoneId.of("UTC"));
        run7 = manualRunService.generate(b -> b
                .externalId("asd7")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(today)
                .name("Some name 7")
                .runSubtype(null)
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("890123")
                                                .type(MovementType.INTERWAREHOUSE)
                                                .orderWarehouse(warehouseFrom1)
                                                .orderWarehouseTo(warehouseTo1)
                                                .deliveryIntervalFrom(Instant.now(clock).plus(2, ChronoUnit.HOURS))
                                                .deliveryIntervalTo(Instant.now(clock).plus(5, ChronoUnit.HOURS))
                                                .build(),
                                        1,
                                        null,
                                        null
                                )
                        )
                )
        );
    }

    void generateDuty() {
        duty = dutyGenerator.generate(DutyGenerator.DutyGenerateParams.builder()
                .dutyStartTime(expectedArrivalDateFrom1)
                .dutyEndTime(expectedArrivalDateTo1)
                .deliveryServiceId(deliveryServiceId)
                .dutyWarehouseId(warehouseFrom1.getYandexId())
                .build());
    }

    void generateDuty1() {
        dutyGenerator.generate(DutyGenerator.DutyGenerateParams.builder()
                .dutyStartTime(expectedArrivalDateFrom1.plus(100, ChronoUnit.DAYS))
                .dutyEndTime(expectedArrivalDateTo1.plus(120, ChronoUnit.DAYS))
                .deliveryServiceId(deliveryServiceId)
                .dutyWarehouseId(warehouseFrom1.getYandexId())
                .build());
    }

    @Test
    @SneakyThrows
    void shouldFilterByEmbarkation() {

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("timestampFrom", "2021-08-04T08:00")
                        .param("timestampTo", "2021-08-04T10:00")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));


        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("timestampFrom", "2021-08-04T08:00")
                        .param("timestampTo", "2021-08-04T09:30")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));


        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("timestampFrom", "2021-08-04T09:30")
                        .param("timestampTo", "2021-08-04T11:00")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("timestampFrom", "2021-08-04T11:00")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)));

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("timestampTo", "2021-08-04T08:00")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)));

        generateDuty1();
        generateDuty1();
        generateDuty1();
        generateDuty1();
        generateDuty1();
        generateDuty1();
        generateDuty1();
        generateDuty();

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("timestampFrom", "2021-08-04T08:00")
                        .param("timestampTo", "2021-08-04T11:00")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(3)));

    }


    @Test
    @SneakyThrows
    void shouldFilterByArrivalAndDeparture() {
        generateRun4();

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("arrival", "сКВА"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].items[1].movement.addressTo").value(Matchers.containsString("Москва")))
                .andExpect(jsonPath("$.content[1].items[1].movement.addressTo").value(Matchers.containsString("Москва")));

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("departure", "пИт")
                        .param("arrival", "сква"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].items[1].movement.addressTo").value(Matchers.containsString("Москва")))
                .andExpect(jsonPath("$.content[0].items[0].movement.addressFrom").value(Matchers.containsString("Питер")));


        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("departure", "пит"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].items[0].movement.addressFrom").value(Matchers.containsString("Питер")))
                .andExpect(jsonPath("$.content[1].items[0].movement.addressFrom").value(Matchers.containsString("Питер")));

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("departure", "Лондон"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)));

    }

    @Test
    @SneakyThrows
    void shouldSuggestByArrivalAndDeparture() {
        generateRun4();

        mockMvc.perform(get("/internal/partner/runs/arrival")
                    .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(2)));

        mockMvc.perform(get("/internal/partner/runs/departure")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId()))
                .andExpect(status().isOk()) //Питер Бологое
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(2)));

        mockMvc.perform(get("/internal/partner/runs/departure")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .param("query", "пИТЕ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0]").value("Питер"));

        mockMvc.perform(get("/internal/partner/runs/arrival")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .param("query", "оск"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0]").value("Москва"));
    }

    @Test
    @SneakyThrows
    void shouldSuggestTransitCity() {
        generateRun4();

        mockMvc.perform(get("/internal/partner/runs/transit")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(3)))
                .andExpect(jsonPath("$[0]").value(Matchers.anyOf(
                        Matchers.equalTo("Бологое"),
                        Matchers.equalTo("Москва"),
                        Matchers.equalTo("Питер"))))
                .andExpect(jsonPath("$[1]").value(Matchers.anyOf(
                        Matchers.equalTo("Бологое"),
                        Matchers.equalTo("Москва"),
                        Matchers.equalTo("Питер"))))
                .andExpect(jsonPath("$[2]").value(Matchers.anyOf(
                        Matchers.equalTo("Бологое"),
                        Matchers.equalTo("Москва"),
                        Matchers.equalTo("Питер"))));

        mockMvc.perform(get("/internal/partner/runs/transit")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("query", "МОС"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0]").value(Matchers.equalTo("Москва")));
    }

    @Test
    @SneakyThrows
    void shouldSuggestTypes() {
        mockMvc.perform(get("/internal/partner/runs/type")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.containsInAnyOrder(
                        StreamEx.of(RunType.values())
                                .removeBy(Function.identity(), RunType.UNSUPPORTED)
                                .map(Enum::name)
                                .toArray(String[]::new)
                )));
    }

    @Test
    @SneakyThrows
    void shouldReturnRuns() {
        var responseString = mockMvc.perform(
                        get("/internal/partner/runs/v2")
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .param("sort", "id,asc")
                )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        FakePage page = tplObjectMapper.readValue(responseString, FakePage.class);

        List<PartnerRunDto> runs = page.getContent();
        Assertions.assertThat(runs).hasSize(2);
        Assertions.assertThat(runs.get(0).getId()).isEqualTo(run.getId());
        Assertions.assertThat(runs.get(0).getDate()).isEqualTo(today);
        Assertions.assertThat(runs.get(0).getName()).isEqualTo(RUN_NAME);
        Assertions.assertThat(runs.get(0).getItems()).hasSize(2);
        Assertions.assertThat(runs.get(0).getItems().get(0).getMovement().getExternalId()).isEqualTo(MOVEMENT_EXTERNAL_ID);
        Assertions.assertThat(runs.get(0).getItems().get(0).getMovement().getLegalEntity()).isEqualTo(warehouseFrom1.getIncorporation());
        Assertions.assertThat(runs.get(0).getItems().get(0).getMovement().getPartnerName()).isEqualTo(warehouseFrom1.getPartner().getName());
        Assertions.assertThat(runs.get(0).getItems().get(0).getMovement().getExpectedArrivalTimestampFrom()).isEqualTo(expectedArrivalDateFrom1);
        Assertions.assertThat(runs.get(0).getItems().get(0).getMovement().getExpectedArrivalTimestampTo()).isEqualTo(expectedArrivalDateTo1);
        Assertions.assertThat(runs.get(0).getItems().get(1).getMovement().getExternalId()).isEqualTo(MOVEMENT_EXTERNAL_ID2);
    }

    @Test
    @SneakyThrows
    void shouldNotFilterIfStatusIsNotProvided() {
        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
        ;
    }

    @SneakyThrows
    @Test
    void shouldFilterByRunId() {
        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("runId", run.getId().toString())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run.getId()))
        ;
    }

    @SneakyThrows
    @Test
    void shouldNotFilterByRunIdIfAnotherCompany() {
        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("runId", run3.getId().toString())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)))
        ;
    }


    @Test
    @SneakyThrows
    void shouldFilterByStatus() {
        mockMvc.perform(
                get("/internal/partner/runs/v2")
                    .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                    .param("status", RunStatus.STARTED.name())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run.getId()))
        ;
    }

    @Test
    @SneakyThrows

    void shouldFilterByCompanyId() {
        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company2.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run3.getId()))
        ;
    }

    @Test
    @SneakyThrows
    void shouldFilterByCourierUid() {
        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("courierUid", String.valueOf(UID))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(ResultMatcher.matchAll(
                   status().isOk(),
                   jsonPath("$.content").value(Matchers.hasSize(1)),
                   jsonPath("$.content[0].id").value(run.getId())
                ));

        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("courierUid", String.valueOf(ANOTHER_UID))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.content").isEmpty())
                );
    }

    @Test
    @SneakyThrows
    void shouldFilterByTransportId() {
        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("transportId", String.valueOf(transport.getId()))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.content").value(Matchers.hasSize(1)),
                        jsonPath("$.content[0].id").value(run.getId())
                ));

        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("transportId", String.valueOf(anotherTransport.getId()))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.content").isEmpty())
                );
    }

    @Test
    @SneakyThrows
    void shouldFilterByTransportTypeId() {
        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("transportTypeId", String.valueOf(transport.getId()))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.content").value(Matchers.hasSize(1)),
                        jsonPath("$.content[0].id").value(run.getId())
                ));

        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("transportTypeId", String.valueOf(anotherTransport.getId()))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.content").isEmpty())
                );
    }

    @Test
    @SneakyThrows
    void shouldFilterByRunDate() {
        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("date", String.valueOf(today))
                        .param("sort", "date")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.content").value(Matchers.hasSize(2)),
                        jsonPath("$.content[*].id").value(Matchers.containsInAnyOrder(
                                run2.getId().intValue(), run.getId().intValue()))
                ));

        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("date", String.valueOf(tomorrow))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.content").isEmpty())
                );
    }


    @Test
    @SneakyThrows
    void shouldFilterByRunDateInterval() {
        generateRun4();

        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("dateEnd", String.valueOf(today))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isBadRequest());

        Integer run4Id = Math.toIntExact(run4.getId());
        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("date", String.valueOf(tenYearsAgo.minusYears(5)))
                        .param("dateEnd", String.valueOf(today.minusYears(5)))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run4Id));

        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("date", String.valueOf(today.minusYears(5)))
                        .param("dateEnd", String.valueOf(tenYearsAgo.minusYears(5)))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(Matchers.equalTo(run4Id)));

        Integer run1Id = Math.toIntExact(run.getId());
        Integer run2Id = Math.toIntExact(run2.getId());
        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("date", String.valueOf(today))
                        .param("dateEnd", String.valueOf(today))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[*].id").value(Matchers.containsInAnyOrder(run1Id, run2Id)));


        mockMvc.perform(
                get("/internal/partner/runs/v2")
                        .param("date", String.valueOf(today.plusYears(5)))
                        .param("dateEnd", String.valueOf(today.plusYears(10)))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)));
    }

    @Test
    @SneakyThrows
    void shouldFilterByTransit() {
        generateRun5();

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("transit", "Москва"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("transit", "Москореп"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)));
    }


    @Test
    @SneakyThrows
    void shouldFilterByPallets() {
        // 2 3 7 10 5
        generateRun4();
        generateRun5();

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("palletsNumberMin", "4")
                        .param("palletsNumberMax", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));
    }

    @Test
    @SneakyThrows
    void shouldFilterByVolume() {
        // 0.5 1. 1.5 4. 2.
        generateRun4();
        generateRun5();

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("volumeMin", "0.99")
                        .param("volumeMax", "3.55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));
    }

    @Test
    @SneakyThrows
    void shouldFilterByVolumeAndPallets() {
        generateRun4();
        generateRun5();

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("sort", "id,asc")
                        .param("volumeMin", "0.99")
                        .param("palletsNumberMax", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));
    }

    @SneakyThrows
    @Test
    void shouldFilterByRunType() {
        generateRun6();

        mockMvc.perform(get("/internal/partner/runs/v2")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .param("type", RunType.LINEHAUL.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("type", RunType.INTERWAREHOUSE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));
    }

    @SneakyThrows
    @Test
    void shouldFilterByRunSubType() {
        generateRun5();
        generateRun6();

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("subtype", RunSubtype.MAIN.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("subtype", RunSubtype.SUPPLEMENTARY_1.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("subtype", RunSubtype.UNSCHEDULED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("subtype", RunSubtype.SUPPLEMENTARY_1.name() + "," + RunSubtype.UNSCHEDULED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));
    }

    @SneakyThrows
    @Test
    void shouldFilterByRunTypeSubtype() {
        generateRun5();
        generateRun6();

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("typeAndSubtype", RunType.INTERWAREHOUSE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("typeAndSubtype", RunType.LINEHAUL.name() + "/" +
                                RunSubtype.MAIN.name() + "," + RunType.INTERWAREHOUSE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));

    }

    @SneakyThrows
    @Test
    void getRunsWithWarning() {
        generateRunNeededWarning();
        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("runId", run7.getId().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].warningDriverNotAssigned").value(true))
        ;

    }

    @SneakyThrows
    @Test
    void getRunsTestQueryCountV2() {
        QueryCountAssertions.assertQueryCountTotalEqual(14, () ->
                mockMvc.perform(get("/internal/partner/runs/v2")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
        );

        generateRun5();
        generateRun6();
        generateRun4();

        QueryCountAssertions.assertQueryCountTotalEqual(14, () ->
                mockMvc.perform(get("/internal/partner/runs/v2")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").value(Matchers.hasSize(5)))
        );
    }

    @SneakyThrows
    @Test
    void getRunsTestQueryCount() {
        QueryCountAssertions.assertQueryCountTotalEqual(13, () ->
                mockMvc.perform(get("/internal/partner/runs")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
        );

        generateRunWithAssigned();

        QueryCountAssertions.assertQueryCountTotalEqual(13, () ->
                mockMvc.perform(get("/internal/partner/runs")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").value(Matchers.hasSize(2)))
        );
    }

    @SneakyThrows
    @Test
    void shouldPaginateResult() {
        generateRun4();
        generateRun5();
        generateRun6();
        generateRunNeededWarning();
        generateRunWithAssigned();

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .param("size", String.valueOf(3))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.numberOfElements").value(3))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(3));

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .param("size", String.valueOf(3))
                        .param("page", String.valueOf(1))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.numberOfElements").value(3))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(3));


        mockMvc.perform(get("/internal/partner/runs/v2")
                        .param("size", String.valueOf(4))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.numberOfElements").value(4))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(4));

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .param("size", String.valueOf(4))
                        .param("page", String.valueOf(1))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(4));
    }

    @Data
    private static class FakePage {
        private List<PartnerRunDto> content;
    }

    @AfterEach
    void cleanUp() {
        clock.setFixed(Instant.parse("1990-01-01T00:00:00Z"), ZoneId.of("UTC"));
    }
}
