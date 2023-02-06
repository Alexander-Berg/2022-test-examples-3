package ru.yandex.chemodan.app.telemost.broadcasts;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceStateDao;
import ru.yandex.chemodan.app.telemost.repository.model.ConferenceStateDto;
import ru.yandex.chemodan.app.telemost.services.BroadcastService;
import ru.yandex.chemodan.app.telemost.services.ChatService;
import ru.yandex.chemodan.app.telemost.services.model.Broadcast;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.chemodan.app.telemost.tools.ConferenceHelper;
import ru.yandex.chemodan.app.telemost.web.v2.model.BroadcastInitData;
import ru.yandex.inside.passport.PassportUid;

public class BroadcastServiceTest extends TelemostBaseContextTest {
    private static final PassportOrYaTeamUid TEST_UID = PassportOrYaTeamUid.passportUid(PassportUid.cons(18));

    @Autowired
    private BroadcastService broadcastService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConferenceStateDao conferenceStateDao;

    @Autowired
    private ConferenceHelper conferenceHelper;

    private Conference conference;

    @Before
    public void init() {
        User testUser = createTestUserForUid(TEST_UID);

        userService.addUserIfNotExists(testUser.getUid());

        conference = generateConference(testUser);
    }

    @Test
    public void testCreateBroadcast() {
        Option<String> caption = Option.of("Caption");
        Option<String> description = Option.of("Description");

        Broadcast broadcast1 = broadcastService.createBroadcast(conference.getDbId(), TEST_UID.asString(),
                new BroadcastInitData(caption, description));

        Assert.assertEquals(conference.getDbId(), broadcast1.getBroadcast().getConferenceId());
        Assert.assertNotEquals("", broadcast1.getBroadcast().getBroadcastKey());
        Assert.assertEquals(caption, broadcast1.getBroadcast().getCaption());
        Assert.assertEquals(description, broadcast1.getBroadcast().getDescription());

        // Repeat creation broadcast
        Broadcast broadcast2 = broadcastService.createBroadcast(conference.getDbId(), TEST_UID.asString(),
                new BroadcastInitData());

        Assert.assertEquals(conference.getDbId(), broadcast2.getBroadcast().getConferenceId());
        Assert.assertNotEquals("", broadcast2.getBroadcast().getBroadcastKey());
        Assert.assertEquals(broadcast1.getBroadcast().getBroadcastKey(),
                broadcast2.getBroadcast().getBroadcastKey());
        Assert.assertEquals(caption, broadcast2.getBroadcast().getCaption());
        Assert.assertEquals(description, broadcast2.getBroadcast().getDescription());
    }

    @Test
    public void testChatCreatedWithAbsentCalendarEvent() {
        Conference conference = conferenceHelper.toConference(conferenceHelper.conferenceDefaultBuilder()
                .eventId(Option.of("some_non_existing"))
        );
        Broadcast broadcast = broadcastService.createBroadcast(conference.getDbId(), TEST_UID.asString(),
                new BroadcastInitData(Option.of("Caption"), Option.of("Description")));
        chatService.tryCreateBroadcastChatIfNeed(broadcast.getUri());
        //assert doesn't fail with NoSuchElementException or ConferenceNotFoundTelemostException
    }

    @Test
    public void testIncrementVersionWhenChatCreated() {
        Option<String> caption = Option.of("Caption");
        Option<String> description = Option.of("Description");

        Broadcast broadcast = broadcastService.createBroadcast(conference.getDbId(), TEST_UID.asString(),
                new BroadcastInitData(caption, description));

        Option<ConferenceStateDto> conferenceStateDto1 = conferenceStateDao.findState(conference.getDbId());

        chatService.tryCreateBroadcastChatIfNeed(broadcast.getUri());

        Option<ConferenceStateDto> conferenceStateDto2 = conferenceStateDao.findState(conference.getDbId());

        Assert.assertEquals(conferenceStateDto1.get().getVersion() + 1, conferenceStateDto2.get().getVersion());
    }
}
