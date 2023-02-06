package ru.yandex.direct.core.entity.user.repository;

import java.util.Collection;

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.user.UserLangUtils.INT_UKRAINIAN_LANG_SUBTAG;
import static ru.yandex.direct.dbschema.ppc.Tables.USERS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UserRepositoryUpdateTest {
    public static final String OLD_FIO = "qqq";
    public static final String OLD_EMAIL = "ddd@ggg.ru";
    public static final String OLD_PHONE = "8273282";
    public static final Language OLD_LANGUAGE = Language.RU;
    public static final boolean OLD_SEND_NEWS = false;
    public static final boolean OLD_SEND_WARN = true;
    public static final boolean OLD_SEND_ACC_NEWS = false;
    public static final Long OLD_GEO_ID = 213L;

    public static final String NEW_FIO = "aaa";
    public static final String NEW_EMAIL = "aaa@ggg.ru";
    public static final String NEW_PHONE = "11111111";
    public static final Language NEW_LANGUAGE = Language.TR;
    public static final boolean NEW_SEND_NEWS = true;
    public static final boolean NEW_SEND_WARN = false;
    public static final boolean NEW_SEND_ACC_NEWS = true;
    public static final Long NEW_GEO_ID = 2L;
    public static final String NEW_DOMAIN_LOGIN = "NEW_DOMAIN_LOGIN";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private Steps steps;

    private UserInfo userInfo;
    private Integer shard;
    private Long uid;
    private User referenceUser;
    private UserInfo secondUserInfo;

    @Before
    public void before() {
        userInfo = steps.userSteps().createUser(createTestUser());
        referenceUser = userInfo.getUser();
        uid = userInfo.getUid();
        shard = userInfo.getShard();

        secondUserInfo = steps.userSteps().createUser(createTestUser());
    }

    private User createTestUser() {
        return TestUsers.generateNewUser()
                .withFio(OLD_FIO)
                .withEmail(OLD_EMAIL)
                .withPhone(OLD_PHONE)
                .withLang(OLD_LANGUAGE)
                .withSendNews(OLD_SEND_NEWS)
                .withSendWarn(OLD_SEND_WARN)
                .withSendAccNews(OLD_SEND_ACC_NEWS)
                .withGeoId(OLD_GEO_ID);
    }

    @Test
    public void updateShouldNotModifyUserWhenArgumentIsEmpty() {
        Collection<User> users = userRepository.fetchByUids(shard, singleton(uid));
        assumeThat(users, hasSize(1));
        User userBeforeUpdate = Iterables.getFirst(users, null);

        userRepository.update(shard, emptyList());

        Collection<User> usersAfter = userRepository.fetchByUids(shard, singleton(uid));
        assumeThat(usersAfter, hasSize(1));
        User userAfterUpdate = Iterables.getFirst(usersAfter, null);

        assertThat(userAfterUpdate, beanDiffer(userBeforeUpdate));
    }

    @Test
    public void updateShouldNotModifyUserWhenModelChangesIsEmpty() {
        Collection<User> users = userRepository.fetchByUids(shard, singleton(uid));
        assumeThat(users, hasSize(1));
        User userBeforeUpdate = Iterables.getFirst(users, null);

        ModelChanges<User> modelChanges = userModelChanges(uid);
        AppliedChanges<User> appliedChanges = modelChanges.applyTo(userBeforeUpdate);

        userRepository.update(shard, singleton(appliedChanges));

        Collection<User> usersAfter = userRepository.fetchByUids(shard, singleton(uid));
        assumeThat(usersAfter, hasSize(1));
        User userAfterUpdate = Iterables.getFirst(usersAfter, null);

        assertThat(userAfterUpdate, beanDiffer(userBeforeUpdate));
    }

    @Test
    public void updateOne() {
        ModelChanges<User> modelChanges = userModelChanges(uid);
        modelChanges.process(NEW_FIO, User.FIO);
        modelChanges.process(NEW_PHONE, User.PHONE);
        modelChanges.process(NEW_EMAIL, User.EMAIL);
        modelChanges.process(NEW_SEND_NEWS, User.SEND_NEWS);
        modelChanges.process(NEW_SEND_ACC_NEWS, User.SEND_ACC_NEWS);
        modelChanges.process(NEW_SEND_WARN, User.SEND_WARN);
        modelChanges.process(NEW_LANGUAGE, User.LANG);
        modelChanges.process(NEW_GEO_ID, User.GEO_ID);
        modelChanges.process(NEW_DOMAIN_LOGIN, User.DOMAIN_LOGIN);

        AppliedChanges<User> appliedChanges = modelChanges.applyTo(referenceUser);

        userRepository.update(shard, singleton(appliedChanges));

        Collection<User> usersAfter = userRepository.fetchByUids(shard, singleton(uid));
        assumeThat(usersAfter, hasSize(1));
        User userAfterUpdate = Iterables.getFirst(usersAfter, null);

        assertThat(userAfterUpdate, allOf(
                hasProperty("fio", is(NEW_FIO)),
                hasProperty("phone", is(NEW_PHONE)),
                hasProperty("email", is(NEW_EMAIL)),
                hasProperty("sendNews", is(NEW_SEND_NEWS)),
                hasProperty("sendAccNews", is(NEW_SEND_ACC_NEWS)),
                hasProperty("sendWarn", is(NEW_SEND_WARN)),
                hasProperty("lang", is(NEW_LANGUAGE)),
                hasProperty("geoId", is(NEW_GEO_ID)),
                hasProperty("domainLogin", is(NEW_DOMAIN_LOGIN))
        ));
    }

    @Test
    public void updateOneShouldNotAffectAnotherUser() {
        assumeThat(secondUserInfo.getUid(), is(not(uid)));
        ModelChanges<User> modelChanges = userModelChanges(uid);
        modelChanges.process(NEW_FIO, User.FIO);
        AppliedChanges<User> appliedChanges = modelChanges.applyTo(referenceUser);
        userRepository.update(shard, singleton(appliedChanges));

        User secondUserAfterUpdate = Iterables.getFirst(
                userRepository.fetchByUids(secondUserInfo.getShard(), singleton(secondUserInfo.getUid())), null);
        assumeThat(secondUserAfterUpdate, notNullValue());
        assertThat(secondUserAfterUpdate.getFio(), is(OLD_FIO));
    }

    @Test
    public void updateUkrainianLanguage() {
        ModelChanges<User> modelChanges = userModelChanges(uid);
        modelChanges.process(Language.UK, User.LANG);
        AppliedChanges<User> appliedChanges = modelChanges.applyTo(referenceUser);
        userRepository.update(shard, singleton(appliedChanges));

        String actualLang = dslContextProvider.ppc(shard)
                .select(USERS.LANG)
                .from(USERS)
                .where(USERS.UID.eq(uid))
                .fetchOne(USERS.LANG);
        assertThat(actualLang, is(INT_UKRAINIAN_LANG_SUBTAG));
    }

    private static ModelChanges<User> userModelChanges(Long id) {
        return new ModelChanges<>(id, User.class);
    }
}
