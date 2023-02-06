package ru.yandex.market.clab.common.service.user;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 * @since 1/17/2019
 */
public class UserServiceTest extends BasePgaasIntegrationTest {

    @Autowired
    private UserService userService;

    private static final long SEED = 3092734236742304L;

    private EnhancedRandom random;

    @Before
    public void before() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(SEED).build();
    }

    @Test
    public void testCreateUser() {
        User user = createUser();

        User createdUser = userService.save(user);

        assertThat(createdUser.getId()).isNotNull();
        createdUser.setId(null);
        assertThat(createdUser).isEqualTo(user);
    }

    @Test
    public void testUpdateUser() {
        User user = createUser();

        User createdUser = userService.save(user);

        User newUser = createUser();
        newUser.setId(createdUser.getId());
        newUser.setLogin(createdUser.getLogin());

        User updatedUser = userService.save(newUser);

        List<User> allUsers = userService.getAll();
        assertThat(allUsers)
            .containsExactly(updatedUser);
    }

    private User createUser() {
        return random.nextObject(User.class, "id");
    }
}
