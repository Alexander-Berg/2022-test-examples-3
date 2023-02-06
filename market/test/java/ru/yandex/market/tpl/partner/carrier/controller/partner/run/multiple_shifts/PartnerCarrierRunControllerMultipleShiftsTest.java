package ru.yandex.market.tpl.partner.carrier.controller.partner.run.multiple_shifts;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportRepository;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.transport.PartnerCarrierTransportCreateDto;
import ru.yandex.market.tpl.partner.carrier.service.user.transport.PartnerCarrierTransportService;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunControllerMultipleShiftsTest extends BaseTplPartnerCarrierWebIntTest {

    private final RunGenerator manualRunService;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper testUserHelper;
    private final PartnerCarrierTransportService partnerCarrierTransportService;
    private final DsRepository dsRepository;
    private final TransportRepository transportRepository;
    private final RunHelper runHelper;
    private final RunRepository runRepository;

    private Company company;
    private User user1;
    private User user2;
    private Transport transport;
    private Transport transport2;
    private Run run;
    private Run run2;
    private Run run3;
    private Run complexRun;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .login("anotherLogin@yandex.ru")
                .build()
        );

        user1 = testUserHelper.findOrCreateUser(UID, Company.DEFAULT_COMPANY_NAME, UserUtil.PHONE);
        user2 = testUserHelper.findOrCreateUser(ANOTHER_UID, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE);

        transport =
                transportRepository.findByIdOrThrow(partnerCarrierTransportService.create(PartnerCarrierTransportCreateDto.builder()
                        .name("Машина")
                        .capacity(new BigDecimal("1.23"))
                        .palletsCapacity(123)
                        .number(TestUserHelper.DEFAULT_TRANSPORT_NUMBER)
                        .build(), company.getId()).getId());

        transport2 =
                transportRepository.findByIdOrThrow(partnerCarrierTransportService.create(PartnerCarrierTransportCreateDto.builder()
                        .name("Машина2")
                        .capacity(new BigDecimal("3.45"))
                        .palletsCapacity(345)
                        .number("а921мр")
                        .build(), company.getId()).getId());


        Long deliveryServiceId = dsRepository.findByCompaniesId(company.getId()).iterator().next().getId();

        OrderWarehouse warehouseTo = orderWarehouseGenerator.generateWarehouse();

        run = manualRunService.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .items(List.of(
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("123")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(warehouseTo)
                                        .outboundArrivalTime(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .outboundDepartureTime(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .inboundArrivalTime(ZonedDateTime.of(2021, 8, 5, 8, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .inboundDepartureTime(ZonedDateTime.of(2021, 8, 5, 9, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
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
                                        .outboundArrivalTime(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .outboundDepartureTime(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .inboundArrivalTime(ZonedDateTime.of(2021, 8, 5, 8, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .inboundDepartureTime(ZonedDateTime.of(2021, 8, 5, 9, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 2, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .build(),
                                2,
                                null,
                                null
                        )
                ))
                .build()
        );

        run2 = manualRunService.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("def")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .clearItems()
                .item(
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("567")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(warehouseTo)
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 7, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 8, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .build(),
                                1,
                                null,
                                null
                        )
                )
                .build()
        );

        run3 = manualRunService.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("ghi")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .clearItems()
                .item(
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("890")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(warehouseTo)
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 7, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 8, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .build(),
                                1,
                                null,
                                null
                        )
                )
                .build()
        );

        var warehouse1 = orderWarehouseGenerator.generateWarehouse();
        var warehouse2 = orderWarehouseGenerator.generateWarehouse();
        var warehouse3 = orderWarehouseGenerator.generateWarehouse();
        var warehouse4 = orderWarehouseGenerator.generateWarehouse();

        complexRun = manualRunService.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd123")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .items(List.of(
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("c-1")
                                        .orderWarehouse(warehouse1)
                                        .orderWarehouseTo(warehouse3)
                                        .outboundArrivalTime(ZonedDateTime.of(2021, 8, 5, 17, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .outboundDepartureTime(ZonedDateTime.of(2021, 8, 5, 18, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .inboundArrivalTime(ZonedDateTime.of(2021, 8, 5, 23, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .inboundDepartureTime(ZonedDateTime.of(2021, 8, 6, 0, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 17, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 6, 0, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .build(),
                                0,
                                0,
                                3
                        ),
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("c-2")
                                        .orderWarehouse(warehouse2)
                                        .orderWarehouseTo(warehouse3)
                                        .outboundArrivalTime(ZonedDateTime.of(2021, 8, 5, 19, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .outboundDepartureTime(ZonedDateTime.of(2021, 8, 5, 20, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .inboundArrivalTime(ZonedDateTime.of(2021, 8, 6, 1, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .inboundDepartureTime(ZonedDateTime.of(2021, 8, 6, 2, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 19, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 6, 2, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .build(),
                                1,
                                1,
                                4
                        ),
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("c-3")
                                        .orderWarehouse(warehouse2)
                                        .orderWarehouseTo(warehouse4)
                                        .outboundArrivalTime(ZonedDateTime.of(2021, 8, 5, 21, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .outboundDepartureTime(ZonedDateTime.of(2021, 8, 5, 22, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .inboundArrivalTime(ZonedDateTime.of(2021, 8, 6, 5, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .inboundDepartureTime(ZonedDateTime.of(2021, 8, 6, 6, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 21, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 6, 6, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .build(),
                                2,
                                2,
                                6
                        ),
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("c-4")
                                        .orderWarehouse(warehouse3)
                                        .orderWarehouseTo(warehouse4)
                                        .outboundArrivalTime(ZonedDateTime.of(2021, 8, 6, 3, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .outboundDepartureTime(ZonedDateTime.of(2021, 8, 6, 3, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .inboundArrivalTime(ZonedDateTime.of(2021, 8, 6, 7, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .inboundDepartureTime(ZonedDateTime.of(2021, 8, 6, 8, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 6, 3, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 6, 8, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .build(),
                                5,
                                5,
                                7
                        )
                ))
                .build()
        );


        runHelper.assignUserShifts(run2.getId(), List.of(
                new RunHelper.AssignUserShift(null, user1.getId(), transport.getId(), 0, false),
                new RunHelper.AssignUserShift(null, user2.getId(), transport2.getId(), 1, true)
        ));

        runHelper.assignUserShifts(run.getId(), List.of(
                new RunHelper.AssignUserShift(null, user1.getId(), transport.getId(), 0, false),
                new RunHelper.AssignUserShift(null, user2.getId(), transport2.getId(), 2, true)
        ));

        runHelper.assignUserShifts(complexRun.getId(), List.of(
                new RunHelper.AssignUserShift(null, user1.getId(), transport.getId(), 0, false),
                new RunHelper.AssignUserShift(null, user2.getId(), transport2.getId(), 1, false),
                new RunHelper.AssignUserShift(null, user1.getId(), transport.getId(), 6, true)
        ));

        runHelper.assignUserAndTransport(run3, user1, transport);

    }

    @Test
    @SneakyThrows
    void getRunWithSeveralShifts() {
        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("runId", run.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].shifts").isArray())
                .andExpect(jsonPath("$.content[0].shifts").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].shifts[0].order").value(0))
                .andExpect(jsonPath("$.content[0].shifts[0].passingPointOrderNumber").value(0))
                .andExpect(jsonPath("$.content[0].shifts[1].order").value(1))
                .andExpect(jsonPath("$.content[0].shifts[1].passingPointOrderNumber").value(2));
    }

    @Test
    @SneakyThrows
    void getRunWithSeveralShiftsOnOneRunItem() {
        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("runId", run2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].shifts").isArray())
                .andExpect(jsonPath("$.content[0].shifts").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].shifts[0].order").value(0))
                .andExpect(jsonPath("$.content[0].shifts[0].isUpdatable").value(true))
                .andExpect(jsonPath("$.content[0].shifts[0].passingPointOrderNumber").value(0))
                .andExpect(jsonPath("$.content[0].shifts[1].order").value(1))
                .andExpect(jsonPath("$.content[0].shifts[1].isUpdatable").value(true))
                .andExpect(jsonPath("$.content[0].shifts[1].passingPointOrderNumber").value(1));


    }

    @Test
    @SneakyThrows
    void getRunWithSeveralShiftsOnComplexRun() {
        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("runId", complexRun.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].shifts").isArray())
                .andExpect(jsonPath("$.content[0].shifts").value(Matchers.hasSize(3)))
                .andExpect(jsonPath("$.content[0].shifts[0].order").value(0))
                .andExpect(jsonPath("$.content[0].shifts[0].passingPointOrderNumber").value(0))
                .andExpect(jsonPath("$.content[0].shifts[1].order").value(1))
                .andExpect(jsonPath("$.content[0].shifts[1].passingPointOrderNumber").value(1))
                .andExpect(jsonPath("$.content[0].shifts[2].order").value(2))
                .andExpect(jsonPath("$.content[0].shifts[2].passingPointOrderNumber").value(6));


    }

    @Test
    @SneakyThrows
    void getRunWithSeveralShiftsFinishedNotUpdatable() {
        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("runId", run2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].shifts").isArray())
                .andExpect(jsonPath("$.content[0].shifts").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].shifts[0].order").value(0))
                .andExpect(jsonPath("$.content[0].shifts[0].isUpdatable").value(true))
                .andExpect(jsonPath("$.content[0].shifts[0].passingPointOrderNumber").value(0))
                .andExpect(jsonPath("$.content[0].shifts[1].order").value(1))
                .andExpect(jsonPath("$.content[0].shifts[1].isUpdatable").value(true))
                .andExpect(jsonPath("$.content[0].shifts[1].passingPointOrderNumber").value(1));

        transactionTemplate.execute(tx -> {
            var newRun = runRepository.findByIdOrThrow(run2.getId());
            testUserHelper.openShift(user1, newRun.getFirstAssignedShift().getId());
            testUserHelper.finishCollectDropships(newRun.getFirstAssignedShift().getFirstRoutePoint());
            testUserHelper.finishDriverChange(newRun.getFirstAssignedShift());

            return null;
        });

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("runId", run2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].shifts").isArray())
                .andExpect(jsonPath("$.content[0].shifts").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].shifts[0].order").value(0))
                .andExpect(jsonPath("$.content[0].shifts[0].isUpdatable").value(false))
                .andExpect(jsonPath("$.content[0].shifts[0].passingPointOrderNumber").value(0))
                .andExpect(jsonPath("$.content[0].shifts[1].order").value(1))
                .andExpect(jsonPath("$.content[0].shifts[1].isUpdatable").value(true))
                .andExpect(jsonPath("$.content[0].shifts[1].passingPointOrderNumber").value(1));

    }

    @Test
    @SneakyThrows
    void getRunWithSeveralShiftsFinishedNotUpdatableLastRoutePoint() {
        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("runId", run2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].shifts").isArray())
                .andExpect(jsonPath("$.content[0].shifts").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].shifts[0].order").value(0))
                .andExpect(jsonPath("$.content[0].shifts[0].isUpdatable").value(true))
                .andExpect(jsonPath("$.content[0].shifts[0].passingPointOrderNumber").value(0))
                .andExpect(jsonPath("$.content[0].shifts[1].order").value(1))
                .andExpect(jsonPath("$.content[0].shifts[1].isUpdatable").value(true))
                .andExpect(jsonPath("$.content[0].shifts[1].passingPointOrderNumber").value(1));

        transactionTemplate.execute(tx -> {
            var newRun = runRepository.findByIdOrThrow(run2.getId());
            testUserHelper.openShift(user1, newRun.getFirstAssignedShift().getId());
            testUserHelper.finishCollectDropships(newRun.getFirstAssignedShift().getFirstRoutePoint());
            testUserHelper.finishDriverChange(newRun.getFirstAssignedShift());
            testUserHelper.openShift(user2, newRun.getLastAssignedShift().getId());
            testUserHelper.finishDriverChange(newRun.getLastAssignedShift());
            testUserHelper.arriveAtRoutePoint(newRun.getLastAssignedShift().getLastRoutePoint());

            return null;
        });

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("runId", run2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].shifts").isArray())
                .andExpect(jsonPath("$.content[0].shifts").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].shifts[0].order").value(0))
                .andExpect(jsonPath("$.content[0].shifts[0].isUpdatable").value(false))
                .andExpect(jsonPath("$.content[0].shifts[0].passingPointOrderNumber").value(0))
                .andExpect(jsonPath("$.content[0].shifts[1].order").value(1))
                .andExpect(jsonPath("$.content[0].shifts[1].isUpdatable").value(false))
                .andExpect(jsonPath("$.content[0].shifts[1].passingPointOrderNumber").value(1));

    }

    @Test
    @SneakyThrows
    void getRunWithSeveralShiftsFinishedNotUpdatableOnFirstShiftOfSimpleRun() {
        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("runId", run3.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].shifts").isArray())
                .andExpect(jsonPath("$.content[0].shifts").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].shifts[0].order").value(0))
                .andExpect(jsonPath("$.content[0].shifts[0].isUpdatable").value(true))
                .andExpect(jsonPath("$.content[0].shifts[0].passingPointOrderNumber").value(1));

        transactionTemplate.execute(tx -> {
            var newRun = runRepository.findByIdOrThrow(run3.getId());
            testUserHelper.openShift(user1, newRun.getFirstAssignedShift().getId());
            runHelper.assignUserShifts(newRun.getId(), List.of(
                    new RunHelper.AssignUserShift(newRun.getLastAssignedShift().getId(), user2.getId(),
                            transport2.getId(), 1, true)),
                    false
            );

            return null;
        });

        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("runId", run3.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].shifts").isArray())
                .andExpect(jsonPath("$.content[0].shifts").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].shifts[0].order").value(0))
                .andExpect(jsonPath("$.content[0].shifts[0].isUpdatable").value(false))
                .andExpect(jsonPath("$.content[0].shifts[0].passingPointOrderNumber").value(1))
                .andExpect(jsonPath("$.content[0].shifts[1].order").value(1))
                .andExpect(jsonPath("$.content[0].shifts[1].isUpdatable").value(true))
                .andExpect(jsonPath("$.content[0].shifts[1].passingPointOrderNumber").value(1));

    }
}
