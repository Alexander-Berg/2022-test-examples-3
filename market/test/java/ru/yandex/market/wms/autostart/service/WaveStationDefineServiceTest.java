package ru.yandex.market.wms.autostart.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;


public class WaveStationDefineServiceTest extends IntegrationTest {

    @Autowired
    WaveStationDefineService service;

    @Test
    @DatabaseSetup("/fixtures/autostart/service/station-assign/before.xml")
    @ExpectedDatabase(value = "/fixtures/autostart/service/station-assign/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void happyAssignTest() {
        service.defineStationForWave("WAVE-001", "user1");
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/service/station-assign/before-2.xml")
    @ExpectedDatabase(value = "/fixtures/autostart/service/station-assign/after-2.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void happyAssignNotLinkedTest() {
        service.defineStationForWave("WAVE-001", "user1");
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/service/station-assign/before-1.xml")
    @ExpectedDatabase(value = "/fixtures/autostart/service/station-assign/before-1.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void invalidStatusAssignTest() {

        assertions.assertThat(service.defineStationForWave("WAVE-001", "user1")).isEqualTo("S00");
        assertions.assertThat(service.defineStationForWave("WAVE-002", "user1")).isEqualTo("S01");
        assertions.assertThatThrownBy(() -> service.defineStationForWave("WAVE-003", "user1"))
                .hasMessage("400 BAD_REQUEST \"Нельзя назначить станцию волне WAVE-003 в статусе 7\"");
        assertions.assertThatThrownBy(() -> service.defineStationForWave("No wave", "user1"))
                .hasMessage("Волна No wave не найдена");
    }
}
