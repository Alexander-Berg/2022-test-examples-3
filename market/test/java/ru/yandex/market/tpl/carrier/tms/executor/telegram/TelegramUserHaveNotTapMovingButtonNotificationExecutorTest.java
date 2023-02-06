package ru.yandex.market.tpl.carrier.tms.executor.telegram;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunItemData;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.carrier.tms.service.telegram.TelegramBotUpdateHandler;

@RequiredArgsConstructor(onConstructor_ = { @Autowired})
@TmsIntTest
public class TelegramUserHaveNotTapMovingButtonNotificationExecutorTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final RunCommandService runCommandService;
    private final MovementGenerator movementGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper testUserHelper;
    private final RunHelper runHelper;

    private final TestableClock clock;

    @Autowired
    @Qualifier("telegramUserHaveNotTapMovingButtonExecutorMock")
    private TelegramUserHaveNotTapMovingButtonNotificationExecutor executor;

    @Autowired
    @Qualifier("telegramBotUpdateHandlerMock")
    private TelegramBotUpdateHandler telegramBotUpdateHandler;

    @BeforeEach
    void setUp() {
        Instant instant = LocalDateTime
                .of(2021, 10, 22, 9, 10, 0)
                .toInstant(ZoneOffset.of("+3"));
        clock.setFixed(instant, ZoneId.of("GMT+3"));

        Mockito.reset(telegramBotUpdateHandler);

    }

    private Run generateConfirmedRunWithStartTime(Instant startTime) {
        var user = testUserHelper.findOrCreateUser(1L, Company.DEFAULT_COMPANY_NAME, UserUtil.PHONE);
        var transport = testUserHelper.findOrCreateTransport();
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        var movement = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                .deliveryIntervalFrom(startTime)
                .deliveryIntervalTo(startTime.plus(10, ChronoUnit.HOURS))
                .build());

        var run = runCommandService.create(RunCommand.Create.builder()
                .externalId("run1")
                .runDate(LocalDate.now(clock))
                .campaignId(company.getCampaignId())
                .items(List.of(RunItemData.builder()
                        .movement(movement)
                        .orderNumber(1)
                        .build()))
                .build()
        );
        runCommandService.confirm(new RunCommand.Confirm(run.getId()));
        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());

        return run;

    }

    @Test
    @SneakyThrows
    void shouldSendFirstNotification() {
        Instant runStartTime = LocalDateTime
                .of(2021, 10, 22, 9, 10, 30)
                .toInstant(ZoneOffset.of("+3"));

        generateConfirmedRunWithStartTime(runStartTime);

        executor.doRealJob(null);

        Mockito.verify(telegramBotUpdateHandler, Mockito.times(2))
                .sendRoutePointAlert(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }
}
