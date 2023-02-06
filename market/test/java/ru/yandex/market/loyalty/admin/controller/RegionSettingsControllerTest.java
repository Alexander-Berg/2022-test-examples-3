package ru.yandex.market.loyalty.admin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.market.loyalty.admin.service.RegionClient;
import ru.yandex.market.loyalty.admin.service.RegionSettingsViewProvider.RegionSettingsView;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminRegionSettingsTest;
import ru.yandex.market.loyalty.core.dao.RegionSettingsDao;
import ru.yandex.market.loyalty.core.model.RegionSettings;
import ru.yandex.market.loyalty.core.service.RegionSettingsService;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestFor(RegionSettingsController.class)
public class RegionSettingsControllerTest extends MarketLoyaltyAdminRegionSettingsTest {

    private static final String REGION_SETTINGS_URI = "/api/region/settings";

    private static final int THRESHOLD_DISABLED_REGION_ID = 11457;
    private static final BigDecimal EXPECTED_UPDATED_THRESHOLD_VALUE = BigDecimal.valueOf(12345.00);
    private static final int THRESHOLD_ENABLED_REGION_ID = 102444;
    private static final int EMISSION_DISABLED_REGION_ID = 11443;
    private static final int DELIVERY_WELCOME_BONUS_REGION_ID = 10251;

    private static final TypeReference<List<RegionSettingsView>> REGION_SETTINGS_VIEW_TYPE_REF =
            new TypeReference<List<RegionSettingsView>>() {
            };

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    GeoClient geoClient;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegionSettingsService regionSettingsService;
    @Autowired
    private RegionSettingsDao regionSettingsDao;

    @Test
    public void shouldEnableRegionThresholdAndSetThresholdValue() throws Exception {
        executeRegionSettingsUpdate(RegionSettings.builder()
                .withRegionId(THRESHOLD_DISABLED_REGION_ID)
                .withThresholdValue(EXPECTED_UPDATED_THRESHOLD_VALUE)
                .withEnabledThreshold()
                .build()
        );

        checkThresholdSettings(
                regionSettingsService::findIfWithEnabledThreshold,
                THRESHOLD_DISABLED_REGION_ID
        );
    }

    @Test
    public void shouldDisableRegionThresholdAndUpdateThresholdValue() throws Exception {
        executeRegionSettingsUpdate(RegionSettings.builder()
                .withRegionId(THRESHOLD_ENABLED_REGION_ID)
                .withThresholdValue(EXPECTED_UPDATED_THRESHOLD_VALUE)
                .build()
        );

        checkThresholdSettings(
                regionSettingsService::findIfWithDisabledThreshold,
                THRESHOLD_ENABLED_REGION_ID
        );
    }

    @Test
    public void shouldEnableCoinEmission() throws Exception {
        enableCoinEmission();

        Optional<RegionSettings> enabledEmission = regionSettingsService.getAllByRegionId(
                EMISSION_DISABLED_REGION_ID
        );
        assertTrue(enabledEmission.orElseThrow(AssertionError::new).isCoinEmissionEnabled());
    }

    private void enableCoinEmission() throws Exception {
        executeRegionSettingsUpdate(RegionSettings.builder()
                .withRegionId(EMISSION_DISABLED_REGION_ID)
                .build()
        );
    }

    @Test
    public void shouldDisableCoinEmission() throws Exception {
        enableCoinEmission();
        assertTrue(regionSettingsService.getAllByRegionId(EMISSION_DISABLED_REGION_ID)
                .orElseThrow(AssertionError::new)
                .isCoinEmissionEnabled()
        );
        executeRegionSettingsUpdate(RegionSettings.builder()
                .withRegionId(EMISSION_DISABLED_REGION_ID)
                .withDisabledCoinEmission()
                .build()
        );
        regionSettingsService.reloadCache();
        assertFalse(regionSettingsService.getAllByRegionId(EMISSION_DISABLED_REGION_ID)
                .orElseThrow(AssertionError::new)
                .isCoinEmissionEnabled()
        );
    }

    @Test
    public void shouldReturnAllWithoutSearchTerm() throws Exception {
        int totalSettingsCount = regionSettingsDao.getAll().size();
        String pagedResponseRaw = doRequestWithCsrf(
                get(REGION_SETTINGS_URI + "/search")
        )
                .andReturn().getResponse().getContentAsString();
        List<RegionSettingsView> regionSettings = readRowResponse(pagedResponseRaw);
        assertThat(
                regionSettings,
                allOf(
                        not(empty()),
                        hasSize(totalSettingsCount),
                        everyItem(
                                hasProperty("regionView", notNullValue())
                        )
                )
        );
    }

    @Test
    public void shouldFindByIdWithGeoApi() throws Exception {
        String expectedRegionName = "Хабаровский край";
        String expectedDescription = "субъект федерации";
        String pagedResponseRaw = doRequestWithCsrf(
                get(REGION_SETTINGS_URI + "/search")
                        .param("searchTerm", "11457")
        ).andReturn().getResponse().getContentAsString();
        List<RegionSettingsView> regionSettings = readRowResponse(pagedResponseRaw);
        assertThat(regionSettings, hasSize(1));
        RegionClient.RegionView regionView = regionSettings
                .get(0)
                .getRegionView();
        assertThat(
                regionView,
                allOf(
                        hasProperty("name", equalTo(expectedRegionName)),
                        hasProperty("typeDescription", equalTo(expectedDescription))
                )
        );
    }

    @Test
    public void shouldEnableDeliveryWelcomeBonus() throws Exception {
        assertFalse(deliverWelcomeBonusDisabledForRegion());
        executeRegionSettingsUpdate(
                RegionSettings.builder()
                        .withRegionId(DELIVERY_WELCOME_BONUS_REGION_ID)
                        .withWelcomeBonusEnabledValue(true)
                        .build()
        );
        assertTrue(deliverWelcomeBonusDisabledForRegion());
    }

    private boolean deliverWelcomeBonusDisabledForRegion() {
        return regionSettingsService.deliveryWelcomeBonusEnabledForRegion(DELIVERY_WELCOME_BONUS_REGION_ID);
    }

    private List<RegionSettingsView> readRowResponse(String response) throws Exception {
        return objectMapper.readValue(response, REGION_SETTINGS_VIEW_TYPE_REF);
    }


    private static void checkThresholdSettings(Function<Integer, Optional<RegionSettings>> supplier, int id) {
        Optional<RegionSettings> thresholdSettings = supplier.apply(id);
        assertTrue(thresholdSettings.isPresent());
        RegionSettings updatedThresholdSettings = thresholdSettings.get();
        assertThat(
                updatedThresholdSettings.getThresholdValue(),
                comparesEqualTo(EXPECTED_UPDATED_THRESHOLD_VALUE)
        );
    }

    @NotNull
    private ResultActions executeRegionSettingsUpdate(RegionSettings regionSettings) throws Exception {
        ResultActions result = doRequestWithCsrf(
                put(REGION_SETTINGS_URI + "/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                regionSettingsAsString(
                                        regionSettings
                                )
                        )
        )
                .andExpect(status().isOk());

        regionSettingsService.reloadCache();
        return result;
    }

    private ResultActions doRequestWithCsrf(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(csrf()));
    }

    private String regionSettingsAsString(RegionSettings regionSettings) throws JsonProcessingException {
        return objectMapper.writeValueAsString(regionSettings);
    }
}
