package ru.yandex.market.wms.timetracker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.config.TtsTestConfig;

@SpringBootTest
@Import({
        TtsTestConfig.class,

})
@ActiveProfiles("test")
public class TimeTrackingSystemTest {

    @Test
    public void canInitContext() {
        Assertions.assertTrue(true);
    }
}
