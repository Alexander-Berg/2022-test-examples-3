package ru.yandex.market.tsum.isolation.impl;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.isolation.ProjectContextFactory;
import ru.yandex.market.tsum.pipe.engine.isolation.VaultService;
import ru.yandex.market.tsum.pipe.engine.isolation.exceptions.IsolationException;
import ru.yandex.market.tsum.pipe.engine.isolation.model.SecretVersion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 03/12/2018
 */
public class ProjectContextProviderImplTest {
    public static final String PROJECT_ID = "project";
    public static final String SECRET_ID = "secret-id";

    private final VaultService vaultService = mock(VaultService.class);
    private final ProjectsDao projectsDao = mock(ProjectsDao.class);
    private final ProjectContextFactory projectContextFactory = mock(ProjectContextFactory.class);

    private final SecretVersion.Builder builder = SecretVersion.builder()
        .withSecretUuid(SECRET_ID)
        .withAllProjectsAllowed(true)
        .withVersion("v1")
        .withSecretName("secret name");

    @Before
    public void setup() {
        ProjectEntity project = new ProjectEntity();
        project.setSecretId(SECRET_ID);
        when(projectsDao.get(PROJECT_ID)).thenReturn(project);

        when(vaultService.getLastSecretVersion(SECRET_ID)).thenReturn(builder.build());
    }

    @Test
    public void cachesContextAndClearsCacheAfterVersionUpdate() {
        ProjectContextProviderImpl provider = new ProjectContextProviderImpl(
            mock(ApplicationContext.class), vaultService, projectsDao, projectContextFactory
        );

        provider.get(PROJECT_ID);
        provider.get(PROJECT_ID);

        verify(projectContextFactory, times(1)).create(any());

        when(vaultService.getLastSecretVersion(SECRET_ID)).thenReturn(builder.withVersion("v2").build());
        provider.get(PROJECT_ID);

        verify(projectContextFactory, times(2)).create(any());
    }

    @Test(expected = IsolationException.class)
    public void recheckSecretPermissions() {
        ProjectContextProviderImpl provider = new ProjectContextProviderImpl(
            mock(ApplicationContext.class), vaultService, projectsDao, projectContextFactory
        );

        when(vaultService.getLastSecretVersion(SECRET_ID)).thenReturn(builder.withAllProjectsAllowed(false).build());

        provider.get(PROJECT_ID);
    }
}
