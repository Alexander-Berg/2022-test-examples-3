package ru.yandex.market.mboc.common.services.mbousers;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.lightmapper.test.EmptyGenericMapperRepositoryMock;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;

/**
 * @author s-ermakov
 */
public class MboUsersRepositoryMock
    extends EmptyGenericMapperRepositoryMock<MboUser, Long> implements MboUsersRepository {
    public MboUsersRepositoryMock() {
        super(MboUser::getUid);
    }

    @Override
    public Optional<MboUser> findByStaffLogin(String staffLogin) {
        return findWhereStream(user -> Objects.equals(user.getStaffLogin(), staffLogin))
            .findFirst();
    }

    @Override
    public Map<String, MboUser> findByStaffLogins(Collection<String> staffLogins) {
        return findWhereStream(user -> staffLogins.contains(user.getStaffLogin()))
            .collect(Collectors.toMap(MboUser::getStaffLogin, Function.identity(), (a, b) -> a));
    }
}
