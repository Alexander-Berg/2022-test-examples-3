package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PutMovementAddCollectDropshipToActiveTaskTest extends BasePlannerWebTest {

    private final RunCommandService runCommandService;
    private final RunHelper runHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private final RunTemplateGenerator runTemplateGenerator;
    private final PutMovementHelper putMovementHelper;
    private final TestUserHelper testUserHelper;
    private final DbQueueTestUtil dbQueueTestUtil;

    private final RunRepository runRepository;
    private final UserShiftRepository userShiftRepository;

    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    @Test
    void shouldAddNewCollectDropshipTaskIfUserShiftIsAlreadyCreated() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.RUN_TEMPLATE_ENABLED, true);

        var company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(RunTemplateGenerator.CAMPAIGN_ID)
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .build());
        var user = testUserHelper.findOrCreateUser(UID);
        var transport = testUserHelper.findOrCreateTransport();

        runTemplateGenerator.generate();
        // create run

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null)
                )
        ));

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_RUN_FROM_TEMPLATE, 1);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.CREATE_RUN_FROM_TEMPLATE);

        List<Run> runs = runRepository.findAll();
        Assertions.assertThat(runs).hasSize(1);

        Run run = runs.get(0);

        // assign run to user shift
        var userShift = runHelper.assignUserAndTransport(run, user, transport);

        // push new movement

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM2", null),
                        new ResourceId("30", "1234"),
                        new ResourceId("300", "1234")
                )
        ));

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_RUN_FROM_TEMPLATE, 3 + 1);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.CREATE_RUN_FROM_TEMPLATE);

        transactionTemplate.execute(tc -> {
            Run run2 = runRepository.findByIdOrThrow(run.getId());
            Assertions.assertThat(run2.streamRunItems().toList())
                    .allMatch(ri -> ri.getCollectTaskId() != null && ri.getReturnTaskId() != null);

            // check if collect dropship is created
            var userShift2 = userShiftRepository.findByIdOrThrow(userShift.getId());

            List<CollectDropshipTask> tasks = userShift2.streamCollectDropshipTasks().toList();
            Assertions.assertThat(tasks).hasSize(2);
            return null;
        });
    }
}
