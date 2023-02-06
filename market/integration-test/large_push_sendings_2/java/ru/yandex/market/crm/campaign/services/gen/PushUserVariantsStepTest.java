package ru.yandex.market.crm.campaign.services.gen;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.crm.campaign.domain.sending.PushPeriodicSending;
import ru.yandex.market.crm.campaign.gen.context.HasVariants;
import ru.yandex.market.crm.campaign.services.sending.GenerateSendingTaskData;
import ru.yandex.market.crm.campaign.services.sending.context.PushPeriodicSendingContext;
import ru.yandex.market.crm.campaign.services.sending.periodic.PushPeriodicSendingYtPathsFactory;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ClusterTasksTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PromoSendingsTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushPeriodicSendingTestHelper;
import ru.yandex.market.crm.campaign.yql.ExecuteYqlTaskData;
import ru.yandex.market.crm.core.services.control.GlobalControlSaltProvider;
import ru.yandex.market.crm.core.services.control.LocalControlSaltModifier;
import ru.yandex.market.crm.core.services.jackson.JacksonConfig;
import ru.yandex.market.crm.core.services.mobile.MobileApplicationDAO;
import ru.yandex.market.crm.core.suppliers.TestSubscriptionsTypesSupplier;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.MobilePlatform;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.tasks.domain.Task;
import ru.yandex.market.crm.tasks.services.ClusterTasksService;
import ru.yandex.market.crm.tasks.test.ClusterTasksServiceFactory;
import ru.yandex.market.crm.tasks.test.ClusterTasksServiceTestConfig;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static ru.yandex.market.crm.util.Dates.MOSCOW_ZONE;

@ContextConfiguration(classes = {ClusterTasksServiceTestConfig.class, JacksonConfig.class})
class PushUserVariantsStepTest extends AbstractServiceLargeTest {

    @Inject
    private YtClient ytClient;
    @Inject
    private PushUserVariantsStep pushUserVariantsStep;
    @Inject
    private PushPeriodicSendingYtPathsFactory pushPeriodicSendingYtPathsFactory;
    @Inject
    private UniformGlobalSplittingDescription globalSplittingDescription;
    @Inject
    private GlobalControlSaltProvider globalControlSaltProvider;
    @Inject
    private PushPeriodicSendingTestHelper pushPeriodicSendingTestHelper;
    @Inject
    private ClusterTasksServiceFactory clusterTasksServiceFactory;
    @Inject
    private ClusterTasksTestHelper clusterTasksTestHelper;
    @Inject
    private MobileApplicationDAO mobileApplicationDAO;
    @Inject
    private PromoSendingsTestHelper promoSendingsTestHelper;

    private ClusterTasksService clusterTasksService;

    @AfterEach
    public void tearDown() throws InterruptedException {
        LocalControlSaltModifier.setClock(Clock.system(MOSCOW_ZONE));
        if (clusterTasksService != null) {
            clusterTasksService.stop();
        }
    }

    /**
     * Во время генерации регулярной Push-рассылки при выделении локального контроля группа пользователей,
     * попадающих в ЛК должна меняться каждый месяц
     */
    @Test
    public void testGenerateDifferentLocalControlInDifferentMonths() throws Exception {
        var groupSize = 1000;
        var ids = IntStream.rangeClosed(1, groupSize)
                .mapToObj(i -> "user-uuid-" + i)
                .sorted()
                .map(uuid -> new PromoSendingsTestHelper.DeviceId(uuid, uuid, MobilePlatform.ANDROID.name()))
                .collect(Collectors.toList());

        var sending = generatePeriodicSending();
        var sendingContext = generatePeriodicSendingContext(sending);

        promoSendingsTestHelper.prepareUserDeviceIdsTable(ids, sendingContext.getUserIdsTable());

        startTask(pushUserVariantsStep, sendingContext);

        var uuidsInControl1 = ytClient.read(sendingContext.getVariantsTable(), YTableEntryTypes.YSON).stream()
                .filter(r -> r.getString("variant").contains("control"))
                .map(r -> Uid.asUuid(r.getString("id_value")))
                .collect(Collectors.toSet());

        var controlGroupSizeInPercent1 = (double) uuidsInControl1.size() / groupSize * 100.0;
        assertThat(controlGroupSizeInPercent1, allOf(greaterThan(20.0), lessThan(40.0)));

        var clock = Clock.fixed(LocalDateTime.now().plusMonths(1).toInstant(ZoneOffset.UTC), MOSCOW_ZONE);
        LocalControlSaltModifier.setClock(clock);

        startTask(pushUserVariantsStep, sendingContext);

        var uuidsInControl2 = ytClient.read(sendingContext.getVariantsTable(), YTableEntryTypes.YSON).stream()
                .filter(r -> r.getString("variant").contains("control"))
                .map(r -> Uid.asUuid(r.getString("id_value")))
                .collect(Collectors.toSet());

        var controlGroupSizeInPercent2 = (double) uuidsInControl2.size() / groupSize * 100.0;
        assertThat(controlGroupSizeInPercent2, allOf(greaterThan(20.0), lessThan(40.0)));

        assertNotEquals(uuidsInControl1, uuidsInControl2);
    }

