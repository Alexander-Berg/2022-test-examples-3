package ru.yandex.market.wms.autostart.nonsort;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.autostartlogic.service.interfaces.WavingService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.wavetypes.WaveFlow;
import ru.yandex.market.wms.autostart.nonconveablewave.BaseNonSortAutostartLargeTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.enums.StartType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.annotation.DatabaseOperation.REFRESH;


class OversizeFilteringInWavingServiceTest extends BaseNonSortAutostartLargeTest {

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/nonsort/oversize-filter/before.xml")
    @ExpectedDatabase(value = "/fixtures/autostart/nonsort/oversize-filter/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void filterOutOrderWithOversizeFlagOff() {

        WavingService wavingService = buildWavingService();
        Optional<WaveFlow> waveFlow = wavingService.getWaveFlow(null);
        assertions.assertThat(waveFlow.isPresent()).isTrue();
        assertions.assertThat(waveFlow.get().getWaveSettings().getWaveType()).isEqualTo(WaveType.ALL);
        assertions.assertThat(waveFlow.get().getStartType()).isEqualTo(StartType.DEFAULT);
        assertions.assertThat(waveFlow.get().getOrders().size()).isEqualTo(4);
        assertions.assertThat(waveFlow.get().getNotSuitableOrders().size()).isZero();
        assertions.assertThat(waveFlow.get().getOversizeOrders().size()).isZero();
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/nonsort/oversize-filter/before.xml")
    @DatabaseSetup(value = "/fixtures/autostart/nonsort/oversize-filter/enabled.xml", type = REFRESH)
    @DatabaseSetup(value = "/fixtures/autostart/nonsort/consolidation-location/setup.xml", type = INSERT)
    void filterOutOrderWithOversizeFlagOn() {
        WavingService wavingService = buildWavingService(2);
        WaveFlow waveFlow = wavingService.getWaveFlow(null).orElseThrow();
        assertions.assertThat(waveFlow.getWaveSettings().getWaveType()).isEqualTo(WaveType.ALL);
        assertions.assertThat(waveFlow.getStartType()).isEqualTo(StartType.DEFAULT);
        assertions.assertThat(waveFlow.getOrders().stream().map(o -> o.getOrder().getOrderKey()))
                .containsExactlyInAnyOrder("001", "002");
        assertions.assertThat(waveFlow.getNotSuitableOrders().size()).isZero();
        assertions.assertThat(waveFlow.getOversizeOrders().size()).isEqualTo(2);
        assertions.assertThat(waveFlow.getOversizeOrders().stream().map(Order::getOrderKey))
                .containsExactlyInAnyOrder("003", "004");
    }
}
