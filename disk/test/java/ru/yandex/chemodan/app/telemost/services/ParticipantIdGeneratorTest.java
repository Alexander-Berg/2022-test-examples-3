package ru.yandex.chemodan.app.telemost.services;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceClientParameters;

public class ParticipantIdGeneratorTest extends TelemostBaseContextTest {

    @Autowired
    private ParticipantIdGenerator participantIdGenerator;

    @Autowired
    private ConferenceService conferenceService;

    @Test
    public void testEmptyClientInstanceId() {
        Conference conference = createConference();
        String peerId1 = participantIdGenerator.createParticipantId(conference, Option.empty(), Option.empty());
        String peerId2 = participantIdGenerator.createParticipantId(conference, Option.empty(), Option.empty());
        Assert.assertNotEquals(peerId1, peerId2);
    }

    @Test
    public void testSamePeerId() {
        Conference conference = createConference();
        Option<String> clientInstanceId = Option.of("client-instance-id-1");
        Option<String> uid = Option.of("123");
        String peerIdWithUid1 = participantIdGenerator.createParticipantId(conference, clientInstanceId, uid);
        String peerIdWithUid2 = participantIdGenerator.createParticipantId(conference, clientInstanceId, uid);
        Assert.assertEquals(peerIdWithUid1, peerIdWithUid2);

        String peerIdWithoutUid1 = participantIdGenerator.createParticipantId(conference, clientInstanceId,
                Option.empty());
        String peerIdWithoutUid2 = participantIdGenerator.createParticipantId(conference, clientInstanceId,
                Option.empty());
        Assert.assertEquals(peerIdWithoutUid1, peerIdWithoutUid2);

        Assert.assertNotEquals(peerIdWithUid1, peerIdWithoutUid1);
    }

    @Test
    public void testDifferentConferences() {
        Conference conference1 = createConference();
        Conference conference2 = createConference();
        Option<String> clientInstanceId = Option.of("client-instance-id-2");
        Option<String> uid = Option.of("124");
        String peerIdWithConference1 = participantIdGenerator.createParticipantId(conference1, clientInstanceId, uid);
        String peerIdWithConference2 = participantIdGenerator.createParticipantId(conference2, clientInstanceId, uid);
        Assert.assertNotEquals(peerIdWithConference1, peerIdWithConference2);
    }

    @Test
    public void testDifferentClientInstanceIds() {
        Conference conference = createConference();
        Option<String> clientInstanceId1 = Option.of("client-instance-id-3");
        Option<String> clientInstanceId2 = Option.of("client-instance-id-4");
        Option<String> uid = Option.of("125");
        String peerIdWithClientInstanceId1 = participantIdGenerator.createParticipantId(conference, clientInstanceId1,
                uid);
        String peerIdWithClientInstanceId2 = participantIdGenerator.createParticipantId(conference, clientInstanceId2,
                uid);
        Assert.assertNotEquals(peerIdWithClientInstanceId1, peerIdWithClientInstanceId2);
    }

    @Test
    public void testDifferentUidsAndClientInstanceIds() {
        Conference conference = createConference();
        Option<String> clientInstanceId1 = Option.of("client-instance-id-5");
        Option<String> uid1 = Option.of("126");

        Option<String> clientInstanceId2 = Option.of("client-instance-id-6");
        Option<String> uid2 = Option.of("127");

        String peerId1 = participantIdGenerator.createParticipantId(conference, clientInstanceId1, uid1);
        String peerId2 = participantIdGenerator.createParticipantId(conference, clientInstanceId2, uid2);
        Assert.assertNotEquals(peerId1, peerId2);
    }

    private Conference createConference() {
        return conferenceService.generateConference(ConferenceClientParameters.builder()
                .externalMeeting(Option.empty())
                .permanent(Option.empty())
                .staffOnly(Option.empty())
                .user(Option.empty())
                .eventId(Option.empty()).build());
    }
}
