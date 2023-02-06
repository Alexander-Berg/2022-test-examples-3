package ru.yandex.market.tpl.carrier.driver.controller.api.v2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
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
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.Task;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DriverChangeTaskControllerTest extends BaseDriverApiIntTest {

    private final TestUserHelper testUserHelper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final RunRepository runRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final TransactionTemplate transactionTemplate;

    Run run;
    User user;
    User user2;
    Transport transport;
    Transport transport2;
    Company company;
    OrderWarehouse warehouseTo;
    Set<UserShift> userShifts1;


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

        userShifts1 = runHelper.assignUserShifts(run.getId(), List.of(
                new RunHelper.AssignUserShift(null, user.getId(), transport.getId(), 1, false),
                new RunHelper.AssignUserShift(null, user2.getId(), transport2.getId(), 1, true))
        );
    }

    @SneakyThrows
    @Test
    void shouldReturnShiftsRoutePointsWithDriverChange() {
        transactionTemplate.execute(
                tc -> {
                    var firstUserShift = userShifts1.stream().min(Comparator.comparingInt(UserShift::getOrderNumber)).orElseThrow();
                    run = runRepository.findByIdOrThrow(run.getId());
                    testUserHelper.openShift(run.getFirstAssignedShift().getUser(), run.getFirstAssignedShift().getId());
                    testUserHelper.arriveAtRoutePoint(run.getFirstAssignedShift().getFirstRoutePoint());
                    firstUserShift.getFirstRoutePoint().streamCollectDropshipTasks()
                            .forEach(task ->
                                    userShiftCommandService.collectDropships(
                                            firstUserShift.getUser(),
                                            new UserShiftCommand.CollectDropships(
                                                    firstUserShift.getId(),
                                                    firstUserShift.getFirstRoutePoint().getId(), task.getId()
                                            )
                                    )
                            );

                    var firstTaskId = userShifts1.stream()
                            .min(Comparator.comparingInt(UserShift::getOrderNumber))
                            .map(UserShift::getLastRoutePoint)
                            .map(RoutePoint::getDriverChangeTask)
                            .map(Task::getId)
                            .orElseThrow();
                    try {
                        mockMvc.perform(put("/driver/v1/market-carrier/tasks/driver-change-task/{task-id}/finish", firstTaskId)
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                                .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                                .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                                .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
                        ).andExpect(status().isOk());
                    } catch (Exception e) {
                        throw new TplIllegalStateException(e.getMessage());
                    }

                    return null;
                }
        );


    }

}
