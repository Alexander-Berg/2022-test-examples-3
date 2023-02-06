package ru.yandex.market.tsum.notifications;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.notifications.Notification;
import ru.yandex.market.tsum.clients.notifications.NotificationTarget;
import ru.yandex.market.tsum.clients.notifications.NotificationTargetType;
import ru.yandex.market.tsum.clients.notifications.email.EmailNotification;
import ru.yandex.market.tsum.clients.notifications.email.EmailNotificationTarget;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotification;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotificationTarget;
import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.clients.startrek.StartrekCommentNotification;
import ru.yandex.market.tsum.clients.startrek.StartrekNotificationTarget;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.notify.common.ContextBuilder;
import ru.yandex.market.tsum.core.notify.common.NotificationCenter;
import ru.yandex.market.tsum.dao.PipelinesDao;
import ru.yandex.market.tsum.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsum.multitesting.MultitestingDao;
import ru.yandex.market.tsum.multitesting.MultitestingDatacenterWeightService;
import ru.yandex.market.tsum.multitesting.model.JanitorOptions;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;
import ru.yandex.market.tsum.notifications.events.CommonMultitestingEvents;
import ru.yandex.market.tsum.notifications.events.MultitestingNotificationEvent;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateDao;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateHistoryDao;
import ru.yandex.market.tsum.per_commit.entity.PerCommitBranchStateEntity;
import ru.yandex.market.tsum.per_commit.entity.PerCommitBranchStateHistoryEntity;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.CommonJobEvents;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.CommonPipeEvents;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.JobNotificationEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.JobNotificationEventMeta;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.NotificationEventMeta;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.NotificationLevel;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.PipeNotificationEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.EmailTargetSettings;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.EventNotificationSettings;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.NotificationTargets;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.ProjectNotificationSettings;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.ProjectNotificationSettingsDao;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.StartrekTargetSettings;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.TelegramTargetSettings;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunchRefImpl;
import ru.yandex.market.tsum.release.RepositoryType;
import ru.yandex.market.tsum.release.dao.ChangelogCommit;
import ru.yandex.market.tsum.release.dao.ChangelogCommitDao;
import ru.yandex.market.tsum.release.dao.Release;
import ru.yandex.market.tsum.release.dao.ReleaseDao;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 14.02.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {NotificatorImplTest.NotificatorImplTestConfiguration.class, TestMongo.class})
public class NotificatorImplTest {
    private static final String PROJECT_ID = "testProject";
    private static final String PIPELINE_ID = "testPipeline";
    private static final String MT_PIPELINE_ID = "testPipeline";
    private static final String PER_COMMIT_PIPELINE_ID = "testPipeline";
    private static final String JOB_ID = "testJobId";

    private static final PipeLaunch PIPE_LAUNCH = PipeLaunch.builder()
        .withLaunchRef(PipeLaunchRefImpl.create(PIPELINE_ID))
        .withTriggeredBy("someUser")
        .withCreatedDate(new Date())
        .withStages(Collections.emptyList())
        .withProjectId(PROJECT_ID)
        .build();

    private static final PipeLaunch MT_PIPE_LAUNCH = PipeLaunch.builder()
        .withLaunchRef(PipeLaunchRefImpl.create(MT_PIPELINE_ID))
        .withTriggeredBy("someUser")
        .withCreatedDate(new Date())
        .withStages(Collections.emptyList())
        .withProjectId(PROJECT_ID)
        .build();

    private static final PipeLaunch PER_COMMIT_PIPE_LAUNCH = PipeLaunch.builder()
        .withLaunchRef(PipeLaunchRefImpl.create(PER_COMMIT_PIPELINE_ID))
        .withTriggeredBy("someUser")
        .withCreatedDate(new Date())
        .withStages(Collections.emptyList())
        .withProjectId(PROJECT_ID)
        .build();

    private static final PipeLaunch PER_COMMIT_OLD_PIPE_LAUNCH = PipeLaunch.builder()
        .withLaunchRef(PipeLaunchRefImpl.create(PER_COMMIT_PIPELINE_ID))
        .withTriggeredBy("someUser")
        .withCreatedDate(new Date())
        .withStages(Collections.emptyList())
        .withProjectId(PROJECT_ID)
        .build();

