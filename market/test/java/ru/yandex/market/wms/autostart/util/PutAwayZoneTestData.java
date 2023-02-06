package ru.yandex.market.wms.autostart.util;

import java.util.List;

import one.util.streamex.StreamEx;

import ru.yandex.market.wms.autostart.settings.repository.AutostartZoneSettingsRepository;

import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.listOf;

public interface PutAwayZoneTestData {
    String S01_ZONE = "S01_ZONE";
    String S02_ZONE = "S02_ZONE";
    String S03_ZONE = "S03_ZONE";
    String S04_ZONE = "S04_ZONE";
    String S05_ZONE = "S05_ZONE";

    static List<String> zones() {
        return listOf(
                S01_ZONE,
                S02_ZONE,
                S03_ZONE,
                S04_ZONE,
                S05_ZONE
        );
    }

    static String zonesJson() {
        return StreamEx.of(zones()).map(z -> "\"" + z + "\"").joining(",", "[", "]");
    }

    static AutostartZoneSettingsRepository.Record s01Settings() {
        return AutostartZoneSettingsRepository.Record.builder()
                .id(S01_ZONE)
                .maxWeightPerPickingOrder(100.1d)
                .maxVolumePerPickingOrder(0.6d)
                .maxItemsPerPickingOrder(80)
                .build();
    }

    static AutostartZoneSettingsRepository.Record s02Empty() {
        return AutostartZoneSettingsRepository.Record.builder()
                .id(S02_ZONE)
                .build();
    }

    static AutostartZoneSettingsRepository.Record s02Updated() {
        return AutostartZoneSettingsRepository.Record.builder()
                .id(S02_ZONE)
                .maxWeightPerPickingOrder(100.2d)
                .maxVolumePerPickingOrder(0.7d)
                .maxItemsPerPickingOrder(90)
                .build();
    }
}
