package ru.yandex.market.tpl.core.service.user;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.service.demo.DemoService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.manual.LocalCacheName.Constants.SC_BY_USER_ID_CACHE;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UserDtoMapperTest {
    private final DemoService demoService;
    private final TestUserHelper userHelper;
    private final UserPropertyService userPropertyService;
    private final UserDtoMapper userDtoMapper;
    private final CacheManager cacheManager;
    private User user;

    @BeforeEach
    void setUp() {
        user = userHelper.findOrCreateUser(824125L);
        userPropertyService.addPropertyToUser(user, UserProperties.FEATURE_LIFE_POS_ENABLED, true);
    }

    @Test
    public void lifePosIdDisabledInDemo() {
        userPropertyService.addPropertyToUser(user, UserProperties.DEMO_ENABLED, true);
        demoService.createClientDemoDelivery(user);

        var dto = userDtoMapper.map(user);
        assertThat(dto.getProperties().get(UserProperties.FEATURE_LIFE_POS_ENABLED.getName())).isNotNull();
        assertThat(dto.getProperties().get(UserProperties.FEATURE_LIFE_POS_ENABLED.getName())).isEqualTo(false);
        Optional<SortingCenter> cachedSc = getCachedScByUserId(user.getId());
        assertThat(cachedSc).isPresent();
        assertThat(cachedSc.get().getId()).isEqualTo(SortingCenter.DEMO_SC_ID);

        //При выходе из демо кеш очищается
        demoService.exitDemo(user);
        cachedSc = getCachedScByUserId(user.getId());
        assertThat(cachedSc).isEmpty();
    }

    @Test
    public void lifePosIsEnabled() {
        var dto = userDtoMapper.map(user);
        assertThat(dto.getProperties().get(UserProperties.FEATURE_LIFE_POS_ENABLED.getName())).isNotNull();
        assertThat(dto.getProperties().get(UserProperties.FEATURE_LIFE_POS_ENABLED.getName())).isEqualTo(true);

        Optional<SortingCenter> cachedSc = getCachedScByUserId(user.getId());
        assertThat(cachedSc).isEmpty();
    }

    private Optional<SortingCenter> getCachedScByUserId(Long userId) {
        return Optional.ofNullable(cacheManager.getCache(SC_BY_USER_ID_CACHE))
                .map(cache -> cache.get(userId, SortingCenter.class));
    }

}
