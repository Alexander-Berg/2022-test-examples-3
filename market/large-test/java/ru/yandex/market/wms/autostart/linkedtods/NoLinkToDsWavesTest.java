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
public class NoLinkToDsWavesTest extends TestcontainersConfiguration {
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
    @DatabaseSetup(value = "/fixtures/autostart/linkedtods/all-nolink/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/linkedtods/all-nolink/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testAllNolink() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/linkedtods/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/linkedtods/single-nolink/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/linkedtods/single-nolink/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testSingleNolink() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/linkedtods/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/linkedtods/oversize-nolink/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/linkedtods/oversize-nolink/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testOversizeNolink() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup("/fixtures/autostart/linkedtods/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/linkedtods/hobbit-nolink/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/linkedtods/hobbit-nolink/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testHobbitNolink() {
        runner.startAutoStart();
    }
}
