package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.TaskInputDto;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.SandboxTaskJobConfig;
import ru.yandex.market.tsum.pipelines.common.resources.YandexDeployStage;
import ru.yandex.market.tsum.pipelines.sre.resources.DeplateSandboxConfig;
import ru.yandex.market.tsum.pipelines.sre.resources.YandexDeployServiceSpec;

import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BaseDeplateJobTest {
    @Autowired
    private JobTester jobTester;

    private TestTsumJobContext context = new TestTsumJobContext("unknown");

    private DeplateSandboxConfig config = Mockito.mock(DeplateSandboxConfig.class);
    private BaseDeplateJob job;

    @Before
    public void setup() {
        job = createJob();
    }

    @Test
    public void testPrepareTask() throws Exception {
        Map<String, String> expected = Map.of("deplate_token", "token", "arc_token", "arc", "arc_user", "user");

        when(config.getToken()).thenReturn(expected.get("deplate_token"));
        when(config.getArcToken()).thenReturn(expected.get("arc_token"));
        when(config.getArcUserName()).thenReturn(expected.get("arc_user"));
        TaskInputDto taskInput = new TaskInputDto("");

        job.prepareTask(context, taskInput);

        Assert.assertEquals(expected,
            taskInput.getCustomFields()
                .stream()
                .filter(item -> expected.containsKey(item.getName()))
                .collect(Collectors.toMap(
                    TaskInputDto.TaskFieldValidateItem::getName,
                    TaskInputDto.TaskFieldValidateItem::getValue
                )));
    }

    @Test
    public void testGetStageFromYandexDeployServiceSpec() {
        YandexDeployServiceSpec spec = new YandexDeployServiceSpec();

        try {
            job.getStageFromYandexDeployServiceSpec(spec, Environment.TESTING, "test");
            Assert.fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            Assert.assertEquals("Empty stage list in service config", e.getMessage());
        }

        spec.setStages(List.of(
            new YandexDeployStage("prestable", Environment.PRESTABLE, "test"),
            new YandexDeployStage("production", Environment.TESTING, "not_test"),
            new YandexDeployStage("production", Environment.PRODUCTION, "test")
        ));

        try {
            job.getStageFromYandexDeployServiceSpec(spec, Environment.TESTING, "test");
            Assert.fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            Assert.assertEquals("Missing stage config for environment", e.getMessage());
        }

        YandexDeployStage stage = job.getStageFromYandexDeployServiceSpec(spec, Environment.PRODUCTION, "test");

        Assert.assertEquals("production", stage.getName());
    }

    @Test
    public void testConvertEnvironment() {
        Assert.assertEquals("unstable", job.convertEnvironment(Environment.LOCAL));
        Assert.assertEquals("production", job.convertEnvironment(Environment.PRODUCTION));
    }

    private BaseDeplateJob createJob() {
        JobInstanceBuilder<BaseDeplateJob> jobBuilder =
            jobTester.jobInstanceBuilder(BaseDeplateJob.class)
                .withBean(Mockito.mock(SandboxClient.class))
                .withResource(config)
                .withResource(
                    SandboxTaskJobConfig.newBuilder("JOB")
                        .build()
                );
        return jobBuilder.create();
    }
}
