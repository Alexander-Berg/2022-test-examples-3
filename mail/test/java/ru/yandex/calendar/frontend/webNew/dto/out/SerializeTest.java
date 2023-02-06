package ru.yandex.calendar.frontend.webNew.dto.out;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.CollectorsF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.webNew.WebNewCodecs;
import ru.yandex.calendar.frontend.webNew.dto.inOut.EventAttachmentData;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.event.EventActions;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.repetition.RepetitionRoutines;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.layer.UserLayersSharing;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.perm.Authorizer;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;

public class SerializeTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private RepetitionRoutines repetitionRoutines;
    @Autowired
    private Authorizer authorizer;

    private String serializeWebEventInfo(WebEventInfo object) {
        return new String(WebNewCodecs.getSerializer(WebEventInfo.class).serializeJson(object));
    }

    @Test
    public void eventInfo() throws IOException {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10007");
        TestUserInfo attendee1 = testManager.prepareUser("yandex-team-mm-10008");
        TestUserInfo attendee2 = testManager.prepareUser("yandex-team-mm-10009");
        Instant start = TestDateTimes.moscow(2012, 3, 27, 13, 6);
        Instant end = TestDateTimes.moscow(2012, 3, 27, 14, 6);
        Event event = testManager.createDefaultEvent(organizer.getUid(), "eventInfo", start, end);

        EventAttachmentData attachment = new EventAttachmentData("id", "file", 1);

        WebUserParticipantInfo userOrganizer = toAttendee(organizer);
        ListF<WebUserParticipantInfo> userAttendees = StreamEx.of(organizer, attendee1, attendee2).map(this::toAttendee).collect(CollectorsF.toList());
        ListF<WebUserParticipantInfo> optionalAttendees = Cf.list();

        EventWithRelations eventWithRelations = eventDbManager.getEventWithRelationsById(event.getId());
        WebEventInfo eventInfo = WebEventInfo.create(
                eventWithRelations.getEventInterval(new InstantInterval(start, end)),
                "externalId", eventWithRelations.getEvent(),
                Option.of(Cf.list(attachment)),
                authorizer.loadEventInfoForPermsCheck(eventWithRelations, UserLayersSharing.empty()),
                Option.empty(), false, Option.of(userOrganizer), Cf.list(),
                userAttendees, optionalAttendees,
                Cf.list(),
                Cf.list(), EventActions.empty(),
                Option.of(Decision.YES), Option.of(Availability.BUSY),
                Option.of(127L), MoscowTime.TZ, Option.empty(), false);

        JsonNode root = new ObjectMapper().reader().readTree(serializeWebEventInfo(eventInfo));

        assertThat(root.get("name").textValue()).isEqualTo("eventInfo");
        assertThat(root.get("description").textValue()).isEqualTo("");
        assertThat(root.get("startTs").textValue()).isEqualTo("2012-03-27T13:06:00");
        assertThat(root.get("endTs").textValue()).isEqualTo("2012-03-27T14:06:00");
        assertThat(root.get("availability").textValue()).isEqualTo("busy");
        assertThat(root.get("layerId").longValue()).isEqualTo(127L);

        Set<String> realEmails = StreamEx.of(root.path("attendees").elements())
                .map(jsonNode -> jsonNode.get("email"))
                .map(JsonNode::textValue)
                .toImmutableSet();
        Set<String> expectedEmails = StreamEx.of(attendee1, attendee2, organizer)
                .map(TestUserInfo::getEmail)
                .map(Email::getEmail)
                .toImmutableSet();

        assertThat(expectedEmails).isEqualTo(realEmails);
    }

    @Test
    public void eventWithDailyRepetition() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10010");
        Instant start = TestDateTimes.moscow(2012, 3, 27, 13, 6);
        Instant end = TestDateTimes.moscow(2012, 3, 27, 14, 6);
        Event event = testManager.createDefaultEvent(organizer.getUid(), "eventInfo", start, end, true);

        long repetitionId = testManager.createDailyRepetitionAndLinkToEvent(event.getId());

        WebUserParticipantInfo userAttendee = toAttendee(organizer);

        EventWithRelations eventWithRelations = eventDbManager.getEventWithRelationsById(event.getId());
        Repetition repetition = repetitionRoutines.getRepetitionById(repetitionId);

        EventAttachmentData attachment = new EventAttachmentData("id", "file", 1);

        WebEventInfo eventInfo = WebEventInfo.create(
                eventWithRelations.getEventInterval(new InstantInterval(start, end)),
                "externalId", eventWithRelations.getEvent(),
                Option.of(Cf.list(attachment)),
                authorizer.loadEventInfoForPermsCheck(eventWithRelations, UserLayersSharing.empty()),
                Option.of(repetition), false,
                Option.empty(), Cf.list(), Cf.list(userAttendee), Cf.list(),
                Cf.list(),
                Cf.list(), EventActions.empty(),
                Option.empty(), Option.empty(),
                Option.empty(), MoscowTime.TZ, Option.empty(), false);

        assertThat(serializeWebEventInfo(eventInfo)).isEqualTo(
                "{" +
                        "\"id\":" + event.getId() + "," +
                        "\"externalId\":\"externalId\"," +
                        "\"sequence\":" + event.getSequence() + "," +
                        "\"type\":\"user\"," +
                        "\"name\":\"eventInfo\"," +
                        "\"description\":\"\"," +
                        "\"attachments\":[{\"fileName\":\"file\",\"url\":\"id\",\"size\":1}]," +
                        "\"location\":\"\"," +
                        "\"descriptionHtml\":\"\"," +
                        "\"locationHtml\":\"\"," +
                        "\"startTs\":\"2012-03-27T13:06:00\"," +
                        "\"endTs\":\"2012-03-27T14:06:00\"," +
                        "\"instanceStartTs\":\"2012-03-27T09:06:00\"," +
                        "\"isAllDay\":false," +
                        "\"isRecurrence\":false," +
                        "\"attendees\":[{" +
                        "\"uid\":" + organizer.getUid() + "," +
                        "\"name\":\"" + organizer.getLogin() + "\"," +
                        "\"email\":\"" + organizer.getEmail() + "\"," +
                        "\"decision\":\"yes\"" +
                        "}]," +
                        "\"optionalAttendees\":[]," +
                        "\"resources\":[]," +
                        "\"subscribers\":[]," +
                        "\"notifications\":[]," +
                        "\"repetition\":{\"type\":\"daily\",\"each\":1}," +
                        "\"actions\":{" +
                        "\"accept\":false,\"reject\":false,\"delete\":false,\"attach\":false,\"detach\":false," +
                        "\"edit\":false,\"invite\":false,\"move\":false,\"changeOrganizer\":false" +
                        "}," +
                        "\"participantsCanInvite\":false," +
                        "\"participantsCanEdit\":false," +
                        "\"othersCanView\":false," +
                        "\"organizerLetToEditAnyMeeting\":false," +
                        "\"canAdminAllResources\":false," +
                        "\"repetitionNeedsConfirmation\":false," +
                        "\"totalAttendees\":1," +
                        "\"totalOptionalAttendees\":0," +
                        "\"data\":{}," +
                        "\"conferenceUrl\":\"\"" +
                        "}");
    }

    private WebUserParticipantInfo toAttendee(TestUserInfo user) {
        val userInfo = new WebUserInfo(Option.of(user.getUid().getUid()), user.getLogin().toString(), user.getEmail(), Option.empty(), Option.empty());
        return new WebUserParticipantInfo(userInfo, Decision.YES);
    }
}
