package ru.yandex.market.wms.autostart.autostartlogic.waves2.services;


import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStation;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.WaveSettings;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.settings.WaveSettingsCreator;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.wavetypes.WaveFlow;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.wavetypes.WaveFlowFactory;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.enums.LinkedToDsType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

public class CandidateStationServiceTest extends IntegrationTest {

    @Autowired
    private CandidateStationService service;

    @Autowired
    private WaveSettingsCreator waveSettingsCreator;

    @Test
    @DatabaseSetup("/service/candidate-station/data.xml")
    public void testDefaultGetStations() {
        WaveFlow flow = WaveFlowFactory.build(WaveSettings.builder()
                        .waveType(WaveType.ALL)
                        .build(),
                Collections.emptyList());
        var stations = service.getCandidateStations(flow);
        assertions.assertThat(stations.stream().map(CandidateSortStation::getName))
                .containsExactlyInAnyOrder("S03", "S04", "S05");
    }

    @ParameterizedTest
    @DatabaseSetup("/service/candidate-station/data.xml")
    @DatabaseSetup("/service/candidate-station/hold-enabled.xml")
    @MethodSource("noLinksList")
    public void testDefaultGetStationsWithActiveEmptyWavesOn(LinkedToDsType linkType) {
        var settings = waveSettingsCreator.buildWaveSettings(WaveType.ALL, linkType, null)
                .toBuilder()
                .activeBatchesPerPutwall(2)
                .build();
        WaveFlow flow = WaveFlowFactory.build(settings, List.of());
        var stations = service.getCandidateStations(flow);
        assertions.assertThat(stations.stream().map(CandidateSortStation::getName))
                .containsExactlyInAnyOrder("S05");
    }

    @Test
    @DatabaseSetup("/service/candidate-station/data.xml")
    public void testDefaultGetStationsWithActiveEmptyWavesOnWrongLink() {

        var settings = waveSettingsCreator.buildWaveSettings(WaveType.ALL, LinkedToDsType.TO_LINKED_DS, null)
                .toBuilder()
                .activeBatchesPerPutwall(2)
                .build();
        WaveFlow flow = WaveFlowFactory.build(settings, List.of());
        var stations = service.getCandidateStations(flow);
        assertions.assertThat(stations.stream().map(CandidateSortStation::getName))
                .containsExactlyInAnyOrder("S03", "S04", "S05");
    }

    public static List<LinkedToDsType> noLinksList() {
        return List.of(LinkedToDsType.NO_LINK_TO_DS, LinkedToDsType.TO_UNLINKED_DS);
    }
}
