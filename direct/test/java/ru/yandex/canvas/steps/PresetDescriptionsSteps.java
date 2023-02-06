package ru.yandex.canvas.steps;

import java.util.Collections;

import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.PresetTag;
import ru.yandex.canvas.service.video.presets.PresetTheme;

public class PresetDescriptionsSteps {
    public static PresetDescription leastPresetDescription() {
        return leastPresetDescription(123L);
    }

    public static PresetDescription leastPresetDescription(Long presetId) {
        PresetDescription presetDescription = new PresetDescription(presetId);
        presetDescription.setPresetTheme(PresetTheme.EMPTY);
        presetDescription.setPresetName("Preset empty name");
        presetDescription.setTags(Collections.singletonList(PresetTag.CPC));
        presetDescription.setSkipOffset(5L);
        presetDescription.setThumbnail("preset_5_thumbnail");
        presetDescription.setAllowRecentVideoSource(true);
        presetDescription.setAllowStockVideo(false);

        return presetDescription;
    }
}
