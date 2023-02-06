package ru.yandex.market.mboc.common.users;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import ru.yandex.market.mbo.lightmapper.test.IntGenericMapperRepositoryMock;
import ru.yandex.market.mboc.auth.AuthUser;

/**
 * @author yuramalinov
 * @created 10.08.18
 */
public class UserRepositoryMock extends IntGenericMapperRepositoryMock<User> implements UserRepository {
    public UserRepositoryMock() {
        super(User::setId, User::getId);
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return findFirst(user -> Objects.equals(login, user.getLogin()));
    }

    @Override
    public Map<String, User> findByLoginsMap(Collection<String> logins) {
        return Map.of();
    }

    @Override
    public List<User> findByLogins(Collection<String> logins) {
        return List.of();
    }

    @Override
    public List<User> findStaffUpdateRequired() {
        return Collections.emptyList();
    }

    @Override
    public User getOrCreateUser(String login, boolean forUpdate) {
        return findByLogin(login).orElseGet(() -> insert(new User(login)));
    }

    @Override
    public AuthUser createDebugUser(String login, Set<String> roles) {
        throw new UnsupportedOperationException();
    }
}