    /**
     * Во время генерации регулярной Push-рассылки при выделении локального контроля группа пользователей,
     * попадающих в ЛК не должна меняться в рамках одного месяца
     */
    @Test
    public void testGenerateSimilarLocalControlInOneMonth() throws Exception {
        var groupSize = 1000;
        var ids = IntStream.rangeClosed(1, groupSize)
                .mapToObj(i -> "user-uuid-" + i)
                .sorted()
                .map(uuid -> new PromoSendingsTestHelper.DeviceId(uuid, uuid, MobilePlatform.ANDROID.name()))
                .collect(Collectors.toList());

        var sending = generatePeriodicSending();
        var sendingContext = generatePeriodicSendingContext(sending);

        promoSendingsTestHelper.prepareUserDeviceIdsTable(ids, sendingContext.getUserIdsTable());

        var clock = Clock.fixed(LocalDateTime.now().withDayOfMonth(1).toInstant(ZoneOffset.UTC), MOSCOW_ZONE);
        LocalControlSaltModifier.setClock(clock);

        startTask(pushUserVariantsStep, sendingContext);

        var uuidsInControl1 = ytClient.read(sendingContext.getVariantsTable(), YTableEntryTypes.YSON).stream()
                .filter(r -> r.getString("variant").contains("control"))
                .map(r -> Uid.asUuid(r.getString("id_value")))
                .collect(Collectors.toSet());

        var controlGroupSizeInPercent1 = (double) uuidsInControl1.size() / groupSize * 100.0;
        assertThat(controlGroupSizeInPercent1, allOf(greaterThan(20.0), lessThan(40.0)));

        clock = Clock.fixed(LocalDateTime.now(clock).plusDays(1).toInstant(ZoneOffset.UTC), MOSCOW_ZONE);
        LocalControlSaltModifier.setClock(clock);

        startTask(pushUserVariantsStep, sendingContext);

        var uuidsInControl2 = ytClient.read(sendingContext.getVariantsTable(), YTableEntryTypes.YSON).stream()
                .filter(r -> r.getString("variant").contains("control"))
                .map(r -> Uid.asUuid(r.getString("id_value")))
                .collect(Collectors.toSet());

        var controlGroupSizeInPercent2 = (double) uuidsInControl2.size() / groupSize * 100.0;
        assertThat(controlGroupSizeInPercent2, allOf(greaterThan(20.0), lessThan(40.0)));

        assertEquals(uuidsInControl1, uuidsInControl2);
    }

    private PushPeriodicSending generatePeriodicSending() {
        var variant = PushPeriodicSendingTestHelper.variant(70);
        var config = pushPeriodicSendingTestHelper.prepareConfig();
        config.setVariants(List.of(variant));

        return pushPeriodicSendingTestHelper.prepareSending(config);
    }

    private PushPeriodicSendingContext generatePeriodicSendingContext(PushPeriodicSending sending) {
        return new PushPeriodicSendingContext(
                sending,
                List.of(),
                new GenerateSendingTaskData(),
                globalSplittingDescription,
                pushPeriodicSendingYtPathsFactory.create(sending),
                mobileApplicationDAO,
                globalControlSaltProvider.getSalt(),
                new TestSubscriptionsTypesSupplier()
        );
    }

    private void startTask(PushUserVariantsStep userVariantsStep, HasVariants context) throws InterruptedException {
        if (clusterTasksService != null) {
            clusterTasksService.stop();
        }

        Task<Void, ExecuteYqlTaskData> task = new ClusterTasksTestHelper.StepWrapper<>(userVariantsStep, context);
        clusterTasksService = clusterTasksServiceFactory.create(List.of(task));

        clusterTasksService.start();
        long taskId = clusterTasksService.submitTask(userVariantsStep.getId(), null);
        clusterTasksTestHelper.waitCompleted(taskId, Duration.ofMinutes(5));
    }
}
