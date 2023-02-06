package ru.yandex.market.checkout.checkouter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsCheckouterService;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.util.SettingsUtils.sameSettings;

/**
 * @author kl1san
 */
public class PushApiSettingsControllerTest extends AbstractWebTestBase {

    @Autowired
    private SettingsCheckouterService settingsService;
    @Autowired
    private TestSerializationService testSerializationService;

    private static final Settings DEFAULT_SETTINGS = Settings.builder()
            .urlPrefix("prefix")
            .authType(AuthType.URL)
            .authToken("token")
            .dataType(DataType.XML)
            .partnerInterface(true)
            .build();

    @Test
    public void shouldGetExistingSettings() throws Exception {
        long newShopId = 500L;
        settingsService.updateSettings(newShopId, DEFAULT_SETTINGS, false);
        MvcResult mvcResult = mockMvc.perform(get("/pushapi-settings/{shopId}", newShopId))
                .andExpect(status().isOk())
                .andReturn();
        Settings settings =
                testSerializationService.deserializeCheckouterObject(mvcResult.getResponse().getContentAsString(),
                        Settings.class);
        assertThat(settings, sameSettings(DEFAULT_SETTINGS));
    }

    @Test
    public void shouldFailToGetMissingSettings() throws Exception {
        mockMvc.perform(get("/pushapi-settings/{shopId}", 88005553535L))
                .andExpect(status().isNotFound());
        Settings settings = settingsService.getSettings(88005553535L, false);
        assertNull(settings);
    }

    @Test
    public void shouldWriteNewSettings() throws Exception {
        long newShopId = 505L;
        mockMvc.perform(post("/pushapi-settings/{shopId}", newShopId)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(DEFAULT_SETTINGS)))
                .andExpect(status().isOk());

        Settings storedSettings = settingsService.getSettings(newShopId, false);
        assertNotNull(storedSettings);
        assertThat(storedSettings, sameSettings(DEFAULT_SETTINGS));
    }

    @Test
    public void shouldUpdateSettings() throws Exception {
        long newShopId = 506L;
        settingsService.updateSettings(newShopId, DEFAULT_SETTINGS, false);
        Settings modifiedSettings = DEFAULT_SETTINGS.toBuilder()
                .urlPrefix("newUrlPrefix")
                .build();

        mockMvc.perform(post("/pushapi-settings/{shopId}", newShopId)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(modifiedSettings)))
                .andExpect(status().isOk());

        Settings storedSettings = settingsService.getSettings(newShopId, false);
        assertNotNull(storedSettings);
        assertThat(storedSettings, sameSettings(modifiedSettings));
    }

    @Test
    public void shouldDeleteSettings() throws Exception {
        long newShopId = 507L;
        settingsService.updateSettings(newShopId, DEFAULT_SETTINGS, false);
        assertNotNull(settingsService.getSettings(newShopId, false));

        mockMvc.perform(delete("/pushapi-settings/{shopId}", newShopId))
                .andExpect(status().isOk());
        assertNull(settingsService.getSettings(newShopId, false));
    }

    @Test
    public void shouldSerializeCorrectly() throws Exception {
        long newShopId = 508L;
        String settingsBody = "{\"urlPrefix\":\"prefix\",\"authToken\":\"token\",\"dataType\":\"XML\"," +
                "\"authType\":\"URL\",\"fingerprint\":\"f00d\",\"partnerInterface\":true}";
        mockMvc.perform(post("/pushapi-settings/{shopId}", newShopId)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(settingsBody))
                .andExpect(status().isOk());
        MvcResult mvcResult = mockMvc.perform(get("/pushapi-settings/{shopId}", newShopId))
                .andExpect(status().isOk())
                .andReturn();
        String settingsBodyFromApi = mvcResult.getResponse().getContentAsString();
        assertEquals(settingsBody, settingsBodyFromApi);
    }
}
