package ru.yandex.chemodan.app.telemost.web.v2;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.repository.dao.UserDao;
import ru.yandex.chemodan.app.telemost.repository.model.UserDto;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.web.TelemostWebActionBaseTest;
import ru.yandex.chemodan.app.telemost.web.v2.actions.BillingActionsV2;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

public class TelemostBillingWebActionTest extends TelemostWebActionBaseTest {

    public static final String EXISTING_UID = "100500111";
    public static final String NON_EXISTING_UID = "100500112";

    @Autowired
    public JdbcTemplate3 jdbcTemplate;

    @Autowired
    public BillingActionsV2 billingActionsV2;

    @Autowired
    public UserDao userDao;

    @Before
    public void resetUIDs() {
        userDao.upsert(PassportOrYaTeamUid.parseUid(EXISTING_UID), false);
        jdbcTemplate.update("delete from telemost.users where uid = ? ", NON_EXISTING_UID);
    }

    @Test
    public void checkBroadcastEnables() {
        billingActionsV2.enableBroadcastFeature(NON_EXISTING_UID);
        billingActionsV2.enableBroadcastFeature(EXISTING_UID);

        Option<UserDto> existingUser = userDao.findByUid(PassportOrYaTeamUid.parseUid(EXISTING_UID));
        Option<UserDto> nonExistingUser = userDao.findByUid(PassportOrYaTeamUid.parseUid(NON_EXISTING_UID));

        Assert.notEmpty(existingUser);
        Assert.notEmpty(nonExistingUser);
        Assert.isTrue(existingUser.get().isBroadcastEnabled());
        Assert.isTrue(nonExistingUser.get().isBroadcastEnabled());
    }

    @Test
    public void checkBroadcastDisables() {
        billingActionsV2.disableBroadcastFeature(NON_EXISTING_UID);
        billingActionsV2.disableBroadcastFeature(EXISTING_UID);

        Option<UserDto> existingUser = userDao.findByUid(PassportOrYaTeamUid.parseUid(EXISTING_UID));
        Option<UserDto> nonExistingUser = userDao.findByUid(PassportOrYaTeamUid.parseUid(NON_EXISTING_UID));

        Assert.notEmpty(existingUser);
        Assert.notEmpty(nonExistingUser);
        Assert.isFalse(existingUser.get().isBroadcastEnabled());
        Assert.isFalse(nonExistingUser.get().isBroadcastEnabled());
    }

}
