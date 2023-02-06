package ru.yandex.chemodan.app.telemost.web.v2;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceUserDao;
import ru.yandex.chemodan.app.telemost.repository.model.UserRole;
import ru.yandex.chemodan.app.telemost.web.v1.TelemostCreateConferenceWebActionTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.test.Assert;

public class TelemostSetUserRoleWebActionTest extends AbstractConferenceWebActionTest {

    @Autowired
    ConferenceUserDao conferenceUserDao;

    @Before
    public void initUids() {
        super.before();
        addUsers(Cf.map(100L, UserData.defaultUser("owner", Option.of("owner"), Option.empty(), Cf.map())));
        addUsers(Cf.map(200L, UserData.defaultUser("member", Option.of("member"), Option.empty(), Cf.map())));
    }

    @Test
    public void testCreateConferenceWithUid() throws IOException {
        String conferenceUri = testCreateConferenceByLink("/v2/conferences?uid=100",
                ru.yandex.chemodan.app.telemost.web.v1.TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
        testJoinConferenceByLink("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) + "/connection?uid=200", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
        A3TestHelper helper = getA3TestHelper();
        String encodedUri = URLEncoder.encode(conferenceUri, "UTF-8");
        HttpResponse response = helper.post(
                String.format("/v2/conferences/%s/commands/set-user-role?uid=%s&target_uid=%s&user_role=%s", encodedUri, "100", "200", UserRole.ADMIN.name()));
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    }

}
