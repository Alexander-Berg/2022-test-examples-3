package ru.yandex.market.wms.autostart.linkedtods;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.autostart.autostartlogic.nonsort.AosWaveTypeStartSequenceProvider;
import ru.yandex.market.wms.autostart.autostartlogic.runner.AutostartLogicRunner;
import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@Import(AutostartLogicRunner.class)
public class ToUnlinkedDsWavesTest extends TestcontainersConfiguration {
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
                        new AosWaveTypeStartSequenceProvider.Mode(WaveType.HOBBIT, false),
                        new AosWaveTypeStartSequenceProvider.Mode(WaveType.ALL, false)
                );
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/linkedtods/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/linkedtods/all-unlinked/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/linkedtods/all-unlinked/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testAllUnlinked() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/linkedtods/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/linkedtods/single-unlinked/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/linkedtods/single-unlinked/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testSingleUnlinked() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/linkedtods/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/linkedtods/oversize-unlinked/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/linkedtods/oversize-unlinked/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testOversizeUnlinked() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/linkedtods/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/linkedtods/hobbit-unlinked/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/linkedtods/hobbit-unlinked/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testHobbitUnlinked() {
        runner.startAutoStart();
    }
}
