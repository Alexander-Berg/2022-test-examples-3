package ru.yandex.chemodan.app.telemost.services.model;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.repository.dao.UserStateDtoDao;
import ru.yandex.chemodan.app.telemost.repository.model.UserStateDto;
import ru.yandex.chemodan.app.telemost.tools.ConferenceHelper;

public class LbPassportConsumerTest extends TelemostBaseContextTest {

    private static final int TEST_UID = 111;
    @Autowired
    private LbPassportConsumer lbPassportConsumer;
    @Autowired
    private UserStateDtoDao userStateDtoDao;
    @Autowired
    private ConferenceHelper conferenceHelper;
    private Conference TEST_CONFERENCE;
    private String TEST_PEER_ID;

    @Before
    public void init() {
        clearUserData();
        User user = createTestUserForUid(TEST_UID);

        userService.addUserIfNotExists(user.getUid());

        TEST_CONFERENCE = conferenceHelper.createConference(user);
        TEST_PEER_ID = conferenceHelper.joinConference(TEST_CONFERENCE, user).getUserId();
    }

    @Test
    public void passportDataUpdatedByLog() {
        UserData blackboxData = UserData.defaultUser("test", Option.of("name"), Option.of("ava"), Cf.map());
        addUser((long) TEST_UID, blackboxData);

        validatePassportRequired(TEST_CONFERENCE.getDbId(), TEST_PEER_ID, true);
        lbPassportConsumer.updatePassportData(TEST_UID + "");
        validatePassportRequired(TEST_CONFERENCE.getDbId(), TEST_PEER_ID, false);
    }


    @Test
    public void passportDataRequiredIfFailToGetPassportData() {
        userStateDtoDao.setPassportDataRequiredByUid(PassportOrYaTeamUid.parseUid(TEST_UID + ""), false);
        validatePassportRequired(TEST_CONFERENCE.getDbId(), TEST_PEER_ID, false);
        try {
            lbPassportConsumer.updatePassportData(111 + "");
        } catch (Exception e) {
            validatePassportRequired(TEST_CONFERENCE.getDbId(), TEST_PEER_ID, true);
            return;
        }
        Assert.fail("Exception expected");
    }

    private void validatePassportRequired(UUID conferenceId, String peerId, boolean expected) {
        MapF<String, UserStateDto> states = userStateDtoDao.findStates(conferenceId, Cf.list(peerId));
        for (UserStateDto state : states.values()) {
            Assert.assertEquals(expected, state.isPassportDataRequired());
        }
    }

}
