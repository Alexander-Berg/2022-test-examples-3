package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplate;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

@Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PutMovementShouldNotAutoConfirmTest extends BasePlannerWebTest {

    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final RunTemplateGenerator runTemplateGenerator;
    private final PutMovementHelper putMovementHelper;
    private final TestUserHelper testUserHelper;
    private final RunRepository runRepository;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.RUN_TEMPLATE_ENABLED, true);
    }

    @Test
    void shouldNotAutoConfirmIfItIsNotExplicitlyEnabled() {
        testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(RunTemplateGenerator.CAMPAIGN_ID)
                .build());

        RunTemplate runTemplate = runTemplateGenerator.generate();

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(new ResourceId("TMM123", null)))
        );

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_RUN_FROM_TEMPLATE, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.CREATE_RUN_FROM_TEMPLATE);

        List<Run> runs = runRepository.findAll();
        Assertions.assertThat(runs).hasSize(1);
        Assertions.assertThat(runs.get(0).getStatus()).isEqualTo(RunStatus.CREATED);

    }
}
