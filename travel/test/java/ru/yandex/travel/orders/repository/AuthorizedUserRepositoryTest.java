package ru.yandex.travel.orders.repository;

import java.util.UUID;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.entities.AuthorizedUser;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class AuthorizedUserRepositoryTest {
    @Autowired
    private AuthorizedUserRepository authRepository;

    @Autowired
    private EntityManager em;

    @Test
    public void testPersistAndList() {
        UUID order1 = UUID.randomUUID();
        UUID order2 = UUID.randomUUID();
        UUID someOtherUid = UUID.randomUUID();
        AuthorizedUser auth1 = AuthorizedUser.createGuest(order1, "foo", "uid", AuthorizedUser.OrderUserRole.OWNER);
        AuthorizedUser auth2 = AuthorizedUser.createGuest(order1, "bar", "uid", AuthorizedUser.OrderUserRole.OWNER);
        AuthorizedUser auth3 = AuthorizedUser.createLogged(order1, "uid", "passport1", "login1", AuthorizedUser.OrderUserRole.OWNER);
        AuthorizedUser auth4 = AuthorizedUser.createLogged(order2, "uid", "passport1", "login1", AuthorizedUser.OrderUserRole.OWNER);
        AuthorizedUser auth5 = AuthorizedUser.createLogged(order2, "uid", "passport2", "login2", AuthorizedUser.OrderUserRole.OWNER);
        authRepository.save(auth1);
        authRepository.save(auth2);
        authRepository.save(auth3);
        authRepository.save(auth4);
        authRepository.save(auth5);
        authRepository.flush();
        em.clear();

        assertThat(authRepository.findByIdOrderId(order1)).hasSize(3);
        assertThat(authRepository.findByIdOrderId(order2)).hasSize(2);
        assertThat(authRepository.findByIdOrderId(someOtherUid)).hasSize(0);
    }

    @Test
    public void testGetOwner() {
        UUID order = UUID.randomUUID();
        AuthorizedUser auth1 = AuthorizedUser.createGuest(order, "foo", "uid", AuthorizedUser.OrderUserRole.OWNER);
        AuthorizedUser auth2 = AuthorizedUser.createGuest(order, "bar", "uid", AuthorizedUser.OrderUserRole.VIEWER);
        AuthorizedUser auth3 = AuthorizedUser.createGuest(order, "bar", "uid", AuthorizedUser.OrderUserRole.VIEWER);
        authRepository.save(auth1);
        authRepository.save(auth2);
        authRepository.save(auth3);
        authRepository.flush();
        em.clear();
        var res = authRepository.findByIdOrderIdAndRole(order, AuthorizedUser.OrderUserRole.OWNER);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getSessionKey()).isEqualTo("foo");
    }

    @Test
    public void testGetViewers() {
        UUID order = UUID.randomUUID();
        AuthorizedUser auth1 = AuthorizedUser.createGuest(order, "foo", "uid", AuthorizedUser.OrderUserRole.OWNER);
        AuthorizedUser auth2 = AuthorizedUser.createGuest(order, "bar", "uid", AuthorizedUser.OrderUserRole.VIEWER);
        AuthorizedUser auth3 = AuthorizedUser.createGuest(order, "baz", "uid", AuthorizedUser.OrderUserRole.VIEWER);
        authRepository.save(auth1);
        authRepository.save(auth2);
        authRepository.save(auth3);
        authRepository.flush();
        em.clear();
        var res = authRepository.findByIdOrderIdAndRole(order, AuthorizedUser.OrderUserRole.VIEWER);
        assertThat(res).hasSize(2);
        assertThat(res).extracting(AuthorizedUser::getSessionKey).containsExactly("bar", "baz");
    }
}
