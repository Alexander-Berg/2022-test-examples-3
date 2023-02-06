package ru.yandex.market.tpl.core.service.location;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.api.model.location.tracking.GeoTrackingDto;
import ru.yandex.market.tpl.api.model.location.tracking.UserShiftGeoTrackingDto;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocation;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoTrackingMapperTest {

    private static final long USER_ID = 1L;
    private final ObjectMapper objectMapper = ObjectMappers.baseObjectMapper();

    @DisplayName("Маппинг модели локации гео-трекинга курьера с одной точкой")
    @SneakyThrows
    @Test
    void GeoTrackingDtoMapperTest() {
        String rawResponse = readResource("/location/single-geo-tracking-valid.json");

        UserShiftGeoTrackingDto userShiftGeoTrackingDto =
                objectMapper.readValue(rawResponse, UserShiftGeoTrackingDto.class);

        assertNotNull(userShiftGeoTrackingDto);
        assertNotNull(userShiftGeoTrackingDto.getDeviceId());
        assertNotNull(userShiftGeoTrackingDto.getUserShiftId());
        assertNonNullAllGeoTracking(userShiftGeoTrackingDto.getGeoTracking());
        assertEquals(userShiftGeoTrackingDto.getGeoTracking().size(), 1);
    }

    @DisplayName("Маппинг модели гео-трекинга курьера с множеством точек")
    @SneakyThrows
    @Test
    void GeoTrackingListDtoMapperTest() {
        String rawResponse = readResource("/location/list-geo-tracking-valid.json");

        UserShiftGeoTrackingDto userShiftGeoTrackingDto =
                objectMapper.readValue(rawResponse, UserShiftGeoTrackingDto.class);

        assertNotNull(userShiftGeoTrackingDto);
        assertNotNull(userShiftGeoTrackingDto.getDeviceId());
        assertNotNull(userShiftGeoTrackingDto.getUserShiftId());
        assertNonNullAllGeoTracking(userShiftGeoTrackingDto.getGeoTracking());
    }

    @DisplayName("Маппинг dto в модель UserLocation")
    @SneakyThrows
    @Test
    public void validDtoMapToUserLocationTest() {
        User userMock = Mockito.mock(User.class);
        Mockito.when(userMock.getId()).thenReturn(USER_ID);
        String rawResponse = readResource("/location/list-geo-tracking-valid.json");
        UserShiftGeoTrackingDto userShiftGeoTrackingDto =
                objectMapper.readValue(rawResponse, UserShiftGeoTrackingDto.class);

        List<UserLocation> userLocationList = GeoTrackingMapper.mapToUserLocation(userMock, userShiftGeoTrackingDto);

        assertNotNull(userLocationList);
        assertNotNullUserLocation(userLocationList);
        assertAllMatchUserId(userLocationList, USER_ID);
        assertThat(userLocationList).allMatch(postedAt -> postedAt.getPostedAt() != null);
    }

    @DisplayName("Маппинг пустого курьера и dto в модель UserLocation")
    @SneakyThrows
    @Test
    public void validDtoMapToUserLocationAndEmptyUserTest() {
        String rawResponse = readResource("/location/list-geo-tracking-valid.json");
        UserShiftGeoTrackingDto userShiftGeoTrackingDto =
                objectMapper.readValue(rawResponse, UserShiftGeoTrackingDto.class);

        List<UserLocation> userLocationList = GeoTrackingMapper.mapToUserLocation(null, userShiftGeoTrackingDto);

        assertNotNull(userLocationList);
        assertNotNullUserLocation(userLocationList);
        assertAllMatchUserId(userLocationList, 0);
        assertThat(userLocationList).allMatch(postedAt -> postedAt.getPostedAt() != null);
    }

    @DisplayName("Маппинг невалидного dto в модель UserLocation")
    @SneakyThrows
    @Test
    public void notValidDtoMapToUserLocationTest() {
        User userMock = Mockito.mock(User.class);
        Mockito.when(userMock.getId()).thenReturn(USER_ID);
        String rawResponse = readResource("/location/not-valid-single-geo-tracking-valid.json");
        UserShiftGeoTrackingDto userShiftGeoTrackingDto =
                objectMapper.readValue(rawResponse, UserShiftGeoTrackingDto.class);

        List<UserLocation> userLocationList = GeoTrackingMapper.mapToUserLocation(userMock, userShiftGeoTrackingDto);

        assertNotNullUserLocation(userLocationList);
        assertTrue(userLocationList.isEmpty());
        assertAllMatchUserId(userLocationList, USER_ID);
    }

    private void assertAllMatchUserId(List<UserLocation> userLocationList, long userId) {
        boolean isAllMatchUserId =
                userLocationList.stream()
                        .allMatch(userLocation -> userLocation.getUserId() == userId);
        assertTrue(isAllMatchUserId);
    }

    private void assertNotNullUserLocation(List<UserLocation> userLocationList) {
        assertNotNull(userLocationList);
        for (UserLocation userLocation : userLocationList) {
            assertNotNull(userLocation.getDeviceId());
            assertNotNull(userLocation.getUserShiftId());
            assertNotNull(userLocation.getLatitude());
            assertNotNull(userLocation.getLongitude());
        }
    }

    private void assertNonNullAllGeoTracking(List<GeoTrackingDto> geoTracking) {
        assertNotNull(geoTracking);
        for (GeoTrackingDto trackingDto : geoTracking) {
            assertNotNull(trackingDto.getLatitude());
            assertNotNull(trackingDto.getLongitude());
            assertNotNull(trackingDto.getUserTime());
        }
    }

    private String readResource(String filename) throws Exception {
        return IOUtils.toString(
                this.getClass().getResourceAsStream(filename),
                StandardCharsets.UTF_8
        );
    }
}
