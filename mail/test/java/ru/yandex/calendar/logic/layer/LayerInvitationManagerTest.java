package ru.yandex.calendar.logic.layer;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.LayerInvitation;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.idlent.YandexUser;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.email.Email;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LayerInvitationManagerTest extends AbstractConfTest {
    @Autowired
    private LayerInvitationManager layerInvitationManager;
    @Autowired
    private LayerInvitationDao layerInvitationDao;
    @Autowired
    private TestManager testManager;

    private long layerId;
    private TestUserInfo creator;
    private TestUserInfo guest;

    @Before
    public void clearBeforeTest() {
        creator = testManager.prepareUser("yandex-team-mm-19159");
        guest = testManager.prepareUser("yandex-team-mm-19161");

        layerId = creator.getDefaultLayerId();
    }

    // CAL-6829. А это выглядит как очень большой костыль, уносить в сценарии пока не стал. Завел задачу GREG-836.
    @Test
    public void acceptWithSocialAuth() {
        Email externalEmail = new Email("brylevdaniel@google.com");

        layerInvitationManager.updateLayerSharing(creator.getUserInfo(), layerId,
                Map.of(externalEmail, LayerActionClass.VIEW), true, ActionInfo.webTest());

        TestUserInfo user = testManager.prepareYandexUser(new YandexUser(
                guest.getUid(), PassportLogin.cons(externalEmail.getLocalPart()),
                Option.empty(), Option.of(externalEmail), Option.empty(), Option.empty(), Option.empty()));

        assertThat(findLayerInvitation(user)).isEmpty();

        layerInvitationManager.createLayerInvitationIfAbsentForUser(
                layerId, user.getUid(), LayerActionClass.VIEW, ActionInfo.webTest());

        assertThat(findLayerInvitation(user)).isNotEmpty();
    }

    private Option<LayerInvitation> findLayerInvitation(TestUserInfo user) {
        return layerInvitationDao.findInvitationByLayerIdAndUid(layerId, user.getUid());
    }
}
