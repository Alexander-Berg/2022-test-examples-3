package ru.yandex.market.tpl.carrier.core.domain.run;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

@RequiredArgsConstructor(onConstructor_ = @Autowired)

@CoreTestV2
public class RunCommandServiceTest {

    private final RunCommandService runCommandService;
    private final TestUserHelper testUserHelper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final MovementGenerator movementGenerator;
    private final RunRepository runRepository;

    private final TransactionTemplate transactionTemplate;

    private Run run;

    @Test
    void shouldRecalculateStartEndDateTime() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        LocalDate today = LocalDate.of(2021, 11, 17);

        Movement movement1 = movementGenerator.generate(MovementCommand.Create.builder()
                .outboundArrivalTime(today.atTime(12, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .deliveryIntervalFrom(today.atTime(12, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .inboundArrivalTime(today.atTime(18, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .deliveryIntervalTo(today.atTime(18, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                .build());

        run = runCommandService.create(RunCommand.Create.builder()
                .campaignId(company.getCampaignId())
                .deliveryServiceId(123L)
                .externalId("externalId")
                .runDate(today)
                .items(List.of(new RunItemData(
                        movement1,
                        0,
                        null,
                        null,
                        null
                )))
                .build()
        );

        run = runRepository.findByIdOrThrow(run.getId());

        Assertions.assertThat(ZonedDateTime.ofInstant(run.getStartDateTime(), DateTimeUtil.DEFAULT_ZONE_ID).toLocalTime())
                .isEqualTo(LocalTime.of(12, 0));
        Assertions.assertThat(ZonedDateTime.ofInstant(run.getEndDateTime(), DateTimeUtil.DEFAULT_ZONE_ID).toLocalTime())
                .isEqualTo(LocalTime.of(18, 0));

        Movement movement2 = movementGenerator.generate(MovementCommand.Create.builder()
                .outboundArrivalTime(today.atTime(10, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .deliveryIntervalFrom(today.atTime(10, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .inboundArrivalTime(today.atTime(21, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .deliveryIntervalTo(today.atTime(21, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                .build());

        runCommandService.addOrUpdateItem(new RunCommand.CreateOrUpdateItem(
                run.getId(),
                new RunItemData(
                        movement2,
                        1,
                        null,
                        null,
                        null
                )
        ));

        run = runRepository.findByIdOrThrow(run.getId());

        Assertions.assertThat(ZonedDateTime.ofInstant(run.getStartDateTime(), DateTimeUtil.DEFAULT_ZONE_ID).toLocalTime())
                .isEqualTo(LocalTime.of(10, 0));
        Assertions.assertThat(ZonedDateTime.ofInstant(run.getEndDateTime(), DateTimeUtil.DEFAULT_ZONE_ID).toLocalTime())
                .isEqualTo(LocalTime.of(21, 0));
    }

    @Test
    void addItemShouldBeIdempotent() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        LocalDate today = LocalDate.of(2021, 11, 17);

        Movement movement1 = movementGenerator.generate(MovementCommand.Create.builder()
                .outboundArrivalTime(today.atTime(12, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .deliveryIntervalFrom(today.atTime(12, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .inboundArrivalTime(today.atTime(18, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .deliveryIntervalTo(today.atTime(18, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                .build());

        Run run = runCommandService.create(RunCommand.Create.builder()
                .campaignId(company.getCampaignId())
                .deliveryServiceId(123L)
                .externalId("externalId")
                .runDate(today)
                .items(List.of(new RunItemData(
                        movement1,
                        0,
                        null,
                        null,
                        null
                )))
                .build());

        Movement movement = movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryIntervalFrom(today.atTime(12, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .deliveryIntervalTo(today.atTime(18, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                .build());

        runCommandService.addOrUpdateItem(new RunCommand.CreateOrUpdateItem(
                run.getId(),
                new RunItemData(
                        movement,
                        1,
                        null,
                        null,
                        null
                )
        ));

        runCommandService.addOrUpdateItem(new RunCommand.CreateOrUpdateItem(
                run.getId(),
                new RunItemData(
                        movement,
                        1,
                        null,
                        null,
                        null
                )
        ));


        transactionTemplate.execute(tc -> {
            Run saved = runRepository.findByIdOrThrow(run.getId());
            Assertions.assertThat(saved.streamRunItems().toList()).hasSize(2);
            return null;
        });
    }
}
