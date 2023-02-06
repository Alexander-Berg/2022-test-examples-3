package ru.yandex.direct.libs.curator;

import java.time.Duration;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class CuratorFrameworkProviderTest {
    private static final String BASE_PATH_STR = "/our/lock/path";
    private static final String TEST_SERVERS = "ya.ru:8080";
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(120);

    @Mock
    private CuratorFramework framework;
    private CuratorFrameworkProvider provider;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        provider = spy(new CuratorFrameworkProvider(TEST_SERVERS, BASE_PATH_STR));
        doReturn(framework).when(provider).getDefaultCurator();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHelperNotCreatedIfPathIsNotAbsolute() {
        new CuratorFrameworkProvider(TEST_SERVERS, "test/test2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHelperReturnsErrorWhenNameContainsSlashes() {
        provider.getLock("name/name", TEST_TIMEOUT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHelperReturnsErrorWhenNameContainsSymbols() {
        provider.getLock("n%!@#$^&*()+=me", TEST_TIMEOUT, null);
    }
}
