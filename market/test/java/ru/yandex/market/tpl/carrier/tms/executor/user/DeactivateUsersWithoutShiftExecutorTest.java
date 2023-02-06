package ru.yandex.market.tpl.carrier.tms.executor.user;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.UserStatus;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;

import static ru.yandex.market.tpl.carrier.core.domain.user.UserUtil.ANOTHER_UID;
import static ru.yandex.market.tpl.carrier.core.domain.user.UserUtil.UID;

@RequiredArgsConstructor(onConstructor_=@Autowired)

@TmsIntTest
class DeactivateUsersWithoutShiftExecutorTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final TestUserHelper testUserHelper;
    private final UserCommandService userCommandService;
    private final UserRepository userRepository;

    private final DeactivateUsersWithoutShiftExecutor executor;
    private final RunGenerator runGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunHelper runHelper;
    private final RunRepository runRepository;
    private final UserShiftRepository userShiftRepository;
    private final TransactionTemplate transactionTemplate;

    private User user1;
    private User user2;
    private Company company;
    private Transport transport;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        user1 = testUserHelper.findOrCreateUser(UID, Company.DEFAULT_COMPANY_NAME, UserUtil.PHONE);
        userCommandService.promoteToActive(new UserCommand.PromoteToActive(user1.getId()));
        user1 = userRepository.findByIdOrThrow(user1.getId());
        Assertions.assertThat(user1.getStatus()).isEqualTo(UserStatus.ACTIVE);
        transport = testUserHelper.findOrCreateTransport();

        user2 = testUserHelper.findOrCreateUser(ANOTHER_UID, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE);
        userCommandService.promoteToActive(new UserCommand.PromoteToActive(user2.getId()));
        user2 = userRepository.findByIdOrThrow(user2.getId());
        Assertions.assertThat(user2.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @SneakyThrows
    @Test
    void shouldDeactivateActiveUsersWithoutShifts() {
        executor.doRealJob(null);

        user1 = userRepository.findByIdOrThrow(user1.getId());
        user2 = userRepository.findByIdOrThrow(user2.getId());

        Assertions.assertThat(user1.getStatus()).isEqualTo(UserStatus.NOT_ACTIVE);
        Assertions.assertThat(user1.getStatus()).isEqualTo(UserStatus.NOT_ACTIVE);
    }

    @SneakyThrows
    @Test
    void shouldNotDeactiveUsersWithShift() {
        var run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("run1")
                .deliveryServiceId(123L)
                .campaignId(company.getCampaignId())
                .runDate(today)
                .clearItems()
                .item(
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("6789")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                        .build(),
                                1,
                                null,
                                null
                        )
                )
                .build()
        );

        runHelper.assignUserAndTransport(run, user1, transport);

        var theRun = runRepository.findByIdOrThrow(run.getId());
        testUserHelper.openShift(user1, theRun.getFirstAssignedShift().getId());

        transactionTemplate.execute(tc -> {
            var userShift = userShiftRepository.findByIdOrThrow(theRun.getFirstAssignedShift().getId());
            var collectDropshipTask = userShift.streamCollectDropshipTasks().findFirst().orElseThrow();
            testUserHelper.finishCollectDropships(collectDropshipTask.getRoutePoint());
            testUserHelper.finishFullReturnAtEnd(userShift);
            return null;
        });



        executor.doRealJob(null);

        user1 = userRepository.findByIdOrThrow(user1.getId());
        user2 = userRepository.findByIdOrThrow(user2.getId());

        Assertions.assertThat(user1.getStatus()).isEqualTo(UserStatus.ACTIVE);
        Assertions.assertThat(user2.getStatus()).isEqualTo(UserStatus.NOT_ACTIVE);
    }

}
