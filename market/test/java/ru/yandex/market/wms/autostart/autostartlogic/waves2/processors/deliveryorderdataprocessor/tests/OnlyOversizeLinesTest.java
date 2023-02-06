package ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.tests;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.wms.autostart.autostartlogic.nonsort.FilterOutReason;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.WaveFailReasonBuilder;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.DeliveryOrderDataProcessor;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.ParentTestConfiguration;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.TestConfigurations;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.WaveSettings;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.wavetypes.WaveFlow;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType;
import ru.yandex.market.wms.common.spring.enums.LinkedToDsType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.TestData.consLocationsLink;
import static ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.TestData.makeOrders;
import static ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.TestData.sortStationLink;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_AOS_NONSORT_OVERSIZE;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_AOS_NONSORT_SINGLES;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_NONSORT_OVERSIZE_LINE_LIMIT;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_NONSORT_SINGLE_LINE_LIMIT;

@SpringBootTest
public class OnlyOversizeLinesTest extends ParentTestConfiguration {
    @Autowired
    private DeliveryOrderDataProcessor deliveryOrderDataProcessor;

    @TestConfiguration
    public static class LocalConfiguration {
        @Mock
        private DbConfigService dbConfigService;

        @Bean
        @Primary
        public TestConfigurations.Properties properties() {
            return TestConfigurations.Properties.builder()
                    .oversizeLines(List.of("S1", "S2"))
                    .build();
        }

        @Bean
        @Primary
        public DbConfigService dbConfigService() {
            MockitoAnnotations.openMocks(this);
            when(dbConfigService.getConfigAsBoolean(YM_AOS_NONSORT_SINGLES, false)).thenReturn(true);
            when(dbConfigService.getConfigAsBoolean(YM_AOS_NONSORT_OVERSIZE, false)).thenReturn(true);
            when(dbConfigService.getConfigAsInteger(YM_NONSORT_SINGLE_LINE_LIMIT, 50)).thenReturn(50);
            when(dbConfigService.getConfigAsInteger(YM_NONSORT_OVERSIZE_LINE_LIMIT, 50)).thenReturn(50);
            return dbConfigService;
        }
    }

    @ParameterizedTest
    @MethodSource("modeList")
    public void testWaveTypeAll(LinkedToDsType type) {
        Set<String> carrierCodes = Set.of("C1", "C2");
        WaveSettings waveSettings = WaveSettings.builder()
                .waveType(WaveType.ALL)
                .initTime(currentTime)
                .serverTime(currentTime)
                .maxItemsIntoWave(2)
                .minOrdersIntoPutwall(1)
                .maxOrdersIntoWave(2)
                .linkedToDsType(type)
                .build();

        WaveFlow actualWave = deliveryOrderDataProcessor.process(waveSettings,
                makeOrders(carrierCodes, currentTime, WaveType.ALL),
                List.of(sortStationLink("C1", "S1")));

        assertEquals(WaveType.ALL, actualWave.getWaveSettings().getWaveType());
        assertFalse(actualWave.isSuccessful());
        assertEquals(WaveFailReasonBuilder.Type.ALL_STATIONS_OCCUPIED_OR_DISABLED,
                actualWave.getFailureReason().getType());
        assertEquals(0, actualWave.getOrders().size());
        assertEquals(0, actualWave.getFilteredOrders().size());
        assertEquals(Set.of(), actualWave.getCandidateStations());
    }

    @ParameterizedTest
    @MethodSource("modeList")
    public void testWaveTypeSingle(LinkedToDsType type) {
        Set<String> carrierCodes = Set.of("C1", "C2");
        WaveSettings waveSettings = WaveSettings.builder()
                .waveType(WaveType.SINGLE)
                .initTime(currentTime)
                .serverTime(currentTime)
                .maxItemsIntoWave(2)
                .minOrdersIntoPutwall(1)
                .maxOrdersIntoWave(2)
                .linkedToDsType(type)
                .build();

        WaveFlow actualWave = deliveryOrderDataProcessor.process(waveSettings,
                makeOrders(carrierCodes, currentTime, WaveType.SINGLE),
                List.of(sortStationLink("C1", "S1")));

        assertEquals(WaveType.SINGLE, actualWave.getWaveSettings().getWaveType());
        assertFalse(actualWave.isSuccessful());
        assertEquals(WaveFailReasonBuilder.Type.ALL_CONSOLIDATION_LINES_OCCUPIED,
                actualWave.getFailureReason().getType());
        assertEquals(0, actualWave.getOrders().size());
        assertEquals(0, actualWave.getFilteredOrders().size());
    }

