package ru.yandex.market.clab.common.service.user;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.ClabUser;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author anmalysh
 * @since 21.10.2018
 */

public class UserRepositoryImplTestPgaas extends BasePgaasIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private static final long SEED = 9005089642L;

    private EnhancedRandom random;

    @Before
    public void before() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(SEED).build();
    }

    @Test
    public void saveAndGet() {
        ClabUser user = random.nextObject(ClabUser.class, "id");
        ClabUser saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertEquals(user.getLogin(), saved.getLogin());
        assertArrayEquals(user.getRoles(), saved.getRoles());

        Optional<ClabUser> requested = userRepository.getByLogin(user.getLogin());
        assertThat(requested).contains(saved);
    }

    @Test
    public void saveAndGetAll() {
        List<ClabUser> users = userRepository.getAll();
        assertThat(users).isEmpty();

        ClabUser user = random.nextObject(ClabUser.class, "id");
        ClabUser saved = userRepository.save(user);

        users = userRepository.getAll();
        assertThat(users).containsExactlyInAnyOrder(saved);

    }

}
