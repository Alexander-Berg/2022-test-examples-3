package ru.yandex.market.tpl.core.service.usershift;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocation;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocationRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SpoofingDetectorTest extends TplAbstractTest {
    private final SpoofingDetector spoofingDetector;
    private final TestUserHelper testUserHelper;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final UserLocationRepository userLocationRepository;
    private final TransactionTemplate transactionTemplate;
    private Long userId;
    @Value("${tpl.distanceMetersBetweenNeighborLocation}")
    private long distanceMetersBetweenNeighborLocation;
    @Value("${tpl.minutesDifferenceBetweenNeighborLocation}")
    private int minutesDifferenceBetweenNeighborLocation;

    @BeforeEach
    void setUp() {
        var user = transactionTemplate.execute(t ->
                userRepository.save(testUserHelper.createUserWithoutSchedule(1212134L)));
        this.userId = user.getId();
        assertThat(distanceMetersBetweenNeighborLocation).isGreaterThan(0);
        assertThat(minutesDifferenceBetweenNeighborLocation).isGreaterThan(0);
        assertThat(userLocationRepository.findAll()).isEmpty();
    }

    @Test
    void noSpoofingDetected() {
        initUserLocation(1.0, 1.0, Instant.now());
        initUserLocation(1.0, 1.0, Instant.now());
        assertThat(spoofingDetector.detect(userId)).isFalse();
    }

    @Test
    void detectSpoofing() {
        initUserLocation(1.0, 1.0, Instant.now());
        initUserLocation(45.0, 45.0, Instant.now());
        assertThat(spoofingDetector.detect(userId)).isTrue();
    }


    @Test
    void detectOnlyLastHourSpoofings() {
        Instant now = Instant.now();
        initUserLocation(45.0, 45.0, now.minusSeconds(60 * 60 * 2));
        initUserLocation(1.0, 1.0, now);
        initUserLocation(1.0, 1.00001, now);
        assertThat(spoofingDetector.detect(userId)).isFalse();
    }

    @Test
    void littleSpoofingNotDetected() {
        initUserLocation(1.0, 1.0, Instant.now());
        initUserLocation(1.00001, 1.00001, Instant.now());
        assertThat(spoofingDetector.detect(userId)).isFalse();
    }

    @Test
    void noProblemWithOneOrZeroLocations() {
        assertThat(spoofingDetector.detect(userId)).isFalse();
        initUserLocation(1.0, 1.0, Instant.now());
        assertThat(spoofingDetector.detect(userId)).isFalse();
    }

    private UserLocation initUserLocation(double lat, double lon, Instant time) {
        UserLocation userLocation = new UserLocation();
        userLocation.setLatitude(new BigDecimal(lat));
        userLocation.setLongitude(new BigDecimal(lon));
        userLocation.setUserId(userId);
        UserLocation location = userLocationRepository.save(userLocation);
        jdbcTemplate.update("UPDATE user_location SET created_at = ? WHERE id = ?",
                Timestamp.from(time), location.getId());
        return location;
    }
}
