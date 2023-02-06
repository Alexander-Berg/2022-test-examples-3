package ru.yandex.market.wms.autostart.common.filters;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.autostart.autostartlogic.dao.WaveLogDao;
import ru.yandex.market.wms.autostart.autostartlogic.nonsort.AosWaveTypeStartSequenceProvider;
import ru.yandex.market.wms.autostart.autostartlogic.runner.AutostartLogicRunner;
import ru.yandex.market.wms.autostart.autostartlogic.service.interfaces.IDeliveryCutOffServiceShipmentDateTime;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.AutostartWavingService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.WaveInitialization;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.services.OrderCategorizator;
import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.wms.autostart.autostartlogic.nonsort.FilterOutReason.EMPTY_ORDER;


@Import(AutostartLogicRunner.class)
public class EmptyOrderTest extends TestcontainersConfiguration {
    @Autowired
    AosWaveTypeStartSequenceProvider waveTypeStartSequence;
    @Autowired
    IDeliveryCutOffServiceShipmentDateTime deliveryCutOffService;
    @Autowired
    OrderCategorizator orderCategorizator;
    @Autowired
    WaveInitialization waveInitialization;
    @Autowired
    WaveLogDao waveLogDao;

    @Test
    @DatabaseSetup("/fixtures/autostart/common/filters/before.xml")
    public void testCorrectnessStationsAssigning() {
        AutostartWavingService wavingService = new AutostartWavingService(waveTypeStartSequence, deliveryCutOffService,
                orderCategorizator, waveInitialization, waveLogDao);
        var waveFlow = wavingService.getWaveFlow(null).get();
        assertTrue(waveFlow.isSuccessful());
        assertEquals(1, waveFlow.getOrders().size());
        assertEquals(1, waveFlow.getFilteredOrders().size());
        var order = waveFlow.getOrders().iterator().next();
        var filteredOrder = waveFlow.getFilteredOrders(EMPTY_ORDER).get(0);
        assertEquals("ORDER1", order.getOrder().getOrderKey());
        assertEquals("ORDER2", filteredOrder.getOrderKey());
    }
}
