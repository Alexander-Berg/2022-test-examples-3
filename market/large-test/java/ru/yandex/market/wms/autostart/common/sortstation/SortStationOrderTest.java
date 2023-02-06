package ru.yandex.market.wms.autostart.common.sortstation;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.autostart.autostartlogic.runner.AutostartLogicRunner;
import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@Import(AutostartLogicRunner.class)
public class SortStationOrderTest extends TestcontainersConfiguration {
    @Autowired
    private AutostartLogicRunner runner;

    @Test
    @DatabaseSetup("/fixtures/autostart/common/sortstations/sortorder/before.xml")
    @ExpectedDatabase(value = "/fixtures/autostart/common/sortstations/sortorder/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testCorrectnessStationsAssigning() {
        runner.startAutoStart();
    }
}
