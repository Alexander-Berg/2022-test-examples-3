package ru.yandex.market.tpl.core.domain.user;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.CoreTestV2;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.projection.UserWPropertiesProjection;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.service.binding.BindingType;
import ru.yandex.market.tpl.core.service.user.Region;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyEntity;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyRepository;
import ru.yandex.market.tpl.core.service.user.UserRegionRepository;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTestV2
@RequiredArgsConstructor
class UserPropertyMergingQueryServiceTest {

    private final UserPropertyRepository userPropertyRepository;
    private final TestUserHelper testUserHelper;
    private final UserRegionRepository userRegionRepository;
    private final SortingCenterPropertyRepository sortingCenterPropertyRepository;
    private final UserPropertyMergingQueryService userPropertyMergingQueryService;

    @Test
    void findAllWithUserPropertyEnabled() {
        User user1 = createUser(1L);
        userPropertyRepository.save(new UserPropertyEntity(user1, TplPropertyType.BOOLEAN,
                UserProperties.DEMO_ENABLED.getName(), "true"));
        userRegionRepository.save(new Region(1, user1, BindingType.SOFT));
        User user2 = createUser(2L);
        userPropertyRepository.save(new UserPropertyEntity(user2, TplPropertyType.BOOLEAN,
                UserProperties.DEMO_ENABLED.getName(), "false"));
        userPropertyRepository.save(new UserPropertyEntity(user2, TplPropertyType.BOOLEAN, "test", "true"));
        User user3 = createUser(3L);
        UserShift emptyShift = testUserHelper.createEmptyShift(user1, LocalDate.now());
        sortingCenterPropertyRepository.save(new SortingCenterPropertyEntity(
                emptyShift.getSortingCenter(),
                TplPropertyType.BOOLEAN,
                UserProperties.FEATURE_LIFE_POS_ENABLED.getName(),
                "true",
                Instant.now(),
                null
        ));

        Map<Long, UserWPropertiesProjection> propertiesByUserId = userPropertyMergingQueryService.findAllMergedPropertiesForUsers(
                UserSpecification.builder()
                        .uids(Set.of(1L, 2L, 3L))
                        .build()
        );
        assertThat(propertiesByUserId).hasSize(3);
        assertThat(propertiesByUserId.get(user1.getId()).getProperty(UserProperties.DEMO_ENABLED)).isEqualTo(true);
        assertThat(propertiesByUserId.get(user2.getId()).getProperty(UserProperties.DEMO_ENABLED)).isEqualTo(false);
        assertThat(propertiesByUserId.get(user3.getId()).getProperty(UserProperties.DEMO_ENABLED)).isEqualTo(false);
        assertThat(propertiesByUserId.get(user1.getId()).getProperty(UserProperties.FEATURE_LIFE_POS_ENABLED)).isEqualTo(true);
        assertThat(propertiesByUserId.get(user2.getId()).getProperty(UserProperties.FEATURE_LIFE_POS_ENABLED)).isEqualTo(false);
    }

    private User createUser(long uid) {
        return testUserHelper.createUser(TestUserHelper.UserGenerateParam.builder()
                .userId(uid)
                .workdate(LocalDate.now())
                .build());
    }
}
