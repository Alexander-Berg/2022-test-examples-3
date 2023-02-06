package ru.yandex.direct.intapi.webapp.semaphore;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.redislock.DistributedLock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class SemaphoreInterceptorThrowsExceptionWhenCannotLockTest {
    private TestController testController;

    private IntapiSemaphoreInterceptor intapiSemaphoreInterceptor;

    @Before
    public void setup() {
        testController = new TestController();
        intapiSemaphoreInterceptor = intapiLockInterceptor();
    }

    @Test(expected = IntApiException.class)
    public void interceptorLocksAndUnlocksMethod() throws Exception {
        HandlerMethod handlerMethod = testController.getHandlerForLockedMethod();

        intapiSemaphoreInterceptor
                .preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), handlerMethod);
    }

    public IntapiSemaphoreInterceptor intapiLockInterceptor() {
        IntapiSemaphoreInterceptor interceptor =
                spy(new IntapiSemaphoreInterceptor(mock(LettuceConnectionProvider.class), "tst"));
        DistributedLock lock = mock(DistributedLock.class);
        try {
            when(lock.lock()).thenReturn(false);
        } catch (InterruptedException e) {
            //mocked
        }

        doReturn(lock).when(interceptor).createLock(any(), anyInt(), anyLong(), anyLong());
        return interceptor;
    }
}
