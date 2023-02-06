package ru.yandex.market.sc.core.domain.user.repository;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author hardlight
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserRepositoryTest {
    private final TestFactory testFactory;
    private final UserRepository userRepository;

    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.getOrCreateStoredUser(sortingCenter);
    }

    @Test
    void save() {
        var user2 = testFactory.user(sortingCenter);
        assertThat(userRepository.save(user2)).isEqualTo(user2);
    }

    @Test
    void findByUid() {
        var expected = userRepository.findByUid(user.getUid()).orElseThrow();
        assertThat(expected).isEqualTo(user);
    }
}
