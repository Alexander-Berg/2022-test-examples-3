package ru.yandex.canvas.service;

import java.util.Optional;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.canvas.model.direct.Privileges;
import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты ходят в реальные сервисы, для тестирования нужно подложить TVM тикет в переменную окружения tvm
 * Который "3:serv:*"
 */
public class AuthServiceHttpProxyTest {
    private AuthServiceHttpProxy client;
    private AuthRequestParams authRequestParams;

    @Before
    public void before() {
        TvmIntegration tvmIntegration = mock(TvmIntegration.class);
        when(tvmIntegration.isEnabled()).thenReturn(true);
        when(tvmIntegration.getTicket(any())).thenReturn(System.getenv("tvm"));
        authRequestParams = mock(AuthRequestParams.class);
        when(authRequestParams.getUserId()).thenReturn(Optional.of(221776172L));
        client = new AuthServiceHttpProxy(authRequestParams,
                "http://8998.beta1.intapi.direct.yandex.ru/DisplayCanvas/",
                tvmIntegration, TvmService.DIRECT_INTAPI_TEST,
                new ParallelFetcherFactory(new DefaultAsyncHttpClient(), new FetcherSettings()));
        System.out.println(System.getenv("tvm"));
    }

    @Test
    @Ignore("Ходит в реальную систему")
    //curl --header "x-ya-service-ticket: $(cat ~/.tvm)" "http://8998.beta1.intapi.direct.yandex.ru/DisplayCanvas/ping"
    public void ping() {
        assertThat(client.ping(), is("OK"));
    }

    @Test
    @Ignore("Ходит в реальную систему")
    //curl --header "x-ya-service-ticket: $(cat ~/.tvm)" "http://8998.beta1.intapi.direct.yandex.ru/DisplayCanvas/authenticate/?client_id=450488&user_id=221776172"
    public void authenticate() {
        when(authRequestParams.getClientId()).thenReturn(Optional.of(450488L));
        assertThat(client.authenticate().checkPermissionOn(Privileges.Permission.CREATIVE_CREATE), is(true));
        assertThat(client.authenticate().checkPermissionOn(Privileges.Permission.UNKNOWN_PERMISSION), is(true));
    }

    @Test
    @Ignore("Ходит в реальную систему")
    public void authenticateNullClient() {
        when(authRequestParams.getClientId()).thenReturn(Optional.empty());
        assertThat(client.authenticate().checkPermissionOn(Privileges.Permission.PREVIEW), is(true));
    }
}
