package ru.yandex.market.tsum.release.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.tsum.clients.conductor.ConductorBranch;
import ru.yandex.market.tsum.clients.sandbox.SandboxReleaseType;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceService;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.AbstractResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRef;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRefContainer;
import ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy.ConductorDeployJob;
import ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy.ConductorDeployJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.nanny.NannyReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.nanny.NannyReleaseJobConfig;
import ru.yandex.market.tsum.pipelines.common.resources.ConductorPackage;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 25.09.18
 */
public class ReleaseTimelineUtilsTest {
    @Mock
    private ResourceService resourceService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getEmptyTagsOnNullableContainer() {
        check(null, ConductorDeployJob.class, Collections.emptySet());
    }

    @Test
    public void getEmptyTagsOnEmptyContainer() {
        check(ResourceRefContainer.empty(), NannyReleaseJob.class, Collections.emptySet());
    }

    @Test
    public void getEmptyTagsOnNotDeployClass() {
        ResourceRef resourceRef = new ResourceRef(new ObjectId(), ConductorDeployJobConfig.class.getName(),
            UUID.randomUUID());
        ResourceRefContainer resourceRefContainer = new ResourceRefContainer(Collections.singletonList(resourceRef));

        check(resourceRefContainer, DummyJob.class, Collections.emptySet());
    }

    @Test
    public void getEmptyTagsOnEmptyConfig() {
        ResourceRef resourceRef = new ResourceRef(new ObjectId(), ConductorDeployJobConfig.class.getName(),
            UUID.randomUUID());
        ResourceRefContainer resourceRefContainer = new ResourceRefContainer(Collections.singletonList(resourceRef));

        AbstractResourceContainer resourceContainer = Mockito.mock(AbstractResourceContainer.class);

        Mockito.when(resourceContainer.getSingleOfType(Mockito.any())).thenReturn(null);

        Mockito.when(resourceService.loadResources(Mockito.any())).thenReturn(resourceContainer);

        check(
            resourceRefContainer,
            ConductorDeployJob.class,
            Collections.emptySet()
        );

        check(
            resourceRefContainer,
            NannyReleaseJob.class,
            Collections.emptySet()
        );
    }

    @Test
    public void getConductorStaticTags() {
        ResourceRef resourceRef = new ResourceRef(new ObjectId(), ConductorDeployJobConfig.class.getName(),
            UUID.randomUUID());
        ResourceRefContainer resourceRefContainer = new ResourceRefContainer(Collections.singletonList(resourceRef));

        List<String> packagesToDeploy = Arrays.asList("package1", "package2");

        AbstractResourceContainer resourceContainer = Mockito.mock(AbstractResourceContainer.class);

        Mockito.when(resourceContainer.getSingleOfType(ConductorDeployJobConfig.class)).thenReturn(
            ConductorDeployJobConfig.newBuilder(ConductorBranch.STABLE)
                .setPackagesToDeploy(packagesToDeploy)
                .build()
        );

        Mockito.when(resourceService.loadResources(resourceRefContainer)).thenReturn(resourceContainer);

        check(
            resourceRefContainer,
            ConductorDeployJob.class,
            packagesToDeploy.stream().map(p -> "package:" + p).collect(Collectors.toSet())
        );
    }

    @Test
    public void getConductorConsumedTags() {
        ResourceRefContainer resourceRefContainer = new ResourceRefContainer(
            Arrays.asList(
                new ResourceRef(new ObjectId(), ConductorPackage.class.getName(), UUID.randomUUID()),
                new ResourceRef(new ObjectId(), ConductorPackage.class.getName(), UUID.randomUUID()),
                new ResourceRef(new ObjectId(), ConductorPackage.class.getName(), UUID.randomUUID())
            )
        );

        AbstractResourceContainer resourceContainer = Mockito.mock(AbstractResourceContainer.class);

        Mockito.when(resourceContainer.getOfType(ConductorPackage.class)).thenReturn(
            Arrays.asList(
                new ConductorPackage("package1", "1"),
                new ConductorPackage("package2", "2"),
                new ConductorPackage("package3", "3")
            )
        );

        Mockito.when(resourceService.loadResources(resourceRefContainer)).thenReturn(resourceContainer);

        check(
            resourceRefContainer,
            ConductorDeployJob.class,
            new HashSet<>(Arrays.asList("package:package1:1", "package:package2:2", "package:package3:3"))
        );
    }

    @Test
    public void getNannyTags() {
        ResourceRef resourceRef = new ResourceRef(new ObjectId(), NannyReleaseJobConfig.class.getName(),
            UUID.randomUUID());
        ResourceRefContainer resourceRefContainer = new ResourceRefContainer(Collections.singletonList(resourceRef));

        List<String> sandboxResources = Arrays.asList("resource1", "resource2");

        AbstractResourceContainer resourceContainer = Mockito.mock(AbstractResourceContainer.class);

        Mockito.when(resourceContainer.getSingleOfType(NannyReleaseJobConfig.class)).thenReturn(
            NannyReleaseJobConfig.builder(SandboxReleaseType.STABLE)
                .withSandboxResourceType(sandboxResources.toArray(new String[2]))
                .build()
        );

        Mockito.when(resourceService.loadResources(resourceRefContainer)).thenReturn(resourceContainer);

        check(
            resourceRefContainer,
            NannyReleaseJob.class,
            sandboxResources.stream().map(r -> "nanny-resource:" + r).collect(Collectors.toSet())
        );

    }

    private void check(ResourceRefContainer container, Class jobClass, Set<String> expected) {
        Set<String> tags = ReleaseTimelineUtils.getDeployTags(
            container, jobClass.getName(), resourceService
        );

        Assert.assertEquals(
            expected,
            tags
        );
    }
}
