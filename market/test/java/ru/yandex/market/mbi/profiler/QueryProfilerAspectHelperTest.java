package ru.yandex.market.mbi.profiler;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.market.mbi.profiler.query.QueryProfiler;
import ru.yandex.market.mbi.profiler.query.QueryProfilerAspectHelper;

class QueryProfilerAspectHelperTest {

    private static <T> T createMock(Class<T> clazz) {
        return Mockito.mock(clazz, Mockito.withSettings().defaultAnswer(new ThrowsException(new RuntimeException("Not supported"))));
    }

    @Test
    void invoke() throws Throwable {
        QueryProfiler queryProfiler = new QueryProfiler();
        QueryProfilerAspectHelper queryProfilerAspectHelper = new QueryProfilerAspectHelper();
        queryProfilerAspectHelper.setQueryProfiler(queryProfiler);

        Method method = TestClass.method;

        MethodInvocation methodInvocation = createMock(MethodInvocation.class);
        Mockito.doAnswer(new Answer() {
            int callCount = 0;

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (callCount++ < 3) {
                    return queryProfilerAspectHelper.invoke(methodInvocation, QueryType.JAVASEC);
                } else {
                    return null;
                }
            }
        }).when(methodInvocation).proceed();
        Mockito.doReturn(method).when(methodInvocation).getMethod();

        queryProfilerAspectHelper.invoke(methodInvocation, QueryType.JAVASEC);
    }

    private static class TestClass {
        static Method method;

        static {
            Method method1;
            try {
                method1 = TestClass.class.getMethod("method");
            } catch (NoSuchMethodException e) {
                method1 = null;
            }
            method = method1;
        }

        public void method() {
        }

    }

}