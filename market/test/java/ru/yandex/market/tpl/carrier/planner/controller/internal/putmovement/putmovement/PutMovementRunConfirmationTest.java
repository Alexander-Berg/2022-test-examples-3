package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementHistoryEventType;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.carrier.core.domain.movement.event.history.MovementHistoryEvent;
import ru.yandex.market.tpl.carrier.core.domain.movement.event.history.MovementHistoryEventRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.NewRunTemplateItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.RunTemplateCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementControllerTestUtil.prepareMovement;
import static ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementControllerTestUtil.wrap;

@RequiredArgsConstructor(onConstructor_ = @Autowired)

@Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
public class PutMovementRunConfirmationTest extends BasePlannerWebTest {

    private static final int DELIVERY_SERVICE_ID = 100500;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final PutMovementHelper putMovementHelper;
    private final RunRepository runRepository;
    private final MovementRepository movementRepository;
    private final MovementHistoryEventRepository movementHistoryEventRepository;
    private final RunTemplateGenerator runTemplateGenerator;
    private final TestUserHelper testUserHelper;

    private final DbQueueTestUtil dbQueueTestUtil;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DELIVERY_SERVICES_TO_AUTOCONFIRM, DELIVERY_SERVICE_ID);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.RUN_TEMPLATE_ENABLED, true);

        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        runTemplateGenerator.generate(RunTemplateCommand.Create.builder()
                .campaignId(company.getCampaignId())
                .externalId("externalId")
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .orderNumber(0)
                                .daysOfWeek(EnumSet.of(DayOfWeek.WEDNESDAY))
                                .warehouseYandexIdFrom("10")
                                .warehouseYandexIdTo("200")
                                .build(),
                        NewRunTemplateItemData.builder()
                                .orderNumber(1)
                                .daysOfWeek(EnumSet.of(DayOfWeek.WEDNESDAY))
                                .warehouseYandexIdFrom("20")
                                .warehouseYandexIdTo("200")
                                .build(),
                        NewRunTemplateItemData.builder()
                                .orderNumber(2)
                                .daysOfWeek(EnumSet.of(DayOfWeek.WEDNESDAY))
                                .warehouseYandexIdFrom("30")
                                .warehouseYandexIdTo("200")
                                .build()
                ))
                .build());
    }

    @Test
    void shouldGenerateMovementConfirmedEventForEachAddedMovement() {
        putMovementHelper.performPutMovement(wrap(prepareMovement(
                new ResourceId("TMM1", null),
                new ResourceId("10", "10"),
                new ResourceId("200", "200")
        )));
        putMovementHelper.performPutMovement(wrap(prepareMovement(
                new ResourceId("TMM2", null),
                new ResourceId("20", "20"),
                new ResourceId("200", "200")
        )));
        putMovementHelper.performPutMovement(wrap(prepareMovement(
                new ResourceId("TMM3", null),
                new ResourceId("30", "30"),
                new ResourceId("200", "300")
        )));

        dbQueueTestUtil.executeAllQueueItems(QueueType.CREATE_RUN_FROM_TEMPLATE);

        List<Run> runs = runRepository.findAll();
        Assertions.assertThat(runs).hasSize(1);

        List<Movement> movements = movementRepository.findAll();
        Assertions.assertThat(movements).hasSize(3);

        Assertions.assertThat(movements).allSatisfy(m -> {
            Page<MovementHistoryEvent> events =
                    movementHistoryEventRepository.findByMovementId(m.getId(), Pageable.unpaged());

            Assertions.assertThat(events)
                    .anySatisfy(e -> {
                        Assertions.assertThat(e.getType()).isEqualTo(MovementHistoryEventType.MOVEMENT_CONFIRMED);
                    });
        });
    }
}
