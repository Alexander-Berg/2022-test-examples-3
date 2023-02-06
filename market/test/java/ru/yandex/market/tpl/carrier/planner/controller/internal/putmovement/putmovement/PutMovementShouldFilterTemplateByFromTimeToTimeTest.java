package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplate;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.NewRunTemplateItemData;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

@Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PutMovementShouldFilterTemplateByFromTimeToTimeTest extends BasePlannerWebTest {

    private final PutMovementHelper putMovementHelper;
    private final RunTemplateGenerator runTemplateGenerator;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TestUserHelper testUserHelper;

    private final RunRepository runRepository;

    private final DbQueueTestUtil dbQueueTestUtil;
    private final TransactionTemplate transactionTemplate;

    private Company company;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.RUN_TEMPLATE_ENABLED, true);

        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
    }

    @Test
    void shouldFilterMovementsByFromTimeToTime() {
        RunTemplate template = runTemplateGenerator.generate(cb -> {
            cb.campaignId(company.getCampaignId());
            cb.items(List.of(
                    new NewRunTemplateItemData(
                            "20",
                            "200",
                            0,
                            EnumSet.allOf(DayOfWeek.class),
                            true,
                            0,
                            1,
                            LocalTime.of(3, 30, 0, 0),
                            LocalTime.of(5, 0, 0, 0),
                            null
                    )
            ));
        });

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null),
                        DateTimeInterval.fromFormattedValue(
                                "2021-11-17T03:30:00+03:00/2021-11-17T05:00:00+03:00"
                        ),
                        BigDecimal.ONE,
                        new ResourceId("20", "20"),
                        new ResourceId("200", "200")
                )
        ));

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM2", null),
                        DateTimeInterval.fromFormattedValue(
                                "2021-11-17T00:00:00+03:00/2021-11-17T00:01:00+03:00"
                        ),
                        BigDecimal.ONE,
                        new ResourceId("20", "20"),
                        new ResourceId("200", "200")
                )
        ));

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_RUN_FROM_TEMPLATE, 2);
        dbQueueTestUtil.executeAllQueueItems(QueueType.CREATE_RUN_FROM_TEMPLATE);

        transactionTemplate.execute(tc -> {
            List<Run> runs = runRepository.findByTemplateIdAndRunDate(template.getId(), LocalDate.of(2021, 11, 17));

            Assertions.assertThat(runs).hasSize(1);
            Assertions.assertThat(runs.get(0).streamMovements().findFirst().orElseThrow().getExternalId()).isEqualTo("TMM1");
            return null;
        });

    }

    @Test
    void shouldFilterMovementsByPallets() {
        RunTemplate template = runTemplateGenerator.generate(cb -> {
            cb.campaignId(company.getCampaignId());
            cb.items(List.of(
                    new NewRunTemplateItemData(
                            "20",
                            "200",
                            0,
                            EnumSet.allOf(DayOfWeek.class),
                            true,
                            0,
                            1,
                            null,
                            null,
                            33
                    )
            ));
        });

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null),
                        DateTimeInterval.fromFormattedValue(
                                "2021-11-17T03:30:00+03:00/2021-11-17T05:00:00+03:00"
                        ),
                        BigDecimal.ONE,
                        new ResourceId("20", "20"),
                        new ResourceId("200", "200"),
                        PutMovementControllerTestUtil.INBOUND_DEFAULT_INTERVAL,
                        PutMovementControllerTestUtil.OUTBOUND_DEFAULT_INTERVAL,
                        mb -> mb.setMaxPalletCapacity(15)
                )
        ));

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM2", null),
                        DateTimeInterval.fromFormattedValue(
                                "2021-11-17T00:00:00+03:00/2021-11-17T00:01:00+03:00"
                        ),
                        BigDecimal.ONE,
                        new ResourceId("20", "20"),
                        new ResourceId("200", "200"),
                        PutMovementControllerTestUtil.INBOUND_DEFAULT_INTERVAL,
                        PutMovementControllerTestUtil.OUTBOUND_DEFAULT_INTERVAL,
                        mb -> mb.setMaxPalletCapacity(33)
                )
        ));

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_RUN_FROM_TEMPLATE, 2);
        dbQueueTestUtil.executeAllQueueItems(QueueType.CREATE_RUN_FROM_TEMPLATE);

        transactionTemplate.execute(tc -> {
            List<Run> runs = runRepository.findByTemplateIdAndRunDate(template.getId(), LocalDate.of(2021, 11, 17));


            Assertions.assertThat(runs).hasSize(1);
            Assertions.assertThat(runs.get(0).streamMovements().findFirst().orElseThrow().getExternalId()).isEqualTo("TMM2");
            return null;
        });
    }
}
