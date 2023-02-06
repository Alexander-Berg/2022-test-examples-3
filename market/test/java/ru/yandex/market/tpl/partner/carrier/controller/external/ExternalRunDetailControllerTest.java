package ru.yandex.market.tpl.partner.carrier.controller.external;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunMessageSource;
import ru.yandex.market.tpl.carrier.core.domain.run.RunMessageType;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.NewRunMessageData;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartner;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.run.RunStatus;
import ru.yandex.market.tpl.partner.carrier.service.run.PartnerCarrierRunService;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ExternalRunDetailControllerTest extends BaseTplPartnerCarrierWebIntTest {

    private static final long SORTING_CENTER_ID = 47819L;
    private static final String RUN_NAME = "Маршрут 1";

    private final TestUserHelper testUserHelper;
    private final RunGenerator manualRunService;
    private final PartnerCarrierRunService partnerCarrierRunService;
    private final UserShiftCommandService userShiftCommandService;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final DsRepository dsRepository;
    private final RunRepository runRepository;
    private final RunCommandService runCommandService;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;
    private final TestableClock clock;

    private Company company;
    private Run run;
    private User user1;
    private OrderWarehouse warehouseFrom;
    private OrderWarehouse warehouseTo;
    private Transport transport;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("1990-01-01T00:00:00Z"), ZoneId.of("UTC"));
        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .login("anotherLogin@yandex.ru")
                .build()
        );

        user1 = testUserHelper.findOrCreateUser(UID);
        transport = testUserHelper.findOrCreateTransport();

        Long deliveryServiceId = dsRepository.findByCompaniesId(company.getId()).iterator().next().getId();

        warehouseFrom = orderWarehouseGenerator.generateWarehouse(
                ow -> ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                        new OrderWarehousePartner("123", "Рога и копыта")
                ))
        );
        warehouseTo = orderWarehouseGenerator.generateWarehouse(
                ow -> ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                        new OrderWarehousePartner("234", "Стулья даром")
                ))
        );
        run = manualRunService.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now())
                .name(RUN_NAME)
                .item(
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("123")
                                        .orderWarehouse(warehouseFrom)
                                        .orderWarehouseTo(warehouseTo)
                                        .build(),
                                1,
                                null,
                                null
                        )
                )
                .build()
        );
    }

    @SneakyThrows
    @Test
    void shouldGetRunPointsFinished() {
        // given
        partnerCarrierRunService.assignTransportToRun(run.getId(), transport.getId(), company.getId());
        partnerCarrierRunService.assignCourierToRun(run.getId(), user1.getId(), company.getId());

        transactionTemplate.execute(tc -> {
            run = runRepository.findById(run.getId()).orElseThrow();

            var userShift = run.getFirstAssignedShift();

            var collectDropshipTask =  userShift.streamCollectDropshipTasks().findFirst().orElseThrow();
            testUserHelper.openShift(user1, userShift.getId());
            testUserHelper.arriveAtRoutePoint(userShift, collectDropshipTask.getRoutePoint().getId());
            userShiftCommandService.collectDropships(user1, new UserShiftCommand.CollectDropships(
                    userShift.getId(),
                    collectDropshipTask.getRoutePoint().getId(),
                    collectDropshipTask.getId())
            );

            testUserHelper.finishFullReturnAtEnd(userShift);
            return null;
        });

        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/external/runs")
                        .param("runId", String.valueOf(run.getId()))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                // expect
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.points[0].id").value(warehouseFrom.getId()))
                .andExpect(jsonPath("$.points[0].address").value(warehouseFrom.getAddress().getAddress()))
                .andExpect(jsonPath("$.points[0].partnerName").value(warehouseFrom.getPartner().getName()))
                .andExpect(jsonPath("$.points[0].expectedArrivalTimestamp")
                        .value("1990-01-01T06:00:00Z"))
                .andExpect(jsonPath("$.points[0].defaultExpectedArrivalTimestamp.timestamp")
                        .value("1990-01-01T09:00:00+03:00"))
                .andExpect(jsonPath("$.points[0].arrivalTimestamp")
                        .value("1990-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.points[0].carrierArrivalTimestamp")
                        .value("1990-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.points[0].defaultArrivalTimestamp.timestamp")
                        .value("1990-01-01T03:00:00+03:00"))
                .andExpect(jsonPath("$.points[1].id").value(warehouseTo.getId()))
                .andExpect(jsonPath("$.points[1].address").value(warehouseTo.getAddress().getAddress()))
                .andExpect(jsonPath("$.points[1].partnerName").value(warehouseTo.getPartner().getName()));
    }

    @SneakyThrows
    @Test
    void shouldGetRunPointsNotFinished() {
        // given
        partnerCarrierRunService.assignTransportToRun(run.getId(), transport.getId(), company.getId());
        partnerCarrierRunService.assignCourierToRun(run.getId(), user1.getId(), company.getId());
        run = runRepository.findById(run.getId()).orElseThrow();

        var userShift = run.getFirstAssignedShift();

        testUserHelper.openShift(user1, userShift.getId());

        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/external/runs")
                        .param("runId", String.valueOf(run.getId()))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                // expect
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.points[0].id").value(warehouseFrom.getId()))
                .andExpect(jsonPath("$.points[0].address").value(warehouseFrom.getAddress().getAddress()))
                .andExpect(jsonPath("$.points[0].partnerName").value(warehouseFrom.getPartner().getName()))
                .andExpect(jsonPath("$.points[0].expectedArrivalTimestamp")
                        .value("1990-01-01T06:00:00Z"))
                .andExpect(jsonPath("$.points[0].defaultExpectedArrivalTimestamp.timestamp")
                        .value("1990-01-01T09:00:00+03:00"))
                .andExpect(jsonPath("$.points[0].arrivalTimestamp").doesNotExist())
                .andExpect(jsonPath("$.points[0].defaultArrivalTimestamp.timestamp").doesNotExist())
                .andExpect(jsonPath("$.points[0].defaultArrivalTimestamp.timezoneName")
                        .value("Europe/Moscow"))
                .andExpect(jsonPath("$.points[1].id").value(warehouseTo.getId()))
                .andExpect(jsonPath("$.points[1].address").value(warehouseTo.getAddress().getAddress()))
                .andExpect(jsonPath("$.points[1].partnerName").value(warehouseTo.getPartner().getName()));
    }

    @SneakyThrows
    @Test
    void shouldGetRun() {
        // given
        partnerCarrierRunService.assignTransportToRun(run.getId(), transport.getId(), company.getId());
        partnerCarrierRunService.assignCourierToRun(run.getId(), user1.getId(), company.getId());

        transactionTemplate.execute(tc -> {
            run = runRepository.findById(run.getId()).orElseThrow();
            var userShift = run.getFirstAssignedShift();
            var collectDropshipTask =  userShift.streamCollectDropshipTasks().findFirst().orElseThrow();
            testUserHelper.openShift(user1, userShift.getId());
            testUserHelper.arriveAtRoutePoint(userShift, collectDropshipTask.getRoutePoint().getId());
            userShiftCommandService.collectDropships(user1, new UserShiftCommand.CollectDropships(
                    userShift.getId(),
                    collectDropshipTask.getRoutePoint().getId(),
                    collectDropshipTask.getId())
            );

            testUserHelper.finishFullReturnAtEnd(userShift);
            return null;
        });

        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/external/runs")
                        .param("runId", String.valueOf(run.getId()))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                // expect
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(run.getId()))
                .andExpect(jsonPath("$.name").value(RUN_NAME))
                .andExpect(jsonPath("$.status").value(RunStatus.COMPLETED.name()))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.points[0].id").value(warehouseFrom.getId()))
                .andExpect(jsonPath("$.points[0].address").value(warehouseFrom.getAddress().getAddress()))
                .andExpect(jsonPath("$.points[0].partnerName").value(warehouseFrom.getPartner().getName()))
                .andExpect(jsonPath("$.points[1].id").value(warehouseTo.getId()))
                .andExpect(jsonPath("$.points[1].address").value(warehouseTo.getAddress().getAddress()))
                .andExpect(jsonPath("$.points[1].partnerName").value(warehouseTo.getPartner().getName()))
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages").isNotEmpty())
                .andExpect(jsonPath("$.userLocations").isArray())
                .andExpect(jsonPath("$.userLocations").isNotEmpty())
        ;
    }

    @SneakyThrows
    @Test
    void shouldGetRunMessageTimezoneAndLocation() {
        // given
        partnerCarrierRunService.assignTransportToRun(run.getId(), transport.getId(), company.getId());
        partnerCarrierRunService.assignCourierToRun(run.getId(), user1.getId(), company.getId());
        run = runRepository.findById(run.getId()).orElseThrow();

        var userShift = run.getFirstAssignedShift();

        testUserHelper.openShift(user1, userShift.getId());
        var message = new NewRunMessageData(
                new BigDecimal("12"),
                new BigDecimal("34"),
                RunMessageType.CRITICAL_ISSUE,
                "Не грузят!",
                false,
                1,
                "Europe/Moscow",
                user1.getName(),
                RunMessageSource.CARRIER);

        runCommandService.addMessage(new RunCommand.AddMessage(run.getId(), message));

        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/external/runs")
                        .param("runId", String.valueOf(run.getId()))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )

                // expect
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("messages[1].longitude").value(message.getLongitude()))
                .andExpect(jsonPath("messages[1].latitude").value(message.getLatitude()))
                .andExpect(jsonPath("messages[1].localTimestamp.timezoneName").value(message.getTimezone()))
                .andExpect(jsonPath("messages[1].message").value(message.getMessage()));
    }

}
