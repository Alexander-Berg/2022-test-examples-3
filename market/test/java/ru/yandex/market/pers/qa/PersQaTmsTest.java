package ru.yandex.market.pers.qa;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.pers.qa.config.CoreConfig;
import ru.yandex.market.pers.qa.config.InternalTmsConfig;
import ru.yandex.market.pers.qa.mock.PersQaTmsMockFactory;
import ru.yandex.market.pers.qa.mock.TmsMockConfiguration;

/**
 * @author korolyov
 * 20.06.18
 */

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    TmsMockConfiguration.class,
    CoreConfig.class,
    InternalTmsConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:/test-application.properties")
public class PersQaTmsTest extends PersQACoreTest {

    public static final String SECURITY_DATA_HEADERS = "{\n" +
        "    \"x-yandex-icookie\": \"icookie\"\n" +
        "  }";

    @Autowired
    private ComplexMonitoring complicatedMonitoring;

    @BeforeEach
    public void resetMonitoring() throws NoSuchFieldException, IllegalAccessException {
        if (complicatedMonitoring == null) {
            return;
        }

        // ugh, so ugly (c) ilyakis@
        Field field = ComplexMonitoring.class.getDeclaredField("units");
        field.setAccessible(true);
        ConcurrentHashMap<?, ?> units = (ConcurrentHashMap<?, ?>) field
            .get(complicatedMonitoring);
        units.clear();
    }

    @Override
    protected void resetMocks() {
        PersQaServiceMockFactory.resetMocks();
        PersQaTmsMockFactory.resetMocks();
    }
}
