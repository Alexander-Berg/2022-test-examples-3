package ru.yandex.chemodan.app.telemost.web.v2;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.misc.test.Assert;


public class TelemostCreateConferenceWebActionTest extends AbstractConferenceWebActionTest {

    @Before
    public void initUids() {
        super.before();
        addUsers(Cf.map(123L, UserData.defaultUser("test", Option.of("test"), Option.empty(), Cf.map())));
    }

    @Test
    public void testCreateConferenceWithUid() throws IOException {
        Assert.equals(userService.upsert(PassportOrYaTeamUid.parseUid("123"), true).isBroadcastEnabled(),
                true);
        testCreateConferenceByLink("/v2/conferences?uid=123",
                ru.yandex.chemodan.app.telemost.web.v1.TelemostCreateConferenceWebActionTest.URL_PATTERN,
                true);
    }

    @Test
    public void testCreateConferenceWithoutUid() throws IOException {
        testCreateConferenceByLink("/v2/conferences",
                ru.yandex.chemodan.app.telemost.web.v1.TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
    }

    @Test
    public void testCreateConferenceWithYaTeamUid() throws IOException {
        Assert.equals(userService.upsert(PassportOrYaTeamUid.parseUid("yt:123"), true).isBroadcastEnabled(),
                true);
        testCreateConferenceByLink("/v2/conferences?uid=yt:123",
                ru.yandex.chemodan.app.telemost.web.v1.TelemostCreateConferenceWebActionTest.YA_TEAM_URL_PATTERN,
                true);
    }

    @Test
    public void testCreateConferenceWithYaTeamUidAndExternalMeetings() throws IOException {
        Assert.equals(userService.upsert(PassportOrYaTeamUid.parseUid("yt:123"), true).isBroadcastEnabled(),
                true);
        testCreateConferenceByLink("/v2/conferences?uid=yt:123&external_meeting=true",
                ru.yandex.chemodan.app.telemost.web.v1.TelemostCreateConferenceWebActionTest.URL_PATTERN,
                true);
    }

}
