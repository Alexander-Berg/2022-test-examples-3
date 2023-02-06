package ru.yandex.market.tpl.api.controller.api.location;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.api.model.location.tracking.UserShiftGeoTrackingDto;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.service.location.GeoTrackingService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebLayerTest(GeoTrackingController.class)
class GeoTrackingControllerTest extends BaseShallowTest {

    private static final String API_BULK_GEO_LOCATION_URI = "/api/bulk/geo-location";
    private static final long UID = 1;
    private static final String AUTH_HEADER_VALUE = "OAuth uid-" + UID;
    private static final String VALID_USER_PARAMETER_USER = "{\"id\": 1}";
    private static final String USER_PARAMETER_NAME = "user";
    private static final String UTF_8 = "UTF-8";
    private static final String EMPTY_USER_PARAMETER_VALUE = "{}";
    @MockBean
    private GeoTrackingService locationService;

    @DisplayName("Валидный вызов ручки сохранения точек геолокации курьера")
    @SneakyThrows
    @Test
    public void validPostBulkGeoLocationTest() {
        doNothing().when(locationService).saveUserGeoTrackingInfo(any(User.class), any(UserShiftGeoTrackingDto.class));

        mockMvc.perform(
                post(API_BULK_GEO_LOCATION_URI)
                        .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                        .content(getValidSingleGeoTrackingDto())
                        .param(USER_PARAMETER_NAME, VALID_USER_PARAMETER_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(UTF_8)
        )
                .andDo(print())
                .andExpect(status().is2xxSuccessful());

        verify(locationService, times(1)).saveUserGeoTrackingInfo(any(), any());
    }

    @DisplayName("Вызов ручки сохранения точек геолокации курьера без информации о курьере")
    @SneakyThrows
    @Test
    public void postBulkGeoLocationWithoutUserTest() {
        doNothing().when(locationService).saveUserGeoTrackingInfo(any(User.class), any(UserShiftGeoTrackingDto.class));

        mockMvc.perform(
                post(API_BULK_GEO_LOCATION_URI)
                        .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                        .content(getValidSingleGeoTrackingDto())
                        .param(USER_PARAMETER_NAME, EMPTY_USER_PARAMETER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(UTF_8)
        )
                .andDo(print())
                .andExpect(status().is2xxSuccessful());

        verify(locationService, atLeastOnce()).saveUserGeoTrackingInfo(any(), any());
    }

    @DisplayName("Невалидный вызов ручки сохранения точек геолокации курьера")
    @SneakyThrows
    @Test
    public void uncorrectedPostBulkGeoLocationTest() {
        doNothing().when(locationService).saveUserGeoTrackingInfo(any(User.class), any(UserShiftGeoTrackingDto.class));

        mockMvc.perform(
                post(API_BULK_GEO_LOCATION_URI)
                        .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                        .content(getUncorrectedSingleGeoTrackingDto())
                        .param(USER_PARAMETER_NAME, VALID_USER_PARAMETER_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(UTF_8)
        )
                .andDo(print())
                .andExpect(status().is4xxClientError());

        verify(locationService, never()).saveUserGeoTrackingInfo(any(), any());
    }

    @NotNull
    private String getValidSingleGeoTrackingDto() {
        return "{\n" +
                "    \"geoTracking\": [\n" +
                "        {\n" +
                "            \"longitude\": 37.625265,\n" +
                "            \"latitude\": 55.7415883,\n" +
                "            \"userTime\": \"2020-02-06T18:01:55.648475Z\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"deviceId\": 1,\n" +
                "    \"userShiftId\": 100\n" +
                "}";
    }

    @NotNull
    private String getUncorrectedSingleGeoTrackingDto() {
        return "{\n" +
                "    \"geoTracking\": 123,\n" +
                "    \"deviceId\": 1,\n" +
                "    \"userShiftId\": 100\n" +
                "}";
    }
}
