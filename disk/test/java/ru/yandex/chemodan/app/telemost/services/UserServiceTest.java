package ru.yandex.chemodan.app.telemost.services;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.repository.model.UserDto;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.inside.passport.PassportUid;

public class UserServiceTest extends TelemostBaseContextTest {
    private static final PassportOrYaTeamUid TEST_UID = PassportOrYaTeamUid.passportUid(PassportUid.cons(376338392));

    @Test
    public void testAddAndFindUserByUid() {
        // Add new user
        userService.addUserIfNotExists(TEST_UID, true);

        // Find added user and check features values
        Option<UserDto> user = userService.findByUid(TEST_UID);
        Assert.assertTrue(user.isPresent());
        Assert.assertTrue(user.get().isBroadcastEnabled());
    }
}
