package ru.yandex.market.sc.core.domain.user;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.user.repository.UserRepository;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.util.LocalCacheName.FIND_USER_BY_UID;

/**
 * @author valter
 */
@ActiveProfiles({TplProfiles.TESTS, "cache"})
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UserAuthServiceTest {
    private final TestFactory testFactory;
    private final CacheManager oneHourCacheManager;
    private final UserRepository userRepository;
    private final UserAuthService userAuthService;

    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.getOrCreateStoredUser(sortingCenter);
        Optional.ofNullable(oneHourCacheManager.getCache(FIND_USER_BY_UID)).ifPresent(Cache::clear);
    }

    @Test
    void save() {
        var user2 = testFactory.user(sortingCenter);
        assertThat(userRepository.save(user2)).isEqualTo(user2);
    }

    @Test
    void findByUid() {
        var expected = userAuthService.findByUid(user.getUid()).orElseThrow();
        assertThat(expected).isEqualTo(user);
    }

    @Test
    void findByUidCached() {
        var expectedUser = userAuthService.findByUid(user.getUid());
        var cachedUser = getCachedUser(user.getUid());
        assertThat(cachedUser).isPresent();
        assertThat(cachedUser).isEqualTo(expectedUser);
    }

    private Optional<User> getCachedUser(long uid) {
        return Optional.ofNullable(oneHourCacheManager.getCache(FIND_USER_BY_UID))
                .map(c -> c.get(uid, User.class));
    }
}
