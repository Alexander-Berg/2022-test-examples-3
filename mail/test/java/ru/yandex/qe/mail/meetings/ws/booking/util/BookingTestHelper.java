package ru.yandex.qe.mail.meetings.ws.booking.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.joda.time.Interval;
import org.mockito.stubbing.Answer;
import org.springframework.mail.javamail.JavaMailSender;

import ru.yandex.qe.mail.meetings.api.resource.CalendarActions;
import ru.yandex.qe.mail.meetings.booking.BasicMeetingInfo;
import ru.yandex.qe.mail.meetings.booking.BookingResultMessageBuilder;
import ru.yandex.qe.mail.meetings.booking.FuzzyBookingService;
import ru.yandex.qe.mail.meetings.booking.PersonService;
import ru.yandex.qe.mail.meetings.booking.RoomService;
import ru.yandex.qe.mail.meetings.booking.impl.PersonServiceImpl;
import ru.yandex.qe.mail.meetings.booking.impl.RoomServiceImpl;
import ru.yandex.qe.mail.meetings.services.calendar.CalendarUpdate;
import ru.yandex.qe.mail.meetings.services.calendar.dto.EventDate;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Office;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Resource;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Response;
import ru.yandex.qe.mail.meetings.services.calendar.dto.WebEventCreateData;
import ru.yandex.qe.mail.meetings.services.staff.StaffApiV3;
import ru.yandex.qe.mail.meetings.services.staff.StaffClient;
import ru.yandex.qe.mail.meetings.services.staff.dto.Location;
import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.ws.booking.mocks.CalendarWebMock;
import ru.yandex.qe.mail.meetings.ws.booking.mocks.StaffApiV3Mock;
import ru.yandex.qe.mail.meetings.ws.handlers.BookingHandler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.qe.mail.meetings.ws.booking.util.DatePositionInterval.dpInterval;
import static ru.yandex.qe.mail.meetings.ws.booking.util.DatePositionInterval.weekWithGaps;

public final class BookingTestHelper {
    private static final AtomicInteger counter = new AtomicInteger();

    public static final Office MSK = new Office(1, "MSK", "MSK", "GMT+3", "Moscow", Collections.emptyList());
    public static final Office SPB = new Office(2, "SPB", "SPB", "GMT+3", "Moscow", Collections.emptyList());
    public static final Office ANY = new Office(3, "ANY", "ANY", "GMT+3", "Moscow", Collections.emptyList());
    public static final Office AURORA_STAFF_ID = new Office(181, "Aurora", "SPB", "GMT+3", "Moscow", Collections.emptyList());
    public static final Office AURORA_CAL_ID = new Office(746, "Aurora", "SPB", "GMT+3", "Moscow", Collections.emptyList());

    private static final ThreadLocal<DateFormat> _DF = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    public static final Map<Integer, Office> ID_TO_OFFICE = Stream.of(MSK, SPB, ANY, AURORA_CAL_ID, AURORA_STAFF_ID).collect(Collectors.toMap(Office::getId, o -> o));

    private BookingTestHelper(){}

    public static ObjectWithTimeTable<Person> personFromMsk() {
        return personWithLocation(MSK);
    }

    public static ObjectWithTimeTable<Person> personFromSpb() {
        return personWithLocation(SPB);
    }

    public static ObjectWithTimeTable<Resource.Info> roomWithSingleFreeIntervalOnWeek(Office office, int dayOffset, String interval) {
        return room(
                locationOf(office),
                10,
                BookingTestHelper.RoomParams.NONE
    )
            .addBusyIntervals(
                weekWithGaps(
                        dpInterval(dayOffset, interval)
                )
        );
    }

    public static ObjectWithTimeTable<Person> personWithSingleFreeIntervalOnWeek(Office office, int dayOffset, String interval) {
        return personWithLocation(office)
                .addBusyIntervals(
                        weekWithGaps(
                                dpInterval(dayOffset, interval)
                        )
                );
    }

