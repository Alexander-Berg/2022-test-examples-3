package ru.yandex.market.tsum.notifications;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.notifications.email.EmailNotificationTarget;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotificationTarget;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.notifications.events.CommonMultitestingEvents;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.CommonJobEvents;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.CommonPipeEvents;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.NotificationLevel;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.EmailTargetSettings;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.EventNotificationSettings;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.NotificationTargets;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.ProjectNotificationSettings;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.ProjectNotificationSettingsDao;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.StartrekTargetSettings;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.TelegramTargetSettings;


/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 13.02.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class})
public class ProjectEventNotificationSettingsDaoTest {
    @Autowired
    private MongoTemplate mongoTemplate;

    private ProjectNotificationSettingsDao dao;

    @Before
    public void setUp() throws Exception {
        dao = new ProjectNotificationSettingsDao(mongoTemplate);
    }

    @Test
    public void setEventNotificationSettings() throws Exception {
        String projectId = "test";
        String pipelineId = "wood";
        String jobId = "conductorJob-42";

        EventNotificationSettings settings = new EventNotificationSettings(
            NotificationLevel.NEED_ATTENTION,
            new TelegramTargetSettings(new TelegramNotificationTarget("-123456")),
            new EmailTargetSettings("message for email", new EmailNotificationTarget("some@email.com")),
            new StartrekTargetSettings("message for startrek", true)
        );

        dao.setPipeEventNotificationSettings(projectId, pipelineId, CommonPipeEvents.PIPELINE_STARTED.getId(),
            settings);
        ProjectNotificationSettings notificationSettings = dao.getProjectNotificationSettings(projectId);
        Assert.assertEquals(
            settings,
            notificationSettings.getPipeIdToPipeSettings().get(pipelineId)
                .getPipeEventIdToEventSettings().get(CommonPipeEvents.PIPELINE_STARTED.getId())
        );
        Assert.assertTrue(notificationSettings.getPipeIdToPipeSettings().get(pipelineId)
            .getJobIdToEventIdToEventSettings().isEmpty());

        dao.setJobEventNotificationSettings(projectId, pipelineId, jobId, CommonJobEvents.JOB_FAILED.getId(), settings);
        notificationSettings = dao.getProjectNotificationSettings(projectId);
        Assert.assertEquals(
            settings,
            notificationSettings.getPipeIdToPipeSettings().get(pipelineId)
                .getJobIdToEventIdToEventSettings().get(jobId).get(CommonJobEvents.JOB_FAILED.getId())
        );

        dao.setMultitestingEventNotificationSettings(projectId, CommonMultitestingEvents.ENVIRONMENT_READY.getId(),
            settings);
        notificationSettings = dao.getProjectNotificationSettings(projectId);
        Assert.assertEquals(
            settings,
            notificationSettings.getMtEventIdToEventSettings().get(CommonMultitestingEvents.ENVIRONMENT_READY.getId())
        );
    }

    @Test
    public void replaceChatId() {
        String oldChatId = "1";
        String newChatId = "2";
        String projectId = "testProject";
        String pipeId = "pipeId";
        String jobId = "jobId";
        String eventId = "eventId";
        NotificationLevel notificationLevel = NotificationLevel.INFO;
        EventNotificationSettings settings = new EventNotificationSettings(
            NotificationLevel.INFO,
            new TelegramTargetSettings(new TelegramNotificationTarget(oldChatId)),
            null, null
        );
        NotificationTargets notificationTargets = new NotificationTargets(
            Collections.singleton(new TelegramNotificationTarget(oldChatId)),
            Collections.emptySet(),
            Collections.emptySet()
        );

        dao.setPipeEventNotificationSettings(projectId, pipeId, eventId, settings);
        dao.setJobEventNotificationSettings(projectId, pipeId, jobId, eventId, settings);
        dao.setMultitestingEventNotificationSettings(projectId, eventId, settings);
        dao.setProjectTargetsByEventId(projectId, eventId, notificationTargets);
        dao.setProjectTargetsByEventLevel(projectId, notificationLevel, notificationTargets);

        dao.replaceChatId(oldChatId, newChatId, projectId);

        ProjectNotificationSettings projectSettings = dao.getProjectNotificationSettings(projectId);
        Assert.assertEquals(
            newChatId,
            projectSettings.getPipeIdToPipeSettings().get(pipeId)
                .getPipeEventIdToEventSettings().get(eventId)
                .getTelegramSettings().getTargets().iterator().next().getTargetValue()
        );
        Assert.assertEquals(
            newChatId,
            projectSettings.getPipeIdToPipeSettings().get(pipeId)
                .getJobIdToEventIdToEventSettings().get(jobId).get(eventId)
                .getTelegramSettings().getTargets().iterator().next().getTargetValue()
        );
        Assert.assertEquals(
            newChatId,
            projectSettings.getMtEventIdToEventSettings().get(eventId)
                .getTelegramSettings().getTargets().iterator().next().getTargetValue()
        );
        Assert.assertEquals(
            newChatId,
            projectSettings.getProjectTargets(eventId).getTelegramTargets()
                .iterator().next().getTargetValue()
        );
        Assert.assertEquals(
            newChatId,
            projectSettings.getProjectTargets(notificationLevel).getTelegramTargets()
                .iterator().next().getTargetValue()
        );
    }
}
