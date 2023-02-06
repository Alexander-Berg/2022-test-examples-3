package ru.yandex.market.mbo.aop;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.mbo.configs.TestConfiguration;
import ru.yandex.market.request.trace.Module;

import java.util.function.Consumer;

import static ru.yandex.market.mbo.aop.Measure.COUNT;
import static ru.yandex.market.mbo.aop.Measure.EXCEPTION_COUNT;
import static ru.yandex.market.mbo.aop.Measure.TIMINGS;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class, GatherStatisticsAspectTest.TestClass.class})
@SuppressWarnings("checkstyle:magicNumber")
public class GatherStatisticsAspectTest {
    private static final String EVENT_NAME = "test";
    @Autowired
    private GatherStatisticsAspect aspect;

    @Autowired
    private TestClass testClass;

    @Before
    public void setUp() {
        aspect.onMicrometerPushAction = Mockito.mock(Consumer.class);
        aspect.onTsumPushAction = Mockito.mock(Consumer.class);
    }

    @Test
    public void micrometerTimedTest() {
        testClass.testMicrometer();
        Mockito.verify(aspect.onMicrometerPushAction).accept(EVENT_NAME + "_timings");
    }

    @Test
    public void micrometerSeveralObjectsTest() {
        testClass.testMicrometerSeveralObjects();
        Mockito.verify(aspect.onMicrometerPushAction).accept(EVENT_NAME + "_timings");
        Mockito.verify(aspect.onMicrometerPushAction).accept(EVENT_NAME + "_count");
    }

    @Test
    public void micrometerSeveralObjectsExceptionTest() {
        boolean exceptionRaised = false;

        try {
            testClass.testMicrometerSeveralObjectsWithException();
        } catch (IllegalStateException e) {
            exceptionRaised = true;
        }

        Assert.assertTrue(exceptionRaised);
        Mockito.verify(aspect.onMicrometerPushAction).accept(EVENT_NAME + "_count");
        Mockito.verify(aspect.onMicrometerPushAction).accept(EVENT_NAME + "_failure_count");
    }

    @Test
    public void tsumTimedTest() {
        testClass.testTsumTrace();

        Mockito.verify(aspect.onTsumPushAction).accept(EVENT_NAME);
    }

    @Test
    public void micrometerAndTsumTimedTest() {
        testClass.testMicrometerAndTsumTrace();
        Mockito.verify(aspect.onTsumPushAction).accept(EVENT_NAME);
        Mockito.verify(aspect.onMicrometerPushAction).accept(EVENT_NAME + "_timings");
    }

    @Component
    public static class TestClass {
        @MicrometerMeasured("test")
        void testMicrometer() {

        }

        @MicrometerMeasured(value = "test", measure = {EXCEPTION_COUNT, COUNT, TIMINGS})
        void testMicrometerSeveralObjects() {

        }

        @MicrometerMeasured(value = "test", measure = {EXCEPTION_COUNT, COUNT, TIMINGS})
        void testMicrometerSeveralObjectsWithException() {
            throw new IllegalStateException("");
        }

        @TsumTraceMeasured(module = Module.PGAAS, value = EVENT_NAME)
        void testTsumTrace() {

        }

        @MicrometerMeasured(EVENT_NAME)
        @TsumTraceMeasured(module = Module.PGAAS, value = EVENT_NAME)
        void testMicrometerAndTsumTrace() {

        }
    }
}
