package ru.yandex.direct.core.entity.user.repository;

import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.ClientWithOptions;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UserRepositoryAddSubclientTest {
    private static final int SHARD = 1;

    private static final String SUBCLIENT_LOGIN = "subclient";
    private static final String SUBCLIENT_EMAIL = "subclient@yandex-team.ru";
    private static final String SUBCLIENT_NAME = SUBCLIENT_LOGIN;
    private static final Language SUBCLIENT_NOTIFICATION_LANG = Language.RU;

    @Autowired
    private Steps steps;

    @Autowired
    private UserRepository userRepository;

    private UidAndClientId subclientUidAndClientId;

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        subclientUidAndClientId = UidAndClientId.of(steps.userSteps().generateNewUserUid(),
                clientInfo.getClientId());
    }

    @Test
    public void testAddSubclient() {
        ClientWithOptions subclient = new ClientWithOptions()
                .withUid(subclientUidAndClientId.getUid())
                .withRole(RbacRole.CLIENT)
                .withLogin(SUBCLIENT_LOGIN)
                .withEmail(SUBCLIENT_EMAIL)
                .withName(SUBCLIENT_NAME)
                .withClientId(subclientUidAndClientId.getClientId())
                .withNotificationLang(SUBCLIENT_NOTIFICATION_LANG)
                .withSendNews(true)
                .withSendAccNews(true)
                .withSendWarn(true);

        userRepository.addClient(SHARD, subclient);

        Collection<User> result = userRepository.fetchByUids(SHARD, singleton(subclient.getUid()));

        assertThat(
                result,
                Matchers.contains(
                        allOf(
                                hasProperty("uid", equalTo(subclient.getUid())),
                                hasProperty("login", equalTo(subclient.getLogin())),
                                hasProperty("email", equalTo(subclient.getEmail())),
                                hasProperty("fio", equalTo(subclient.getLogin())),
                                hasProperty("clientId", equalTo(subclient.getClientId())),
                                hasProperty("statusEasy", equalTo(false)),
                                hasProperty("lang", equalTo(subclient.getNotificationLang())),
                                hasProperty("sendNews", equalTo(subclient.isSendNews())),
                                hasProperty("sendAccNews", equalTo(subclient.isSendAccNews())),
                                hasProperty("sendWarn", equalTo(subclient.isSendWarn())))));
    }
}
