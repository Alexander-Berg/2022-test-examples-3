package ru.yandex.market.tpl.carrier.tms.executor.penalty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunPriceControl;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.price_control.PriceControlType;
import ru.yandex.market.tpl.carrier.core.domain.run.price_control.PriceStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.price_control.RunPriceControlRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.price_control.RunPriceStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.ArrivalData;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;

@RequiredArgsConstructor(onConstructor_=@Autowired)

@TmsIntTest
public class RunPenaltyExecutorTest {

    private final TestableClock clock;
    private final RunRepository runRepository;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;
    private final RunCommandService runCommandService;
    private final RoutePointRepository routePointRepository;
    private final RunGenerator runGenerator;
    private final TestUserHelper testUserHelper;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftCommandService userShiftCommandService;
    private final RunPriceControlRepository runPriceControlRepository;
    private final RunHelper runHelper;

    private final RunPenaltyExecutor runPenaltyExecutor;

    private Run run;
    private UserShift userShift;
    private User user;

    @BeforeEach
    void setUp() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("run1")
                .deliveryServiceId(123L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now(clock))
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .externalId("123")
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .deliveryIntervalFrom(ZonedDateTime.now(clock).minusHours(3).toInstant())
                                .deliveryIntervalTo(ZonedDateTime.now(clock).minusHours(2).toInstant())
                                .build()
                        )
                        .orderNumber(1)
                        .build())
                .build()
        );
        user = testUserHelper.findOrCreateUser(1L);
        Transport transport = testUserHelper.findOrCreateTransport();

        userShift = runHelper.assignUserAndTransport(run, user, transport);

        runCommandService.complete(new RunCommand.Complete(run.getId()));
    }

    @SneakyThrows
    @Test
    @Disabled
    void shouldSetPenalties() {
        var point = userShift.streamRoutePoints().findFirst().orElseThrow();
        userShiftCommandService.arriveAtRoutePointManual(new UserShiftCommand.ArriveAtRoutePointManual(
                userShift.getId(),
                point.getId(),
                new ArrivalData(BigDecimal.ZERO, BigDecimal.ZERO, "some_comment"),
                point.getExpectedDateTime().plus(320, ChronoUnit.MINUTES)
        ));
        runCommandService.finaliseRun(new RunCommand.Finalise(run.getId()));

        runPenaltyExecutor.doRealJob(null);
        var penalties = runPriceControlRepository.findByRunId(run.getId());
        var delayPenalty = StreamEx.of(penalties)
                        .filterBy(RunPriceControl::getType, PriceControlType.AUTO_DELAY)
                                .findFirst().orElseThrow();
        Assertions.assertEquals(-600000, delayPenalty.getCent());
        run = runRepository.findById(run.getId()).orElseThrow();
        Assertions.assertEquals(PriceStatus.NEED_CARRIER_APPROVE, run.getPriceStatus());
    }
}
