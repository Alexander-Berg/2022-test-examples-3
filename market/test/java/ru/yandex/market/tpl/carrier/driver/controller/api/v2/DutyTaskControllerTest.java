package ru.yandex.market.tpl.carrier.driver.controller.api.v2;

import java.util.Set;

import javax.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyStatus;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.core.domain.user.UserUtil.TAXI_ID;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DutyTaskControllerTest extends BaseDriverApiIntTest {

    private final DutyGenerator dutyGenerator;
    private final RunHelper runHelper;
    private final MockMvc mockMvc;
    private final TestUserHelper testUserHelper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;

    private final UserShiftCommandService userShiftCommandService;

    Duty duty;
    Run run;
    UserShift userShift;

    User user;
    Transport transport;
    Company company;
    DeliveryService deliveryService;

    @BeforeEach
    void setup() {
        company = testUserHelper.findOrCreateCompany("new_company");
        deliveryService = testUserHelper.deliveryService(10001L, Set.of(company));

        duty = dutyGenerator.generate(db -> db.deliveryServiceId(deliveryService.getId()));
        run = duty.getRun();
        user = testUserHelper.findOrCreateUser(TAXI_ID, UID);
        transport = testUserHelper.findOrCreateTransport();
    }

    @Test
    @Transactional
    void confirmDutyRun() throws Exception {
        userShift = runHelper.assignUserAndTransport(run, user, transport);
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.arriveAtRoutePoint(userShift, userShift.getFirstRoutePoint().getId());

        runHelper.createDutyRun(
                orderWarehouseGenerator.generateWarehouse(),
                orderWarehouseGenerator.generateWarehouse(),
                duty,
                run
        );

        Assertions.assertThat(duty.getStatus()).isEqualTo(DutyStatus.TRIP_CREATED);

        mockMvc.perform(
                        post(ApiParams.LEGACY_BASE_PATH + "/tasks/duty-task/{task-id}/confirmRun",
                                userShift.getFirstRoutePoint().getDutyTask().getId()
                        )
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                )
                .andExpect(status().isOk());

        Assertions.assertThat(duty.getStatus()).isEqualTo(DutyStatus.DUTY_FINISHED);
    }

}