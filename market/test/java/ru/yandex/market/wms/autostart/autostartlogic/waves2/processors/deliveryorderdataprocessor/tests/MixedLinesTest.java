package ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.tests;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.DeliveryOrderDataProcessor;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.ParentTestConfiguration;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.TestConfigurations;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.WaveSettings;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.wavetypes.WaveFlow;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType;
import ru.yandex.market.wms.common.spring.enums.LinkedToDsType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.TestData.consLocationsLink;
import static ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config.TestData.makeOrders;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_AOS_NONSORT_OVERSIZE;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_AOS_NONSORT_SINGLES;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_NONSORT_OVERSIZE_LINE_LIMIT;
import static ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_NONSORT_SINGLE_LINE_LIMIT;

@SpringBootTest
public class MixedLinesTest extends ParentTestConfiguration {
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
                    .singleLines(List.of("S1", "S2"))
                    .oversizeLines(List.of("S3", "S4"))
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

    @Test
    public void testWaveTypeOversize() {
        Set<String> carrierCodes = Set.of("C1", "C2");
        WaveSettings waveSettings = WaveSettings.builder()
                .waveType(WaveType.SINGLE)
                .initTime(currentTime)
                .serverTime(currentTime)
                .maxItemsIntoWave(2)
                .minOrdersIntoPutwall(1)
                .maxOrdersIntoWave(2)
                .linkedToDsType(LinkedToDsType.TO_UNLINKED_DS)
                .build();

        WaveFlow actualWave = deliveryOrderDataProcessor.process(waveSettings,
                makeOrders(carrierCodes, currentTime, WaveType.SINGLE),
                List.of(consLocationsLink("C1", "S3", ConsolidationLocationType.OVERSIZE)));

        assertEquals(WaveType.SINGLE, actualWave.getWaveSettings().getWaveType());
        assertTrue(actualWave.isSuccessful());
        assertEquals(2, actualWave.getOrders().size());
        assertEquals(0, actualWave.getFilteredOrders().size());
    }
}
