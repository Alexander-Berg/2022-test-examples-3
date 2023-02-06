package ru.yandex.chemodan.app.telemost.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.exceptions.ConferenceNotFoundTelemostException;
import ru.yandex.chemodan.app.telemost.repository.dao.UserStateDtoDao;
import ru.yandex.chemodan.app.telemost.services.ConferenceParticipantsService;
import ru.yandex.chemodan.app.telemost.services.ConferenceService;
import ru.yandex.chemodan.app.telemost.services.ParticipantType;
import ru.yandex.chemodan.app.telemost.services.RoomService;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceClientParameters;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceParticipant;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.RoomConnectionInfo;
import ru.yandex.chemodan.app.telemost.services.model.User;

import static ru.yandex.chemodan.app.telemost.TelemostBaseContextTest.TEST_OWNER;

public class ConferenceHelper {
    public static final String BROADCASTER_DISPLAY_NAME = "Broadcaster";

    @Autowired
    private ConferenceService conferenceService;

    @Autowired
    @Qualifier("roomServiceBlockingV2")
    private RoomService roomService;

    @Autowired
    private ConferenceParticipantsService conferenceParticipantsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserStateDtoDao userStateDao;

    public Conference createConference(PassportOrYaTeamUid uid) {
        User user = UserHelper.createTestUserForUid(uid);
        return createConference(user);
    }

    public Conference createConference(User user) {
        return createConference(user, Option.of(Boolean.FALSE));
    }

    @Deprecated
    // Конференции без owner'a создавать по новым правилам нельзя. Оставлено для старых тестов
    public Conference createConference() {
        return createConference(false);
    }

    @Deprecated
    // Конференции без owner'a создавать по новым правилам нельзя. Оставлено для старых тестов
    public Conference createConference(boolean permanent) {
        return conferenceService.generateConference(
                ConferenceClientParameters.builder()
                        .user(Option.empty())
                        .permanent(Option.of(permanent))
                        .staffOnly(Option.of(Boolean.FALSE))
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
    }

    public Conference createConference(User user, Option<Boolean> permanent) {
        return toConference(conferenceDefaultBuilder()
                .user(Option.of(user))
                .permanent(permanent)
        );
    }

    public Conference toConference(ConferenceClientParameters.ConferenceClientParametersBuilder builder) {
        return conferenceService.generateConference(builder.build());
    }

    public ConferenceClientParameters.ConferenceClientParametersBuilder conferenceDefaultBuilder() {
        return ConferenceClientParameters.builder()
                .user(Option.of(UserHelper.createTestUserForUid(TEST_OWNER)))
                .permanent(Option.of(false))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.of(Boolean.TRUE))
                .eventId(Option.empty());
    }

    public void connect(Conference conference, PassportOrYaTeamUid uid) {
        connect(conference, UserHelper.createTestUserForUid(uid));
    }

    public void connect(Conference conference, User user) {
        conferenceService.joinConference(Option.of(user), conferenceService.getConferenceUriId(conference.getUri()),
                Option.empty(), Option.empty());
    }

    public void joinConference(Conference conference, PassportOrYaTeamUid uid) {
        joinConference(conference, UserHelper.createTestUserForUid(uid));
    }

    public RoomConnectionInfo joinConference(Conference conference, User user) {
        return joinConference(conference, user, objectMapper.valueToTree(new Object()));
    }

    public RoomConnectionInfo joinConference(Conference conference) {
        return joinConference(conference, objectMapper.valueToTree(new Object()));
    }

    public RoomConnectionInfo joinConference(Conference conference, JsonNode state) {
        return joinConferenceImpl(conference, Option.empty(), Option.empty(), state);
    }

    public RoomConnectionInfo joinConference(Conference conference, String displayName) {
        return joinConference(conference, displayName, objectMapper.valueToTree(new Object()));
    }

    public RoomConnectionInfo joinConference(Conference conference, String displayName, JsonNode state) {
        return joinConferenceImpl(conference, Option.empty(), Option.of(displayName), state);
    }

    public RoomConnectionInfo joinConference(Conference conference, User user, JsonNode state) {
        return joinConferenceImpl(conference, Option.of(user), Option.empty(), state);
    }

    private RoomConnectionInfo joinConferenceImpl(Conference conference, Option<User> user,
                                                  Option<String> displayName, JsonNode state) {
        RoomConnectionInfo roomInfo = joinToConferenceImpl(conference, user, displayName);
        if (state != null) {
            addState(roomInfo.getRoomId(), roomInfo.getUserId(), state);
        }
        return roomInfo;
    }

    private RoomConnectionInfo joinToConferenceImpl(Conference conference, Option<User> user,
                                                    Option<String> displayName) {
        conferenceService.joinConference(user, conferenceService.getConferenceUriId(conference.getUri()),
                Option.empty(),
                Option.empty());
        if (displayName.isPresent() && displayName.get().equals(BROADCASTER_DISPLAY_NAME)) {
            return roomService.joinToConference(conference, user, displayName, Option.empty(), Option.empty(),
                    ParticipantType.TRANSLATOR);
        }
        return roomService.joinToConference(conference, user, displayName, Option.empty(), Option.empty(),
                ParticipantType.USER);
    }

    private void addState(String roomId, String peerId, JsonNode state) {
        Conference conference = conferenceService.findConferenceUnsafe(roomId)
                .getOrThrow(ConferenceNotFoundTelemostException::new);
        ConferenceParticipant participant = conferenceParticipantsService.findConferenceParticipant(conference, peerId);
        userStateDao.updatePeerState(participant.getDbUserId(), state);
    }
}
