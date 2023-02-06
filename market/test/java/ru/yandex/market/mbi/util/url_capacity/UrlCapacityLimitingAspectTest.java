package ru.yandex.market.mbi.util.url_capacity;

import org.aspectj.lang.ProceedingJoinPoint;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.framework.core.ErrorInfo;
import ru.yandex.common.framework.core.FullActionNameProvider;
import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.common.framework.core.Servantlet;
import ru.yandex.common.framework.http.HttpServRequest;
import ru.yandex.market.partner.util.url_capacity.UrlCapacityLimitingAspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UrlCapacityLimitingAspectTest {

    private static final Long USER_ID = 1L;
    private static final String SOME_SERVANTLET_NAME = "someServantlet";
    private static final String SERVANTLET_ACTION_NAME = "someActionName";

    private UrlCapacityLimitingAspect aspect;

    protected UrlCapacityLimiter urlCapacityLimiter = mock(UrlCapacityLimiter.class);
    protected UrlCapacityLimitFlags urlCapacityLimitFlags = mock(UrlCapacityLimitFlags.class);

    public void setFlagEnabled(boolean value) {
        when(urlCapacityLimitFlags.isEnabled()).thenReturn(value);
    }

    public void setLogsOnly(boolean value) {
        when(urlCapacityLimitFlags.isLogsOnly()).thenReturn(value);
    }

    public void setCanProcessOneMoreUrl(boolean value) {
        when(urlCapacityLimiter.tryProcessOneMoreRequest(any())).thenReturn(value);
    }

    @BeforeEach
    public void init() {
        aspect = new UrlCapacityLimitingAspect(urlCapacityLimitFlags, urlCapacityLimiter);
    }

    @Test
    public void testSimpleUrl() throws Throwable {
        setFlagEnabled(true);
        setLogsOnly(false);
        setCanProcessOneMoreUrl(true);

        aspect.aroundServantletProcess(mockJoinPoint(SOME_SERVANTLET_NAME));

        verify(urlCapacityLimiter, times(1)).tryProcessOneMoreRequest(eq(SOME_SERVANTLET_NAME));
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq(SOME_SERVANTLET_NAME));
    }

    @Test
    public void testServantletWithAction() throws Throwable {
        setFlagEnabled(true);
        setLogsOnly(false);
        setCanProcessOneMoreUrl(true);

        aspect.aroundServantletProcess(mockJoinPointWithActionServantlet(SOME_SERVANTLET_NAME));

        verify(urlCapacityLimiter, times(1)).tryProcessOneMoreRequest(eq(SERVANTLET_ACTION_NAME));
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq(SERVANTLET_ACTION_NAME));
    }

    @Test
    public void test429OnDeny() throws Throwable {
        setFlagEnabled(true);
        setLogsOnly(false);
        setCanProcessOneMoreUrl(false);

        MockServResponse response = new MockServResponse();
        ProceedingJoinPoint joinPoint = mockJoinPointWithSpecifiedResponse(SOME_SERVANTLET_NAME, response);
        aspect.aroundServantletProcess(joinPoint);

        MockServResponse resp = (MockServResponse) joinPoint.getArgs()[1];

        assertThat(resp.hasErrors()).isTrue();
        assertThat(resp.getErrors().size()).isEqualTo(1);

        ErrorInfo errorInfo = resp.getErrors().get(0);
        assertThat(errorInfo.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS_429);

        verify(urlCapacityLimiter, times(1)).tryProcessOneMoreRequest(eq(SOME_SERVANTLET_NAME));
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq(SOME_SERVANTLET_NAME));
    }

    @Test
    public void testNo429OnLogsOnly() throws Throwable {
        setFlagEnabled(true);
        setLogsOnly(true);
        setCanProcessOneMoreUrl(false);

        MockServResponse response = new MockServResponse();
        ProceedingJoinPoint joinPoint = mockJoinPointWithSpecifiedResponse(SOME_SERVANTLET_NAME, response);
        aspect.aroundServantletProcess(joinPoint);

        MockServResponse resp = (MockServResponse) joinPoint.getArgs()[1];

        assertThat(resp.hasErrors()).isFalse();

        verify(urlCapacityLimiter, times(1)).tryProcessOneMoreRequest(eq(SOME_SERVANTLET_NAME));
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq(SOME_SERVANTLET_NAME));
    }

    @Test
    public void testSimpleUrlWithFlagDisabled() throws Throwable {
        setFlagEnabled(false);
        setCanProcessOneMoreUrl(true);

        aspect.aroundServantletProcess(mockJoinPoint(SOME_SERVANTLET_NAME));

        verify(urlCapacityLimiter, never()).tryProcessOneMoreRequest(eq(SOME_SERVANTLET_NAME));
        verify(urlCapacityLimiter, never()).requestProcessed(eq(SOME_SERVANTLET_NAME));
    }

    @Test
    public void testProceedThrowsException() throws Throwable {
        setFlagEnabled(true);
        setLogsOnly(false);
        setCanProcessOneMoreUrl(true);

        assertThatThrownBy(() ->
                aspect.aroundServantletProcess(mockJoinPointWithExceptionOnProceed(SOME_SERVANTLET_NAME)  )
        ).isInstanceOf(Exception.class);

        //В случае исключения так или иначе гарантируем проверку и очистку счетчика
        verify(urlCapacityLimiter, times(1)).tryProcessOneMoreRequest(eq(SOME_SERVANTLET_NAME));
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq(SOME_SERVANTLET_NAME));
    }

    private ProceedingJoinPoint mockJoinPoint(String name) {
        ServRequest req = new HttpServRequest(USER_ID, null, name);
        ServResponse resp = new MockServResponse();

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.getTarget()).thenReturn(new SimpleServantlet());
        when(point.getArgs()).thenReturn(new Object[]{req, resp});

        return point;
    }

    private ProceedingJoinPoint mockJoinPointWithActionServantlet(String name) {
        ServRequest req = new HttpServRequest(USER_ID, null, name);
        ServResponse resp = new MockServResponse();

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.getTarget()).thenReturn(new ActionServantlet());
        when(point.getArgs()).thenReturn(new Object[]{req, resp});

        return point;
    }

    private ProceedingJoinPoint mockJoinPointWithSpecifiedResponse(String name, ServResponse response) {
        ServRequest req = new HttpServRequest(USER_ID, null, name);

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.getTarget()).thenReturn(new SimpleServantlet());
        when(point.getArgs()).thenReturn(new Object[]{req, response});

        return point;
    }

    private ProceedingJoinPoint mockJoinPointWithExceptionOnProceed(String name) throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(name);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Some exception!"));

        return joinPoint;
    }

    private static class SimpleServantlet implements Servantlet {

        @Override
        public void process(ServRequest req, ServResponse res) {

        }
    }

    private static class ActionServantlet implements Servantlet, FullActionNameProvider {

        @Override
        public String getFullActionName(ServRequest request) {
            return SERVANTLET_ACTION_NAME;
        }

        @Override
        public void process(ServRequest req, ServResponse res) {

        }
    }
}
