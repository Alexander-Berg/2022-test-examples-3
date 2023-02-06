package ru.yandex.direct.core.entity.testuser.service;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.direct.core.entity.testuser.model.TestUser;
import ru.yandex.direct.core.entity.testuser.repository.TestUsersRepository;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacRole;

import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@Service
@ParametersAreNonnullByDefault
public class TestUsersService {

    private final ShardHelper shardHelper;
    private final TestUsersRepository testUsersRepository;

    @Autowired
    public TestUsersService(ShardHelper shardHelper, TestUsersRepository testUsersRepository) {
        this.shardHelper = shardHelper;
        this.testUsersRepository = testUsersRepository;
    }

    public List<TestUser> getAll() {
        List<TestUser> users = testUsersRepository.getAll();
        List<Long> uids = mapList(users, TestUser::getUid);

        var loginStream = StreamEx.of(shardHelper.getLoginsByUids(uids))
                .map(l -> l.stream().findFirst().orElse(null));

        var logins = StreamEx.of(uids).zipWith(loginStream).toMap();

        users.forEach(user -> user.setLogin(logins.get(user.getUid())));
        return users;
    }

    public void setRole(Long uid, String domainLogin, RbacRole role) {
        testUsersRepository.insertOrUpdate(new TestUser(uid, domainLogin, role));
    }

    /**
     * @return true, если тестовый пользователь был удален
     */
    public boolean remove(Long uid) {
        int deletedCount = testUsersRepository.remove(uid);
        return deletedCount > 0;
    }
}
