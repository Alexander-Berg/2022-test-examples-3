package ru.yandex.market.tpl.carrier.core.domain.duty.dbqueue;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.carrier.core.BaseCoreTest;
import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;

@CoreTestV2
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DutyFinishServiceTest extends BaseCoreTest {

    private final DutyGenerator dutyGenerator;
    private final DutyFinishService dutyFinishService;
    private final TestUserHelper testUserHelper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunHelper runHelper;

    private Duty duty1;
    private OrderWarehouse orderWarehouse;

    @BeforeEach
    void setup() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        Company company1 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName("Another company")
                .campaignId(1001L)
                .deliveryServiceIds(Set.of(124L))
                .login("login-1")
                .build());
        testUserHelper.deliveryService(123L, Set.of(company));
        testUserHelper.deliveryService(124L, Set.of(company1));
        orderWarehouse = orderWarehouseGenerator.generateWarehouse();
        duty1 = dutyGenerator.generate();

    }

    @Test
    @Transactional
    void processPayloadTest() {
        Run run = duty1.getRun();
        User user = testUserHelper.findOrCreateUser(123L);
        runHelper.assignUserAndTransport(run, user,
                testUserHelper.findOrCreateTransport());
        testUserHelper.openShift(user, run.getFirstAssignedShift().getId());
        testUserHelper.arriveAtRoutePoint(run.getFirstAssignedShift().getFirstRoutePoint());

        dutyFinishService.processPayload(new DutyFinishPayload("1", CarrierSource.SYSTEM, duty1.getId()));

        Assertions.assertThat(duty1.getStatus()).isEqualTo(DutyStatus.DUTY_FINISHED);
    }

    @Test
    @Transactional
    void processPayloadNotArrived() {
        Run run = duty1.getRun();
        User user = testUserHelper.findOrCreateUser(123L);
        runHelper.assignUserAndTransport(run, user,
                testUserHelper.findOrCreateTransport());
        dutyFinishService.processPayload(new DutyFinishPayload("1", CarrierSource.SYSTEM, duty1.getId()));

        Assertions.assertThat(duty1.getStatus()).isEqualTo(DutyStatus.DUTY_FINISHED);
    }


}
