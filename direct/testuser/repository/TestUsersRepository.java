package ru.yandex.direct.core.entity.testuser.repository;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.testuser.model.TestUser;
import ru.yandex.direct.dbschema.ppcdict.Ppcdict;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;

import static ru.yandex.direct.dbschema.ppcdict.tables.Testusers.TESTUSERS;

@Repository
@ParametersAreNonnullByDefault
public class TestUsersRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestUsersRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public List<TestUser> getAll() {
        return dslContextProvider.ppcdict()
                .select(TESTUSERS.UID, TESTUSERS.DOMAIN_LOGIN, TESTUSERS.ROLE)
                .from(Ppcdict.PPCDICT.TESTUSERS)
                .fetch().stream().map(record -> new TestUser(
                        record.get(TESTUSERS.UID),
                        record.get(TESTUSERS.DOMAIN_LOGIN),
                        roleFromString(record.get(TESTUSERS.ROLE))))
                .collect(Collectors.toList());
    }

    public void insertOrUpdate(TestUser testUser) {
        dslContextProvider.ppcdict()
                .insertInto(Ppcdict.PPCDICT.TESTUSERS)
                .set(TESTUSERS.UID, testUser.getUid())
                .set(TESTUSERS.DOMAIN_LOGIN, testUser.getDomainLogin())
                .set(TESTUSERS.ROLE, RbacRole.toSource(testUser.getRole()).getLiteral())
                .onDuplicateKeyUpdate()
                .set(TESTUSERS.DOMAIN_LOGIN, testUser.getDomainLogin())
                .set(TESTUSERS.ROLE, RbacRole.toSource(testUser.getRole()).getLiteral())
                .execute();
    }

    public int remove(long uid) {
        return dslContextProvider.ppcdict()
                .deleteFrom(Ppcdict.PPCDICT.TESTUSERS)
                .where(TESTUSERS.UID.eq(uid))
                .execute();
    }

    private RbacRole roleFromString(String name) {
        for (RbacRole role : RbacRole.values()) {
            if (RbacRole.toSource(role).getLiteral().equals(name)) {
                return role;
            }
        }
        throw new IllegalArgumentException(String.format("No RbacRole named %s", name));
    }
}
