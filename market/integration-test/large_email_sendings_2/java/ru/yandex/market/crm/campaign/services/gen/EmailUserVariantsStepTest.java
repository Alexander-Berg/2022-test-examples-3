package ru.yandex.market.crm.campaign.services.gen;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.crm.campaign.domain.sending.EmailPeriodicSending;
import ru.yandex.market.crm.campaign.gen.context.HasVariants;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.GenerateSendingTaskData;
import ru.yandex.market.crm.campaign.services.sending.context.EmailPeriodicSendingContext;
import ru.yandex.market.crm.campaign.services.sending.periodic.EmailPeriodicSendingYtPathsFactory;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.BlockTemplateTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ClusterTasksTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailPeriodicSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PromoSendingsTestHelper;
import ru.yandex.market.crm.campaign.yql.ExecuteYqlTaskData;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType.Channel;
import ru.yandex.market.crm.core.services.control.GlobalControlSaltProvider;
import ru.yandex.market.crm.core.services.control.LocalControlSaltModifier;
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
import static ru.yandex.market.crm.campaign.test.utils.EmailPeriodicSendingTestHelper.DEFAULT_SUBJECT;
import static ru.yandex.market.crm.campaign.test.utils.EmailPeriodicSendingTestHelper.DEFAULT_VARIANT;
import static ru.yandex.market.crm.campaign.test.utils.EmailSendingConfigUtils.variant;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscriptionFilter;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.ADVERTISING;
import static ru.yandex.market.crm.util.Dates.MOSCOW_ZONE;

@ContextConfiguration(classes = {ClusterTasksServiceTestConfig.class})
class EmailUserVariantsStepTest extends AbstractServiceLargeTest {

    @Inject
    private YtClient ytClient;
    @Inject
    private UserVariantsStep userVariantsStep;
    @Inject
    private EmailPeriodicSendingYtPathsFactory emailPeriodicSendingYtPathsFactory;
    @Inject
    private UniformGlobalSplittingDescription globalSplittingDescription;
    @Inject
    private GlobalControlSaltProvider globalControlSaltProvider;
    @Inject
    private EmailPeriodicSendingTestHelper emailPeriodicSendingTestHelper;
    @Inject
    private SegmentService segmentService;
    @Inject
    private BlockTemplateTestHelper blockTemplateTestHelper;
    @Inject
    private ClusterTasksServiceFactory clusterTasksServiceFactory;
    @Inject
    private ClusterTasksTestHelper clusterTasksTestHelper;
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
     * Во время генерации регулярной Email-рассылки при выделении локального контроля группа пользователей,
     * попадающих в ЛК должна меняться каждый месяц
     */
    @Test
    public void testGenerateDifferentLocalControlInDifferentMonths() throws Exception {
        var groupSize = 1000;
        var emails = IntStream.rangeClosed(1, groupSize)
                .mapToObj(i -> "user-" + i + "@yandex.ru")
                .sorted()
                .map(Uid::asEmail)
                .collect(Collectors.toList());

        var sending = generatePeriodicSending();
        var sendingContext = generatePeriodicSendingContext(sending);

        promoSendingsTestHelper.prepareUserIdsTable(emails, sendingContext.getUserIdsTable());

        startTask(userVariantsStep, sendingContext);

        var emailsInControl1 = ytClient.read(sendingContext.getVariantsTable(), YTableEntryTypes.YSON).stream()
                .filter(r -> r.getString("variant").contains("control"))
                .map(r -> Uid.asEmail(r.getString("id_value")))
                .collect(Collectors.toSet());

        var controlGroupSizeInPercent1 = (double) emailsInControl1.size() / groupSize * 100.0;
        assertThat(controlGroupSizeInPercent1, allOf(greaterThan(20.0), lessThan(40.0)));

        var clock = Clock.fixed(LocalDateTime.now().plusMonths(1).toInstant(ZoneOffset.UTC), MOSCOW_ZONE);
        LocalControlSaltModifier.setClock(clock);

        startTask(userVariantsStep, sendingContext);

        var emailsInControl2 = ytClient.read(sendingContext.getVariantsTable(), YTableEntryTypes.YSON).stream()
                .filter(r -> r.getString("variant").contains("control"))
                .map(r -> Uid.asEmail(r.getString("id_value")))
                .collect(Collectors.toSet());

        var controlGroupSizeInPercent2 = (double) emailsInControl2.size() / groupSize * 100.0;
        assertThat(controlGroupSizeInPercent2, allOf(greaterThan(20.0), lessThan(40.0)));

        assertNotEquals(emailsInControl1, emailsInControl2);
    }

