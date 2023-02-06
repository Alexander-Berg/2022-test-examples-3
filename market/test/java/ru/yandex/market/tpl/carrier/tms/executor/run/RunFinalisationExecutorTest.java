package ru.yandex.market.tpl.carrier.tms.executor.run;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.price_control.RunPriceStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.property.RunPropertyType;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

@RequiredArgsConstructor(onConstructor_ = @Autowired)

@TmsIntTest
public class RunFinalisationExecutorTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final TestableClock clock;
    private final RunRepository runRepository;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;
    private final RunCommandService runCommandService;
    private final RunGenerator runGenerator;
    private final TestUserHelper testUserHelper;
    private final TransactionTemplate transactionTemplate;

    private final RunFinalisationExecutor runFinalisationExecutor;

    private Run run;

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

        runCommandService.confirm(new RunCommand.Confirm(run.getId()));
        runCommandService.complete(new RunCommand.Complete(run.getId()));
    }

    @SneakyThrows
    @Test
    void shouldFinaliseRun() {
        clock.setFixed(ZonedDateTime.of(2000, 1, 1, 10, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant(),
                DateTimeUtil.DEFAULT_ZONE_ID);
        runFinalisationExecutor.doRealJob(null);

        transactionTemplate.execute(tc -> {
            var result = runRepository.findById(run.getId());
            Assertions.assertTrue(
                    Boolean.parseBoolean(result.get().getPropertyValue(RunPropertyType.RunPropertyName.IS_FINALISED))
            );

            Assertions.assertEquals(
                    RunPriceStatus.APPROVED_BY_DS,
                    result.get().getPriceStatusOld()
            );
            return null;
        });
    }
}
