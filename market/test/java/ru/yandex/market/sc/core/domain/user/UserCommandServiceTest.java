package ru.yandex.market.sc.core.domain.user;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.model.ChangeSortingCenterRequestDto;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.user.repository.UserRepository;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.domain.user.model.UserRole.PARTNER;
import static ru.yandex.market.sc.core.domain.user.model.UserRole.PI_ADMIN;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UserCommandServiceTest {
    private final TestFactory testFactory;
    private final UserRepository userRepository;
    private final UserCommandService userCommandService;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void findByUserIdAndSortingCenterOrCreatePartnerUser() {
        long uid = 123L;
        userRepository.findByUid(uid).ifPresent(u -> {
            throw new RuntimeException("Such user is present.");
        });
        userCommandService.findByUserIdAndSortingCenterOrCreatePartnerUser(123L, sortingCenter);
        var newUser = userRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("Such user is not present."));
        assertThat(newUser.getRole()).isEqualTo(PARTNER);
    }

    @Test
    void findOrCreatePIAdmin() {
        long uid = 1300000000000000L + sortingCenter.getId();
        userRepository.findByUid(uid).ifPresent(u -> {
            throw new RuntimeException("Such user is present.");
        });
        var user1 = userCommandService.findOrCreatePIAdmin(sortingCenter);
        var user2 = userCommandService.findOrCreatePIAdmin(sortingCenter);
        assertThat(user1).isEqualTo(user2);
        var newUser = userRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("Such user is not present."));
        assertThat(newUser.getRole()).isEqualTo(PI_ADMIN);
    }

    @Test
    void changeSortingCenterUser() {
        User user = testFactory.getOrCreateStoredUser(sortingCenter);
        var sortingCenter2 = testFactory.storedSortingCenter(1234L);
        assertThatThrownBy(() -> userCommandService.changeSortingCenter(
                new ChangeSortingCenterRequestDto(sortingCenter2.getId()), user.getUid())
        ).isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void changeSortingCenterSupportUser() {
        User user = testFactory.getOrCreateSupportStoredUser(sortingCenter);
        var sortingCenter2 = testFactory.storedSortingCenter(1234L);
        userCommandService.changeSortingCenter(
                new ChangeSortingCenterRequestDto(sortingCenter2.getId()), user.getUid());
        assertThat(userRepository.findByUid(user.getUid())
                .map(User::getSortingCenter)
                .map(SortingCenter::getId)
                .orElse(null)).isEqualTo(sortingCenter2.getId());
    }
}
