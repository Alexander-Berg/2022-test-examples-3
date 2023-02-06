package ru.yandex.chemodan.app.telemost.services;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.room.proto.MediatorOuterClass;
import ru.yandex.chemodan.app.telemost.room.proto.RoomGrpc;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceClientParameters;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceParticipant;
import ru.yandex.chemodan.app.telemost.services.model.ParticipantsData;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.RoomConnectionInfo;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.time.MoscowTime;

import static org.junit.Assert.assertEquals;

public class ConferenceParticipantsServiceTest extends TelemostBaseContextTest {

    private static final String AVATAR_URL = "https://avatars.mdst.yandex.net/get-yapic/0/0-0/islands-200";

    private static final String NAME = "feelgood";

    @Autowired
    private ConferenceParticipantsService conferenceParticipantsService;

    @Autowired
    @Qualifier("roomServiceBlockingV2")
    private RoomService roomServiceBlockingV2;

    @Autowired
    private RoomGrpc.RoomBlockingStub roomBlockingStub;

    private static final User TEST_USER = new User(PassportOrYaTeamUid.passportUid(PassportUid.cons(3000185708L)),
            Option.of(NAME), Option.of(AVATAR_URL), Option.of(true), true, false, MoscowTime.TZ, "ru");

    @Before
    public void initUser() {
        super.before();
        userService.addUserIfNotExists(TEST_USER.getUid());
        addUsers(Cf.map(TEST_USER.getUid().getPassportUid().getUid(),
                UserData.staff(NAME, Option.of(NAME), Option.of("0/0-0"), Cf.map())));
    }

    @Test
    public void testFindConferenceUsers() {
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.empty())
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        roomServiceBlockingV2.createRoom(conference);

        RoomConnectionInfo connectionInfo1 =
                roomServiceBlockingV2.joinToConference(conference, Option.empty(), Option.of("some_name"),
                        Option.empty(), Option.empty(), ParticipantType.USER);
        RoomConnectionInfo connectionInfo2 =
                roomServiceBlockingV2.joinToConference(conference, Option.of(TEST_USER), Option.of("some_name2"),
                        Option.empty(), Option.empty(), ParticipantType.USER);

        MapF<String, ConferenceParticipant> users = conferenceParticipantsService
                .findConferenceUsers(conference, Cf.list(connectionInfo1.getUserId(), connectionInfo2.getUserId()))
                .toMapMappingToKey(ConferenceParticipant::getPeerId);
        ConferenceParticipant user1 = users.getOrThrow(connectionInfo1.getUserId());
        assertEquals("some_name", user1.getDisplayName());

        ConferenceParticipant user2 = users.getOrThrow(connectionInfo2.getUserId());
        assertUserEquals(user2, TEST_USER);
    }

    @Test
    public void testSyncDataForJustCreatedRoom() {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(PassportUid.cons(3L));
        userService.addUserIfNotExists(passportUser);

        Option<User> user = Option.of(createTestUserForUid(passportUser));
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(user)
                .permanent(Option.empty())
                .staffOnly(Option.empty())
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        roomServiceBlockingV2.createRoomAndUserForConference(conference, user, Option.empty(), Option.of("client-instance-1"));
        ParticipantsData participantsData = conferenceParticipantsService.getParticipantsData(conference);
        Assert.assertEquals(1, participantsData.getParticipantIds().size());
    }

    @Test
    public void testSyncDataForExternallyAddedUsers() {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(PassportUid.cons(4L));
        userService.addUserIfNotExists(passportUser);

        Option<User> user = Option.of(createTestUserForUid(passportUser));
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(user)
                .permanent(Option.empty())
                .staffOnly(Option.empty())
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        roomServiceBlockingV2.createRoomAndUserForConference(conference, user, Option.empty(), Option.of("client-instance-2"));
        roomBlockingStub.addParticipant(MediatorOuterClass.AddParticipantRequest.newBuilder()
                .setRoomId(conference.getRoomId())
                .setParticipantId(UUID.randomUUID().toString()).build());
        ParticipantsData participantsData = conferenceParticipantsService.getParticipantsData(conference);
        Assert.assertEquals(2, participantsData.getParticipantIds().size());
    }

    private void assertUserEquals(ConferenceParticipant user, User userToCompare) {
        assertEquals(userToCompare.getAvatarUrl(), user.getAvatarUrl());
        assertEquals(userToCompare.getDisplayName().getOrElse(""), user.getDisplayName());
        assertEquals(userToCompare.getUid().asString(), user.getUid().map(PassportOrYaTeamUid::asString).getOrElse(""));
    }
}