    @Test
    public void testWaveTypeOversizeNoLink() {
        Set<String> carrierCodes = Set.of("C1", "C2");
        WaveSettings waveSettings = WaveSettings.builder()
                .waveType(WaveType.OVERSIZE)
                .initTime(currentTime)
                .serverTime(currentTime)
                .maxItemsIntoWave(2)
                .minOrdersIntoPutwall(1)
                .maxOrdersIntoWave(2)
                .linkedToDsType(LinkedToDsType.NO_LINK_TO_DS)
                .build();

        WaveFlow actualWave = deliveryOrderDataProcessor.process(waveSettings,
                makeOrders(carrierCodes, currentTime, WaveType.OVERSIZE),
                List.of(sortStationLink("C1", "S1")));

        assertEquals(WaveType.OVERSIZE, actualWave.getWaveSettings().getWaveType());
        assertTrue(actualWave.isSuccessful());
        assertEquals(2, actualWave.getOrders().size());
        assertEquals(0, actualWave.getFilteredOrders().size());
    }

    @Test
    public void testWaveTypeOversizeToUnlinked() {
        Set<String> carrierCodes = Set.of("C1", "C2");
        WaveSettings waveSettings = WaveSettings.builder()
                .waveType(WaveType.OVERSIZE)
                .initTime(currentTime)
                .serverTime(currentTime)
                .maxItemsIntoWave(2)
                .minOrdersIntoPutwall(1)
                .maxOrdersIntoWave(2)
                .linkedToDsType(LinkedToDsType.TO_UNLINKED_DS)
                .build();

        WaveFlow actualWave = deliveryOrderDataProcessor.process(waveSettings,
                makeOrders(carrierCodes, currentTime, WaveType.OVERSIZE),
                List.of(consLocationsLink("C1", "S1", ConsolidationLocationType.OVERSIZE)));
        Order filtered = actualWave.getFilteredOrders().iterator().next().getOrder();

        assertEquals(WaveType.OVERSIZE, actualWave.getWaveSettings().getWaveType());
        assertTrue(actualWave.isSuccessful());
        assertEquals(1, actualWave.getOrders().size());
        assertEquals(1, actualWave.getFilteredOrders().size());
        assertEquals("C1", filtered.getCarrierCode());
        assertTrue(actualWave.getGroupOrders().stream().anyMatch(
                dsc -> dsc.getFilteredOrders(FilterOutReason.ORDER_LINKED_TO_DS).size() == 1));
    }

    @Test
    public void testWaveTypeOversizeToLinked() {
        Set<String> carrierCodes = Set.of("C1", "C2");
        WaveSettings waveSettings = WaveSettings.builder()
                .waveType(WaveType.OVERSIZE)
                .initTime(currentTime)
                .serverTime(currentTime)
                .maxItemsIntoWave(2)
                .minOrdersIntoPutwall(1)
                .maxOrdersIntoWave(2)
                .linkedToDsType(LinkedToDsType.TO_LINKED_DS)
                .build();

        WaveFlow actualWave = deliveryOrderDataProcessor.process(waveSettings,
                makeOrders(carrierCodes, currentTime, WaveType.OVERSIZE),
                List.of(consLocationsLink("C1", "S1", ConsolidationLocationType.OVERSIZE)));
        Order filtered = actualWave.getFilteredOrders().iterator().next().getOrder();

        assertEquals(WaveType.OVERSIZE, actualWave.getWaveSettings().getWaveType());
        assertTrue(actualWave.isSuccessful());
        assertEquals(1, actualWave.getOrders().size());
        assertEquals(1, actualWave.getFilteredOrders().size());
        assertEquals("C2", filtered.getCarrierCode());
        assertTrue(actualWave.getGroupOrders().stream().anyMatch(
                dsc -> dsc.getFilteredOrders(FilterOutReason.ORDER_NOT_LINKED_TO_DS).size() == 1));
    }
}
