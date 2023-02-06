package ru.yandex.market.tpl.core.service.location.calculator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.usershift.location.UserLocation;
import ru.yandex.market.tpl.core.service.location.calculator.dto.CoordinateDto;
import ru.yandex.market.tpl.core.service.location.calculator.dto.CoordinateDtoMapper;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@Slf4j
class UserLocationDistanceCalculatorModuleTest {

    private final ObjectMapper objectMapper = ObjectMappers.baseObjectMapper();
    @InjectMocks
    private UserLocationSpoofingService spoofingServiceInject;
    private UserLocationDistanceCalculator distanceCalculator;

    @BeforeEach
    public void setUp() {
        distanceCalculator = new UserLocationDistanceCalculator(spoofingServiceInject);
    }

    @DisplayName("Расчёт пробега по реальным точкам")
    @SneakyThrows
    @Test
    public void calcDistanceNotZeroTest() {
        String rawResponse = readResource("/location/user_location_by_user_shift.json");
        List<UserLocation> userLocations = objectMapper.readValue(rawResponse, new TypeReference<List<UserLocation>>() {
        });
        List<CoordinateDto> track = userLocations.stream()
                .map(CoordinateDtoMapper::map)
                .collect(Collectors.toList());

        List<CoordinateDto> afterSpoofingLocationStatistic =
                spoofingServiceInject.removeSpoofingCoordinate(track);

        double distance = distanceCalculator.evaluate(track).doubleValue();

        log.info("Raw user location " + userLocations.size()
                + " After spoofing location statistic " + afterSpoofingLocationStatistic.size()
                + " Distance:" + distance);
        assertNotNull(userLocations);
        assertTrue(distance > 0);
    }

    @DisplayName("Расчёт пробега по реальным точкам с неотсортированным списком точек")
    @SneakyThrows
    @Test
    public void calcDistanceNotZeroWithShakeDateTest() {
        String rawResponse = readResource("/location/user_location_by_user_shift.json");
        List<UserLocation> userLocations = objectMapper.readValue(rawResponse, new TypeReference<List<UserLocation>>() {
        });
        List<CoordinateDto> track = userLocations.stream()
                .map(CoordinateDtoMapper::map)
                .collect(Collectors.toList());
        List<CoordinateDto> shuffleTrack = shuffleTrack(track);

        double trackDistance = distanceCalculator.evaluate(track).doubleValue();
        double shuffleTrackDistance = distanceCalculator.evaluate(shuffleTrack).doubleValue();

        assertNotNull(userLocations);
        assertTrue(trackDistance > 0);
        assertTrue(shuffleTrackDistance > 0);
        assertEquals(trackDistance, shuffleTrackDistance);
    }

    private List<CoordinateDto> shuffleTrack(List<CoordinateDto> track) {
        List<CoordinateDto> shuffleTrack = new ArrayList<>(track);
        Collections.shuffle(track);
        return shuffleTrack;
    }

    private String readResource(String filename) throws Exception {
        return IOUtils.toString(
                this.getClass().getResourceAsStream(filename),
                StandardCharsets.UTF_8
        );
    }
}
