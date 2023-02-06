package ru.yandex.market.mboc.common.users;

import java.util.Optional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mboc.common.BaseIntegrationTestClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuramalinov
 * @created 10.05.18
 */
@Transactional
public class UserRepositoryTest extends BaseIntegrationTestClass {
    @Autowired
    private UserRepository userRepository;

    @Test
    public void testInsert() {
        User someone = userRepository.insert(new User("someone"));
        assertThat(someone.getId()).isPositive();

        Optional<User> user = userRepository.findByLogin("someone");
        assertThat(user).isPresent();
    }

    @Test(expected = DuplicateKeyException.class)
    public void testUnique() {
        userRepository.insert(new User("someone"));
        userRepository.insert(new User("someone"));
    }

    @Test(expected = DuplicateKeyException.class)
    public void testUniqueUpdate() {
        userRepository.insert(new User("someone"));
        User user = userRepository.insert(new User("sometwo"));
        user.setLogin("someone");
        userRepository.update(user);
    }

    @Test
    public void testUpdate() {
        User user = userRepository.insert(new User("someone"));
        user.setLogin("another");
        userRepository.update(user);

        User updated = userRepository.findById(user.getId());

        assertThat(updated.getLogin()).isEqualTo("another");
    }

    @Test
    public void testGetOrCreateEmpty() {
        User user = userRepository.getOrCreateUser("someone");
        assertThat(user).isNotNull();
        assertThat(user.getId()).isPositive();
    }

    @Test
    public void testGetOrCreateNonEmpty() {
        User user = userRepository.insert(new User("sometwo"));
        User created = userRepository.getOrCreateUser("sometwo");
        assertThat(created.getId()).isEqualTo(user.getId());
    }

    @Test
    public void testSaveRoles() {
        User user = userRepository.insert(new User("sometwo").addRole(UserRoles.VIEWER));
        User fromDb = userRepository.findById(user.getId());
        assertThat(fromDb.getRoles()).containsExactly(UserRoles.VIEWER);
    }

    @Test
    public void testAddRole() {
        User user = userRepository.insert(new User("sometwo"));
        user = userRepository.findById(user.getId());
        user.addRole(UserRoles.VIEWER);
        userRepository.update(user);

        user = userRepository.findById(user.getId());
        assertThat(user.getRoles()).containsExactly(UserRoles.VIEWER);
    }

    @Test
    public void testDeleteRole() {
        User user = userRepository.insert(new User("sometwo")
            .addRole(UserRoles.VIEWER).addRole(UserRoles.MANAGE_ASSORTMENT));
        user = userRepository.findById(user.getId());
        user.removeRole(UserRoles.MANAGE_ASSORTMENT);
        userRepository.update(user);

        user = userRepository.findById(user.getId());
        assertThat(user.getRoles()).containsExactly(UserRoles.VIEWER);
    }
}
