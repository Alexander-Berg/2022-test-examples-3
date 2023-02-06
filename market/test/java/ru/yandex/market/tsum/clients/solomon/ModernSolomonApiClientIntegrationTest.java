package ru.yandex.market.tsum.clients.solomon;

import java.util.Arrays;
import java.util.Collection;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.tsum.clients.solomon.models.SolomonProjectAccessibility;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
@Ignore("integration test")
public class ModernSolomonApiClientIntegrationTest {
    // NB токен надо выписывать на себя через новое приложение в oauth
    // с доступом к редактированию настроек соломона, потому что у роботов
    // доступ к market-mobile-blue есть
    private static final String TEST_TOKEN = "*********";

    private static final String SOLOMON_API_URL = "https://solomon.yandex-team.ru/api";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    NettyHttpClientContext contextMock;

    private ModernSolomonApiClient client;

    private final String projectName;
    private final SolomonProjectAccessibility accessibility;

    public ModernSolomonApiClientIntegrationTest(String projectName, SolomonProjectAccessibility accessibility) {
        this.projectName = projectName;
        this.accessibility = accessibility;
    }

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
            new Object[]{"market-infra", SolomonProjectAccessibility.ACCESSIBLE},
            new Object[]{"marketinfra", SolomonProjectAccessibility.NOT_FOUND},
            new Object[]{"market-mobile-blue", SolomonProjectAccessibility.ACCESS_DENIED}
        );
    }

    @Before
    public void setUp() throws Exception {
        Mockito.when(contextMock.getClient()).thenReturn(new DefaultAsyncHttpClient());
        client = new ModernSolomonApiClient(contextMock, TEST_TOKEN, SOLOMON_API_URL);
    }

    @Test
    public void getProjectAccessibility() {
        assertThat(client.getProjectAccessibility(projectName), equalTo(accessibility));
    }

}
