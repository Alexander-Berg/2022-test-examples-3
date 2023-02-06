package ru.yandex.canvas.controllers;

import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.presets.PresetTag;
import ru.yandex.canvas.service.PresetsService;
import ru.yandex.canvas.service.SessionParams;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.canvas.service.SessionParams.SessionTag.CPM_BANNER;
import static ru.yandex.canvas.service.SessionParams.SessionTag.CPM_GEOPRODUCT;
import static ru.yandex.canvas.service.SessionParams.SessionTag.CPM_GEO_PIN;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class CreativesBatchesControllerMockTest {
    private static final Set<Integer> COMMON_PRESET_IDS = Set.of(1, 2, 3);
    private static final Set<Integer> CPM_GEOPRODUCT_PRESET_IDS = Set.of(4, 5, 6);
    private static final Set<Integer> CPM_GEO_PIN_PRESET_IDS = Set.of(7, 8, 9);
    private static final Map<PresetTag, Set<Integer>> PRESET_IDS_BY_TAG = ImmutableMap.of(
            PresetTag.COMMON, COMMON_PRESET_IDS,
            PresetTag.CPM_GEOPRODUCT, CPM_GEOPRODUCT_PRESET_IDS,
            PresetTag.CPM_GEO_PIN, CPM_GEO_PIN_PRESET_IDS);
    @Autowired
    SessionParams sessionParams;

    @MockBean
    PresetsService presetsService;

    @Autowired
    CreativesBatchesController controller;

    @Before
    public void setUp() throws Exception {
        when(presetsService.getPresetIdsByTag()).thenReturn(PRESET_IDS_BY_TAG);
    }

    @Test
    public void getPresetIds_emptySessionParams_returnCommon() {
        when(sessionParams.isPresent()).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(CPM_BANNER);
        when(sessionParams.sessionIs(CPM_BANNER)).thenReturn(true);

        Set<Integer> result = controller.getPresetIds();
        assertThat(result, Matchers.containsInAnyOrder(1, 2, 3, null));
    }

    @Test
    public void getPresetIds_cpmGeoproduct_returnCpmGeoproduct()
    {
        when(sessionParams.isPresent()).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(CPM_GEOPRODUCT);
        when(sessionParams.sessionIs(CPM_GEOPRODUCT)).thenReturn(true);

        Set<Integer> result = controller.getPresetIds();
        assertThat(result, is(CPM_GEOPRODUCT_PRESET_IDS));
    }

    @Test
    public void getPresetIds_cpmGeoPin_returnCpmGeoPin()
    {
        when(sessionParams.isPresent()).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(CPM_GEO_PIN);
        when(sessionParams.sessionIs(CPM_GEO_PIN)).thenReturn(true);

        Set<Integer> result = controller.getPresetIds();
        assertThat(result, is(CPM_GEO_PIN_PRESET_IDS));
    }
}
