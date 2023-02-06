package ru.yandex.direct.core.entity.user.repository;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.USERS_OPTIONS;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UserRepositoryFetchByUidsNoUsersOptionsTest {

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private UserRepository userRepository;
    private UserInfo userInfo1;

    @Before
    public void setUp() throws Exception {
        userInfo1 = steps.userSteps().createDefaultUser();
        dslContextProvider.ppc(userInfo1.getShard())
                .deleteFrom(USERS_OPTIONS)
                .where(USERS_OPTIONS.UID.eq(userInfo1.getUid()))
                .execute();
    }

    @Test
    public void fetchByUids() {
        List<Long> uids = List.of(userInfo1.getUid());
        List<User> actualUsers = userRepository.fetchByUids(userInfo1.getShard(), uids);
        assertThat(actualUsers).hasSize(1);
    }

}
