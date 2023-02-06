package ru.yandex.market.wms.autostart.common.cutoff_exceed;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.autostart.autostartlogic.runner.AutostartLogicRunner;
import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@Import(AutostartLogicRunner.class)
public class CutoffExceedAllWaveTest extends TestcontainersConfiguration {
    @Autowired
    private AutostartLogicRunner runner;

    @Test
    @DatabaseSetup("/fixtures/autostart/common/cutoff_exceed/all/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/with_popzones.xml", type = INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/all/combineds_on/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/common/cutoff_exceed/all/combineds_on/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testAllCombineDsOn() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/common/cutoff_exceed/all/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/with_popzones.xml", type = INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/all/combineds_off-flag_on/before.xml",
            type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/common/cutoff_exceed/all/combineds_off-flag_on/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testAllCombineDsOffFlagOn() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/common/cutoff_exceed/all/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/with_popzones.xml", type = INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/all/combineds_off-flag_off/before.xml",
            type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/common/cutoff_exceed/all/combineds_off-flag_off/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testAllCombineDsOffFlagOff() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/common/cutoff_exceed/all/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/all/combineds_off-flag_on/before.xml",
            type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/common/cutoff_exceed/all/combineds_off-flag_on/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testAllCombineDsOffFlagOnNoPopzones() {
        runner.startAutoStart();
    }
}
