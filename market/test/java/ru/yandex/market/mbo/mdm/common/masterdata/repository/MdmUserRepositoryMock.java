package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.lightmapper.test.GenericMapperRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.MdmUser;

public class MdmUserRepositoryMock
    extends GenericMapperRepositoryMock<MdmUser, String>
    implements MdmUserRepository {

    private final HashMap<String, Set<String>> users;

    public MdmUserRepositoryMock() {
        super(null, MdmUser::getLogin);
        this.users = new HashMap<>();
    }

    @Override
    public List<MdmUser> findByIds(Collection<String> keys) {
        return keys.stream().map(key -> new MdmUser().setLogin(key).setRoles(users.getOrDefault(key, Set.of())))
            .collect(Collectors.toList());
    }

    @Override
    public MdmUser insert(MdmUser user) {
        users.computeIfAbsent(user.getLogin(), a -> new HashSet<>()).addAll(user.getRolesSet());
        return user;
    }

    public void addUserRole(String user, String role) {
        var roles = new HashSet<String>();
        roles.add(role);
        insert(new MdmUser().setLogin(user).setRoles(roles));
    }

    @Override
    public MdmUser findByLogin(String login) {
        List<MdmUser> result = findByIds(List.of(login));
        if (!result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }

    @Override
    public boolean userHasGrants(String login, String role) {
        MdmUser mdmUser = findByLogin(login);
        if (mdmUser != null) {
            return mdmUser.getRolesSet().contains(role);
        }
        return false;
    }

    @Override
    protected String nextId() {
        throw new UnsupportedOperationException();
    }
}
