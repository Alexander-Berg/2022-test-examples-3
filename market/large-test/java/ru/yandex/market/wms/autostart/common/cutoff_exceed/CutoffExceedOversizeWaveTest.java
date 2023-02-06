package ru.yandex.market.wms.autostart.common.cutoff_exceed;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.wms.autostart.autostartlogic.nonsort.AosWaveTypeStartSequenceProvider;
import ru.yandex.market.wms.autostart.autostartlogic.runner.AutostartLogicRunner;
import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;


@Import(AutostartLogicRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CutoffExceedOversizeWaveTest extends TestcontainersConfiguration {
    @Autowired
    private AutostartLogicRunner runner;

    @Autowired
    @MockBean
    private AosWaveTypeStartSequenceProvider startSequenceProvider;

    @BeforeEach
    void beforeEach() {
        Mockito.when(startSequenceProvider.getNext(null))
                .thenReturn(
                        new AosWaveTypeStartSequenceProvider.Mode(WaveType.SINGLE, false),
                        new AosWaveTypeStartSequenceProvider.Mode(WaveType.OVERSIZE, false),
                        new AosWaveTypeStartSequenceProvider.Mode(WaveType.ALL, false)
                );
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/oversize/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/with_popzones.xml", type = INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/oversize/combineds_on/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/common/cutoff_exceed/oversize/combineds_on/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testOversizeCombineDsOn() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/oversize/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/with_popzones.xml", type = INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/oversize/combineds_off-flag_on/before.xml",
            type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/common/cutoff_exceed/oversize/combineds_off-flag_on/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testOversizeCombineDsOffFlagOn() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/oversize/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/with_popzones.xml", type = INSERT)
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/oversize/combineds_off-flag_off/before.xml",
            type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/common/cutoff_exceed/oversize/combineds_off-flag_off/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testOversizeCombineDsOffFlagOff() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/oversize/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/common/cutoff_exceed/oversize/combineds_off-flag_on/before.xml",
            type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/common/cutoff_exceed/oversize/combineds_off-flag_on/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testOversizeCombineDsOffFlagOnNoPopzones() {
        runner.startAutoStart();
    }
}
