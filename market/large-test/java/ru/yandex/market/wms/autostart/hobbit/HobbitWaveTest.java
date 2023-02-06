package ru.yandex.market.wms.autostart.hobbit;

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
public class HobbitWaveTest extends TestcontainersConfiguration {
    @Autowired
    private AutostartLogicRunner runner;

    @Autowired
    @MockBean
    private AosWaveTypeStartSequenceProvider startSequenceProvider;

    @BeforeEach
    void beforeEach() {
        Mockito.when(startSequenceProvider.getNext(null))
                .thenReturn(
                        new AosWaveTypeStartSequenceProvider.Mode(WaveType.HOBBIT, false),
                        new AosWaveTypeStartSequenceProvider.Mode(WaveType.ALL, false)
                );
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/hobbit/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/hobbit/default/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/hobbit/default/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void testDefault() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/hobbit/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/hobbit/priority/before.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/hobbit/priority/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void testPriority() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/hobbit/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/hobbit/occupiedstations/before-to-all.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/hobbit/occupiedstations/after-to-all.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testOccupiedStationsToAll() {
        runner.startAutoStart();
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/hobbit/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/hobbit/occupiedstations/before-to-none.xml", type = INSERT)
    @ExpectedDatabase(value = "/fixtures/autostart/hobbit/occupiedstations/after-to-none.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testOccupiedStationsToNone() {
        runner.startAutoStart();
    }
}
