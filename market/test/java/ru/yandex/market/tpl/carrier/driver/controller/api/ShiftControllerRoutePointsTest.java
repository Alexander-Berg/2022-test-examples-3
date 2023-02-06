package ru.yandex.market.tpl.carrier.driver.controller.api;

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
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.transport.TestTransportTypeHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.EcologicalClass;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.DriverChangeTaskSubtype;
import ru.yandex.market.tpl.carrier.core.domain.usershift.TaskType;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ShiftControllerRoutePointsTest extends BaseDriverApiIntTest  {

    private final TestUserHelper testUserHelper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final RunRepository runRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    Run run;
    Run run2;
    User user;
    User user2;
    Transport transport;
    Transport transport2;
    Company company;
    OrderWarehouse warehouseTo;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        warehouseTo = orderWarehouseGenerator.generateWarehouse();

        TestTransportTypeHelper.TransportTypeGenerateParam transportTypeGenerateParam =
                TestTransportTypeHelper.TransportTypeGenerateParam.builder()
                        .name(TestUserHelper.DEFAULT_TRANSPORT_NAME)
                        .capacity(BigDecimal.ZERO)
                        .palletsCapacity(33)
                        .company(company)
                        .grossWeightTons(new BigDecimal("2.0"))
                        .maxLoadOnAxleTons(new BigDecimal("4.0"))
                        .maxWeightTons(new BigDecimal("8.0"))
                        .lengthMeters(new BigDecimal("3"))
                        .heightMeters(new BigDecimal("4"))
                        .widthMeters(new BigDecimal("5"))
                        .ecologicalClass(EcologicalClass.EURO5)
                        .build();
        transport = testUserHelper.findOrCreateTransport(TestUserHelper.DEFAULT_TRANSPORT_NAME,
                transportTypeGenerateParam, company);
        user = testUserHelper.findOrCreateUser(UID);
        transport2 = testUserHelper.findOrCreateTransport(TestUserHelper.DEFAULT_TRANSPORT_NAME + "2",
                transportTypeGenerateParam, company);
        user2 = testUserHelper.findOrCreateUser(162022, "+79999999999",
                "Костюков", "Павел", "Владимирович"
        );

        run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd")
                .deliveryServiceId(123L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .item(new RunGenerator.RunItemGenerateParam(
                        MovementCommand.Create.builder()
                                .externalId("123")
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                .weight(new BigDecimal("1000"))
                                .build(),
                        1,
                        null,
                        null
                ))
                .build()
        );

        run2 = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd1")
                .deliveryServiceId(123L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .items(List.of(new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("124_1")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(warehouseTo)
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .weight(new BigDecimal("1000"))
                                        .build(),
                                1,
                                0,
                                2),
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("124_2")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(warehouseTo)
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 7, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 8, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .weight(new BigDecimal("1000"))
                                        .build(),
                                2,
                                1,
                                3)

                ))
                .build()
        );

        runHelper.assignUserAndTransport(run, user, transport);
        runHelper.assignUserShifts(run2.getId(), List.of(
                new RunHelper.AssignUserShift(null, user.getId(), transport.getId(), 1, false),
                new RunHelper.AssignUserShift(null, user2.getId(), transport2.getId(), 2, true))
        );

        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_ENABLED, true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_LOOK_AHEAD_MINUTES, 12 * 60);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_LOOK_BEHIND_MINUTES, -12 * 60);
    }

    @SneakyThrows
    @Test
    void shouldReturnShiftsRoutePoints() {
        run = runRepository.findById(run.getId()).orElseThrow();

        Long id = run.getFirstAssignedShift().getId();
        mockMvc.perform(get("/api/shifts/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(2)));
    }

    @SneakyThrows
    @Test
    void shouldReturnShiftsRoutePointsWithDriverChange() {
        run2 = runRepository.findByIdOrThrow(run2.getId());

        Long firstId = run2.getFirstAssignedShift().getId();
        mockMvc.perform(get("/api/shifts/{id}", firstId)
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$[2].tasks[0].type").value(TaskType.DRIVER_CHANGE.name()))
                .andExpect(jsonPath("$[2].tasks[0].subtype").value(DriverChangeTaskSubtype.PASS.name()));

    }
}