    private static final String NOT_CUSTOMIZED_PIPE_ID = "testPipeline2";
    private static final String NOT_CUSTOMIZED_PROJECT = "testProject2";
    private static final String PROJECT_ID_WITH_EMAIL_SUBJECT_TEMPLATE = "testProject3";
    private static final String PIPE_ID_WITH_EMAIL_SUBJECT_TEMPLATE = "testPipeline3";

    private static final PipeLaunch PIPE_LAUNCH_OF_NOT_CUSTOMIZED_PROJECT = PipeLaunch.builder()
        .withLaunchRef(PipeLaunchRefImpl.create(NOT_CUSTOMIZED_PIPE_ID))
        .withTriggeredBy("someUser")
        .withCreatedDate(new Date())
        .withStages(Collections.emptyList())
        .withProjectId(PROJECT_ID)
        .build();

    private static final PipeLaunch PIPE_LAUNCH_WITH_EMAIL_SUBJECT_TEMPLATE = PipeLaunch.builder()
        .withLaunchRef(PipeLaunchRefImpl.create(PIPE_ID_WITH_EMAIL_SUBJECT_TEMPLATE))
        .withTriggeredBy("someUser")
        .withCreatedDate(new Date())
        .withStages(Collections.emptyList())
        .withProjectId(PROJECT_ID_WITH_EMAIL_SUBJECT_TEMPLATE)
        .build();

    private static final JobNotificationEventMeta CUSTOM_JOB_EVENT = new JobNotificationEventMeta(
        "CONDUCTOR_STATUS_CHANGED", "Статус тикета в кондукторе изменился",
        NotificationEventMeta.DefaultMessages.newBuilder().withCommonDefault("Обычный текст: {{jobTitle}}").build(),
        "", new NotificationEventMeta.Argument("jobTitle", "Тайтл джобы")
    );

    private static final EventNotificationSettings NOTIFY_TELEGRAM_AND_STARTREK = new EventNotificationSettings(
        NotificationLevel.INFO,
        new TelegramTargetSettings("Кастомный текст. Тайтл джобы: {{jobTitle}}", new TelegramNotificationTarget(
            "180349965")),
        null,
        new StartrekTargetSettings(true)
    );

    private static final EventNotificationSettings NOTIFY_ALL = new EventNotificationSettings(
        NotificationLevel.INFO,
        new TelegramTargetSettings("Кастомный текст. Тайтл джобы: {{jobTitle}}", new TelegramNotificationTarget(
            "180349965")),
        new EmailTargetSettings(new EmailNotificationTarget("sid-hugo@yandex-team.ru")),
        new StartrekTargetSettings(true)
    );

    private static final EventNotificationSettings NOTIFY_EMAIL_WITH_SUBJECT_TEMPLATE = new EventNotificationSettings(
        NotificationLevel.INFO,
        null,
        new EmailTargetSettings("", Collections.singleton(new EmailNotificationTarget("sid-hugo@yandex-team.ru")),
            "pipe: {{pipelineId}}"),
        null
    );

    private static final EventNotificationSettings NEED_ATTENTION = new EventNotificationSettings(
        NotificationLevel.NEED_ATTENTION,
        new TelegramTargetSettings(),
        new EmailTargetSettings(),
        new StartrekTargetSettings()
    );

    private static final String CURRENT_LAUNCH_TELEGRAM = "123";
    private static final String CURRENT_LAUNCH_EMAIL = "current@launch.ru";
    private static final String CURRENT_LAUNCH_STARTREK = "CURRENTLAUNCH-42";

    private static final String PROJECT_EVENT_TARGET_TELEGRAM = "234";
    private static final String PROJECT_EVENT_TARGET_EMAIL = "projectEvent@target.ru";
    private static final String PROJECT_LEVEL_TARGET_TELEGRAM = "345";
    private static final String PROJECT_LEVEL_TARGET_EMAIL = "projectLevel@target.ru";

    @Autowired
    private NotificatorImpl notificator;

    @Autowired
    private NotificationCenter notificationCenter;

    private final Map<String, Object> context = ContextBuilder.create()
        .with("jobTitle", "Тайтл Джобы")
        .with("pipelineId", PIPELINE_ID)
        .build();

