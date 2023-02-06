package ru.yandex.market.pers.tms;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.pers.grade.core.MockedTest;

@Import(PersTmsMockConfig.class)
@TestPropertySource({"classpath:/test-application.properties", "classpath:/test-custom-application.properties"})
public abstract class MockedPersTmsTest extends MockedTest {
    @Autowired
    ComplexMonitoring complexMonitoring;

    @Before
    public void resetMonitoring() throws NoSuchFieldException, IllegalAccessException {
        if (complexMonitoring == null) {
            return;
        }

        // ugh, so ugly
        Field field = ComplexMonitoring.class.getDeclaredField("units");
        field.setAccessible(true);
        ConcurrentHashMap<?, ?> units = (ConcurrentHashMap<?, ?>) field
            .get(complexMonitoring);
        units.clear();
    }
}
