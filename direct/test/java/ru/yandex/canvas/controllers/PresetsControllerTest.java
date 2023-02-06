package ru.yandex.canvas.controllers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.common.collect.Ordering;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.elements.Element;
import ru.yandex.canvas.model.presets.Preset;
import ru.yandex.canvas.model.presets.PresetItem;
import ru.yandex.canvas.model.presets.PresetTag;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.SessionParams;

import static java.util.Collections.singleton;
import static java.util.Comparator.reverseOrder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author skirsanov
 */
@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PresetsControllerTest {
    private static final long USER_ID = 1;
    private static final long CLIENT_ID = 2;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PresetsController presetsController;
    @Autowired
    private SessionParams sessionParams;
    @Autowired
    private DirectService directService;

    @Before
    public void setUp() {

        Mockito.clearInvocations(sessionParams);
        Mockito.reset(sessionParams);

        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.TEXT);
        when(sessionParams.sessionIs(SessionParams.SessionTag.TEXT)).thenReturn(true);
        when(sessionParams.getPresetIds()).thenReturn(null);

        when(directService.getFeatures(any(), any())).thenReturn(Collections.emptySet());
    }

    private static void assertPresetItemsSorted(final Preset preset) {
        assertTrue(Ordering.from(Comparator.comparing(PresetItem::goesFirst, reverseOrder())
                .thenComparing(PresetItem::getWidth)
                .thenComparing(PresetItem::getHeight))
                .isOrdered(preset.getItems()));
    }

    @Test
    public void testSmoke() throws Exception {
        ResultActions presetsResult = this.mockMvc.perform(get("/presets")
                .param("client_id", String.valueOf(CLIENT_ID))
                .param("user_id", String.valueOf(USER_ID))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(e -> System.err.println("!!" + e.getResponse().getContentAsString()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.total", Matchers.equalTo(4)))
                .andExpect(jsonPath("$.items", Matchers.hasSize(4)))
                .andExpect(jsonPath("$.items[*]",
                        everyItem(allOf(hasKey("id"), hasKey("name"), /*hasKey("thumbnail"),*/ hasKey("items")))));

        JSONArray responseArray =
                new JSONObject(presetsResult.andReturn().getResponse().getContentAsString()).getJSONArray("items");

        for (int i = 0; i < responseArray.length(); i++) {

            int presetId = responseArray.getJSONObject(i).getInt("id");

            this.mockMvc.perform(get("/presets/" + presetId)
                    .param("client_id", String.valueOf(CLIENT_ID))
                    .param("user_id", String.valueOf(USER_ID))
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(jsonPath("$.id", is(presetId)));
        }

        int nonExistingPresetId = Integer.MAX_VALUE;

        this.mockMvc.perform(get("/presets/" + nonExistingPresetId)
                .param("client_id", String.valueOf(CLIENT_ID))
                .param("user_id", String.valueOf(USER_ID))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testPresetsSorting() {

        final List<Preset> presets = presetsController.getList().getItems();

        // check that presets are sorted by Id
        Assert.assertTrue(Ordering.from(Comparator.comparing(Preset::getId)).isOrdered(presets));

        /* check that presetsItems in each preset from GET /presets and GET /presets/id
         * are sorted by width and height with (300, 250) on first position
         */
        presets.forEach(preset -> {
            assertPresetItemsSorted(presetsController.getById(preset.getId()));
            assertPresetItemsSorted(preset);
        });
    }

    @Test
    public void test_getList_PresetsFiltering() {
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_GEOPRODUCT);
        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_GEOPRODUCT)).thenReturn(true);

        List<Preset> presets = presetsController.getList().getItems();
        presets.forEach(p -> assertEquals(singleton(PresetTag.CPM_GEOPRODUCT), p.getTags()));
    }

    @Test
    public void test_getId_PresetsFiltering() {

        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_GEOPRODUCT);
        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_GEOPRODUCT)).thenReturn(true);

        Preset preset = presetsController.getById(10);
        assertEquals(singleton(PresetTag.CPM_GEOPRODUCT), preset.getTags());
    }

    @Test
    public void test_getTwoDifferentLocales() {
        setLocale(new Locale("ru"));
        List<Preset> presetsRu = presetsController.getList().getItems();
        setLocale(new Locale("en"));
        List<Preset> presetsEn = presetsController.getList().getItems();

        StreamEx.zip(presetsRu, presetsEn, ImmutablePair::new)
                .forEach(pair -> {
                    Preset ru = pair.getLeft();
                    Preset en = pair.getRight();
                    assertEquals(ru.getId(), en.getId()); // Sorting
                    assertEquals(ru.getName(), en.getName());
                    assertEquals(ru.getItems().size(), en.getItems().size());
                    for (int i = 0; i < ru.getItems().size(); i++) {
                        compareElementTexts(ru, en, i);
                    }
                });
    }

    public void compareElementTexts(Preset ru, Preset en, int i) {
        List<Element> ruElements = ru.getItems().get(i).getElements();
        List<Element> enElements = en.getItems().get(i).getElements();
        for (int j = 0; j < ruElements.size(); j++) {
            Element ruElement = ruElements.get(j);
            Element enElement = enElements.get(j);
            if (ruElement.getOptions() instanceof Element.ColoredTextOptions) {
                assertEquals(ruElement.getType(), enElement.getType());
                Element.ColoredTextOptions ruOptions = (Element.ColoredTextOptions) ruElement.getOptions();
                Element.ColoredTextOptions enOptions = (Element.ColoredTextOptions) enElement.getOptions();
                if (ruOptions.getPlaceholder() == null) {
                    assertNull(enOptions.getPlaceholder());
                } else {
                    assertNotEquals(ruOptions.getPlaceholder(), enOptions.getPlaceholder());
                }
            }
        }
    }

    @Test
    public void testInbanner() throws Exception {
        when(directService.getFeatures(any(), any())).thenReturn(Set.of("in_banner_creatives_support"));
        when(sessionParams.getInBannerFlag()).thenReturn(null);
        ResultActions presetsResult = this.mockMvc.perform(get("/presets")
                        .param("client_id", String.valueOf(CLIENT_ID))
                        .param("user_id", String.valueOf(USER_ID))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(e -> System.err.println("!!" + e.getResponse().getContentAsString()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.total", Matchers.equalTo(11)))
                .andExpect(jsonPath("$.items", Matchers.hasSize(11)));
    }

    @Test
    public void testInbannerSorting() throws Exception {
        when(directService.getFeatures(any(), any())).thenReturn(Set.of("in_banner_creatives_support"));
        when(sessionParams.getInBannerFlag()).thenReturn(true);
        ResultActions presetsResult = this.mockMvc.perform(get("/presets")
                        .param("client_id", String.valueOf(CLIENT_ID))
                        .param("user_id", String.valueOf(USER_ID))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(e -> System.err.println("!!" + e.getResponse().getContentAsString()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.total", Matchers.equalTo(11)))
                .andExpect(jsonPath("$.items", Matchers.hasSize(11)));

        /*Отдавать отсортированный список шаблонов:
        сначала шаблоны инбаннеров (7 шт)
        затем стандартные шаблоны*/
        JSONArray responseArray =
                new JSONObject(presetsResult.andReturn().getResponse().getContentAsString()).getJSONArray("items");

        for (int i = 0; i < 7; i++) {
            String name = responseArray.getJSONObject(i).getString("name");
            assertTrue(name.contains("In-Banner"));
        }
    }

    @Test
    public void testInbannerSkip() throws Exception {
        when(directService.getFeatures(any(), any())).thenReturn(Set.of("in_banner_creatives_support"));
        when(sessionParams.getInBannerFlag()).thenReturn(false);
        // фича есть, но параметр false, то покажу без инбанера.
        ResultActions presetsResult = this.mockMvc.perform(get("/presets")
                        .param("client_id", String.valueOf(CLIENT_ID))
                        .param("user_id", String.valueOf(USER_ID))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(e -> System.err.println("!!" + e.getResponse().getContentAsString()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.total", Matchers.equalTo(4)))
                .andExpect(jsonPath("$.items", Matchers.hasSize(4)));
    }
}
