package ru.yandex.market.wms.autostart.oversize;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.autostart.autostartlogic.nonsort.AosWaveTypeStartSequenceProvider;
import ru.yandex.market.wms.autostart.autostartlogic.nonsort.AosWaveTypeStartSequenceProvider.Mode;
import ru.yandex.market.wms.autostart.autostartlogic.runner.AutostartLogicRunner;
import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static com.github.springtestdbunit.annotation.DatabaseOperation.REFRESH;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@Import(AutostartLogicRunner.class)
public class OversizeTest extends TestcontainersConfiguration {

    @Autowired
    private AutostartLogicRunner runner;

    @Autowired
    @MockBean
    private AosWaveTypeStartSequenceProvider startSequenceProvider;

    @BeforeEach
    void beforeEach() {
        Mockito.when(startSequenceProvider.getNext(null))
                .thenReturn(new Mode(WaveType.OVERSIZE, false), new Mode(WaveType.ALL, false));
    }

    /**
     * Один заказ с одним товаром.
     * Должна запуститься волна с этим заказом.
     */
    @Test
    @DatabaseSetup("/fixtures/autostart/oversize/base_setup.xml")
    @DatabaseSetup("/fixtures/autostart/oversize/one-item/before.xml")
    @ExpectedDatabase(value = "/fixtures/autostart/oversize/one-item/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void createOneItemWave() {
        runner.startAutoStart();
    }

    /**
     * Один заказ с двумя товароми.
     * Должна запуститься волна с этим заказом.
     */
    @Test
    @DatabaseSetup("/fixtures/autostart/oversize/base_setup.xml")
    @DatabaseSetup("/fixtures/autostart/oversize/two-items/before.xml")
    @ExpectedDatabase(value = "/fixtures/autostart/oversize/two-items/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void createTwoItemsWave() {
        runner.startAutoStart();
    }

    /**
     * Один заказ с двумя товароми как в предудущем тесте, но однопосылочная СД.
     * Волна не должна запуститься.
     */
    @Test
    @DatabaseSetup("/fixtures/autostart/oversize/base_setup.xml")
    @DatabaseSetup("/fixtures/autostart/oversize/two-items/before.xml")
    @DatabaseSetup(value = "/fixtures/autostart/oversize/two-items/single-packing-ds.xml", type = REFRESH)
    @ExpectedDatabase(value = "/fixtures/autostart/oversize/two-items/before.xml", assertionMode = NON_STRICT_UNORDERED)
    void doNotCreateTwoItemsWave() {
        runner.startAutoStart();
    }

    /**
     * Один заказ с несколькими одинаковыми товарами (5 шт), которые на стоке в одной зоне.
     * Макс. размер назначения - 2, должно получиться 3 назначения (2+2+1).
     */
    @Test
    @DatabaseSetup("/fixtures/autostart/oversize/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/oversize/few-ass-by-ass-size/before.xml", type = REFRESH)
    @ExpectedDatabase(value = "/fixtures/autostart/oversize/few-ass-by-ass-size/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createWaveWithFewAssignmentsByAssignmentSize() {
        runner.startAutoStart();
    }

    /**
     * Один заказ с двумя разными товарами, которые на стоке в одной зоне.
     * Макс. размер назначения - 1, должно получиться 2 назначения (1+1).
     */
    @Test
    @DatabaseSetup("/fixtures/autostart/oversize/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/oversize/few-ass-by-ass-size2/before.xml", type = REFRESH)
    @ExpectedDatabase(value = "/fixtures/autostart/oversize/few-ass-by-ass-size2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createWaveWithFewAssignmentsByAssignmentSize2() {
        runner.startAutoStart();
    }

    /**
     * Один заказ с несколькими одинаковыми товарами (7 шт), которые на стоке в разных зонах по 3 шт.
     * Макс. размер назначения - 3, должно получиться 3 назначения (3+3+1).
     */
    @Test
    @DatabaseSetup("/fixtures/autostart/oversize/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/oversize/few-ass-by-zone/before.xml", type = REFRESH)
    @ExpectedDatabase(value = "/fixtures/autostart/oversize/few-ass-by-zone/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createWaveWithFewAssignmentsByZone() {
        runner.startAutoStart();
    }

    /**
     * 4 сингл-заказа с одинаковыми товарами, которые на стоке в одной зоне
     * Макс. размер волны - 2, должно получиться 2 волны по 2 заказа.
     */
    @Test
    @DatabaseSetup("/fixtures/autostart/oversize/base_setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/oversize/few-full-waves/before.xml", type = REFRESH)
    @ExpectedDatabase(value = "/fixtures/autostart/oversize/few-full-waves/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createFewFullWaves() {
        runner.startAutoStart();
    }
}
