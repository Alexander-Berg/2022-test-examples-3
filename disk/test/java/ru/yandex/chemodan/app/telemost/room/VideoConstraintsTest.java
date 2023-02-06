package ru.yandex.chemodan.app.telemost.room;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.room.proto.MediatorOuterClass;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.time.MoscowTime;

public class VideoConstraintsTest extends TelemostBaseContextTest {

    @Autowired
    private RoomBlockingManager roomBlockingManager;

    @Test
    public void testEmptyConstraintsForUser() {
        User user = createTestUserForUid(PassportOrYaTeamUid.passportUid(PassportUid.cons(3L)));
        MediatorOuterClass.AddParticipantRequest request =
                roomBlockingManager.createAddParticipantRequest("peer-id", "room-id", false, Option.of(user));
        Assert.assertFalse(request.getVideoConstraints().getHighDefinitionP2P());
    }

    @Test
    public void testEmptyConstraintsForEmptyUser() {
        MediatorOuterClass.AddParticipantRequest request =
                roomBlockingManager.createAddParticipantRequest("peer-id", "room-id", false, Option.empty());
        Assert.assertFalse(request.getVideoConstraints().getHighDefinitionP2P());
    }

    @Test
    public void testConstraintsForMailProUser() {
        User user = new User(PassportOrYaTeamUid.passportUid(PassportUid.cons(4L)),
                Option.empty(), Option.empty(), Option.empty(), false, true, MoscowTime.TZ, "ru");
        MediatorOuterClass.AddParticipantRequest request =
                roomBlockingManager.createAddParticipantRequest("peer-id", "room-id", false, Option.of(user));
        Assert.assertTrue(request.getVideoConstraints().getHighDefinitionP2P());
    }

    @Test
    public void testConstraintsForUserInExperiment() {
        PassportUid uid = PassportUid.cons(5L);
        User user = new User(PassportOrYaTeamUid.passportUid(uid), Option.empty(), Option.empty(), Option.empty(),
                false, false, MoscowTime.TZ, "ru");
        addExperimentFlagForUser(uid, AbstractRoomManager.VIDEO_CONSTRAINTS_MAIL_PRO_EXPERIMENT_FLAG);
        MediatorOuterClass.AddParticipantRequest request =
                roomBlockingManager.createAddParticipantRequest("peer-id", "room-id", false, Option.of(user));
        Assert.assertTrue(request.getVideoConstraints().getHighDefinitionP2P());
    }
}
