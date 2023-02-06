package ru.yandex.market.tpl.core.domain.usershift.location;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RequiredArgsConstructor
class UserLocationRepositoryTest extends TplAbstractTest {
    private final UserLocationRepository userLocationRepository;
    private final TestUserHelper testUserHelper;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Test
    void findUserLocationsForLastHour() {
        var user = transactionTemplate.execute(t ->
                userRepository.save(testUserHelper.createUserWithoutSchedule(1212134L)));
        UserLocation userLocation1 = new UserLocation();
        userLocation1.setLatitude(new BigDecimal(13));
        userLocation1.setLongitude(new BigDecimal(13));
        userLocation1.setUserId(user.getId());
        UserLocation userLocation2 = new UserLocation();
        userLocation2.setLatitude(new BigDecimal(14));
        userLocation2.setLongitude(new BigDecimal(14));
        userLocation2.setUserId(user.getId());
        UserLocation location1 = userLocationRepository.save(userLocation1);
        UserLocation location2 = userLocationRepository.save(userLocation2);
        List<UserLocation> userLocations = userLocationRepository.findUserLocationsForLastHour(user.getId());
        assertThat(userLocations).containsExactlyInAnyOrder(location1, location2);

        jdbcTemplate.update("UPDATE user_location SET created_at = now() - Interval '2 hours' WHERE id = ?",
                location1.getId());
        List<UserLocation> userLocations2 = userLocationRepository.findUserLocationsForLastHour(user.getId());
        assertThat(userLocations2).containsExactlyInAnyOrder(location2);
    }

    @Test
    void findUserLocationsForLast_withZeros() {
        //given
        User user = transactionTemplate.execute(t ->
                userRepository.save(testUserHelper.createUserWithoutSchedule(1212134L)));
        UserShift emptyShift = testUserHelper.createEmptyShift(user, LocalDate.now());

        var nonZeroUserLocation = addNonZeroUserLocation(user.getId(), emptyShift.getId());
        addZeroUserLocation(user.getId(), emptyShift.getId());

        //when
        Optional<UserLocation> lastLocation = userLocationRepository.findLastLocation(user.getId(), emptyShift.getId());

        //then
        assertFalse(lastLocation.isEmpty());
        assertThat(lastLocation.get()).isEqualTo(nonZeroUserLocation);
    }

    @DisplayName("Проверка маппинга модели")
    @Test
    void testModelMapping() {
        UserLocation draft = new UserLocation();
        draft.setLatitude(new BigDecimal(13));
        draft.setLongitude(new BigDecimal(13));

        UserLocation userLocation = transactionTemplate.execute(t -> {
            User userToSave = testUserHelper.createUserWithoutSchedule(10007L);
            User user = userRepository.save(userToSave);
            draft.setUserId(user.getId());
            return userLocationRepository.save(draft);
        });

        assertNotNull(userLocation);
        assertNotNull(userLocation.getCreatedAt());
        assertEquals(userLocation.getGeoPoint(), draft.getGeoPoint());
        assertEquals(userLocation.getUpdatedAt(), draft.getUpdatedAt());
    }

    @Test
    void deduplicateLocations() {
        User user = transactionTemplate.execute(t ->
                userRepository.save(testUserHelper.createUserWithoutSchedule(1212134L)));


        Instant postedAt = clock.instant();
        UserLocation userLocation = getUserLocation(
                user.getId(),
                postedAt,
                BigDecimal.ONE,
                BigDecimal.TEN
        );

        UserLocation duplicatedUserLocation = getUserLocation(
                user.getId(),
                postedAt,
                BigDecimal.ONE,
                BigDecimal.TEN
        );

        userLocationRepository.saveAll(List.of(userLocation, duplicatedUserLocation));

        List<UserLocation> all = userLocationRepository.findAll();

        assertThat(all).hasSize(1);
        UserLocation saved = all.iterator().next();
        assertThat(saved.getLatitude()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(saved.getLongitude()).isEqualByComparingTo(BigDecimal.TEN);
    }

    private UserLocation getUserLocation(
            Long userId,
            Instant postedAt,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        UserLocation userLocation = new UserLocation();
        userLocation.setLatitude(latitude);
        userLocation.setLongitude(longitude);
        userLocation.setUserId(userId);
        userLocation.setPostedAt(postedAt);
        return userLocation;
    }

    private UserLocation addNonZeroUserLocation(Long userId, Long emptyShiftId) {
        return adduserLocation(userId, emptyShiftId, BigDecimal.ONE);
    }

    private UserLocation addZeroUserLocation(Long userId, Long emptyShiftId) {
        return adduserLocation(userId, emptyShiftId, BigDecimal.ZERO);
    }

    private UserLocation adduserLocation(Long userId, Long emptyShiftId, BigDecimal location) {
        UserLocation userLocation = new UserLocation();
        userLocation.setLatitude(location);
        userLocation.setLongitude(location);
        userLocation.setUserId(userId);
        userLocation.setUserShiftId(emptyShiftId);
        return userLocationRepository.save(userLocation);
    }

}
