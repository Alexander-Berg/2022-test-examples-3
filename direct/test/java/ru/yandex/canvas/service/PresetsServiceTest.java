package ru.yandex.canvas.service;

import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.canvas.model.presets.Preset;
import ru.yandex.canvas.model.presets.PresetSelectionCriteria;
import ru.yandex.canvas.model.presets.PresetTag;

import static java.util.Collections.emptySet;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.canvas.service.PresetsService.DEFAULT_BANNER_PRESET_IDS;

public class PresetsServiceTest {

    PresetsService presetsService;

    @Mock
    DirectService directService;

    @Before
    public void before() {
        initMocks(this);

        when(directService.getFeatures(any(), any()))
                .thenReturn(Set.of("only_adaptive_creatives"));

        presetsService = new PresetsService(new ObjectMapper(), directService);
    }

    @Test
    public void getListTestWithFeature() {
        when(directService.getFeatures(any(), any()))
                .thenReturn(Set.of("only_adaptive_creatives"));

        PresetSelectionCriteria presetSelectionCriteria = PresetSelectionCriteria.any(true);
        presetSelectionCriteria.getTags().add(PresetTag.COMMON);

        var answer = presetsService.getList(presetSelectionCriteria, 1L, null);

        assertTrue(answer
                .stream()
                .map(Preset::getId)
                .noneMatch(DEFAULT_BANNER_PRESET_IDS::contains));
    }

    @Test
    public void getListTestWithoutFeature() {
        when(directService.getFeatures(any(), any()))
                .thenReturn(emptySet());

        PresetSelectionCriteria presetSelectionCriteria = PresetSelectionCriteria.any(true);
        presetSelectionCriteria.getTags().add(PresetTag.COMMON);

        var answer = presetsService.getList(presetSelectionCriteria, 1L, null);

        assertTrue(answer
                .stream()
                .map(Preset::getId)
                .collect(Collectors.toList())
                .containsAll(DEFAULT_BANNER_PRESET_IDS));
    }
}
