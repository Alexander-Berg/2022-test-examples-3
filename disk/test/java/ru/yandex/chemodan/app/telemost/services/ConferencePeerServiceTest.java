package ru.yandex.chemodan.app.telemost.services;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.random.Random2;

public class ConferencePeerServiceTest extends TelemostBaseContextTest {

    private static final PassportUid NON_MAIL_PRO_UID = PassportUid.cons(1234L);

    private static final PassportUid MAIL_PRO_UID = PassportUid.cons(1235L);

    private static final PassportUid MAIL_PRO_DISABLED_UID = PassportUid.cons(1236L);

    @Autowired
    private ConferencePeerService conferencePeerService;

    @Before
    public void initUids() {
        super.before();
        addUsers(Cf.map(
                NON_MAIL_PRO_UID.getUid(), UserData.defaultUser("test", Option.of("test"), Option.empty(), Cf.map()),
                MAIL_PRO_UID.getUid(), UserData.defaultUser("test", Option.of("test"), Option.empty(),
                        Cf.map(Random2.R.randomElement(ConferencePeerService.MAIL_PRO_ENABLED_ATTRIBUTES),
                                ConferencePeerService.ENABLE_MAIL_PRO_ATTRIBUTE_VALUE)),
                MAIL_PRO_DISABLED_UID.getUid(), UserData.defaultUser("test", Option.of("test"), Option.empty(),
                        Cf.map(
                                Random2.R.randomElement(ConferencePeerService.MAIL_PRO_ENABLED_ATTRIBUTES),
                                ConferencePeerService.ENABLE_MAIL_PRO_ATTRIBUTE_VALUE,
                                Random2.R.randomElement(ConferencePeerService.MAIL_PRO_DISABLED_ATTRIBUTES), "true"))));
    }

    @Test
    public void testNonMailProUid() {
        User user = conferencePeerService.findUser(PassportOrYaTeamUid.passportUid(NON_MAIL_PRO_UID));
        Assert.assertFalse(user.isMailPro());
    }

    @Test
    public void testMailProUid() {
        User user = conferencePeerService.findUser(PassportOrYaTeamUid.passportUid(MAIL_PRO_UID));
        Assert.assertTrue(user.isMailPro());
    }

    @Test
    public void testMailProDisabledUid() {
        User user = conferencePeerService.findUser(PassportOrYaTeamUid.passportUid(MAIL_PRO_DISABLED_UID));
        Assert.assertFalse(user.isMailPro());
    }
}
