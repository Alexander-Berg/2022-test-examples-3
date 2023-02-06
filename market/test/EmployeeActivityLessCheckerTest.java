package ru.yandex.market.jmf.module.ticket.test;

import java.time.Duration;
import java.time.OffsetDateTime;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.module.ticket.DistributionService;
import ru.yandex.market.jmf.module.ticket.EmployeeDistributionStatus;
import ru.yandex.market.jmf.module.ticket.EmployeeLastActivity;
import ru.yandex.market.jmf.module.ticket.impl.EmployeeActivityLessChecker;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@SpringJUnitConfig(classes = {
        ModuleTicketTestConfiguration.class
})
@ActiveProfiles("singleTx")
public class EmployeeActivityLessCheckerTest {

    private TicketTestUtils.TestContext ctx;
    @Inject
    private BcpService bcpService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private DistributionService distributionService;
    @Inject
    private EmployeeActivityLessChecker activityLessChecker;

    @BeforeEach
    public void setUp() {
        ctx = ticketTestUtils.create();
    }

    @Test
    public void expiredLunch() {
        EmployeeDistributionStatus distribution = execute(EmployeeDistributionStatus.STATUS_LUNCH, 300);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_OFFLINE, distribution.getStatus(),
                "Оператор должен оказаться в статусе offline т.к. находится без активности более 120 сек");
    }

    @Test
    public void activeLunch() {
        EmployeeDistributionStatus distribution = execute(EmployeeDistributionStatus.STATUS_LUNCH, 115);
        Assertions.assertEquals(EmployeeDistributionStatus.STATUS_LUNCH, distribution.getStatus(),
                "Оператор должен находится в статусе lunch т.к. находится без активности менее 120 сек");
    }

    private EmployeeDistributionStatus execute(String status, long activityLessTime) {
        distributionService.setEmployeeStatus(ctx.employee0, EmployeeDistributionStatus.STATUS_NOT_READY);
        distributionService.setEmployeeStatus(ctx.employee0, status);
        bcpService.create(EmployeeLastActivity.FQN, Maps.of(
                EmployeeLastActivity.EMPLOYEE, ctx.employee0,
                EmployeeLastActivity.LAST_ACTIVITY, OffsetDateTime.now().minus(Duration.ofSeconds(activityLessTime))
        ));

        activityLessChecker.process();

        return distributionService.getEmployeeStatus(ctx.employee0);
    }
}
