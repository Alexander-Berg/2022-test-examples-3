package ru.yandex.market.tsum.pipelines.startrek.jobs;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.TaskResource;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.ResourcesJobContext;
import ru.yandex.market.tsum.pipelines.common.jobs.platform.DeployJobResource;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.common.resources.SandboxTaskId;
import ru.yandex.market.tsum.pipelines.startrek.config.StartrekEnvironmentConfig;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipelines.common.jobs.platform.DeployJobResource.tagOf;

/**
 * @author Sergey Filippov <rolenof@yandex-team.ru>
 */
@RunWith(MockitoJUnitRunner.class)
public class DockerConfigJobTest {
    @InjectMocks
    private DockerConfigJob job = new DockerConfigJob();

    @Mock
    private ReleaseInfo releaseInfo;
    @Mock
    private StartrekEnvironmentConfig config;
    @Mock
    private JobContext context;
    @Mock
    private ResourcesJobContext resourcesContext;
    @Mock
    private SandboxClient sandboxClient;
    @Mock
    private SandboxTaskId sandboxTaskId;
    @Mock
    private TaskResource sandboxResource;

    @Before
    public void setUp() {
        when(context.resources()).thenReturn(resourcesContext);
    }

    @Test
    public void test() throws Exception {
        when(config.getQloudApplication()).thenReturn("tools.separator");
        when(config.getComponents()).thenReturn(Collections.singletonList("separator"));
        when(sandboxTaskId.getId()).thenReturn(1L);
        when(sandboxClient.getResources(1L)).thenReturn(Collections.singletonList(sandboxResource));
        when(sandboxResource.getType()).thenReturn("YA_PACKAGE");
        when(sandboxResource.getAttribute("resource_version")).thenReturn("registry.yandex.net/tools/separator:2019" +
            ".01");

        doAnswer(invocation -> {
            DeployJobResource resource = invocation.getArgument(0);
            assertEquals("tools.separator", resource.getApplicationId());
            assertEquals(1, resource.getRepositoriesMap().size());
            assertEquals(tagOf("registry.yandex.net/tools/separator", "2019.01"), resource.getRepositoriesMap().get(
                "separator"));
            return null;
        }).when(resourcesContext).produce(any(DeployJobResource.class));

        job.execute(context);

        verify(resourcesContext).produce(any(DeployJobResource.class));
    }
}
