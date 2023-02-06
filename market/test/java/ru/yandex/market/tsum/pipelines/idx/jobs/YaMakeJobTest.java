package ru.yandex.market.tsum.pipelines.idx.jobs;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.sandbox.TaskResource;
import ru.yandex.market.tsum.clients.startrek.StartrekCommentNotification;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class YaMakeJobTest {
    @Test
    public void startrekNotificationWorks() {
        JobState jobState = Mockito.mock(JobState.class);
        Mockito.when(jobState.getTitle()).thenReturn("foo");

        JobContext context = Mockito.mock(JobContext.class);
        Mockito.when(context.getPipeLaunchUrl()).thenReturn("http://example.yandex.net/pipe");
        Mockito.when(context.getJobLaunchDetailsUrl()).thenReturn("http://example.yandex.net/pipe/job");
        Mockito.when(context.getJobState()).thenReturn(jobState);

        List<TaskResource> resources = new ArrayList<>();
        resources.add(
            createTaskResource("http://example.yandex.net/bar", "BAR", "bar desc")
        );
        resources.add(
            createTaskResource("http://example.yandex.net/baz", "BAZ", "baz desc")
        );

        StartrekCommentNotification notification = YaMakeNotifications.createStartrekNotification(context, resources,
            true);

        String expectedComment = "" +
            "**foo:** %%ya make%% завершился успешно.\n" +
            "\n" +
            "Джоба Sandbox создала ресурсы:\n" +
            "- ((http://example.yandex.net/bar BAR)): bar desc\n" +
            "- ((http://example.yandex.net/baz BAZ)): baz desc\n" +
            "\n" +
            "((http://example.yandex.net/pipe Перейти к пайплайну))\n" +
            "((http://example.yandex.net/pipe/job Перейти к пайплайн задаче))";

        Assert.assertEquals(expectedComment, notification.getStartrekComment());
    }

    private static @NotNull
    TaskResource createTaskResource(String httpLink, String type, String description) {
        TaskResource resource = new TaskResource(2);
        resource.setHttpLink(httpLink);
        resource.setType(type);
        resource.setDescription(description);
        return resource;
    }

}