    @Captor
    ArgumentCaptor<Notification> notificationCaptor;
    @Captor
    ArgumentCaptor<Set<NotificationTarget>> targetsCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.reset(notificationCenter);
    }

    @Test
    public void notifyJobEvent() throws Exception {
        notificator.notifyAboutEvent(
            new JobNotificationEvent(CommonJobEvents.JOB_STARTED, PIPE_LAUNCH, JOB_ID),
            context
        );
        verifyNotifyOnlyTelegram(CommonJobEvents.JOB_STARTED);
    }

    @Test
    public void notifyCustomJobEvent() throws Exception {
        notificator.notifyAboutEvent(new JobNotificationEvent(CUSTOM_JOB_EVENT, PIPE_LAUNCH, JOB_ID), context);

        checkPerLaunchNotifications(true);

        Set<NotificationTarget> sentToTargets = targetsCaptor.getAllValues().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        Assert.assertEquals(
            new HashSet<>(Arrays.asList(
                new EmailNotificationTarget(CURRENT_LAUNCH_EMAIL),
                new StartrekNotificationTarget(CURRENT_LAUNCH_STARTREK),
                new TelegramNotificationTarget(CURRENT_LAUNCH_TELEGRAM),
                NOTIFY_TELEGRAM_AND_STARTREK.getTelegramSettings().getTargets().iterator().next()
            )),
            sentToTargets
        );
    }

    @Test
    public void notifyPipeEvent() throws Exception {
        notificator.notifyAboutEvent(
            new PipeNotificationEvent(CommonPipeEvents.PIPELINE_FAILED, PIPE_LAUNCH), context
        );
        verifyNotifyAll(CommonPipeEvents.PIPELINE_FAILED);
    }

    @Test
    public void notifyMultitestingEvent() throws Exception {
        notificator.notifyAboutEvent(
            new MultitestingNotificationEvent(CommonMultitestingEvents.ENVIRONMENT_FAILED, PIPE_LAUNCH), context
        );
        verifyNotifyAll(CommonMultitestingEvents.ENVIRONMENT_FAILED);
    }

    @Test
    public void notifyExistingReleaseNotExistingProjectSettings() {
        notificator.notifyAboutEvent(
            new JobNotificationEvent(CommonJobEvents.JOB_STARTED, PIPE_LAUNCH_OF_NOT_CUSTOMIZED_PROJECT, JOB_ID),
            context
        );
        Mockito.verify(notificationCenter, Mockito.never()).notify(Mockito.any(), Mockito.anyList());

        notificator.notifyAboutEvent(
            new PipeNotificationEvent(CommonPipeEvents.PIPELINE_STARTED, PIPE_LAUNCH_OF_NOT_CUSTOMIZED_PROJECT),
            context
        );
        Mockito.verify(notificationCenter, Mockito.never()).notify(Mockito.any(), Mockito.anyList());

        notificator.notifyAboutEvent(
            new PipeNotificationEvent(CommonPipeEvents.PIPELINE_SUCCEEDED, PIPE_LAUNCH_OF_NOT_CUSTOMIZED_PROJECT),
            context
        );
        checkPerLaunchNotifications(false);

        Set<String> sentToTargets = targetsCaptor.getAllValues().stream()
            .flatMap(Collection::stream)
            .map(NotificationTarget::getTarget)
            .collect(Collectors.toSet());
        Assert.assertEquals(
            new HashSet<>(Arrays.asList(
                CURRENT_LAUNCH_EMAIL, CURRENT_LAUNCH_TELEGRAM
            )),
            sentToTargets
        );
    }

    private void checkPerLaunchNotifications(boolean checkStartrek) {
        int times = checkStartrek ? 3 : 2;
        Mockito.verify(notificationCenter, Mockito.times(times))
            .notify(notificationCaptor.capture(), targetsCaptor.capture());

        Assert.assertTrue(notificationCaptor.getAllValues().size() == times);
        boolean hasTelegram = false;
        boolean hasStartrek = false;
        boolean hasEmail = false;
        for (Notification notification : notificationCaptor.getAllValues()) {
            if (notification instanceof TelegramNotification) {
                hasTelegram = true;
            } else if (notification instanceof StartrekCommentNotification) {
                hasStartrek = true;
            } else if (notification instanceof EmailNotification) {
                hasEmail = true;
            }
        }
        Assert.assertTrue(hasTelegram && (!checkStartrek || hasStartrek) && hasEmail);
    }

    @Test
    public void notifyExistingReleaseNotExistingJobSettings() throws Exception {
        notificator.notifyAboutEvent(
            new JobNotificationEvent(CommonJobEvents.JOB_NEEDS_MANUAL_TRIGGER, PIPE_LAUNCH, JOB_ID),
            context
        );
        Mockito.verify(notificationCenter, Mockito.never()).notify(Mockito.any(), Mockito.anyList());

        notificator.notifyAboutEvent(
            new PipeNotificationEvent(CommonPipeEvents.PIPELINE_NEEDS_MANUAL_TRIGGER, PIPE_LAUNCH),
            context
        );
        Mockito.verify(notificationCenter, Mockito.never()).notify(Mockito.any(), Mockito.anyList());
    }

    @Test
    public void notifyExistingEnvironmentNotExistingProjectSettings() throws Exception {
        notificator.notifyAboutEvent(
            new MultitestingNotificationEvent(CommonMultitestingEvents.ENVIRONMENT_READY,
                PIPE_LAUNCH_OF_NOT_CUSTOMIZED_PROJECT),
            context
        );
        Mockito.verify(notificationCenter, Mockito.never()).notify(Mockito.any(), Mockito.anyList());
    }

    @Test
    public void notifyExistingEnvironmentNotExistingJobSettings() throws Exception {
        notificator.notifyAboutEvent(
            new MultitestingNotificationEvent(CommonMultitestingEvents.ENVIRONMENT_READY, PIPE_LAUNCH),
            context
        );
        Mockito.verify(notificationCenter, Mockito.never()).notify(Mockito.any(), Mockito.anyList());
    }

    @Test
    public void notifyProjectTargets() {
        notificator.notifyAboutEvent(new JobNotificationEvent(CommonJobEvents.JOB_FAILED, PIPE_LAUNCH, JOB_ID));

        Mockito.verify(notificationCenter, Mockito.times(2)).notify(notificationCaptor.capture(),
            targetsCaptor.capture());

        Set<NotificationTarget> sentToTargets = targetsCaptor.getAllValues().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        Assert.assertEquals(
            new HashSet<>(Arrays.asList(
                new EmailNotificationTarget(PROJECT_EVENT_TARGET_EMAIL),
                new TelegramNotificationTarget(PROJECT_EVENT_TARGET_TELEGRAM),
                new EmailNotificationTarget(PROJECT_LEVEL_TARGET_EMAIL),
                new TelegramNotificationTarget(PROJECT_LEVEL_TARGET_TELEGRAM)
            )),
            sentToTargets
        );
    }

    @Test
    public void createMessage() {
        String customMessageBody = "custom message ";
        String defaultMessgeBody = "default message ";
        String customMessage = customMessageBody + "{{param1}}";
        String defaultMessage = defaultMessgeBody + "{{param1}} {{param2}}";
        String param1 = "first param";
        String param2 = "second param";

        Assert.assertEquals(
            defaultMessgeBody + " ",
            notificator.createMessage(null, defaultMessage, ContextBuilder.create().build())
        );
        Assert.assertEquals(
            customMessageBody,
            notificator.createMessage(
                new TelegramTargetSettings(customMessage, new TelegramNotificationTarget("unused target")),
                defaultMessage,
                null
            )
        );
        Assert.assertEquals(
            defaultMessgeBody + param1 + " " + param2,
            notificator.createMessage(null, defaultMessage, ContextBuilder.create().with("param1", param1).with(
                "param2", param2).build())
        );
        Assert.assertEquals(
            customMessageBody + param1,
            notificator.createMessage(
                new EmailTargetSettings(customMessage, new EmailNotificationTarget("unused target")),
                defaultMessage,
                ContextBuilder.create().with("param1", param1).with("param2", param2).build()
            )
        );
    }

    @Test
    public void subjectTemplate() {
        notificator.notifyAboutEvent(
            new PipeNotificationEvent(CommonPipeEvents.PIPELINE_STARTED, PIPE_LAUNCH_WITH_EMAIL_SUBJECT_TEMPLATE),
            context
        );

        Mockito.verify(notificationCenter, Mockito.only()).notify(notificationCaptor.capture(),
            targetsCaptor.capture());

        EmailNotification emailNotification = (EmailNotification) notificationCaptor.getValue();

        Assert.assertEquals(
            "pipe: testPipeline",
            emailNotification.getEmailSubject()
        );
    }

    @Test
    public void notNotifyCommonPipeEventsInMtPipe() {
        notificator.notifyAboutEvent(
            new PipeNotificationEvent(CommonPipeEvents.PIPELINE_FAILED, MT_PIPE_LAUNCH),
            context
        );
        Mockito.verify(notificationCenter, Mockito.never()).notify(Mockito.any(), Mockito.anyList());
    }

    @Test
    public void notifyMtEventsInMtPipe() {
        notificator.notifyAboutEvent(
            new MultitestingNotificationEvent(CommonMultitestingEvents.ENVIRONMENT_FAILED, MT_PIPE_LAUNCH),
            context
        );
        Mockito.verify(notificationCenter, Mockito.times(2)).notify(Mockito.any(), Mockito.anyCollection());
    }

    @Test
    public void notifyJobEventsInMtPipe() {
        notificator.notifyAboutEvent(
            new JobNotificationEvent(CommonJobEvents.JOB_FAILED, MT_PIPE_LAUNCH, JOB_ID),
            context
        );
        Mockito.verify(notificationCenter, Mockito.times(2)).notify(Mockito.any(), Mockito.anyCollection());
    }

    @Test
    public void notNotifyCommonPipeEventsInPerCommitPipe() {
        notificator.notifyAboutEvent(
            new PipeNotificationEvent(CommonPipeEvents.PIPELINE_FAILED, PER_COMMIT_PIPE_LAUNCH),
            context
        );
        Mockito.verify(notificationCenter, Mockito.never()).notify(Mockito.any(), Mockito.anyList());
    }

    @Test
    public void notifyJobEventInPerCommitPipe() {
        notificator.notifyAboutEvent(
            new JobNotificationEvent(CommonJobEvents.JOB_FAILED, PER_COMMIT_PIPE_LAUNCH, JOB_ID),
            context
        );
        Mockito.verify(notificationCenter, Mockito.times(2)).notify(Mockito.any(), Mockito.anyCollection());
    }

    @Test
    public void notNotifyCommonPipeEventsInOldPerCommitPipe() {
        notificator.notifyAboutEvent(
            new PipeNotificationEvent(CommonPipeEvents.PIPELINE_FAILED, PER_COMMIT_OLD_PIPE_LAUNCH),
            context
        );
        Mockito.verify(notificationCenter, Mockito.never()).notify(Mockito.any(), Mockito.anyList());
    }

    @Test
    public void notifyJobEventInOldPerCommitPipe() {
        notificator.notifyAboutEvent(
            new JobNotificationEvent(CommonJobEvents.JOB_FAILED, PER_COMMIT_OLD_PIPE_LAUNCH, JOB_ID),
            context
        );
        Mockito.verify(notificationCenter, Mockito.times(2)).notify(Mockito.any(), Mockito.anyCollection());
    }

    private void verifyNotifyOnlyTelegram(NotificationEventMeta event) {
        Mockito.verify(notificationCenter, Mockito.only()).notify(notificationCaptor.capture(),
            targetsCaptor.capture());
        Assert.assertTrue(
            notificationCaptor.getValue() instanceof TelegramNotification
        );
        Assert.assertEquals(
            notificator.createMessage(
                NOTIFY_TELEGRAM_AND_STARTREK.getTelegramSettings(),
                event.getDefaultMessages().getTelegramDefault(), context
            ),
            ((TelegramNotification) notificationCaptor.getValue()).getTelegramMessage()
        );
        Assert.assertEquals(1, targetsCaptor.getValue().size());
        Assert.assertEquals(
            NOTIFY_TELEGRAM_AND_STARTREK.getTelegramSettings().getTargets().iterator().next(),
            targetsCaptor.getValue().iterator().next()
        );
    }

    private void verifyNotifyAll(NotificationEventMeta notificationEventMeta) {
        Mockito.verify(notificationCenter, Mockito.times(2)).notify(notificationCaptor.capture(),
            targetsCaptor.capture());
        TelegramNotification telegramNotification = (TelegramNotification) notificationCaptor.getAllValues().stream()
            .filter(t -> t instanceof TelegramNotification)
            .findFirst().get();
        EmailNotification emailNotification = (EmailNotification) notificationCaptor.getAllValues().stream()
            .filter(t -> t instanceof EmailNotification)
            .findFirst().get();

        Assert.assertEquals(
            notificator.createMessage(
                NOTIFY_ALL.getTelegramSettings(),
                notificationEventMeta.getDefaultMessages().getTelegramDefault(), context
            ),
            telegramNotification.getTelegramMessage()
        );
        Assert.assertEquals(
            notificator.createMessage(
                NOTIFY_ALL.getEmailSettings(),
                notificationEventMeta.getDefaultMessages().getEmailDefault(), context
            ),
            emailNotification.getEmailMessage()
        );
        Assert.assertEquals(2, targetsCaptor.getAllValues().size());
        Set<NotificationTarget> allTargets = new HashSet<>();
        allTargets.addAll(NOTIFY_ALL.getTelegramSettings().getTargets());
        allTargets.addAll(NOTIFY_ALL.getEmailSettings().getTargets());
        Assert.assertEquals(
            allTargets,
            targetsCaptor.getAllValues().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet())
        );
    }

    @Configuration
    static class NotificatorImplTestConfiguration {
        @Autowired
        private MongoTemplate mongoTemplate;

        @Bean
        public NotificationCenter notificationCenter() {
            return mock(NotificationCenter.class);
        }

        @Bean
        public ProjectNotificationSettingsDao projectNotificationSettingsDao() {
            ProjectNotificationSettingsDao projectNotificationSettingsDao =
                new ProjectNotificationSettingsDao(mongoTemplate);
            projectNotificationSettingsDao.setJobEventNotificationSettings(
                PROJECT_ID, PIPELINE_ID, JOB_ID, CommonJobEvents.JOB_STARTED.getId(),
                NOTIFY_TELEGRAM_AND_STARTREK
            );
            projectNotificationSettingsDao.setPipeEventNotificationSettings(
                PROJECT_ID, PIPELINE_ID, CommonPipeEvents.PIPELINE_FAILED.getId(),
                NOTIFY_ALL
            );
            projectNotificationSettingsDao.setPipeEventNotificationSettings(
                PROJECT_ID_WITH_EMAIL_SUBJECT_TEMPLATE, PIPE_ID_WITH_EMAIL_SUBJECT_TEMPLATE,
                CommonPipeEvents.PIPELINE_STARTED.getId(),
                NOTIFY_EMAIL_WITH_SUBJECT_TEMPLATE
            );
            projectNotificationSettingsDao.setMultitestingEventNotificationSettings(
                PROJECT_ID, CommonMultitestingEvents.ENVIRONMENT_FAILED.getId(),
                NOTIFY_ALL
            );
            projectNotificationSettingsDao.setJobEventNotificationSettings(
                PROJECT_ID, PIPELINE_ID, JOB_ID, CUSTOM_JOB_EVENT.getId(),
                NOTIFY_TELEGRAM_AND_STARTREK
            );
            projectNotificationSettingsDao.setJobEventNotificationSettings(
                PROJECT_ID, PIPELINE_ID, JOB_ID, CommonJobEvents.JOB_FAILED.getId(), NEED_ATTENTION
            );
            projectNotificationSettingsDao.setProjectTargetsByEventId(
                PROJECT_ID,
                CommonJobEvents.JOB_FAILED.getId(),
                new NotificationTargets(
                    Collections.singleton(new TelegramNotificationTarget(NotificationTargetType.DIRECT,
                        PROJECT_EVENT_TARGET_TELEGRAM)),
                    Collections.singleton(new EmailNotificationTarget(NotificationTargetType.DIRECT,
                        PROJECT_EVENT_TARGET_EMAIL))
                )
            );
            projectNotificationSettingsDao.setProjectTargetsByEventLevel(
                PROJECT_ID,
                NotificationLevel.NEED_ATTENTION,
                new NotificationTargets(
                    Collections.singleton(new TelegramNotificationTarget(NotificationTargetType.DIRECT,
                        PROJECT_LEVEL_TARGET_TELEGRAM)),
                    Collections.singleton(new EmailNotificationTarget(NotificationTargetType.DIRECT,
                        PROJECT_LEVEL_TARGET_EMAIL))
                )
            );

            ProjectNotificationSettings projectSettings = new ProjectNotificationSettings();
            projectSettings.setProjectId(NOT_CUSTOMIZED_PROJECT);
            projectNotificationSettingsDao.saveProjectNotificationSettings(projectSettings);
            return projectNotificationSettingsDao;
        }

        @Bean
        public PipeLaunchNotificationSettingsDao launchNotificationSettingsDao() {
            PipeLaunchNotificationSettingsDao pipeLaunchNotificationSettingsDao =
                new PipeLaunchNotificationSettingsDao(mongoTemplate);
            pipeLaunchNotificationSettingsDao.addTargets(
                PIPE_LAUNCH.getIdString(), CUSTOM_JOB_EVENT, JOB_ID,
                new NotificationTargets(
                    Collections.emptySet(),
                    Collections.singleton(new EmailNotificationTarget(NotificationTargetType.DIRECT,
                        CURRENT_LAUNCH_EMAIL)),
                    Collections.singleton(new StartrekNotificationTarget(CURRENT_LAUNCH_STARTREK))
                )
            );

            pipeLaunchNotificationSettingsDao.addTargets(
                PIPE_LAUNCH.getIdString(), CUSTOM_JOB_EVENT, JOB_ID,
                new NotificationTargets(
                    Collections.singleton(new TelegramNotificationTarget(NotificationTargetType.DIRECT,
                        CURRENT_LAUNCH_TELEGRAM)),
                    Collections.emptySet(),
                    Collections.emptySet()
                )
            );

            pipeLaunchNotificationSettingsDao.addTargets(
                PIPE_LAUNCH_OF_NOT_CUSTOMIZED_PROJECT.getIdString(), CommonPipeEvents.PIPELINE_SUCCEEDED,
                new NotificationTargets(
                    Collections.singleton(new TelegramNotificationTarget(NotificationTargetType.DIRECT,
                        CURRENT_LAUNCH_TELEGRAM)),
                    Collections.singleton(new EmailNotificationTarget(NotificationTargetType.DIRECT,
                        CURRENT_LAUNCH_EMAIL)),
                    Collections.singleton(new StartrekNotificationTarget(CURRENT_LAUNCH_STARTREK))
                )
            );
            return pipeLaunchNotificationSettingsDao;
        }

        @Bean
        public ReleaseDao releaseDao() {
            ReleaseDao releaseDao = mock(ReleaseDao.class);
            Release.Builder releaseBuilder = Release.builder()
                .withPipeId("pipe-id")
                .withPipeLaunchId(new ObjectId().toString())
                .withCommit("rev1", Instant.now());

            when(releaseDao.getReleaseByPipeLaunchId(PIPE_LAUNCH.getId())).thenReturn(
                releaseBuilder.withProjectId(PROJECT_ID).build()
            );

            when(releaseDao.getReleaseByPipeLaunchId(PIPE_LAUNCH_OF_NOT_CUSTOMIZED_PROJECT.getId())).thenReturn(
                releaseBuilder.withProjectId(NOT_CUSTOMIZED_PROJECT).build()
            );

            when(releaseDao.getReleaseByPipeLaunchId(PIPE_LAUNCH_WITH_EMAIL_SUBJECT_TEMPLATE.getId())).thenReturn(
                releaseBuilder.withProjectId(PROJECT_ID_WITH_EMAIL_SUBJECT_TEMPLATE).build()
            );

            return releaseDao;
        }

        @Bean
        public StaffApiClient staffApi() {
            StaffApiClient staffApiClient = mock(StaffApiClient.class);
            StaffPerson staffPerson = new StaffPerson(
                null,
                123,
                null,
                List.of(new StaffPerson.PersonAccount(StaffPerson.AccountType.TELEGRAM, "john_Doe_666")),
                null,
                null);
            when(staffApiClient.getPerson("johnDoe666")).thenReturn(
                Optional.of(staffPerson)
            );

            return staffApiClient;
        }

        @Bean
        public ChangelogCommitDao changelogCommitDao() {
            ChangelogCommitDao changelogCommitDao = mock(ChangelogCommitDao.class);
            ChangelogCommit changelogCommit = ChangelogCommit.builder()
                .withCommitId(RepositoryType.ARCADIA, "rev1")
                .withAuthorLogin("johnDoe666")
                .withMessage("fix tests")
                .withDate(Instant.now())
                .build();

            when(changelogCommitDao.getByRevisions(List.of("rev1"))).thenReturn(
                List.of(changelogCommit)
            );

            return changelogCommitDao;
        }

        @Bean
        public MultitestingDao multitestingDao() {
            MultitestingDao multitestingDao = mock(MultitestingDao.class);
            when(multitestingDao.getEnvironmentByPipeLaunchId(MT_PIPE_LAUNCH.getId().toString())).thenReturn(
                new MultitestingEnvironment(
                    null, PROJECT_ID, null, 0, null, null, null, null, MultitestingDatacenterWeightService.DEFAULT_DC,
                    null, null, null, null, false, JanitorOptions.DEFAULT_OPTIONS,
                    null)
            );
            return multitestingDao;
        }

        @Bean
        public PerCommitBranchStateDao perCommitBranchStateDao() {
            PerCommitBranchStateDao perCommitBranchStateDao = mock(PerCommitBranchStateDao.class);
            when(perCommitBranchStateDao.getByPipeLaunchId(PER_COMMIT_PIPE_LAUNCH.getId())).thenReturn(
                PerCommitBranchStateEntity.builder()
                    .withProject(PROJECT_ID)
                    .withLaunchSetting(null)
                    .withRepository("testRepo")
                    .withBranch("testBranch")
                    .withMultitesting(false)
                    .withPipelineId(PER_COMMIT_PIPELINE_ID)
                    .withPullRequestId(null)
                    .withPullRequestNumber(null)
                    .withStatusCheckName(null)
                    .build()
            );
            return perCommitBranchStateDao;
        }

        @Bean
        public PerCommitBranchStateHistoryDao perCommitBranchStateHistoryDao() {
            PerCommitBranchStateHistoryDao perCommitBranchStateHistoryDao = mock(PerCommitBranchStateHistoryDao.class);
            when(perCommitBranchStateHistoryDao.getByPipeLaunchId(PER_COMMIT_OLD_PIPE_LAUNCH.getId())).thenReturn(
                new PerCommitBranchStateHistoryEntity(
                    PerCommitBranchStateEntity.builder()
                        .withProject(PROJECT_ID)
                        .withLaunchSetting(null)
                        .withRepository("testRepo")
                        .withBranch("testBranch")
                        .withMultitesting(false)
                        .withPipelineId(PER_COMMIT_PIPELINE_ID)
                        .withPullRequestId(null)
                        .withPullRequestNumber(null)
                        .withStatusCheckName(null)
                        .build()
                )
            );
            return perCommitBranchStateHistoryDao;
        }

        @Bean
        public PipelinesDao pipelinesDao() {
            PipelineEntity pipeline = mock(PipelineEntity.class);
            when(pipeline.getProjectId()).thenReturn(PROJECT_ID);

            PipelinesDao pipelinesDao = mock(PipelinesDao.class);
            when(pipelinesDao.get(Mockito.anyString())).thenReturn(pipeline);

            PipelineEntity customPipeline = mock(PipelineEntity.class);
            when(customPipeline.getProjectId()).thenReturn(PROJECT_ID_WITH_EMAIL_SUBJECT_TEMPLATE);

            when(pipelinesDao.get(Mockito.eq(PIPE_LAUNCH_WITH_EMAIL_SUBJECT_TEMPLATE.getPipeId())))
                .thenReturn(customPipeline);

            return pipelinesDao;
        }

        @Bean
        public NotificatorImpl notificator() {
            return new NotificatorImpl(
                notificationCenter(),
                projectNotificationSettingsDao(),
                launchNotificationSettingsDao(),
                releaseDao(),
                multitestingDao(),
                perCommitBranchStateDao(),
                perCommitBranchStateHistoryDao(),
                pipelinesDao(),
                staffApi(),
                changelogCommitDao());
        }
    }
}
