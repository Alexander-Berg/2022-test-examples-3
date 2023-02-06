package ru.yandex.direct.core.entity.application.service;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.application.model.AccessType;
import ru.yandex.direct.core.entity.application.repository.ApiAppAccessRepository;
import ru.yandex.direct.env.EnvironmentType;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ApiAppAccessServiceTest {

    private ApiAppAccessService serviceUnderTest;
    private ApiAppAccessRepository apiAppAccessRepository;

    @Parameterized.Parameters(name = "accessType приложения: {0}; окружение: {1}; ожидаемый результат проверки: {2}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {AccessType.NORMAL, EnvironmentType.PRODUCTION, true},
                {AccessType.NORMAL, EnvironmentType.PRESTABLE, true},
                {AccessType.NORMAL, EnvironmentType.TESTING, true},
                {AccessType.NORMAL, EnvironmentType.DEVELOPMENT, true},
                {AccessType.NORMAL, EnvironmentType.SANDBOX, true},
                {AccessType.NORMAL, EnvironmentType.SANDBOX_TESTING, true},
                {AccessType.NORMAL, EnvironmentType.SANDBOX_DEVELOPMENT, true},

                {AccessType.TEST, EnvironmentType.PRODUCTION, false},
                {AccessType.TEST, EnvironmentType.PRESTABLE, false},
                {AccessType.TEST, EnvironmentType.TESTING, false},
                {AccessType.TEST, EnvironmentType.DEVELOPMENT, false},
                {AccessType.TEST, EnvironmentType.SANDBOX, true},
                {AccessType.TEST, EnvironmentType.SANDBOX_TESTING, true},
                {AccessType.TEST, EnvironmentType.SANDBOX_DEVELOPMENT, true},

                {null, EnvironmentType.PRODUCTION, false},
                {null, EnvironmentType.PRESTABLE, false},
                {null, EnvironmentType.TESTING, false},
                {null, EnvironmentType.DEVELOPMENT, false},
                {null, EnvironmentType.SANDBOX, false},
                {null, EnvironmentType.SANDBOX_TESTING, false},
                {null, EnvironmentType.SANDBOX_DEVELOPMENT, false},
        });
    }

    @Parameterized.Parameter(0)
    public AccessType accessType;

    @Parameterized.Parameter(1)
    public EnvironmentType environmentType;

    @Parameterized.Parameter(2)
    public boolean expectedResult;

    @Before
    public void before() {
        apiAppAccessRepository = mock(ApiAppAccessRepository.class);
        when(apiAppAccessRepository.getAccessType(any())).thenReturn(accessType);

        serviceUnderTest = new ApiAppAccessService(apiAppAccessRepository, environmentType);
    }

    @Test
    public void returnsValidResult() {
        assertThat(serviceUnderTest.checkApplicationAccess(""), is(expectedResult));
    }
}
