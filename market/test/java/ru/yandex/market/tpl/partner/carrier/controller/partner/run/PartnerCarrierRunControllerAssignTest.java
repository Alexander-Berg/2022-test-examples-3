package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator.RunGenerateParam;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator.RunItemGenerateParam;
import ru.yandex.market.tpl.carrier.core.domain.run.RunItem;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.RunType;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointType;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartner;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.run.PartnerRunPriceStatus;
import ru.yandex.market.tpl.partner.carrier.model.run.RunSubtype;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.AssignDriverDto;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.AssignRunTransportDto;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunControllerAssignTest extends BaseTplPartnerCarrierWebIntTest {
    private static final long SORTING_CENTER_ID = 47819L;

    private final DsRepository dsRepository;
    private final RunRepository runRepository;
    private final RunCommandService runCommandService;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;

    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final RunGenerator runGenerator;
    private final DutyGenerator dutyGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;

    private Company company;
    private Run run;
    private User user1;
    private Transport transport;

    private long movementId1;
    private long movementId2;

    private long deliveryServiceId;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .login("anotherLogin@yandex.ru")
                .build()
        );

        user1 = testUserHelper.findOrCreateUser(UID);
        transport = testUserHelper.findOrCreateTransport();

        deliveryServiceId = dsRepository.findByCompaniesId(company.getId()).iterator().next().getId();

        OrderWarehouse warehouseTo =
                orderWarehouseGenerator.generateWarehouse(w -> w.setTimezone("Asia/Yekaterinburg"));

        run = runGenerator.generate(RunGenerateParam.builder()
                .externalId("asd")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .item(new RunItemGenerateParam(
                        MovementCommand.Create.builder()
                                .externalId("123")
                                .orderWarehouse(
                                        orderWarehouseGenerator.generateWarehouse(w -> w.setTimezone("Europe/Moscow")))
                                .orderWarehouseTo(warehouseTo)
                                .deliveryIntervalFrom(
                                        ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID)
                                                .toInstant())
                                .deliveryIntervalTo(
                                        ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID)
                                                .toInstant())
                                .build(),
                        1,
                        null,
                        null
                ))
                .item(new RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("345")
                                        .orderWarehouse(
                                                orderWarehouseGenerator.generateWarehouse(w -> w.setTimezone("Europe/Moscow")))
                                        .orderWarehouseTo(warehouseTo)
                                        .deliveryIntervalFrom(
                                                ZonedDateTime.of(2021, 8, 5, 2, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID)
                                                        .toInstant())
                                        .deliveryIntervalTo(
                                                ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID)
                                                        .toInstant())
                                        .build(),
                                2,
                                null,
                                null
                        )
                ).build()
        );

        movementId1 = run.streamRunItems()
                .filter(ri -> ri.getMovement().getExternalId().equals("123"))
                .findFirst().orElseThrow()
                .getMovement().getId();

        movementId2 = run.streamRunItems()
                .filter(ri -> ri.getMovement().getExternalId().equals("345"))
                .findFirst().orElseThrow()
                .getMovement().getId();

    }

    @SneakyThrows
    @Test
    void shouldGetRuns() {
        mockMvc.perform(
                        get("/internal/partner/runs/v2")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].driver").doesNotExist())
                .andExpect(jsonPath("$.content[0].id").value(run.getId()))
                .andExpect(jsonPath("$.content[0].status").value(RunStatus.CREATED.name()))
                .andExpect(jsonPath("$.content[0].runPriceStatus").value(PartnerRunPriceStatus.NOT_READY.name()));

    }

    @SneakyThrows
    @Test
    void shouldGetRunsFinalised() {
        runCommandService.finaliseRunOld(run.getId());
        mockMvc.perform(
                        get("/internal/partner/runs/v2")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].runPriceStatus")
                        .value(PartnerRunPriceStatus.NEED_DS_CONFIRMATION.name())
                );
    }

    @SneakyThrows
    @Test
    void shouldGetTimestampsInRuns() {
        mockMvc.perform(
                        get("/internal/partner/runs/v2")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].items[1].movement.defaultExpectedArrivalTimestampFrom.timestamp")
                        .value("2021-08-05T02:00:00+03:00"))
                .andExpect(jsonPath("$.content[0].items[1].movement.defaultExpectedArrivalTimestampFrom.timezoneName")
                        .value("Europe/Moscow"))
                .andExpect(jsonPath("$.content[0].items[1].movement.localExpectedArrivalTimestampFrom.timestamp")
                        .value("2021-08-05T02:00:00+03:00"))
                .andExpect(jsonPath("$.content[0].items[1].movement.localExpectedArrivalTimestampFrom.timezoneName")
                        .value("Europe/Moscow"))
                .andExpect(jsonPath("$.content[0].items[1].movement.defaultExpectedArrivalTimestampTo.timestamp")
                        .value("2021-08-05T05:00:00+03:00"))
                .andExpect(jsonPath("$.content[0].items[1].movement.defaultExpectedArrivalTimestampTo.timezoneName")
                        .value("Europe/Moscow"))
                .andExpect(jsonPath("$.content[0].items[1].movement.localExpectedArrivalTimestampTo.timestamp")
                        .value("2021-08-05T07:00:00+05:00"))
                .andExpect(jsonPath("$.content[0].items[1].movement.localExpectedArrivalTimestampTo.timezoneName")
                        .value("Asia/Yekaterinburg"));
    }

    @SneakyThrows
    @Test
    void shouldGetRunWithDuty() {
        Duty duty = dutyGenerator.generate(d -> DutyGenerator.DutyGenerateParams.builder()
                .deliveryServiceId(deliveryServiceId)
                .dutyStartTime(ZonedDateTime.of(2021, 1, 2, 10, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                .dutyEndTime(ZonedDateTime.of(2021, 1, 2, 20, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                .pallets(15)
                .priceCents(680000L)
                .dutyWarehouseId(orderWarehouseGenerator
                        .generateWarehouse(w -> w.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                                new OrderWarehousePartner("666", "СЦ Климовск")
                        ))).getYandexId()));


        mockMvc.perform(
                        get("/internal/partner/runs/v2?date=2021-01-02")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(duty.getRunDuty().get(0).getRun().getId()))
                .andExpect(jsonPath("$.content[0].status").value(RunStatus.CREATED.name()))
                .andExpect(jsonPath("$.content[0].type").value(RunType.LINEHAUL.name()))
                .andExpect(jsonPath("$.content[0].subtype").value(RunSubtype.DUTY.name()))
                .andExpect(jsonPath("$.content[0].duty.id").value(duty.getId()))
                .andExpect(jsonPath("$.content[0].duty.from.timestamp")
                        .value("2021-01-02T10:00:00+03:00"))
                .andExpect(jsonPath("$.content[0].duty.to.timestamp")
                        .value("2021-01-02T20:00:00+03:00"))
                .andExpect(jsonPath("$.content[0].duty.partnerName")
                        .value(duty.getDutyWarehouse().getPartner().getName()))
                .andExpect(jsonPath("$.content[0].duty.address")
                        .value(duty.getDutyWarehouse().getAddress().getAddress()))
                .andExpect(jsonPath("$.content[0].duty.pallets").value(duty.getPallets()));
    }

    @SneakyThrows
    @Test
    void shouldAssignRun() {
        mockMvc.perform(
                        post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk());
        mockMvc.perform(
                        post("/internal/partner/runs/{runId}/assign", run.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            run = runRepository.findByIdOrThrow(run.getId());
            UserShift userShift = run.getFirstAssignedShift();
            List<RunItem> runItems = run.streamRunItems().toList();
            Map<Long, RunItem> runItemsMap = run.streamRunItems().toMap(RunItem::getId, Function.identity());

            Assertions.assertThat(userShift.streamCollectDropshipTasks().count()).isEqualTo(2);

            List<RoutePoint> routePoints = userShift.streamRoutePoints().toList();
            Assertions.assertThat(routePoints).hasSize(3);

            RoutePoint first = routePoints.get(0);
            first.streamCollectDropshipTasks()
                    .findFirst(cdt -> cdt.getRunItemIds().stream().findFirst().map(id -> runItemsMap.get(id).getMovement().getId()).orElseThrow() == movementId1
                            && cdt.getId().equals(runItems.get(0).getCollectTaskId()))
                    .orElseThrow();

            RoutePoint second = routePoints.get(1);
            second.streamCollectDropshipTasks()
                    .findFirst(cdt -> cdt.getRunItemIds().stream().findFirst().map(id -> runItemsMap.get(id).getMovement().getId()).orElseThrow() == movementId2
                            && cdt.getId().equals(runItems.get(1).getCollectTaskId())
                    )
                    .orElseThrow();

            RoutePoint third = routePoints.get(2);
            OrderReturnTask orderReturnTask = third.streamReturnTasks().findFirst().orElseThrow();
            Assertions.assertThat(runItems)
                    .allSatisfy(ri -> {
                        Assertions.assertThat(ri.getReturnTaskId()).isEqualTo(orderReturnTask.getId());
                    });
            return null;
        });

        mockMvc.perform(
                        get("/internal/partner/runs/v2")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].driver.id").value(user1.getId()))
                .andExpect(jsonPath("$.content[0].status").value(RunStatus.ASSIGNED.name()));
    }

    @SneakyThrows
    @Test
    void shouldAssignRunAfterConfirmation() {
        mockMvc.perform(
                        post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk());
        mockMvc.perform(
                post("/internal/partner/runs/{runId}/confirm", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());
        Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.CONFIRMED);

        mockMvc.perform(
                        post("/internal/partner/runs/{runId}/assign", run.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            run = runRepository.findByIdOrThrow(run.getId());
            UserShift userShift = run.getFirstAssignedShift();
            Map<Long, RunItem> runItemsMap = run.streamRunItems().toMap(RunItem::getId, Function.identity());

            Assertions.assertThat(userShift.streamCollectDropshipTasks().count()).isEqualTo(2);

            CollectDropshipTask first = StreamEx.of(userShift.streamCollectDropshipTasks())
                    .findFirst(cdt -> cdt.getRunItemIds().stream().findFirst().map(id -> runItemsMap.get(id).getMovement().getId()).orElseThrow() == movementId1)
                    .orElseThrow();

            CollectDropshipTask second = StreamEx.of(userShift.streamCollectDropshipTasks())
                    .findFirst(cdt -> cdt.getRunItemIds().stream().findFirst().map(id -> runItemsMap.get(id).getMovement().getId()).orElseThrow() == movementId2)
                    .orElseThrow();

            OrderReturnTask orderReturnTask = Iterators.getOnlyElement(
                            userShift.streamRoutePoints().filterBy(RoutePoint::getType, RoutePointType.ORDER_RETURN).iterator())
                    .getOrderReturnTask();
            return null;
        });

        mockMvc.perform(
                        get("/internal/partner/runs/v2")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].driver.id").value(user1.getId()))
                .andExpect(jsonPath("$.content[0].status").value(RunStatus.ASSIGNED.name()));
    }

    @SneakyThrows
    @Test
    void shouldApproveRunPrice() {
        runCommandService.finaliseRunOld(run.getId());
        mockMvc.perform(post("/internal/partner/runs/" + run.getId() + "/price/APPROVE_BY_DS")
                        .param("comment", "Огонек красавчик")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk());
        mockMvc.perform(get("/internal/partner/runs/v2")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].runPriceStatus")
                        .value(PartnerRunPriceStatus.APPROVED_BY_DS.name())
                );
    }
}