    /**
     * Во время генерации регулярной Email-рассылки при выделении локального контроля группа пользователей,
     * попадающих в ЛК не должна меняться в рамках одного месяца
     */
    @Test
    public void testGenerateSimilarLocalControlInOneMonth() throws Exception {
        var groupSize = 1000;
        var emails = IntStream.rangeClosed(1, groupSize)
                .mapToObj(i -> "user-" + i + "@yandex.ru")
                .sorted()
                .map(Uid::asEmail)
                .collect(Collectors.toList());

        var sending = generatePeriodicSending();
        var sendingContext = generatePeriodicSendingContext(sending);

        promoSendingsTestHelper.prepareUserIdsTable(emails, sendingContext.getUserIdsTable());

        var clock = Clock.fixed(LocalDateTime.now().withDayOfMonth(1).toInstant(ZoneOffset.UTC), MOSCOW_ZONE);
        LocalControlSaltModifier.setClock(clock);

        startTask(userVariantsStep, sendingContext);

        var emailsInControl1 = ytClient.read(sendingContext.getVariantsTable(), YTableEntryTypes.YSON).stream()
                .filter(r -> r.getString("variant").contains("control"))
                .map(r -> Uid.asEmail(r.getString("id_value")))
                .collect(Collectors.toSet());

        var controlGroupSizeInPercent1 = (double) emailsInControl1.size() / groupSize * 100.0;
        assertThat(controlGroupSizeInPercent1, allOf(greaterThan(20.0), lessThan(40.0)));

        clock = Clock.fixed(LocalDateTime.now(clock).plusDays(1).toInstant(ZoneOffset.UTC), MOSCOW_ZONE);
        LocalControlSaltModifier.setClock(clock);

        startTask(userVariantsStep, sendingContext);

        var emailsInControl2 = ytClient.read(sendingContext.getVariantsTable(), YTableEntryTypes.YSON).stream()
                .filter(r -> r.getString("variant").contains("control"))
                .map(r -> Uid.asEmail(r.getString("id_value")))
                .collect(Collectors.toSet());

        var controlGroupSizeInPercent2 = (double) emailsInControl2.size() / groupSize * 100.0;
        assertThat(controlGroupSizeInPercent2, allOf(greaterThan(20.0), lessThan(40.0)));

        assertEquals(emailsInControl1, emailsInControl2);
    }

    private EmailPeriodicSending generatePeriodicSending() {
        var campaign = emailPeriodicSendingTestHelper.prepareCampaign();

        var segment = segmentService.addSegment(segment(
                subscriptionFilter(ADVERTISING)
        ));

        var variant = variant(
                DEFAULT_VARIANT,
                70,
                blockTemplateTestHelper.prepareMessageTemplate(),
                DEFAULT_SUBJECT,
                blockTemplateTestHelper.prepareCreativeBlock()
        );

        return emailPeriodicSendingTestHelper.prepareSending(
                campaign,
                segment,
                emailSending -> emailSending.getConfig().setVariants(List.of(variant))
        );
    }

    private EmailPeriodicSendingContext generatePeriodicSendingContext(EmailPeriodicSending sending) {
        var config = sending.getConfig();
        var subscriptionId = config.getSubscriptionType();

        var subscriptionType = subscriptionId == null ?
                null :
                new SubscriptionType(subscriptionId, "", "", true, false, Set.of(Channel.EMAIL));

        return new EmailPeriodicSendingContext(
                sending,
                config,
                null,
                List.of(),
                new GenerateSendingTaskData(),
                globalSplittingDescription,
                globalControlSaltProvider.getSalt(),
                emailPeriodicSendingYtPathsFactory.create(sending),
                subscriptionType
        );
    }

    private void startTask(UserVariantsStep userVariantsStep, HasVariants context) throws InterruptedException {
        if (clusterTasksService != null) {
            clusterTasksService.stop();
        }

        Task<Void, ExecuteYqlTaskData> task = new ClusterTasksTestHelper.StepWrapper<>(userVariantsStep, context);
        clusterTasksService = clusterTasksServiceFactory.create(List.of(task));

        clusterTasksService.start();
        long taskId = clusterTasksService.submitTask(userVariantsStep.getId(), null);
        clusterTasksTestHelper.waitCompleted(taskId, Duration.ofMinutes(2));
    }
}
