package ru.yandex.market.tpl.carrier.tms.dbqueue.usershift.finish;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.DriverQueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;


@TmsIntTest
@RequiredArgsConstructor(onConstructor_=@Autowired)
class AutoFinishUserShiftServiceTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final DbQueueTestUtil dbQueueTestUtil;
    private final TestUserHelper testUserHelper;
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final RunRepository runRepository;
    private final CompanyRepository companyRepository;
    private final OrderWarehouseGenerator orderWarehouseGenerator;

    private final ConfigurationServiceAdapter configurationServiceAdapter;


    private User user;
    private Company company;
    private Transport transport;
    private Run run;
    private UserShift userShift ;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.CLOSE_RUNS_AFTER_LAST_TASK_COMPLETED_DURATION, "PT0H"); //0 hours

        user = testUserHelper.findOrCreateUser(1L);
        company = companyRepository.findCompanyByName(Company.DEFAULT_COMPANY_NAME).orElseThrow();
        transport = testUserHelper.findOrCreateTransport();
        run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("run1")
                .deliveryServiceId(1L)
                .runDate(LocalDate.now())
                .campaignId(company.getCampaignId())
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .build())
                        .orderNumber(1)
                        .build()
                ).build());
        userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());
    }

    @Test
    @SneakyThrows
    void shouldNotCloseUserShiftIfNotLastPoint() {
        dbQueueTestUtil.assertQueueHasSize(DriverQueueType.AUTO_FINISH_USER_SHIFT, 0);
        Run updatedRun = runRepository.findById(run.getId()).orElseThrow();
        UserShift updatedUserShift = updatedRun.getFirstAssignedShift();

        Assertions.assertEquals(UserShiftStatus.ON_TASK, updatedUserShift.getStatus());
        Assertions.assertEquals(RunStatus.STARTED, updatedRun.getStatus());
    }

    @Test
    @SneakyThrows
    void shouldCloseUserShift() {
        testUserHelper.finishCollectDropships(userShift.streamCollectDropshipTasks().findFirst().orElseThrow().getRoutePoint());
        testUserHelper.arriveAtRoutePoint(userShift.getLastRoutePoint());

        dbQueueTestUtil.assertQueueHasSize(DriverQueueType.AUTO_FINISH_USER_SHIFT, 1);
        dbQueueTestUtil.executeAllQueueItems(DriverQueueType.AUTO_FINISH_USER_SHIFT);

        Run updatedRun = runRepository.findById(run.getId()).orElseThrow();
        UserShift updatedUserShift = updatedRun.getFirstAssignedShift();

        Assertions.assertEquals(UserShiftStatus.SHIFT_FINISHED, updatedUserShift.getStatus());
        Assertions.assertEquals(RunStatus.COMPLETED, updatedRun.getStatus());
    }


    @Test
    @SneakyThrows
    void shouldCloseShiftIdempotent() {
        testUserHelper.finishCollectDropships(userShift.streamCollectDropshipTasks().findFirst().orElseThrow().getRoutePoint());
        testUserHelper.finishFullReturnAtEnd(userShift);

        dbQueueTestUtil.assertQueueHasSize(DriverQueueType.AUTO_FINISH_USER_SHIFT, 1);

        Run updatedRun = runRepository.findById(run.getId()).orElseThrow();
        UserShift updatedUserShift = updatedRun.getFirstAssignedShift();
        Assertions.assertEquals(UserShiftStatus.SHIFT_FINISHED, updatedUserShift.getStatus());
        Assertions.assertEquals(RunStatus.COMPLETED, updatedRun.getStatus());

        dbQueueTestUtil.executeAllQueueItems(DriverQueueType.AUTO_FINISH_USER_SHIFT);

        updatedRun = runRepository.findById(run.getId()).orElseThrow();
        updatedUserShift = updatedRun.getFirstAssignedShift();
        Assertions.assertEquals(UserShiftStatus.SHIFT_FINISHED, updatedUserShift.getStatus());
        Assertions.assertEquals(RunStatus.COMPLETED, updatedRun.getStatus());
    }

}
