package ru.yandex.calendar.frontend.webNew.dto.out;

import java.util.EnumSet;

import lombok.val;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.bender.WebDateTime;
import ru.yandex.calendar.frontend.webNew.WebNewCodecs;
import ru.yandex.calendar.frontend.webNew.dto.inOut.RepetitionData;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.beans.generated.Settings;
import ru.yandex.calendar.logic.beans.generated.SettingsYt;
import ru.yandex.calendar.logic.event.EventActions;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.avail.AvailabilityQueryRefusalReason;
import ru.yandex.calendar.logic.event.grid.ViewType;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.layer.LayerType;
import ru.yandex.calendar.logic.notification.Channel;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.resource.ResourceType;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvAcceptingType;
import ru.yandex.calendar.logic.user.Group;
import ru.yandex.calendar.logic.user.SettingsInfo;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.util.color.Color;
import ru.yandex.calendar.util.dates.DayOfWeek;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class SerializeSmallTest {
    private <T> String serialize(T object, Class<T> clazz) {
        return new String(WebNewCodecs.getSerializer(clazz).serializeJson(object));
    }

    @Test
    public void eventInfoShort() {
        val event = new Event();
        event.setId(1L);
        event.setName("eventInfoShort");
        event.setDescription("eventInfoShort description");
        event.setStartTs(TestDateTimes.moscow(2012, 3, 27, 13, 6));
        event.setEndTs(TestDateTimes.moscow(2012, 3, 27, 14, 6));
        event.setIsAllDay(false);
        val eventInfoShort = new EventInfoShort(event, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);

        assertThat(serialize(eventInfoShort, EventInfoShort.class))
                .isEqualTo("{" +
                        "\"id\":1," +
                        "\"name\":\"eventInfoShort\"," +
                        "\"description\":\"eventInfoShort description\"," +
                        "\"startTs\":\"2012-03-27T13:06:00\"," +
                        "\"endTs\":\"2012-03-27T14:06:00\"," +
                        "\"isAllDay\":false" +
                        "}");
    }

    @Test
    public void subjectsAvailabilityIntervals() {
        val organizerUser = new WebUserInfo(Option.of(1L), "Organizer", new Email("organizer@ya.ru"), Option.empty(), Option.empty());
        val organizer = new WebUserParticipantInfo(organizerUser, Decision.YES);
        ListF<SubjectAvailabilityIntervals.AvailabilityIntervalResourceInfo> resources = Cf.list();
        val intervals1 = Cf.list(
                new SubjectAvailabilityIntervals.AvailabilityInterval(
                        Availability.BUSY, Option.empty(),
                        WebDateTime.localDateTime(new LocalDateTime(2012, 4, 28, 16, 54)),
                        WebDateTime.localDateTime(new LocalDateTime(2012, 4, 28, 17, 54)),
                        Option.empty(),
                        TestDateTimes.utc(2015, 4, 6, 18, 0),
                        Option.empty(), Option.empty(),
                        Option.empty(), Option.of(Cf.list()), Option.of(resources), Option.empty(), Option.empty()),
                new SubjectAvailabilityIntervals.AvailabilityInterval(
                        Availability.BUSY, Option.empty(),
                        WebDateTime.localDateTime(new LocalDateTime(2012, 5, 28, 16, 54)),
                        WebDateTime.localDateTime(new LocalDateTime(2012, 5, 28, 17, 54)),
                        Option.empty(), TestDateTimes.utc(2015, 4, 6, 18, 0),
                        Option.empty(), Option.of("Meeting"),
                        Option.empty(), Option.of(Cf.list(organizer)), Option.of(resources), Option.empty(), Option.empty())
        );
        val intervals2 = Cf.list(
                new SubjectAvailabilityIntervals.AvailabilityInterval(
                        Availability.BUSY, Option.empty(),
                        WebDateTime.localDateTime(new LocalDateTime(2012, 6, 28, 16, 54)),
                        WebDateTime.localDateTime(new LocalDateTime(2012, 6, 28, 17, 54)),
                        Option.empty(), TestDateTimes.utc(2015, 4, 6, 18, 0),
                        Option.empty(), Option.empty(),
                        Option.empty(), Option.of(Cf.list()), Option.of(resources), Option.empty(), Option.empty())
        );
        val user1Availability = new SubjectAvailabilityIntervals(new Email("user1@yandex.ru"), intervals1);
        val user2Availability = new SubjectAvailabilityIntervals(new Email("user2@yandex.ru"), intervals2);
        val user3Availability = new SubjectAvailabilityIntervals(
                new Email("user3@yandex.ru"), AvailabilityQueryRefusalReason.UNKNOWN_USER);
        val x = new SubjectsAvailabilityIntervals(
                Cf.list(user1Availability, user2Availability, user3Availability));

        assertThat(serialize(x, SubjectsAvailabilityIntervals.class)).isEqualTo(
                "{\"subjectAvailabilities\":[" +
                        "{\"email\":\"user1@yandex.ru\"," +
                        "\"status\":\"ok\"," +
                        "\"intervals\":[" +
                        "{" +
                        "\"availability\":\"busy\"," +
                        "\"start\":\"2012-04-28T16:54:00\"," +
                        "\"end\":\"2012-04-28T17:54:00\"," +
                        "\"attendees\":[],\"resources\":[]" +
                        "}," +
                        "{" +
                        "\"availability\":\"busy\"," +
                        "\"start\":\"2012-05-28T16:54:00\"," +
                        "\"end\":\"2012-05-28T17:54:00\"," +
                        "\"instanceStart\":\"2015-04-06T18:00:00\"," +
                        "\"eventName\":\"Meeting\"," +
                        "\"attendees\":[{" +
                        "\"uid\":1," +
                        "\"name\":\"Organizer\"," +
                        "\"email\":\"organizer@ya.ru\"," +
                        "\"decision\":\"yes\"" +
                        "}],\"resources\":[]" +
                        "}" +
                        "]}," +
                        "{\"email\":\"user2@yandex.ru\"," +
                        "\"status\":\"ok\"," +
                        "\"intervals\":[" +
                        "{" +
                        "\"availability\":\"busy\"," +
                        "\"start\":\"2012-06-28T16:54:00\"," +
                        "\"end\":\"2012-06-28T17:54:00\"," +
                        "\"attendees\":[],\"resources\":[]" +
                        "}" +
                        "]}," +
                        "{\"email\":\"user3@yandex.ru\"," +
                        "\"status\":\"unknown-user\"" +
                        "}" +
                        "]}");
    }


    @Test
    public void subjectsAvailabilityIntervalsWithActionsAndSequence() {
        val organizerUser = new WebUserInfo(Option.of(1L),"Organizer", new Email("organizer@ya.ru"), Option.empty(), Option.empty());
        val organizer = new WebUserParticipantInfo(organizerUser, Decision.YES);
        ListF<SubjectAvailabilityIntervals.AvailabilityIntervalResourceInfo> resources = Cf.list();
        val intervals1 = Cf.list(
                new SubjectAvailabilityIntervals.AvailabilityInterval(
                        Availability.BUSY, Option.empty(),
                        WebDateTime.localDateTime(new LocalDateTime(2012, 4, 28, 16, 54)),
                        WebDateTime.localDateTime(new LocalDateTime(2012, 4, 28, 17, 54)),
                        Option.empty(),
                        TestDateTimes.utc(2015, 4, 6, 18, 0),
                        Option.empty(), Option.empty(),
                        Option.empty(), Option.of(Cf.list()), Option.of(resources),
                        Option.of(new EventActions(true,true, false, false, false, true, true, false, false)),
                        Option.of(42))
        );
        val intervals2 = Cf.list(
                new SubjectAvailabilityIntervals.AvailabilityInterval(
                        Availability.BUSY, Option.empty(),
                        WebDateTime.localDateTime(new LocalDateTime(2012, 6, 28, 16, 54)),
                        WebDateTime.localDateTime(new LocalDateTime(2012, 6, 28, 17, 54)),
                        Option.empty(), TestDateTimes.utc(2015, 4, 6, 18, 0),
                        Option.empty(), Option.empty(),
                        Option.empty(), Option.of(Cf.list()), Option.of(resources), Option.empty(), Option.of(7))
        );
        val user1Availability = new SubjectAvailabilityIntervals(new Email("user1@yandex.ru"), intervals1);
        val user2Availability = new SubjectAvailabilityIntervals(new Email("user2@yandex.ru"), intervals2);
        val user3Availability = new SubjectAvailabilityIntervals(
                new Email("user3@yandex.ru"), AvailabilityQueryRefusalReason.UNKNOWN_USER);
        val x = new SubjectsAvailabilityIntervals(
                Cf.list(user1Availability, user2Availability, user3Availability));

        assertThat(serialize(x, SubjectsAvailabilityIntervals.class)).isEqualTo(
                "{\"subjectAvailabilities\":[" +
                        "{\"email\":\"user1@yandex.ru\"," +
                        "\"status\":\"ok\"," +
                        "\"intervals\":[" +
                        "{" +
                        "\"availability\":\"busy\"," +
                        "\"start\":\"2012-04-28T16:54:00\"," +
                        "\"end\":\"2012-04-28T17:54:00\"," +
                        "\"attendees\":[],\"resources\":[]," +
                        "\"actions\":{\"accept\":true,\"reject\":true,\"delete\":false,\"attach\":false," +
                        "\"detach\":false,\"edit\":true,\"invite\":true,\"move\":false,\"changeOrganizer\":false}," +
                        "\"sequence\":42" +
                        "}" +
                        "]}," +
                        "{\"email\":\"user2@yandex.ru\"," +
                        "\"status\":\"ok\"," +
                        "\"intervals\":[" +
                        "{" +
                        "\"availability\":\"busy\"," +
                        "\"start\":\"2012-06-28T16:54:00\"," +
                        "\"end\":\"2012-06-28T17:54:00\"," +
                        "\"attendees\":[],\"resources\":[]," +
                        "\"sequence\":7" +
                        "}" +
                        "]}," +
                        "{\"email\":\"user3@yandex.ru\"," +
                        "\"status\":\"unknown-user\"" +
                        "}" +
                        "]}");
    }

    @Test
    public void subjectsAvailabilities() {
        val subjectsAvailabilities = new SubjectsAvailabilities(Cf.list(
                SubjectAvailability.known(new Email("user1@yandex.ru"), Availability.AVAILABLE),
                SubjectAvailability.known(new Email("user2@yandex.ru"), Availability.BUSY),
                SubjectAvailability.known(new Email("user3@yandex.ru"), Availability.MAYBE),
                SubjectAvailability.unknown(new Email("user4@yandex.ru"))));

        assertThat(serialize(subjectsAvailabilities, SubjectsAvailabilities.class))
                .isEqualTo("{\"subjectAvailabilities\":[" +
                        "{\"email\":\"user1@yandex.ru\",\"availability\":\"available\"}," +
                        "{\"email\":\"user2@yandex.ru\",\"availability\":\"busy\"}," +
                        "{\"email\":\"user3@yandex.ru\",\"availability\":\"maybe\"}," +
                        "{\"email\":\"user4@yandex.ru\",\"availability\":\"unknown\"}" +
                        "]}");
    }

    @Test
    public void moveResourceEventIds() {
        val ids = new MoveResourceEventsIds(1, 2);

        assertThat(serialize(ids, MoveResourceEventsIds.class))
                .isEqualTo("{\"sourceEventId\":1,\"targetEventId\":2}");
    }

    @Test
    public void resourcesInfo() {
        val r1Email = new Email("resource1@yandex-team.ru");
        val r2Email = new Email("resource1@yandex-team.ru");

        val resource1 = new ResourcesInfo.ResourceInfo("resource1", r1Email, ResourceType.ROOM, 0, Option.empty(), Option.empty(), Option.empty());
        val resource2 = new ResourcesInfo.ResourceInfo("resource2", r2Email, ResourceType.ROOM, 0, Option.empty(), Option.empty(), Option.empty());

        val info = new ResourcesInfo(Cf.list(resource1, resource2), 2);

        assertThat(serialize(info, ResourcesInfo.class))
                .isEqualTo("{\"resources\":[" +
                        "{\"name\":\"resource1\",\"email\":\"" + r1Email + "\",\"type\":\"room\",\"officeId\":0}," +
                        "{\"name\":\"resource2\",\"email\":\"" + r2Email + "\",\"type\":\"room\",\"officeId\":0}" +
                        "],\"foundTotal\":2}");
    }

    @Test
    public void resourceInfo() {
        val info = new WebResourceInfo(
                1, "7. Пятниц", "КР 7-9", new Email("fridays@yt.ru"), "1124", "1802", "",
                14, 24, true, 2, 0, true, true, false, Option.of(7), Option.empty(),
                true, ResourceType.ROOM, Option.empty(), Option.empty(),
                Option.of("Москва"), Option.of("БЦ Морозов"), Option.of("КР-2"), Option.empty());

        assertThat(serialize(info, WebResourceInfo.class)).isEqualTo(
                "{\"officeId\":1,\"name\":\"7. Пятниц\",\"alterName\":\"КР 7-9\",\"email\":\"fridays@yt.ru\",\"phone\":\"1124\"," +
                        "\"video\":\"1802\",\"description\":\"\",\"seats\":14,\"capacity\":24,\"voiceConferencing\":true," +
                        "\"projector\":2,\"lcdPanel\":0,\"markerBoard\":true,\"desk\":true,\"guestWifi\":false,\"floor\":7," +
                        "\"active\":true,\"resourceType\":\"room\"," +
                        "\"cityName\":\"Москва\",\"officeName\":\"БЦ Морозов\",\"groupName\":\"КР-2\"}");
    }

    @Test
    public void suggestInfo() {
        val office1 = new SuggestInfo.OfficeInfo(1, "office 1");
        val office2 = new SuggestInfo.OfficeInfo(2, "office 2");

        val interval1 = new LocalDateTimeInterval(new LocalDateTime(2012, 9, 19, 10, 30),
                new LocalDateTime(2012, 9, 19, 11, 0));

        val placesForInterval1 = Cf.list(
                new SuggestInfo.Place(office1, interval1, Cf.list(resource(11)), false),
                new SuggestInfo.Place(office2, shiftHours(interval1, 2), Cf.list(resource(21)), false)); //ListF

        val suggestForInterval1 = new SuggestInfo.IntervalAndPlaces(interval1, placesForInterval1, false);

        val interval2 = new LocalDateTimeInterval(new LocalDateTime(2012, 9, 19, 11, 30),
                new LocalDateTime(2012, 9, 19, 12, 0));

        val placesForInterval2 = Cf.list(
                new SuggestInfo.Place(office1, interval2, Cf.list(resource(11), resource(12)), false),
                new SuggestInfo.Place(office2, shiftHours(interval2, 2), Cf.list(resource(22), resource(23)), false));

        val suggestForInterval2 = new SuggestInfo.IntervalAndPlaces(interval2, placesForInterval2, true);

        val interval3 = new LocalDateTimeInterval(new LocalDateTime(2012, 9, 20, 10, 0),
                new LocalDateTime(2012, 9, 20, 10, 30));

        val placesForInterval3 = Cf.list(
                new SuggestInfo.Place(office1, interval3, Cf.list(resource(13)), false),
                new SuggestInfo.Place(office2, shiftHours(interval3, 1), Cf.list(resource(22)), false));

        val suggestForInterval3 = new SuggestInfo.IntervalAndPlaces(
                interval3, placesForInterval3, false);

        val suggest = Cf.list(suggestForInterval1, suggestForInterval2,
                suggestForInterval3);

        val nextSearchStart = new LocalDateTime(2012, 9, 20, 11, 0);

        val suggestInfo = SuggestInfo.withPlaces(
                suggest, Option.empty(), Option.of(nextSearchStart));

        assertThat(serialize(suggestInfo, SuggestInfo.WithPlaces.class)).isEqualTo(
                "{\"intervals\":[" +
                        "{\"start\":\"2012-09-19T10:30:00\",\"end\":\"2012-09-19T11:00:00\",\"places\":[" +
                        "{\"officeId\":1,\"officeName\":\"office 1\"," +
                        "\"start\":\"2012-09-19T10:30:00\",\"end\":\"2012-09-19T11:00:00\",\"resources\":[" +
                        "{" +
                        "\"id\":11,\"name\":\"resource 11\",\"email\":\"calendar-resource-11@test\"," +
                        "\"type\":\"room\",\"hasPhone\":false,\"hasVideo\":false}" +
                        "],\"hasMoreFreeResources\":false}," +
                        "{\"officeId\":2,\"officeName\":\"office 2\"," +
                        "\"start\":\"2012-09-19T12:30:00\",\"end\":\"2012-09-19T13:00:00\",\"resources\":[" +
                        "{" +
                        "\"id\":21,\"name\":\"resource 21\",\"email\":\"calendar-resource-21@test\"," +
                        "\"type\":\"room\",\"hasPhone\":false,\"hasVideo\":false}" +
                        "],\"hasMoreFreeResources\":false}" +
                        "],\"isPreferred\":false}," +
                        "{\"start\":\"2012-09-19T11:30:00\",\"end\":\"2012-09-19T12:00:00\",\"places\":[" +
                        "{\"officeId\":1,\"officeName\":\"office 1\"," +
                        "\"start\":\"2012-09-19T11:30:00\",\"end\":\"2012-09-19T12:00:00\",\"resources\":[" +
                        "{" +
                        "\"id\":11,\"name\":\"resource 11\",\"email\":\"calendar-resource-11@test\"," +
                        "\"type\":\"room\",\"hasPhone\":false,\"hasVideo\":false" +
                        "}," +
                        "{" +
                        "\"id\":12,\"name\":\"resource 12\",\"email\":\"calendar-resource-12@test\"," +
                        "\"type\":\"room\",\"hasPhone\":false,\"hasVideo\":false}" +
                        "],\"hasMoreFreeResources\":false}," +
                        "{\"officeId\":2,\"officeName\":\"office 2\"," +
                        "\"start\":\"2012-09-19T13:30:00\",\"end\":\"2012-09-19T14:00:00\",\"resources\":[" +
                        "{" +
                        "\"id\":22,\"name\":\"resource 22\",\"email\":\"calendar-resource-22@test\"," +
                        "\"type\":\"room\",\"hasPhone\":false,\"hasVideo\":false}," +
                        "{" +
                        "\"id\":23,\"name\":\"resource 23\",\"email\":\"calendar-resource-23@test\"," +
                        "\"type\":\"room\",\"hasPhone\":false,\"hasVideo\":false}" +
                        "],\"hasMoreFreeResources\":false}" +
                        "],\"isPreferred\":true}," +
                        "{\"start\":\"2012-09-20T10:00:00\",\"end\":\"2012-09-20T10:30:00\",\"places\":[" +
                        "{\"officeId\":1,\"officeName\":\"office 1\"," +
                        "\"start\":\"2012-09-20T10:00:00\",\"end\":\"2012-09-20T10:30:00\",\"resources\":[" +
                        "{" +
                        "\"id\":13,\"name\":\"resource 13\",\"email\":\"calendar-resource-13@test\"," +
                        "\"type\":\"room\",\"hasPhone\":false,\"hasVideo\":false}" +
                        "],\"hasMoreFreeResources\":false}," +
                        "{\"officeId\":2,\"officeName\":\"office 2\"," +
                        "\"start\":\"2012-09-20T11:00:00\",\"end\":\"2012-09-20T11:30:00\",\"resources\":[" +
                        "{" +
                        "\"id\":22,\"name\":\"resource 22\",\"email\":\"calendar-resource-22@test\"," +
                        "\"type\":\"room\",\"hasPhone\":false,\"hasVideo\":false}" +
                        "],\"hasMoreFreeResources\":false}" +
                        "],\"isPreferred\":false}" +
                        "]," +
                        "\"nextSearchStart\":\"2012-09-20T11:00:00\"" +
                        "}");
    }

    private static SuggestInfo.WebResourceInfo resource(long id) {
        return new SuggestInfo.WebResourceInfo(
                id, "resource " + id, new Email("calendar-resource-" + id + "@test"),
                ResourceType.ROOM, Option.empty(), false, false, Option.empty(), Option.empty(), Option.empty());
    }

    @Test
    public void suggestInfoWithNoPlaces() {
        val interval1 = new SuggestInfo.IntervalWithDue(
                new LocalDateTime(2012, 9, 19, 10, 30),
                new LocalDateTime(2012, 9, 19, 11, 0), Option.empty());

        val intervals = Cf.list(
                new SuggestInfo.IntervalAndOptions(Cf.list(interval1, shiftHours(interval1, 1)), true),
                new SuggestInfo.IntervalAndOptions(Cf.list(
                        withDue(interval1, new LocalDate(2014, 12, 22)), shiftHours(interval1, 2)), false));

        val prevSearchStart = new LocalDateTime(2012, 9, 20, 11, 0);

        val suggestInfo = SuggestInfo.withNoPlaces(intervals, Option.of(prevSearchStart), Option.empty());

        assertThat(serialize(suggestInfo, SuggestInfo.WithNoPlaces.class)).isEqualTo(
                "{\"intervals\":[" +
                        "{" +
                        "\"options\":[" +
                        "{\"start\":\"2012-09-19T10:30:00\",\"end\":\"2012-09-19T11:00:00\"}," +
                        "{\"start\":\"2012-09-19T11:30:00\",\"end\":\"2012-09-19T12:00:00\"}]," +
                        "\"isPreferred\":true" +
                        "},{" +
                        "\"options\":[" +
                        "{" +
                        "\"start\":\"2012-09-19T10:30:00\"," +
                        "\"end\":\"2012-09-19T11:00:00\"," +
                        "\"dueDate\":\"2014-12-22\"" +
                        "}," +
                        "{\"start\":\"2012-09-19T12:30:00\",\"end\":\"2012-09-19T13:00:00\"}]," +
                        "\"isPreferred\":false" +
                        "}]," +
                        "\"backwardSearchStart\":\"2012-09-20T11:00:00\"" +
                        "}");
    }

    private static SuggestInfo.IntervalWithDue shiftHours(SuggestInfo.IntervalWithDue interval, int hours) {
        return new SuggestInfo.IntervalWithDue(
                interval.getStart().plusHours(hours), interval.getEnd().plusHours(hours), interval.getDueDate());
    }

    private static SuggestInfo.IntervalWithDue withDue(SuggestInfo.IntervalWithDue interval, LocalDate due) {
        return new SuggestInfo.IntervalWithDue(interval.getStart(), interval.getEnd(), Option.of(due));
    }

    private static LocalDateTimeInterval shiftHours(LocalDateTimeInterval interval, int hours) {
        return new LocalDateTimeInterval(interval.getStart().plusHours(hours), interval.getEnd().plusHours(hours));
    }

    @Test
    public void layersInfo() {
        val l1 = new LayersInfo.LayerInfo(1, "one", false, true, false, false,
                Option.empty(), Color.parseRgb("#89ABCD"), 30,
                Cf.list(new Notification(Channel.EMAIL, Duration.standardMinutes(60)),
                        new Notification(Channel.SMS, Duration.standardMinutes(15))),
                true, true, LayerType.USER, Option.empty());

        val l2 = new LayersInfo.LayerInfo(
                2, "two", true, false, false, true,
                Option.empty(), Color.fromRgb(0), 30, Cf.list(), true, false, LayerType.FEED, Option.of(1));
        val layersInfo = new LayersInfo(Cf.list(l1, l2));

        assertThat(serialize(layersInfo, LayersInfo.class)).isEqualTo(
                "{\"layers\":[" +
                    "{\"id\":1,\"name\":\"one\",\"isToggledOn\":false,\"canAddEvent\":true," +
                        "\"isEventsClosedByDefault\":false,\"isOwner\":false,\"color\":\"#89abcd\",\"defaultEventsDurationMinutes\":30," +
                        "\"notifications\":[" +
                            "{\"channel\":\"email\",\"offset\":\"60m\"}," +
                            "{\"channel\":\"sms\",\"offset\":\"15m\"}" +
                        "],\"affectsAvailability\":true," +
                        "\"isDefault\":true," +
                        "\"type\":\"user\"" +
                    "}," +
                    "{\"id\":2,\"name\":\"two\",\"isToggledOn\":true,\"canAddEvent\":false," +
                        "\"isEventsClosedByDefault\":false,\"isOwner\":true,\"color\":\"#000000\",\"defaultEventsDurationMinutes\":30,\"notifications\":[]," +
                        "\"affectsAvailability\":true," +
                        "\"isDefault\":false," +
                        "\"type\":\"feed\"," +
                        "\"participantsCount\":1" +
                    "}" +
                "]}");
    }

    @Test
    public void officesInfo() {
        val office1 = new OfficesInfo.OfficeInfo("office 1", 1, "city 1", 18000000);
        val office2 = new OfficesInfo.OfficeInfo("office 2", 2, "city 1", 18000000);

        val info = new OfficesInfo(Cf.list(office1, office2));

        assertThat(serialize(info, OfficesInfo.class)).isEqualTo(
                "{\"offices\":[" +
                        "{\"name\":\"office 1\",\"id\":1,\"cityName\":\"city 1\",\"tzOffset\":18000000}," +
                        "{\"name\":\"office 2\",\"id\":2,\"cityName\":\"city 1\",\"tzOffset\":18000000}" +
                        "]}");
    }

    @Test
    public void userSettingsInfo() {
        val settings = new Settings();
        settings.setUserName("name");
        settings.setUid(new PassportUid(3));
        settings.setUserLogin("login");
        settings.setEmail(new Email("aaa@ya.ru"));
        settings.setTimezoneJavaid(MoscowTime.TZ.getID());
        settings.setGeoTzJavaid("Asia/Magadan");
        settings.setTranslitSms(false);
        settings.setNoNtfStartTm(0);
        settings.setNoNtfEndTm((int) Hours.hours(12).toStandardDuration().getMillis());
        settings.setNoNtfStartTs(new LocalDate(2013, 12, 15).toDateTimeAtStartOfDay(MoscowTime.TZ).toInstant());
        settings.setNoNtfEndTs(new LocalDate(2013, 12, 16).toDateTimeAtStartOfDay(MoscowTime.TZ).toInstant());
        settings.setStartWeekday(DayOfWeek.MONDAY);
        settings.setGridTopHours(8);
        settings.setViewType(ViewType.WEEK);
        settings.setShowTodo(true);
        settings.setShowAvailability(false);
        settings.setMapsEnabled(true);
        settings.setInvAcceptType(InvAcceptingType.MANUAL);
        settings.setShowShortForm(false);
        settings.setCreationTs(Instant.parse("2000-01-01"));

        settings.setTodoPlannedEmailTm(10 * 60 * 60 * 1000);
        settings.setTodoExpiredEmailTm(19 * 30 * 60 * 1000);

        val settingsYt = new SettingsYt();
        settingsYt.setUid(settings.getUid());
        settingsYt.setActiveOfficeId(500L);

        settingsYt.setRemindUndecided(true);
        settingsYt.setLetParticipantsEdit(false);
        settingsYt.setXivaReminderEnabled(true);
        settingsYt.setNoNtfDuringAbsence(true);

        val userInfo = new UserInfo(new PassportUid(3), EnumSet.noneOf(Group.class), Cf.list(), Cf.list(), false, false);

        val office = new Office();
        office.setId(127L);
        office.setTimezoneId("Europe/Simferopol");

        val info = new UserSettingsInfo(new SettingsInfo(
                settings, Option.of(settingsYt)), userInfo, Option.of(office));

        assertThat(serialize(info, UserSettingsInfo.class)).isEqualTo(
                "{" +
                        "\"uid\":3,\"login\":\"login\",\"email\":\"aaa@ya.ru\",\"name\":\"name\"," +
                        "\"isRoomAdmin\":false,\"isExternalUser\":false," +
                        "\"tz\":\"Europe/Moscow\",\"lastOfferedGeoTz\":\"Asia/Magadan\"," +
                        "\"transliterateSms\":false," +
                        "\"noNotificationsRange\":{" +
                        "\"sinceDate\":\"2013-12-15\",\"untilDate\":\"2013-12-16\"," +
                        "\"fromTime\":\"00:00\",\"toTime\":\"12:00\"" +
                        "}," +
                        "\"todoEmailTimes\":{\"planned\":\"10:00\",\"expired\":\"09:30\"}," +
                        "\"weekStartDay\":\"mon\"," +
                        "\"dayStartHour\":8,\"defaultView\":\"week\"" +
                        ",\"autoAcceptEventInvitations\":false," +
                        "\"showTodosInGrid\":true,\"showAvailabilityToAnyone\":false,\"useYaMaps\":true," +
                        "\"showShortForm\":false," +
                        "\"currentOfficeId\":127,\"currentOfficeTz\":\"Europe/Simferopol\"," +
                        "\"remindUndecided\":true,\"letParticipantsEdit\":false," +
                        "\"noNotificationsInYaMail\":false,\"noNotificationsDuringAbsence\":true," +
                        "\"hiredAt\":\"2000-01-01\"" +
                        "}");
    }

    @Test
    public void weeklyRepetition() {
        val repetition = new Repetition();
        repetition.setType(RegularRepetitionRule.WEEKLY);
        repetition.setRWeeklyDays("MON");
        repetition.setDueTs(TestDateTimes.moscow(2012, 4, 20, 14, 15));
        repetition.setREach(5);
        repetition.setRMonthlyLastweekNull();
        val repetitionData = RepetitionData.fromRepetition(repetition, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);

        assertThat(serialize(repetitionData, RepetitionData.class))
                .isEqualTo("{\"type\":\"weekly\",\"each\":5,\"weeklyDays\":\"MON\",\"dueDate\":\"2012-04-19\"}");
    }

    @Test
    public void monthlyNumber() {
        val repetition = new Repetition();
        repetition.setType(RegularRepetitionRule.MONTHLY_NUMBER);
        repetition.setRWeeklyDaysNull();
        repetition.setDueTs(TestDateTimes.moscow(2012, 4, 20, 14, 15));
        repetition.setREach(5);
        repetition.setRMonthlyLastweekNull();
        val repetitionData = RepetitionData.fromRepetition(repetition, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);

        assertThat(serialize(repetitionData, RepetitionData.class)).isEqualTo(
                "{\"type\":\"monthly-number\",\"each\":5,\"dueDate\":\"2012-04-19\"}");
    }

    @Test
    public void usersAndResourcesInfo() {
        val resource = new WebResourceInfo(
                1, "7. Пятниц", "КР 7-9", new Email("fridays@yt.ru"), "1124", "1802", "",
                14, 24, true, 2, 0, true, true, false, Option.empty(), Option.empty(),
                true, ResourceType.ROOM, Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty());

        val user = new WebUserInfo(Option.of(1L), "Friend", new Email("friend@ya.ru"), Option.empty(), Option.empty());
        val unknown = "xxx";

        val info = new UsersAndResourcesInfo(Cf.list(user), Cf.list(resource), Cf.list(unknown));

        assertThat(serialize(info, UsersAndResourcesInfo.class)).isEqualTo(
                "{\"users\":[{\"uid\":1,\"name\":\"Friend\",\"email\":\"friend@ya.ru\"}]," +
                        "\"resources\":[{" +
                        "\"officeId\":1,\"name\":\"7. Пятниц\",\"alterName\":\"КР 7-9\",\"email\":\"fridays@yt.ru\",\"phone\":\"1124\"," +
                        "\"video\":\"1802\",\"description\":\"\",\"seats\":14,\"capacity\":24,\"voiceConferencing\":true," +
                        "\"projector\":2,\"lcdPanel\":0,\"markerBoard\":true,\"desk\":true,\"guestWifi\":false," +
                        "\"active\":true,\"resourceType\":\"room\"" +
                        "}]," +
                        "\"notFound\":[\"xxx\"]}");
    }

    @Test
    public void userOrResourceInfo() {
        val user = new WebUserInfo(Option.of(2L), "Resu", new Email("resu@users.com"), Option.empty(), Option.of(1L));
        val userInfo = UserOrResourceInfo.user(user);

        assertThat(serialize(userInfo, UserOrResourceInfo.User.class))
                .isEqualTo("{\"uid\":2,\"name\":\"Resu\",\"email\":\"resu@users.com\",\"officeId\":1,\"type\":\"user\"}");

        val resource = new WebResourceInfo(
                1, "7. Пятниц", "КР 7-9", new Email("fridays@yt.ru"), "1124", "1802", "",
                14, 24, true, 2, 0, true, true, true, Option.empty(), Option.empty(),
                true, ResourceType.ROOM, Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty());
        val resourceInfo = UserOrResourceInfo.resource(resource);

        assertThat(serialize(resourceInfo, UserOrResourceInfo.Resource.class)).isEqualTo(
                "{\"officeId\":1,\"name\":\"7. Пятниц\",\"alterName\":\"КР 7-9\",\"email\":\"fridays@yt.ru\",\"phone\":\"1124\"," +
                        "\"video\":\"1802\",\"description\":\"\",\"seats\":14,\"capacity\":24," +
                        "\"voiceConferencing\":true,\"projector\":2,\"lcdPanel\":0," +
                        "\"markerBoard\":true,\"desk\":true,\"guestWifi\":true,\"active\":true,\"resourceType\":\"room\"," +
                        "\"type\":\"resource\"" +
                        "}");
    }

    @Test
    public void contactsInfo() {
        val contact1 = new ContactsInfo.ContactInfo("Bfa", new Email("bfa@yt.ru"), Option.empty());
        val contact2 = new ContactsInfo.ContactInfo("Aaz", new Email("aaz@yt.ru"), Option.empty());

        val info = new ContactsInfo(Cf.list(contact1, contact2));

        assertThat(serialize(info, ContactsInfo.class))
                .isEqualTo("{\"contacts\":[{\"name\":\"Bfa\",\"email\":\"bfa@yt.ru\"},{\"name\":\"Aaz\",\"email\":\"aaz@yt.ru\"}]}");
    }

    @Test
    public void statusResult() {
        val result = StatusResult.ok();
        assertThat(serialize(result, StatusResult.class)).isEqualTo("{\"status\":\"ok\"}");
    }

    @Test
    public void parseTimeInEventNameResult() {
        val start = new LocalDateTime(2012, 12, 4, 22, 0);
        val result = new ParseTimeInEventNameResult("Dinner", start, start.plusHours(1), false);

        assertThat(serialize(result, ParseTimeInEventNameResult.class))
                .isEqualTo("{\"name\":\"Dinner\",\"startTs\":\"2012-12-04T22:00:00\",\"endTs\":\"2012-12-04T23:00:00\",\"isAllDay\":false}");
    }
}