    public static Location locationOf(Office office) {
        return new Location(new ru.yandex.qe.mail.meetings.services.staff.dto.Office(
                office.getId(),
                office.getName()
        ));
    }

    public static ObjectWithTimeTable<Person> personWithLocation(@Nonnull Office office) {
        final int uid = counter.incrementAndGet();

        var person = mock(Person.class);

        when(person.getLocation()).thenReturn(locationOf(office));
        when(person.getLogin()).thenReturn("login-" + uid);
        when(person.getUid()).thenReturn("" + uid);

        return ObjectWithTimeTable.of(person);
    }

    public static ObjectWithTimeTable<Resource.Info> room(Location location, int capacity, RoomParams... params) {
        return room(location, capacity, "room", params);
    }

    public static ObjectWithTimeTable<Resource.Info> room(Location location, int capacity, String type, RoomParams... params) {
        var resourceId = counter.incrementAndGet();
        var name = "resource-" + resourceId;
        var resource = mock(Resource.Info.class);
        when(resource.getId()).thenReturn(resourceId);
        when(resource.getEmail()).thenReturn(name + "@yandex-team.ru");
        when(resource.getName()).thenReturn(name);
        when(resource.getOfficeId()).thenReturn(location.getOffice().getId());
        when(resource.getType()).thenReturn(type);
        when(resource.getCapacity()).thenReturn(capacity);
        when(resource.getSeats()).thenReturn(capacity);

        var paramSet = params != null ? Arrays.stream(params).collect(Collectors.toSet()) : Collections.emptySet();

        if (paramSet.contains(RoomParams.LCD)) {
            when(resource.getLcdPanel()).thenReturn(1);
        } else {
            when(resource.getLcdPanel()).thenReturn(0);
        }

        if (paramSet.contains(RoomParams.VOICE_CONF)) {
            when(resource.isVoiceConferencing()).thenReturn(true);
        } else {
            when(resource.isVoiceConferencing()).thenReturn(false);
        }

        if (paramSet.contains(RoomParams.VIDEO_CONF)) {
            when(resource.isHasVideo()).thenReturn(true);
            when(resource.getVideo()).thenReturn("" + resourceId);
        } else {
            when(resource.isHasVideo()).thenReturn(true);
            when(resource.getVideo()).thenReturn(null);
        }

        return ObjectWithTimeTable.of(resource);
    }

    public static Office officeMock(Office stub, List<Resource> resources) {
        var office = mock(Office.class);
        when(office.getId()).thenReturn(stub.getId());
        when(office.getCity()).thenReturn(stub.getCity());
        when(office.getName()).thenReturn(stub.getName());
        when(office.getTzId()).thenReturn(stub.getTzId());
        when(office.getTzOffset()).thenReturn(stub.getTzOffset());
        when(office.getResources()).thenReturn(resources);
        return office;
    }

    public static BasicMeetingInfo basicInfoFor(@Nonnull String login) {
        return new BasicMeetingInfo(login, "name", "desc", false, false, false);
    }

    public static List<EventDate> eventDates(List<Interval> busyTimes) {
        return busyTimes
                .stream()
                .map(i -> new EventDate(
                        counter.incrementAndGet(),
                        counter.get(),
                        i.getStart().toDate(),
                        i.getEnd().toDate()
                ))
                .collect(Collectors.toUnmodifiableList());
    }

    private static CalendarActions mockCalendarActions() {
        var actions = mock(CalendarActions.class);
        when(actions.findResource(anyInt(), anyString(), anyInt(), anyBoolean(), anyString(), anyString(), anyString())).thenReturn("id-" + counter.incrementAndGet());
        return actions;
    }

    private static CalendarUpdate mockCalendarUpdate() {
        var update = mock(CalendarUpdate.class);
        doMock(update);
        return update;
    }

    public static void doMock(CalendarUpdate update) {
        when(update.createEvent(any(WebEventCreateData.class))).thenAnswer((Answer<Response>) invocation -> new Response(
                Response.OK,
                counter.incrementAndGet(),
                counter.incrementAndGet(),
                new Date(),
                new Date(),
                "",
                "",
                "",
                "",
                Collections.emptyList()
        ));
    }

