package ru.yandex.market.tpl.core.service.user.redirect;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserService;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstanceRepository;
import ru.yandex.market.tpl.core.service.user.UserAuthService;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.manual.LocalCacheName.Constants.CACHE_USER_REDIRECTS;

/**
 * @author kukabara
 */
@RequiredArgsConstructor
class UserRedirectServiceTest extends TplAbstractTest {

    public static final long UID = 2352L;
    public static final long TO_UID = 124L;
    public static final long ANOTHER_UID = 23124L;

    private final UserService userService;
    private final UserRedirectService userRedirectService;
    private final UserAuthService userAuthService;
    private final TestUserHelper testUserHelper;
    private final CacheManager oneMinuteCacheManager;
    private final VehicleGenerateService vehicleGenerateService;
    private final VehicleInstanceRepository vehicleInstanceRepository;

    @Test
    void userWithoutRedirect() {
        testUserHelper.findOrCreateUser(UID);
        findByUidAndCheck(UID, UID);
    }

    @Test
    void userWithRedirectWhenUidNotExists() {
        createRedirect(ANOTHER_UID, TO_UID);
        findByUidAndCheck(ANOTHER_UID, TO_UID);
    }

    @Test
    void userWithRedirectWhenUidExists() {
        testUserHelper.findOrCreateUser(UID);
        createRedirect(UID, TO_UID);
        findByUidAndCheck(UID, TO_UID);
    }

    @Test
    void userWithoutRedirectWhenDeleteRedirect() {
        testUserHelper.findOrCreateUser(UID);
        createRedirect(UID, TO_UID);
        userRedirectService.delete(UID);
        findByUidAndCheck(UID, UID);
    }

    @Test
    void shouldDeleteUser() {
        testUserHelper.findOrCreateUser(ANOTHER_UID);
        UserRedirect userRedirect = userRedirectService.create(UID, ANOTHER_UID);
        userService.deleteUserById(userRedirect.getToUserId());
    }

    @Test
    void shouldDeleteUserAndUnlinkedVehicleInstance() {
        var user = testUserHelper.findOrCreateUser(ANOTHER_UID);
        vehicleGenerateService.assignVehicleToUser(
                VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                        .vehicle(vehicleGenerateService.generateVehicle())
                        .users(List.of(user))
                        .build());
        assertThat(vehicleInstanceRepository.findAll()).hasSize(1);
        userService.deleteUserById(user.getId());
        assertThat(vehicleInstanceRepository.findAll()).hasSize(0);
    }

    private void createRedirect(long uidFrom, long uidTo) {
        testUserHelper.findOrCreateUser(uidTo);
        userRedirectService.create(uidFrom, uidTo);
        oneMinuteCacheManager.getCache(CACHE_USER_REDIRECTS).clear();
    }

    private void findByUidAndCheck(long uid, long expectedUid) {
        Optional<User> user = userAuthService.findByUid(uid);
        assertThat(user).isPresent();
        assertThat(user.get().getUid()).isEqualTo(expectedUid);
        assertThat(user.get().isRedirect()).isEqualTo(uid != expectedUid);
    }
}
