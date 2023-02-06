package ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.tests;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.wms.autostart.autostartlogic.waves2.WaveFailReasonBuilder;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.DeliveryOrderDataProcessor;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.ParentTestConfiguration;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.TestConfigurations;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.WaveSettings;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.wavetypes.WaveFlow;
import ru.yandex.market.wms.autostart.model.entity.StationToCarrier;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType;
import ru.yandex.market.wms.common.spring.enums.LinkedToDsType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.TestData.consLocationsLink;
import static ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.TestData.makeOrders;
import static ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.TestData.sortStationLink;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_AOS_NONSORT_OVERSIZE;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_AOS_NONSORT_SINGLES;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_NONSORT_OVERSIZE_LINE_LIMIT;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_NONSORT_SINGLE_LINE_LIMIT;

@SpringBootTest
public class NoStationsTest extends ParentTestConfiguration {
    @Autowired
    private DeliveryOrderDataProcessor deliveryOrderDataProcessor;

    @TestConfiguration
    public static class LocalConfiguration {
        @Mock
        private DbConfigService dbConfigService;

        @Bean
        @Primary
        public TestConfigurations.Properties properties() {
            return TestConfigurations.Properties.builder().build();
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

    private static List<StationToCarrier> allStationLinks = Stream.concat(
            Stream.of(sortStationLink("C1", "S1"), sortStationLink("C2", "S2")),
            Stream.of(consLocationsLink("C1", "SINGL1", ConsolidationLocationType.SINGLES),
                    consLocationsLink("C2", "SINGL2", ConsolidationLocationType.SINGLES)))
            .toList();

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
                allStationLinks);

        assertEquals(WaveType.ALL, actualWave.getWaveSettings().getWaveType());
        assertFalse(actualWave.isSuccessful());
        assertEquals(WaveFailReasonBuilder.Type.ALL_STATIONS_OCCUPIED_OR_DISABLED,
                actualWave.getFailureReason().getType());
        assertEquals(0, actualWave.getOrders().size());
        assertEquals(0, actualWave.getFilteredOrders().size());
        assertEquals(0, actualWave.getCandidateStations().size());
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
                allStationLinks);

        assertEquals(WaveType.SINGLE, actualWave.getWaveSettings().getWaveType());
        assertFalse(actualWave.isSuccessful());
        assertEquals(WaveFailReasonBuilder.Type.ALL_CONSOLIDATION_LINES_OCCUPIED,
                actualWave.getFailureReason().getType());
        assertEquals(0, actualWave.getOrders().size());
        assertEquals(0, actualWave.getFilteredOrders().size());
    }

    @ParameterizedTest
    @MethodSource("modeList")
    public void testWaveTypeOversize(LinkedToDsType type) {
        Set<String> carrierCodes = Set.of("C1", "C2");
        WaveSettings waveSettings = WaveSettings.builder()
                .waveType(WaveType.OVERSIZE)
                .initTime(currentTime)
                .serverTime(currentTime)
                .maxItemsIntoWave(2)
                .minOrdersIntoPutwall(1)
                .maxOrdersIntoWave(2)
                .linkedToDsType(type)
                .build();

        WaveFlow actualWave = deliveryOrderDataProcessor.process(waveSettings,
                makeOrders(carrierCodes, currentTime, WaveType.OVERSIZE),
                allStationLinks);

        assertEquals(WaveType.OVERSIZE, actualWave.getWaveSettings().getWaveType());
        assertFalse(actualWave.isSuccessful());
        assertEquals(WaveFailReasonBuilder.Type.ALL_CONSOLIDATION_LINES_OCCUPIED,
                actualWave.getFailureReason().getType());
        assertEquals(0, actualWave.getOrders().size());
        assertEquals(0, actualWave.getFilteredOrders().size());
    }
}
