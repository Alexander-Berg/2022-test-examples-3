package ru.yandex.market.tpl.core.service.location;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ru.yandex.market.tpl.api.model.location.tracking.GeoTrackingDto;
import ru.yandex.market.tpl.api.model.location.tracking.UserShiftGeoTrackingDto;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocationRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GeoTrackingServiceTest {

    private static final long USER_ID = 1L;
    private static final long USER_SHIFT_ID = 1L;
    private static final String DEVICE_ID = "1";

    @InjectMocks
    private GeoTrackingService geoTrackingService;
    @Mock
    private GeoTrackingValidator geoTrackingValidator;
    @Mock
    private UserLocationRepository userLocationRepository;
    @Mock
    private User mockUser;

    @BeforeEach
    public void setUp() {
        when(mockUser.getId()).thenReturn(USER_ID);
    }

    @DisplayName("Добавление информации о точках геолокации, пустое дто")
    @Test
    public void saveEmptyUserGeoTrackingInfoTest() {
        UserShiftGeoTrackingDto userShiftGeoTrackingDto = new UserShiftGeoTrackingDto();

        geoTrackingService.saveUserGeoTrackingInfo(mockUser, userShiftGeoTrackingDto);

        verify(geoTrackingValidator, atLeastOnce()).validateAndGetValidTrackingDto(any());
        verify(userLocationRepository, never()).save(any());
    }

    @DisplayName("Добавление информации о точках геолокации")
    @Test
    public void saveUserGeoTrackingInfoTest() {
        UserShiftGeoTrackingDto userShiftGeoTrackingList = buildUserShiftGeoTrackingDto();
        int countOfGeoTrackingDto = 3;
        when(geoTrackingValidator.validateAndGetValidTrackingDto(anyList()))
                .thenReturn(buildListOfDefaultGeoTrackingDto(countOfGeoTrackingDto));

        geoTrackingService.saveUserGeoTrackingInfo(mockUser, userShiftGeoTrackingList);

        verify(geoTrackingValidator, atLeastOnce()).validateAndGetValidTrackingDto(any());
        verify(userLocationRepository, atLeastOnce()).saveAll(anyList());
    }

    @DisplayName("Добавление информации о точках геолокации, нет информации о курьере")
    @Test
    public void saveUserGeoTrackingInfoWhenUserNullTest() {
        UserShiftGeoTrackingDto userShiftGeoTrackingList = buildUserShiftGeoTrackingDto();
        int countOfGeoTrackingDto = 3;
        when(geoTrackingValidator.validateAndGetValidTrackingDto(anyList()))
                .thenReturn(buildListOfDefaultGeoTrackingDto(countOfGeoTrackingDto));

        geoTrackingService.saveUserGeoTrackingInfo(null, userShiftGeoTrackingList);

        verify(geoTrackingValidator, atLeastOnce()).validateAndGetValidTrackingDto(any());
        verify(userLocationRepository, atLeastOnce()).saveAll(anyList());
    }

    private UserShiftGeoTrackingDto buildUserShiftGeoTrackingDto() {
        UserShiftGeoTrackingDto userShiftGeoTrackingDto = new UserShiftGeoTrackingDto();
        GeoTrackingDto trackingDto = buildDefaultGeoTrackingDto();
        userShiftGeoTrackingDto.setGeoTracking(List.of(trackingDto));
        userShiftGeoTrackingDto.setUserShiftId(USER_SHIFT_ID);
        userShiftGeoTrackingDto.setDeviceId(DEVICE_ID);
        return userShiftGeoTrackingDto;
    }

    private List<GeoTrackingDto> buildListOfDefaultGeoTrackingDto(int count) {
        GeoTrackingDto trackingDto = buildDefaultGeoTrackingDto();
        return IntStream.range(0, count)
                .mapToObj(i -> trackingDto)
                .collect(Collectors.toList());
    }

    private GeoTrackingDto buildDefaultGeoTrackingDto() {
        GeoTrackingDto trackingDto = new GeoTrackingDto();
        trackingDto.setUserTime(Instant.now());
        trackingDto.setLongitude(BigDecimal.ONE);
        trackingDto.setLatitude(BigDecimal.ONE);
        return trackingDto;
    }
}
