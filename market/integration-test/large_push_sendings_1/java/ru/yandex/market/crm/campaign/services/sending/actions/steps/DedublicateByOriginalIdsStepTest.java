package ru.yandex.market.crm.campaign.services.sending.actions.steps;

import java.time.Duration;
import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.campaign.domain.actions.steps.SendPushesStep;
import ru.yandex.market.crm.campaign.services.actions.ActionYtPaths;
import ru.yandex.market.crm.campaign.services.actions.contexts.SendPushesStepContext;
import ru.yandex.market.crm.campaign.services.actions.steps.DedublicateByOriginalIdsStep;
import ru.yandex.market.crm.campaign.services.actions.tasks.ActionExecutionContext;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ClusterTasksTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PromoSendingsTestHelper;
import ru.yandex.market.crm.campaign.yql.ExecuteYqlTaskData;
import ru.yandex.market.crm.core.util.MobileAppInfoUtil;
import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.Task;
import ru.yandex.market.crm.tasks.services.ClusterTasksService;
import ru.yandex.market.crm.tasks.test.ClusterTasksServiceFactory;
import ru.yandex.market.crm.tasks.test.ClusterTasksServiceTestConfig;
import ru.yandex.market.crm.yql.YqlTemplateService;
import ru.yandex.market.crm.yql.client.YqlClient;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author zloddey
 */
@ContextConfiguration(classes = {ClusterTasksServiceTestConfig.class})
public class DedublicateByOriginalIdsStepTest extends AbstractServiceLargeTest {

    private DedublicateByOriginalIdsStep step;
    private SendPushesStepContext sendingContext;
    private ClusterTasksService clusterTasksService;

    @Inject
    private YqlClient yqlClient;
    @Inject
    private YqlTemplateService yqlTemplateService;
    @Inject
    private ClusterTasksServiceFactory clusterTasksServiceFactory;
    @Inject
    private ClusterTasksTestHelper clusterTasksTestHelper;
    @Inject
    private PromoSendingsTestHelper promoSendingsTestHelper;

    @BeforeEach
    public void setUp() {
        step = new DedublicateByOriginalIdsStep(yqlClient, yqlTemplateService);

        SendPushesStep sendStep = new SendPushesStep();
        sendStep.setId(RandomStringUtils.randomAlphabetic(10));

        ActionExecutionContext parentContext = new ActionExecutionContext(
                null,
                sendStep,
                null,
                null,
                null,
                null,
                new ActionYtPaths(YPath.cypressRoot()),
                null
        );

        sendingContext = new SendPushesStepContext(parentContext, null, List.of(), null, null);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        if (clusterTasksService != null) {
            clusterTasksService.stop();
        }
    }

    @Test
    public void shouldRetainOnlyOneDeviceIdRowForEqualOriginalDeviceIds() {
        var ids = List.of(
                new PromoSendingsTestHelper.DeviceId("uuid_11", "uuid_1", MobileAppInfoUtil.APP_INFO_PLATFORM_ANDROID),
                new PromoSendingsTestHelper.DeviceId("uuid_12", "uuid_1", MobileAppInfoUtil.APP_INFO_PLATFORM_IPHONE),
                new PromoSendingsTestHelper.DeviceId("uuid_2", "uuid_2", MobileAppInfoUtil.APP_INFO_PLATFORM_IPHONE)
        );
        promoSendingsTestHelper.prepareUserDeviceIdsTable(ids, sendingContext.getDeviceIdsTable());

        startTask();

        List<YTreeMapNode> rows = ytClient.read(sendingContext.getDeviceIdsTable(), YTreeMapNode.class);

        MatcherAssert.assertThat(rows.size(), equalTo(2));

        MatcherAssert.assertThat(rows.get(0).getString("id_value"), equalTo("uuid_11"));
        MatcherAssert.assertThat(rows.get(0).getString("platform"),
                equalTo(MobileAppInfoUtil.APP_INFO_PLATFORM_ANDROID));

        MatcherAssert.assertThat(rows.get(1).getString("id_value"), equalTo("uuid_2"));
        MatcherAssert.assertThat(rows.get(1).getString("platform"),
                equalTo(MobileAppInfoUtil.APP_INFO_PLATFORM_IPHONE));
    }

    private void startTask() {
        clusterTasksService = clusterTasksServiceFactory.create(List.of(new StepWrapper(step)));
        clusterTasksService.start();

        long taskId = clusterTasksService.submitTask(step.getId(), null);
        clusterTasksTestHelper.waitCompleted(taskId, Duration.ofMinutes(5));
    }

    private class StepWrapper implements Task<Void, ExecuteYqlTaskData> {
        private final DedublicateByOriginalIdsStep wrapped;

        private StepWrapper(DedublicateByOriginalIdsStep wrapped) {
            this.wrapped = wrapped;
        }

        @Nonnull
        @Override
        public ExecutionResult run(Void context,
                                   ExecuteYqlTaskData data,
                                   Control<ExecuteYqlTaskData> control) throws Exception {
            return wrapped.run(sendingContext, data, control);
        }

        @Override
        public String getId() {
            return wrapped.getId();
        }
    }
}
