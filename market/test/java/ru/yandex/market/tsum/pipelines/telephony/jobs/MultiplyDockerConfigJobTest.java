package ru.yandex.market.tsum.pipelines.telephony.jobs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.TaskResource;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.ResourcesJobContext;
import ru.yandex.market.tsum.pipelines.common.jobs.platform.DeployJobResource;
import ru.yandex.market.tsum.pipelines.common.resources.SandboxTaskId;
import ru.yandex.market.tsum.pipelines.telephony.config.BuildToPlatformDeployMappingConfig;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipelines.common.jobs.platform.DeployJobResource.tagOf;

/**
 * @author Aleksandr Kudrevatykh <kudrale@yandex-team.ru>
 */
@RunWith(MockitoJUnitRunner.class)
public class MultiplyDockerConfigJobTest {
    @InjectMocks
    private MultiplyDockerConfigJob job = new MultiplyDockerConfigJob();

    @Mock
    private BuildToPlatformDeployMappingConfig config1;
    @Mock
    private BuildToPlatformDeployMappingConfig config2;
    @Mock
    private JobContext context;
    @Mock
    private ResourcesJobContext resourcesContext;
    @Mock
    private SandboxClient sandboxClient;
    @Mock
    private SandboxTaskId sandboxTaskId1;
    @Mock
    private SandboxTaskId sandboxTaskId2;
    @Mock
    private TaskResource sandboxResource1;
    @Mock
    private TaskResource sandboxResource2;

    @Before
    public void setUp() {
        when(context.resources()).thenReturn(resourcesContext);
    }

    @Test
    public void testSingle() throws Exception {
        when(config1.getQloudApplication()).thenReturn("telephony.telephony-platform");
        when(config1.getComponents()).thenReturn(Collections.singletonList("spn"));
        when(config1.getPackagePath()).thenReturn("yabs/telephony/platform/spn/package.json");
        job.setDeployConfigMapping(Collections.singletonList(config1));
        when(sandboxTaskId1.getId()).thenReturn(1L);
        job.setTaskIds(Collections.singletonList(sandboxTaskId1));
        when(sandboxClient.getResources(1L)).thenReturn(Collections.singletonList(sandboxResource1));
        when(sandboxResource1.getType()).thenReturn("YA_PACKAGE");
        when(sandboxResource1.getDescription()).thenReturn("yabs/telephony/platform/spn/package.json");
        when(sandboxResource1.getAttribute("resource_version")).thenReturn("registry.yandex.net/telephony/spn:v1");

        doAnswer(invocation -> {
            DeployJobResource resource = invocation.getArgument(0);
            assertEquals("telephony.telephony-platform", resource.getApplicationId());
            assertEquals(1, resource.getRepositoriesMap().size());
            assertEquals(tagOf("registry.yandex.net/telephony/spn", "v1"), resource.getRepositoriesMap().get("spn"));
            return null;
        }).when(resourcesContext).produce(any(DeployJobResource.class));

        job.execute(context);

        verify(resourcesContext).produce(any(DeployJobResource.class));
    }

    @Test
    public void testMultiply() throws Exception {
        when(config1.getQloudApplication()).thenReturn("telephony.telephony-platform");
        when(config1.getComponents()).thenReturn(Collections.singletonList("spn"));
        when(config1.getPackagePath()).thenReturn("yabs/telephony/platform/spn/package.json");

        when(config2.getQloudApplication()).thenReturn("telephony.telephony-platform-core");
        when(config2.getComponents()).thenReturn(Collections.singletonList("core"));
        when(config2.getPackagePath()).thenReturn("yabs/telephony/platform/core/package.json");

        job.setDeployConfigMapping(Arrays.asList(config1, config2));
        when(sandboxTaskId1.getId()).thenReturn(1L);
        when(sandboxTaskId2.getId()).thenReturn(2L);
        job.setTaskIds(Arrays.asList(sandboxTaskId1, sandboxTaskId2));
        when(sandboxClient.getResources(1L)).thenReturn(Arrays.asList(sandboxResource1, sandboxResource2));
        when(sandboxResource1.getType()).thenReturn("YA_PACKAGE");
        when(sandboxResource1.getDescription()).thenReturn("yabs/telephony/platform/spn/package.json");
        when(sandboxResource1.getAttribute("resource_version")).thenReturn("registry.yandex.net/telephony/spn:v1");

        when(sandboxResource2.getType()).thenReturn("YA_PACKAGE");
        when(sandboxResource2.getDescription()).thenReturn("yabs/telephony/platform/core/package.json");
        when(sandboxResource2.getAttribute("resource_version")).thenReturn("registry.yandex.net/telephony/core:v2");

        ArgumentCaptor<DeployJobResource> captor = ArgumentCaptor.forClass(DeployJobResource.class);

        job.execute(context);

        verify(resourcesContext, times(2)).produce(captor.capture());
        DeployJobResource resource = captor.getAllValues().stream().filter(res -> res.getApplicationId().equals(
            "telephony.telephony-platform")).findFirst().orElseThrow(AssertionError::new);
        assertEquals(1, resource.getRepositoriesMap().size());
        assertEquals(tagOf("registry.yandex.net/telephony/spn", "v1"), resource.getRepositoriesMap().get("spn"));

        resource = captor.getAllValues().stream().filter(res -> res.getApplicationId().equals("telephony" +
            ".telephony-platform-core")).findFirst().orElseThrow(AssertionError::new);
        assertEquals(1, resource.getRepositoriesMap().size());
        assertEquals(tagOf("registry.yandex.net/telephony/core", "v2"), resource.getRepositoriesMap().get("core"));
    }

    @Test
    public void testUnknownPackage() throws Exception {
        job.setDeployConfigMapping(List.of());
        when(sandboxTaskId1.getId()).thenReturn(1L);
        job.setTaskIds(Collections.singletonList(sandboxTaskId1));
        when(sandboxClient.getResources(1L)).thenReturn(Collections.singletonList(sandboxResource1));
        when(sandboxResource1.getType()).thenReturn("YA_PACKAGE");
        when(sandboxResource1.getDescription()).thenReturn("yabs/telephony/platform/spn/package.json");
        when(sandboxResource1.getAttribute("resource_version")).thenReturn("registry.yandex.net/telephony/spn:v1");

        job.execute(context);

        verify(resourcesContext, never()).produce(any(DeployJobResource.class));
    }
}
