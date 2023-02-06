package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

@RequiredArgsConstructor(onConstructor_=@Autowired)

@Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
public class PutMovementCreateRunWithoutTemplateTest extends BasePlannerWebTest {

    private static final long DELIVERY_SERVICE_ID = 100500L;

    private final TestUserHelper testUserHelper;
    private final PutMovementHelper putMovementHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;

    private final RunRepository runRepository;

    private Company company;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.RUN_TEMPLATE_ENABLED, true);

        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .campaignId(RunTemplateGenerator.CAMPAIGN_ID)
                .deliveryServiceIds(Set.of(DELIVERY_SERVICE_ID))
                .build());
    }

    @Test
    void shouldCreateRunIfDeliveryServiceIsWhitelisted() {
        performCreateRun();

        List<Run> runs = runRepository.findAll();
        Assertions.assertThat(runs).hasSize(1);

        Run run = runs.get(0);
        Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.CREATED);
        Assertions.assertThat(run.getStartDateTime()).isEqualTo(PutMovementControllerTestUtil.DEFAULT_INTERVAL.getFrom().toInstant());
        Assertions.assertThat(run.getEndDateTime()).isEqualTo(PutMovementControllerTestUtil.DEFAULT_INTERVAL.getTo().toInstant());
        Assertions.assertThat(run.getDeliveryServiceId()).isEqualTo(RunTemplateGenerator.DELIVERY_SERVICE_ID);
        Assertions.assertThat(run.getCompany().getId()).isEqualTo(company.getId());
    }

    private void performCreateRun() {
        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null)
                )
        ));

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_RUN_FROM_TEMPLATE, 1);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.CREATE_RUN_FROM_TEMPLATE);
    }
}