    private static StaffClient mockStaffClient(List<ObjectWithTimeTable<Person>> persons) {
        StaffApiV3 api = new StaffApiV3Mock(persons);
        return new StaffClient(api);
    }

    private static MetricRegistry metricRegistry() {
        MetricRegistry registry = mock(MetricRegistry.class);
        when(registry.counter(anyString())).thenReturn(new Counter());
        return registry;
    }

    public static class ObjectWithTimeTable<T> {
        @Nonnull
        public final T obj;
        public final List<Interval> busyTimes = new ArrayList<>();

        static <T> ObjectWithTimeTable<T> of(@Nonnull T obj) {
            return new ObjectWithTimeTable<>(obj);
        }

        private ObjectWithTimeTable(T obj) {
            this.obj = obj;
        }

        public ObjectWithTimeTable<T> addBusyIntervals(@Nonnull List<Interval> intervals) {
            intervals.forEach(this::addBusyInterval);
            return this;
        }
        public ObjectWithTimeTable<T> addBusyInterval(@Nonnull Interval interval) {
            this.busyTimes.add(interval);
            return this;
        }
    }

    public static class BookingObjects {
        public final FuzzyBookingService fuzzyBookingService;
        public final CalendarWebMock calendarWeb;
        public final StaffClient staffClient;
        public final CalendarActions calendarActions;
        public final CalendarUpdate calendarUpdate;
        public final PersonService personService;
        public final RoomService roomService;
        public final BookingHandler booking;

        public BookingObjects(CalendarWebMock calendarWeb, StaffClient staffClient) {
            this.calendarWeb = calendarWeb;
            this.staffClient = staffClient;

            this.calendarUpdate = mockCalendarUpdate();
            this.calendarActions = mockCalendarActions();
            this.personService = new PersonServiceImpl(calendarWeb);

            var roomService = new RoomServiceImpl(calendarWeb, 14L, true);
            roomService.updateRoomInfo();
            roomService.updateSchedule();
            this.roomService = roomService;

            this.fuzzyBookingService = new FuzzyBookingService(roomService, staffClient, personService, calendarWeb, calendarUpdate, calendarActions);

            this.booking = new BookingHandler(fuzzyBookingService, mock(JavaMailSender.class), mock(BookingResultMessageBuilder.class), metricRegistry());
        }
    }

    public static class Config {
        private final List<ObjectWithTimeTable<Person>> persons = new ArrayList<>();
        private final List<ObjectWithTimeTable<Resource.Info>> resources = new ArrayList<>();
        private final Map<String, Resource.Info> unvisibleResourceForUser = new HashMap<>();

        private Config() {
        }

        public static Config create() {
            return new Config();
        }

        public Config addPerson(@Nonnull ObjectWithTimeTable<Person> personWithTimeTable) {
            this.persons.add(personWithTimeTable);
            return this;
        }

        public Config addRoom(@Nonnull ObjectWithTimeTable<Resource.Info> resourcesWithTimeTable) {
            this.resources.add(resourcesWithTimeTable);
            return this;
        }

        public Config makeResourceUnvisible(Person p, Resource.Info resource) {
            unvisibleResourceForUser.put(p.getUid(), resource);
            return this;
        }

        public BookingObjects createObjects() {
            var calendar = new CalendarWebMock(resources, persons, unvisibleResourceForUser);
            var staffClient = mockStaffClient(persons);
            return new BookingObjects(calendar, staffClient);
        }

        @SafeVarargs
        public final Config addPersons(ObjectWithTimeTable<Person>... persons) {
            Arrays.stream(persons).forEach(this::addPerson);
            return this;
        }

        @SafeVarargs
        public final Config addRooms(ObjectWithTimeTable<Resource.Info>... persons) {
            Arrays.stream(persons).forEach(this::addRoom);
            return this;
        }
    }

    public enum RoomParams {
        NONE,
        LCD,
        VOICE_CONF,
        VIDEO_CONF
    }
}
