package ru.yandex.direct.intapi.webapp.semaphore;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.redislock.DistributedLock;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.intapi.webapp.semaphore.IntapiSemaphoreInterceptor.REQUEST_LOCK_ENTRY_NAME;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

public class SemaphoreInterceptorAcquiresSemaphoreTest {
    private TestController testController;

    private IntapiSemaphoreInterceptor intapiSemaphoreInterceptor;

    @Before
    public void setup() {
        testController = new TestController();
        intapiSemaphoreInterceptor = intapiLockInterceptor();
    }

    @Test
    public void lockIsSavedAsRequestAttribute() throws Exception {
        HandlerMethod handlerMethod = testController.getHandlerForLockedMethod();

        MockHttpServletRequest request = new MockHttpServletRequest();

        MockHttpServletResponse response = new MockHttpServletResponse();

        intapiSemaphoreInterceptor.preHandle(request, response, handlerMethod);

        DistributedLock lock = (DistributedLock) request.getAttribute(REQUEST_LOCK_ENTRY_NAME);
        assertThat("lock присутствует в атрибутах запроса", lock, not(nullValue()));
    }

    @Test
    public void interceptorLocksAndUnlocksMethod() throws Exception {
        HandlerMethod handlerMethod = testController.getHandlerForLockedMethod();

        MockHttpServletRequest request = new MockHttpServletRequest();

        MockHttpServletResponse response = new MockHttpServletResponse();

        intapiSemaphoreInterceptor.preHandle(request, response, handlerMethod);

        DistributedLock lock = (DistributedLock) request.getAttribute(REQUEST_LOCK_ENTRY_NAME);

        assumeThat("lock присутствует в атрибутах запроса", lock, not(nullValue()));

        verify(lock).lock();

        intapiSemaphoreInterceptor.afterCompletion(request, response, handlerMethod, null);
        verify(lock).unlock();
    }

    @Test
    public void interceptorNotLocksMethodWithoutAnnotation() throws Exception {
        HandlerMethod handlerMethod = testController.getHandlerForNotLockedMethod();

        MockHttpServletRequest request = new MockHttpServletRequest();

        MockHttpServletResponse response = new MockHttpServletResponse();

        intapiSemaphoreInterceptor.preHandle(request, response, handlerMethod);

        DistributedLock lock = (DistributedLock) request.getAttribute(REQUEST_LOCK_ENTRY_NAME);

        assertThat("lock не присутствует в атрибутах запроса", lock, nullValue());
    }

    public IntapiSemaphoreInterceptor intapiLockInterceptor() {
        IntapiSemaphoreInterceptor interceptor =
                spy(new IntapiSemaphoreInterceptor(mock(LettuceConnectionProvider.class), "tst"));
        DistributedLock lock = mock(DistributedLock.class);
        try {
            when(lock.lock()).thenReturn(true);
            when(lock.unlock()).thenReturn(true);
        } catch (InterruptedException e) {
            //mocked
        }

        doReturn(lock).when(interceptor).createLock(any(), anyInt(), anyLong(), anyLong());
        return interceptor;
    }
}
