package ru.yandex.market.tsum.pipelines.checkout.jobs;

import java.nio.charset.Charset;
import java.util.List;

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.TaskResource;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipelines.common.resources.SandboxResource;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class,
    PandoraTemplateFromResourceSelectorJobTest.SandboxConfig.class})
public class PandoraTemplateFromResourceSelectorJobTest {
    @Autowired
    private PipeTester pipeTester;

    @Test
    public void template_should_be_loaded() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(PandoraTemplateFromResourceSelectorJob.class, "template");
        builder.withManualResource(SandboxResource.class);
        builder.withManualResource(PandoraTemplateFilePath.class);
        final Pipeline pipeline = builder.build();


        final Resource sandboxResource = new SandboxResource("", 1179135276L, "BUILD_OUTPUT", 2687236956L);
        final Resource filePath = new PandoraTemplateFilePath("market/checkout/checkouter-load-test/checkouter" +
            "-production-fire/load-test-production-dev.tmpl");
        final String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline,
            List.of(sandboxResource, filePath));

        final PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        final JobState state = pipeLaunch.getJobState("template");
        Assert.assertFalse(state.isLastStatusChangeTypeFailed());
        final List<PandoraGeneratorTemplateConfig> template = pipeTester.getProducedResourcesOfType(pipeLaunchId,
            "template", PandoraGeneratorTemplateConfig.class);
        Assert.assertThat(
            template,
            IsIterableWithSize.iterableWithSize(1));
        Assert.assertEquals("https://proxy.sandbox.yandex-team.ru/2687236956/market/checkout/checkouter-load-test" +
                "/checkouter-production-fire/load-test-production-dev.tmpl",
            template.get(0).getTemplate());
    }

    public static class SandboxConfig {
        @Bean
        public SandboxClient sandboxClient() {
            final SandboxClient mock = Mockito.mock(SandboxClient.class);
            Mockito.when(mock.getResource(Mockito.anyLong())).
                thenAnswer(new Answer<TaskResource>() {
                    @Override
                    public TaskResource answer(InvocationOnMock invocation) throws Throwable {
                        final Long id = invocation.getArgument(0, Long.class);
                        final TaskResource value = new TaskResource(id);
                        value.setHttpLink("https://proxy.sandbox.yandex-team.ru/" + id);
                        return value;
                    }
                });
            Mockito.when(mock.getRawResourceContent(Mockito.any(TaskResource.class), Mockito.any(Charset.class)))
                .thenAnswer(new Answer<String>() {
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable {
                        final TaskResource argument = invocation.getArgument(0, TaskResource.class);
                        return argument.getHttpLink();
                    }
                });
            return mock;
        }
    }
}
