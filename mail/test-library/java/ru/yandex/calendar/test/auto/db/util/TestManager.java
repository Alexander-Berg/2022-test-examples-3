package ru.yandex.calendar.test.auto.db.util;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Either;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.bolts.collection.Tuple4;
import ru.yandex.bolts.function.Function;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventAttachment;
import ru.yandex.calendar.logic.beans.generated.EventFields;
import ru.yandex.calendar.logic.beans.generated.EventInvitation;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.beans.generated.EventResource;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Layer;
import ru.yandex.calendar.logic.beans.generated.LayerFields;
import ru.yandex.calendar.logic.beans.generated.LayerUser;
import ru.yandex.calendar.logic.beans.generated.MainEvent;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.beans.generated.Rdate;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.beans.generated.ResourceHelper;
import ru.yandex.calendar.logic.beans.generated.SettingsYt;
import ru.yandex.calendar.logic.beans.generated.TodoItem;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventInvitationDao;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.ExternalId;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventResourceDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.event.repetition.RepetitionRoutines;
import ru.yandex.calendar.logic.event.repetition.RepetitionUtils;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.layer.LayerDao;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.layer.LayerType;
import ru.yandex.calendar.logic.layer.LayerUserDao;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationDbManager;
import ru.yandex.calendar.logic.notification.NotificationRoutines;
import ru.yandex.calendar.logic.resource.ResourceDao;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.ResourceType;
import ru.yandex.calendar.logic.resource.reservation.ResourceReservationDao;
import ru.yandex.calendar.logic.resource.schedule.ResourceScheduleDao;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantData;
import ru.yandex.calendar.logic.sharing.participant.ParticipantsData;
import ru.yandex.calendar.logic.sharing.perm.EventActionClass;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.logic.todo.TodoDao;
import ru.yandex.calendar.logic.user.Group;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.logic.user.TestUsers;
import ru.yandex.calendar.logic.user.UserDao;
import ru.yandex.calendar.logic.user.UserGroupsDao;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.logic.user.UserRoutines;
import ru.yandex.calendar.micro.yt.entity.YtUser;
import ru.yandex.calendar.util.base.UidGen;
import ru.yandex.calendar.util.dates.DateTimeManager;
import ru.yandex.calendar.util.dates.DayOfWeek;
import ru.yandex.calendar.util.idlent.YandexUser;
import ru.yandex.commune.admin.web.support.ResourceServletSupport;
import ru.yandex.commune.dynproperties.DynamicProperty;
import ru.yandex.commune.dynproperties.DynamicPropertyManager;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox.PassportAuthDomain;
import ru.yandex.inside.passport.blackbox.PassportDomain;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.TranslitUtils;
import ru.yandex.misc.db.q.SqlQueryUtils;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.ip.InternetDomainName;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.lang.Validate;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.reflection.MethodX;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.TimeUtils;
import ru.yandex.misc.web.servlet.HttpServletRequestX;
import ru.yandex.misc.web.servletContainer.SingleWarJetty;

import static ru.yandex.calendar.frontend.display.DisplayManager.ROBOT_RES_MASTER_EMAIL;
import static ru.yandex.calendar.frontend.display.DisplayManager.ROBOT_RES_MASTER_UID;

@Slf4j
public class TestManager {
    public static final int NEXT_YEAR = DateTime.now(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).getYear() + 1;

    @Autowired
    private GenericBeanDao genericBeanDao;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private TodoDao todoDao;
    @Autowired
    private UserRoutines userRoutines;
    @Autowired
    private UserManager userManager;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private LayerDao layerDao;
    @Autowired
    private LayerUserDao layerUserDao;
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private EventInvitationDao eventInvitationDao;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private EventResourceDao eventResourceDao;
    @Autowired
    private ResourceScheduleDao resourceScheduleDao;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private UserDao userDao;
    @Autowired
    private NotificationDbManager notificationDbManager;
    @Autowired
    private DateTimeManager dateTimeManager;
    @Autowired
    private UserGroupsDao userGroupsDao;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private ResourceReservationDao resourceReservationDao;
    @Autowired
    private RepetitionRoutines repetitionRoutines;
    @Autowired
    private NotificationRoutines notificationRoutines;
    @Autowired
    private DynamicPropertyManager dynamicPropertyManager;

    public static final PassportUid UID = new PassportUid(19850L); // cal1 in test passport (password: tester)

    public static final PassportUid UID2 = new PassportUid(19851L); // cal2 in test passport (password: tester)

    public static final Email testExchangeUserEmail = new Email("testuser2013@msft.yandex-team.ru");
    public static final Email testExchangeSmolnyEmail = new Email("conf_smolny_2013@msft.yandex-team.ru");
    public static final Email testExchangeThreeLittlePigsEmail = new Email("conf_rr_3_1_2013@msft.yandex-team.ru");
    public static final Email testExchangeConfRr21 = new Email("conf_rr_2_1_2013@msft.yandex-team.ru");

    public static final Instant DEFAULT_TIME = new DateTime(2010, 9, 20, 18, 30, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();

    public static final DateTimeZone chrono = TimeUtils.EUROPE_MOSCOW_TIME_ZONE;
    // Timestamps
    public static final Instant eventStartTs = new DateTime(2009, 4, 27, 10, 0, 0, 0, chrono).toInstant();
    public static final Instant eventEndTs = new DateTime(2009, 4, 27, 12, 0, 0, 0, chrono).toInstant();
    public static final Instant recurrenceStartTs = new DateTime(2009, 4, 28, 13, 0, 0, 0, chrono).toInstant();
    public static final Instant recurrenceEndTs = new DateTime(2009, 4, 28, 15, 0, 0, 0, chrono).toInstant();
    public static final Instant recurrenceId = new DateTime(2009, 4, 28, 10, 0, 0, 0, chrono).toInstant();

    public static final int SEQUENCE = 2;

    public static final String DEF_NAME = "name";
    public static final int DEF_NTF_OFFSET = -15;
    public static final int DEF_NTF_LAYER_OFFSET = -55;

    public Tuple2<Event, Event> createEventWithRepetitionAndRecurInstWithSeq() {
        long repetitionId = createDailyRepetition();
        Event optFields = new Event();
        optFields.setRepetitionId(repetitionId);
        optFields.setSequence(SEQUENCE);
        optFields.setStartTs(eventStartTs);
        optFields.setEndTs(eventEndTs);

        long layerId = createLayer(UID);
        String externalId = CalendarUtils.generateExternalId();

        Event optFields1 = new Event();
        optFields1.setRecurrenceId(recurrenceId);
        optFields1.setSequence(SEQUENCE);
        optFields1.setStartTs(recurrenceStartTs);
        optFields1.setEndTs(recurrenceEndTs);

        Tuple2List<Event, String> eventsExternalIds = Cf.Tuple2List.arrayList();
        eventsExternalIds.add(optFields, externalId);
        eventsExternalIds.add(optFields1, externalId);
        ListF<Event> events = batchCreateEventOnLayer(UID, eventsExternalIds, layerId);

        return Tuple2.tuple(events.get(0), events.get(1));
    }

    public Event createSingleEventWithSeq() {
        Event optFields = new Event();
        optFields.setSequence(SEQUENCE);
        return createEventAndLayer(UID, eventStartTs, eventEndTs, optFields, CalendarUtils.generateExternalId());
    }

    public Event createEventOnLayer(PassportUid uid, Instant startTs, Instant endTs, Event eventOptionalFields, long layerId, String externalId) {
        eventOptionalFields.setStartTs(startTs);
        eventOptionalFields.setEndTs(endTs);
        return createEventOnLayer(uid, eventOptionalFields, layerId, externalId);
    }

    public Event createEventOnLayer(PassportUid uid, Event eventOptionalFields, long layerId, String externalId) {
        Event event = batchCreateEvent(uid, Tuple2List.fromPairs(eventOptionalFields, externalId)).single();

        EventLayer eventLayer = new EventLayer();

        eventLayer.setEventId(event.getId());
        eventLayer.setLayerId(layerId);
        eventLayer.setLCreatorUid(uid);

        eventLayer.setEventStartTs(new Instant(0));
        eventLayer.setEventEndTs(new Instant(0));
        eventLayer.setRepetitionDueTsNull();

        eventLayer.setIsPrimaryInst(true);
        genericBeanDao.insertBean(eventLayer);

        return event;
    }

    public ListF<Event> batchCreateEvent(final PassportUid uid, Tuple2List<Event, String> eventsExternalIds) {
        DateTimeZone tz = dateTimeManager.getTimeZoneForUid(uid);
        final MapF<String, Long> mainEventsByExternalId = mainEventDao.saveMainEvents(
                eventsExternalIds.get2().unique().map(ExternalId.consF()),
                tz, TestDateTimes.moscow(2000, 1, 1, 0, 0)).toMap();

        ListF<Event> events = eventsExternalIds.map((eventFields, externalId) -> {
            Event event = new Event();
            event.setCreatorUid(uid);
            event.setType(EventType.USER);
            event.setStartTs(TestDateTimes.moscow(2011, 2, 14, 20, 45));
            event.setEndTs(TestDateTimes.moscow(2011, 2, 14, 21, 0));
            event.setIsAllDay(false);
            event.setCreationTs(TestDateTimes.moscow(2011, 2, 14, 20, 45)); // assume created when started,
            event.setLastUpdateTs(event.getCreationTs()); // and updated when created
            event.setName(DEF_NAME);
            event.setRepetitionIdNull();
            event.setRecurrenceIdNull();

            event.setFields(eventFields);

            event.setMainEventId(mainEventsByExternalId.getOrThrow(externalId));

            return event;
        });

        for (Tuple2<Event, Long> t : genericBeanDao.insertBeansGetGeneratedKeys(events, events.first().getSetFields())) {
            Event event = t._1;
            long id = t._2;
            event.setId(id);
        }

        return events;
    }

    public ListF<Event> batchCreateEventOnLayer(final PassportUid uid, Tuple2List<Event, String> eventsExternalIds, final long layerId) {
        ListF<Event> events = batchCreateEvent(uid, eventsExternalIds);

        ListF<EventLayer> eventLayers = events.map(event -> {
            EventLayer eventLayer = new EventLayer();
            eventLayer.setEventId(event.getId());
            eventLayer.setLayerId(layerId);
            eventLayer.setLCreatorUid(uid);

            eventLayer.setEventStartTs(new Instant(0));
            eventLayer.setEventEndTs(new Instant(0));
            eventLayer.setRepetitionDueTsNull();
            return eventLayer;
        });

        genericBeanDao.insertBeans(eventLayers, eventLayers.first().getSetFields());

        return events;
    }

    public long createLayer(PassportUid uid) {
        return createLayers(Cf.list(uid)).single()._1.getId();
    }

    public long createLayer(PassportUid uid, LayerType type) {
        return createLayers(Cf.list(uid), Instant.now(), type).single()._1.getId();
    }

    public Tuple2List<Layer, LayerUser> createLayers(ListF<PassportUid> uids) {
        return createLayers(uids, TestDateTimes.moscow(2011, 12, 1, 0, 2), LayerType.USER);
    }

    public Tuple2List<Layer, LayerUser> createLayers(ListF<PassportUid> uids, final Instant now, LayerType type) {
        if (uids.isEmpty()) {
            return Cf.Tuple2List.cons();
        }

        ListF<Layer> layers = uids.map(uid -> {
            Layer layer = new Layer();
            layer.setName("layer");
            layer.setCreatorUid(uid);
            layer.setType(type);
            layer.setCreationTs(now);
            layer.setLastUpdateTs(now);
            layer.setCollLastUpdateTs(now);
            return layer;
        });

        for (Tuple2<Layer, Long> t : genericBeanDao.insertBeansGetGeneratedKeys(layers, layers.first().getSetFields())) {
            Layer layer = t._1;
            Long id = t._2;
            layer.setId(id);
        }

        ListF<LayerUser> layerUsers = layers.map(layer -> {
            LayerUser layerUser = new LayerUser();
            layerUser.setUid(layer.getCreatorUid());
            layerUser.setLayerId(layer.getId());
            layerUser.setPerm(LayerActionClass.ADMIN);
            layerUser.setFieldDefaults(layerRoutines.createDefaultLayerUserOverrides(layer.getCreatorUid().getDomain()));
            return layerUser;
        });

        Tuple2List<LayerUser, Long> layerUsersWithIds = genericBeanDao
                .insertBeansGetGeneratedKeys(layerUsers, layerUsers.first().getSetFields());

        for (Tuple2<LayerUser, Long> t : layerUsersWithIds) {
            t._1.setId(t._2);
        }
        MapF<Long, LayerUser> byLayerId = layerUsersWithIds.get1().toMapMappingToKey(LayerUser.getLayerIdF());

        return layers.zipWith(l -> byLayerId.getOrThrow(l.getId()));
    }

    private Event createEventAndLayer(PassportUid uid, Instant startTs, Instant endTs, Event eventOptionalFields, String externalId) {
        long layerId = createLayer(uid);
        return createEventOnLayer(uid, startTs, endTs, eventOptionalFields, layerId, externalId);
    }

    public Rdate createRdate(long eventId) {
        Rdate rdate = new Rdate();
        rdate.setStartTs(new DateTime(2009, 4, 30, 17, 0, 0, 0, chrono).toInstant());
        rdate.setEventId(eventId);
        genericBeanDao.insertBean(rdate);
        return rdate;
    }

    public Rdate createExdate(Instant exdateStartTs, long eventId) {
        Rdate rdate = new Rdate();
        rdate.setIsRdate(false);
        rdate.setStartTs(exdateStartTs);
        rdate.setEventId(eventId);
        genericBeanDao.insertBean(rdate);
        return rdate;
    }

    private Rdate createRdate(Instant rdateStartTs, Option<Instant> rdateEndTs, long eventId) {
        Rdate rdate = new Rdate();
        rdate.setIsRdate(true);
        rdate.setStartTs(rdateStartTs);
        rdate.setEndTs(rdateEndTs);
        rdate.setEventId(eventId);
        genericBeanDao.insertBean(rdate);
        return rdate;
    }

    public Rdate createRdate(Instant rdateStartTs, long eventId) {
        return createRdate(rdateStartTs, Option.empty(), eventId);
    }

    public Rdate createRdate(Instant rdateStartTs, Instant rdateEndTs, long eventId) {
        return createRdate(rdateStartTs, Option.of(rdateEndTs), eventId);
    }

    public long createWeeklyRepetition(DayOfWeek dow) {
        Repetition repetition = new Repetition();
        repetition.setREach(1);
        repetition.setType(RegularRepetitionRule.WEEKLY);
        repetition.setRWeeklyDays(dow.getDbValue());
        return genericBeanDao.insertBeanGetGeneratedKey(repetition);
    }

    public long createDailyRepetitionWithDueTs(Instant dueTs) {
        Repetition repetition = new Repetition();
        repetition.setREach(1);
        repetition.setDueTs(dueTs);
        repetition.setType(RegularRepetitionRule.DAILY);
        return genericBeanDao.insertBeanGetGeneratedKey(repetition);
    }

    public Event createDailyRepeatedEventWithNotification(PassportUid uid, Instant startTs) {
        long repetitionId = createDailyRepetition();
        Event optFields = new Event();
        Instant endTs = startTs.plus(Duration.standardHours(1));
        optFields.setRepetitionId(repetitionId);
        Event event = createEventAndLayer(uid, startTs, endTs, optFields, CalendarUtils.generateExternalId());
        createEventUserWithDefaultEmailNotification(uid, event.getId());
        return event;
    }

    public Event createSingleEventWithNotification(PassportUid uid, Instant startTs) {
        Event optFields = new Event();
        Instant endTs = startTs.plus(Duration.standardHours(1));
        Event event = createEventAndLayer(uid, startTs, endTs, optFields, CalendarUtils.generateExternalId());
        createEventUserWithDefaultEmailNotification(uid, event.getId());
        return event;
    }

    public Tuple2<Event, Event> createEventWithRepetitionAndRecurInstWithSameTime(PassportUid uid, int recurInstStartTsShiftHours) {
        long layerId = createLayer(uid);
        String externalId = CalendarUtils.generateExternalId();

        long repetitionId = createDailyRepetition();
        Event optFields = new Event();
        optFields.setRepetitionId(repetitionId);
        optFields.setStartTs(eventStartTs);
        optFields.setEndTs(eventEndTs);

        Event optFields1 = new Event();
        Instant recurIdStartTs = addHours(eventStartTs, recurInstStartTsShiftHours, chrono);
        Instant recurIdEndTs = addHours(eventEndTs, recurInstStartTsShiftHours, chrono);
        optFields1.setRecurrenceId(eventStartTs);
        optFields1.setStartTs(recurIdStartTs);
        optFields1.setEndTs(recurIdEndTs);

        Tuple2List<Event, String> eventsExternalIds = Cf.Tuple2List.arrayList();
        eventsExternalIds.add(optFields, externalId);
        eventsExternalIds.add(optFields1, externalId);
        ListF<Event> events = batchCreateEventOnLayer(uid, eventsExternalIds, layerId);

        return Tuple2.tuple(events.get(0), events.get(1));
    }

    public Event createEventWithDailyRepetition(PassportUid uid) {
        long repetitionId = createDailyRepetition();
        return createRepeatedEvent(uid, repetitionId);
    }

    public Event createRepeatedEvent(PassportUid uid, long repetitionId) {
        return createRepeatedEvent(uid, repetitionId, eventStartTs, eventEndTs);
    }

    public Event createRepeatedEvent(PassportUid uid, long repetitionId, Instant eventStartTs, Instant eventEndTs) {
        val optFields = new Event();
        optFields.setRepetitionId(repetitionId);
        val layerId = layerRoutines.getOrCreateDefaultLayer(uid);
        return createEventOnLayer(
                uid, eventStartTs, eventEndTs, optFields, layerId, CalendarUtils.generateExternalId());
    }

    public Tuple2<Event, Event> createEventWithRepetitionAndRecurrence(PassportUid uid, long repetitionId,
                                                                       Instant recurrenceStartTs,
                                                                       Instant recurrenceEndTs) {
        long layerId = layerRoutines.getOrCreateDefaultLayer(uid);
        String externalId = CalendarUtils.generateExternalId();

        Event optFields = new Event();
        optFields.setRepetitionId(repetitionId);
        optFields.setStartTs(eventStartTs);
        optFields.setEndTs(eventEndTs);

        Event optFields1 = new Event();
        optFields1.setRecurrenceId(recurrenceId);
        optFields1.setStartTs(recurrenceStartTs);
        optFields1.setEndTs(recurrenceEndTs);

        Tuple2List<Event, String> eventsExternalIds = Cf.Tuple2List.arrayList();
        eventsExternalIds.add(optFields, externalId);
        eventsExternalIds.add(optFields1, externalId);
        ListF<Event> events = batchCreateEventOnLayer(uid, eventsExternalIds, layerId);

        return Tuple2.tuple(events.get(0), events.get(1));
    }

    public Tuple2<Event, Event> createEventWithRepetitionAndRecurrence(PassportUid uid, Instant recurrenceStartTs,
                                                                       Instant recurrenceEndTs) {
        return createEventWithRepetitionAndRecurrence(uid, createDailyRepetition(), recurrenceStartTs, recurrenceEndTs);
    }

    public Tuple2<Event, Event> createEventWithRepetitionAndRecurrence(PassportUid uid) {
        return createEventWithRepetitionAndRecurrence(uid, recurrenceStartTs, recurrenceEndTs);
    }

    public Tuple4<Event, Long, Rdate, Event> createEventWithRepetitionAndRdateAndRecurrence(PassportUid uid) {
        long layerId = layerRoutines.getOrCreateDefaultLayer(uid);
        String externalId = CalendarUtils.generateExternalId();

        long repetitionId = createDailyRepetition();
        Event optFields = new Event();
        optFields.setRepetitionId(repetitionId);
        optFields.setStartTs(eventStartTs);
        optFields.setEndTs(eventEndTs);

        Event optFields1 = new Event();
        optFields1.setRecurrenceId(recurrenceId);
        optFields1.setStartTs(recurrenceStartTs);
        optFields1.setEndTs(recurrenceEndTs);

        Tuple2List<Event, String> eventsExternalIds = Cf.Tuple2List.arrayList();
        eventsExternalIds.add(optFields, externalId);
        eventsExternalIds.add(optFields1, externalId);
        ListF<Event> events = batchCreateEventOnLayer(uid, eventsExternalIds, layerId);

        Rdate rdate = createRdate(events.get(0).getId());
        return Tuple4.tuple(events.get(0), repetitionId, rdate, events.get(1));
    }

    public Tuple2<Event, Rdate> createEventWithRdate(PassportUid uid) {
        Event event = createDefaultEventWithEventLayerAndEventUser(uid, "createEventWithRdate");
        Rdate rdate = createRdate(event.getId());
        return Tuple2.tuple(event, rdate);
    }

    public Tuple2<Event, Rdate> createEventWithRepetitionAndExdate(PassportUid uid) {
        long repetitionId = createDailyRepetition();
        Event event = createRepeatedEvent(uid, repetitionId);
        Rdate exdate = createExdate(new DateTime(2009, 4, 29, 10, 0, 0, 0, chrono).toInstant(), event.getId());
        return Tuple2.tuple(event, exdate);
    }

    public static Instant addHours(Instant instant, int amount, DateTimeZone chrono) {
        DateTime dt = new DateTime(instant.getMillis(), chrono);
        return new Instant(dt.plusHours(amount).getMillis());
    }

    public void cleanUser(PassportUid uid) {
        cleanUsers(Cf.list(uid));
    }

    public void cleanUsers(ListF<PassportUid> uids) {
        userRoutines.deleteUsers(uids, ActionInfo.webTest());
    }

    public void cleanUser(YandexUser user) {
        cleanUser(user.getUid());
    }

    public void cleanUser(PassportLogin login) {
        cleanUser(userManager.getUidByLoginForTest(login));
    }

    public TestUserInfo prepareUser(String login) {
        return prepareUsers(Cf.list(login)).single();
    }

    public ListF<TestUserInfo> prepareUsers(ListF<String> logins) {
        // XXX: calls blackbox twice
        Tuple2List<PassportLogin, PassportUid> uids = userManager.getUidByLoginForTestBatch(logins.map(PassportLogin::cons));
        return prepareUsersByUid(uids.get2(), TestDateTimes.moscow(2011, 12, 1, 0, 11));
    }

    private ListF<TestUserInfo> prepareUsersByUid(ListF<PassportUid> uids, Instant now) {
        ListF<YandexUser> users = userManager
                .getUserByUidBatch(uids)
                .get2()
                .map(Option::get);

        cleanUsers(uids);
        settingsRoutines.createSettingsIfNotExistsForUids(uids);

        val layerByUid = createDefaultLayersForUsers(users.map(YandexUser.getUidF()), now)
            .get1()
            .toMapMappingToKey(LayerFields.CREATOR_UID.getF());

        val userInfoByUid = userManager.getUserInfos(users.map(YandexUser.getUidF())).toMapMappingToKey(UserInfo.getUidF());

        return users.map(user -> new TestUserInfo(
                userInfoByUid.getTs(user.getUid()), user.getLogin(),
                user.getEmail().get(), layerByUid.getOrThrow(user.getUid()).getId()));
    }

    public void prepareYandexTeamUser(YtUser user) {
        userManager.registerYandexTeamUserForTest(user);
    }

    public TestUserInfo prepareYandexUser(YandexUser user) {
        return prepareYandexUser(user, new Group[0]);
    }

    public TestUserInfo prepareYandexUser(YandexUser user, Group ... groups) {
        userManager.registerYandexUserForTest(user);
        cleanUser(user.getUid());

        settingsRoutines.createSettingsIfNotExistsForUids(Cf.list(user.getUid()));
        long layerId = createDefaultLayerForUser(user.getUid(), TestDateTimes.moscow(2011, 11, 30, 23, 44));

        for (Group group : groups) {
            userGroupsDao.addGroup(Either.left(user.getUid()), group);
        }

        return new TestUserInfo(userManager.getUserInfo(user.getUid()),
                user.getLogin(), user.getEmail().getOrThrow("email is required"), layerId);
    }

    public TestUserInfo prepareRandomYaTeamUser(long offset) {
        return prepareYandexUser(createRandomYaTeamUser(offset));
    }

    public TestUserInfo prepareRandomYaTeamUser(long offset, String login) {
        return prepareYandexUser(createRandomYaTeamUserWithCustomLogin(offset, Option.of(login)));
    }

    public TestUserInfo prepareRandomYaTeamSuperUser(long offset) {
        val yaUser = createRandomYaTeamUser(offset);
        TestUserInfo user = prepareYandexUser(yaUser).withSuperUser(true);

        userGroupsDao.addGroup(Either.left(user.getUid()), Group.SUPER_USER);
        userManager.registerYandexUserForTest(yaUser);  // invalidate userManager.userInfoByUidCache cache

        return user;
    }

    public TestUserInfo prepareYaTeamUser(YandexUser yaUser, boolean isSuperUser) {
        val user = prepareYandexUser(yaUser).withSuperUser(isSuperUser);

        userGroupsDao.addGroup(Either.left(user.getUid()), Group.SUPER_USER);
        userManager.registerYandexUserForTest(yaUser);  // invalidate userManager.userInfoByUidCache cache

        return user;
    }

    public TestUserInfo prepareResourceMaster() {
        val user = createResourceMaster();
        return prepareYaTeamUser(user, true);
    }

    public long createEventUser(
            PassportUid uid, long eventId, Decision decision,
            Option<Boolean> isParticipantOrganizerO)
    {
        return createEventUser(uid, eventId, decision, isParticipantOrganizerO, new EventUser());
    }

    public long createEventUser(
            PassportUid uid, long eventId, Decision decision,
            Option<Boolean> isParticipantOrganizerO, EventUser eventUserOverrides)
    {
        EventUser eventUser = new EventUser();
        eventUser.setEventId(eventId);
        eventUser.setUid(uid);
        eventUser.setDecision(decision);
        eventUser.setAvailability(Availability.MAYBE);
        eventUser.setFields(eventUserOverrides);
        boolean isParticipant = isParticipantOrganizerO.isPresent();
        eventUser.setIsOrganizer(isParticipant && isParticipantOrganizerO.get());
        eventUser.setIsAttendee(isParticipant);
        eventUser.setPrivateToken(UidGen.createPrivateToken());
        // XXX fill ActionInfo data
        return genericBeanDao.insertBeanGetGeneratedKey(eventUser);
    }

    public EventLayer createEventLayer(long layerId, long eventId) {
        // XXX wrong - fix this to test creation of hidden event in exchange // ssytnik@
        return createEventLayer(layerId, eventId, false);
    }

    public EventLayer createEventLayer(long layerId, long eventId, boolean isPrimary) {
        EventLayer eventLayer = new EventLayer();
        eventLayer.setEventId(eventId);
        eventLayer.setLayerId(layerId);
        eventLayer.setIsPrimaryInst(isPrimary);
        eventLayer.setLCreatorUid(layerDao.findLayerById(layerId).getCreatorUid());

        eventLayer.setEventStartTs(new Instant(0));
        eventLayer.setEventEndTs(new Instant(0));
        eventLayer.setRepetitionDueTsNull();
        // XXX fill ActionInfo data
        genericBeanDao.insertBean(eventLayer);
        return eventLayer;
    }

    public void createOrUpdateLayerUser(PassportUid uid, long layerId, LayerActionClass perm) {
        Option<LayerUser> layerUserO = layerUserDao.findLayerUserByLayerIdAndUid(layerId, uid);

        if(layerUserO.isPresent()) {
            layerUserO.get().setPerm(perm);
            genericBeanDao.updateBean(layerUserO.get());
            return;
        }

        LayerUser layerUser = new LayerUser();
        layerUser.setUid(uid);
        layerUser.setLayerId(layerId);
        layerUser.setPerm(perm);
        layerUser.setFieldDefaults(layerRoutines.createDefaultLayerUserOverrides(uid.getDomain()));
        genericBeanDao.insertBeanGetGeneratedKey(layerUser);
    }

    public EventResource createEventResource(long resourceId, long eventId) {
        return createEventResource(resourceId, eventId, new Instant(0), new Instant(0));
    }

    public EventResource createEventResource(
            long resourceId, long eventId, Instant startTs, Instant endTs)
    {
        EventResource eventResource = new EventResource();
        eventResource.setEventId(eventId);
        eventResource.setResourceId(resourceId);

        eventResource.setEventStartTs(startTs);
        eventResource.setEventEndTs(endTs);
        eventResource.setRepetitionDueTsNull();

        // XXX fill ActionInfo data
        genericBeanDao.insertBean(eventResource);
        return eventResource;
    }

    public static Event createDefaultEventTemplate(PassportUid uid, String eventName, Option<Instant> startTsO) {
        Event e = new Event();
        e.setCreatorUid(uid);
        e.setName(eventName);
        e.setLocation("");
        e.setType(EventType.USER);
        e.setStartTs(startTsO.getOrElse(DEFAULT_TIME));
        e.setEndTs(e.getStartTs().plus(Duration.standardHours(1)));
        e.setIsAllDay(false);
        e.setCreationTs(e.getStartTs());
        e.setLastUpdateTs(e.getCreationTs());
        return e;
    }

    public EventData createDefaultEventData(PassportUid uid, String eventName) {
        return createDefaultEventDataInner(uid, eventName, Option.empty());
    }

    public EventData createDefaultEventData(PassportUid uid, String eventName, Instant startTs) {
        return createDefaultEventDataInner(uid, eventName, Option.of(startTs));
    }

    private EventData createDefaultEventDataInner(PassportUid uid, String eventName, Option<Instant> startTs) {
        EventData eventData = new EventData();
        eventData.setEvent(createDefaultEventTemplate(uid, eventName, startTs));
        eventData.setLayerId(layerRoutines.getOrCreateDefaultLayer(uid));
        eventData.setTimeZone(dateTimeManager.getTimeZoneForUid(uid));
        return eventData;
    }

    public Event createDefaultEvent(PassportUid uid, String eventName, boolean isClosed) {
        return createDefaultEvent(uid, eventName, new Event(), isClosed);
    }

    public Event createDefaultEvent(PassportUid uid, String eventName) {
        return createDefaultEvent(uid, eventName, new Event());
    }

    public Event createDefaultEventInFuture(PassportUid uid, String eventName) {
        val now = TestDateTimes.moscow(NEXT_YEAR, 1, 1, 0, 0);
        return createDefaultEvent(uid, eventName, new Event(), false, now, Optional.of(now));
    }

    public Event createDefaultEventWithDailyRepetitionInFuture(PassportUid uid, String eventName) {
        Event e = createDefaultEventInFuture(uid, eventName);
        e.setRepetitionId(createDailyRepetitionAndLinkToEvent(e.getId()));

        return e;
    }

    public Event createDefaultEventWithDailyRepetition(PassportUid uid, String eventName) {
        Event e = createDefaultEvent(uid, eventName);
        e.setRepetitionId(createDailyRepetitionAndLinkToEvent(e.getId()));

        return e;
    }

    public Event createDefaultRecurrence(PassportUid uid, long masterEventId, ReadableInstant recurrenceId) {
        return createDefaultRecurrence(uid, masterEventId, recurrenceId, Duration.ZERO);
    }

    public Event createDefaultRecurrence(
            PassportUid uid, long masterEventId, ReadableInstant recurrenceId, Duration startEndShift)
    {
        Event masterEvent = eventDao.findEventById(masterEventId);

        DateTimeZone tz = dateTimeManager.getTimeZoneForUid(uid);

        Validate.none(masterEvent.getRecurrenceId());
        Validate.some(masterEvent.getRepetitionId());
        Validate.equals(
                new LocalTime(masterEvent.getStartTs(), tz),
                new LocalTime(recurrenceId, tz));

        Event recurrenceData = new Event();
        recurrenceData.setRecurrenceId(recurrenceId.toInstant());

        Duration eventDuration = EventRoutines.getInstantInterval(masterEvent).getDuration();
        recurrenceData.setStartTs(recurrenceId.toInstant().plus(startEndShift));
        recurrenceData.setEndTs(recurrenceId.toInstant().plus(eventDuration).plus(startEndShift));

        String recurrenceEventName = masterEvent.getName() + "-recurrence-" + recurrenceId;
        return createDefaultEvent(uid, recurrenceEventName, recurrenceData, masterEvent.getMainEventId());
    }

    public Event createDefaultEvent(PassportUid uid, String eventName, ReadableInstant startTs) {
        return createDefaultEvent(uid, eventName, startTs.toInstant(), startTs.toInstant().plus(Duration.standardHours(1)));
    }

    public Event createDefaultEvent(PassportUid uid, String eventName, ReadableInstant startTs, ReadableInstant endTs,
                                    boolean isClosed) {
        Event eventOverrides = new Event();
        eventOverrides.setStartTs(startTs.toInstant());
        eventOverrides.setEndTs(endTs.toInstant());
        return createDefaultEvent(uid, eventName, eventOverrides, isClosed);
    }

    public Event createDefaultEvent(PassportUid uid, String eventName, ReadableInstant startTs, ReadableInstant endTs) {
        Event eventOverrides = new Event();
        eventOverrides.setStartTs(startTs.toInstant());
        eventOverrides.setEndTs(endTs.toInstant());
        return createDefaultEvent(uid, eventName, eventOverrides);
    }

    public Event createDefaultEvent(PassportUid uid, String eventName, Event eventOverrides) {
        return createDefaultEvent(uid, eventName, eventOverrides, false);
    }

    public Event createDefaultEvent(PassportUid uid, String eventName, Event eventOverrides, boolean isClosed) {
        return createDefaultEvent(uid, eventName, eventOverrides, isClosed, TestDateTimes.moscow(2000, 1, 1, 0, 0), Optional.empty());
    }

    public Event createDefaultEvent(PassportUid uid, String eventName, Event eventOverrides, boolean isClosed, ReadableInstant now,
                                    Optional<Instant> startTs) {
        DateTimeZone tz = dateTimeManager.getTimeZoneForUid(uid);

        long mainEventId = eventRoutines.createMainEvent(tz, ActionInfo.webTest(now));
        return createDefaultEvent(uid, eventName, eventOverrides, mainEventId, Optional.of(isClosed), startTs);
    }

    public Event createDefaultEwsExportedEvent(PassportUid uid, String eventName) {
        return createDefaultEwsExportedEvent(uid, eventName, DEFAULT_TIME);
    }

    public Event createDefaultEwsExportedEvent(PassportUid uid, String eventName, ReadableInstant startTs) {
        DateTimeZone tz = dateTimeManager.getTimeZoneForUid(uid);

        long mainEventId = eventRoutines.createMainEvent(CalendarUtils.generateExternalId(), tz, true,
                ActionInfo.webTest(TestDateTimes.moscow(2000, 1, 1, 0, 0)));

        Event eventOverrides = new Event();
        eventOverrides.setStartTs(startTs.toInstant());
        eventOverrides.setEndTs(startTs.toInstant().plus(Duration.standardHours(1)));

        return createDefaultEvent(uid, eventName, eventOverrides, mainEventId);
    }

    public Event createDefaultEvent(PassportUid uid, String eventName, Event eventOverrides, long mainEventId) {
        return createDefaultEvent(uid, eventName, eventOverrides, mainEventId, Optional.empty());
    }

    public Event createDefaultEvent(PassportUid uid, String eventName, Event eventOverrides, long mainEventId,
                                    Optional<Boolean> isClosed) {
        return createDefaultEvent(uid, eventName, eventOverrides, mainEventId, isClosed, Optional.empty());
    }

    public Event createDefaultEvent(PassportUid uid, String eventName, Event eventOverrides, long mainEventId,
                                    Optional<Boolean> isClosed, Optional<Instant> startTs) {
        layerRoutines.getOrCreateDefaultLayer(uid); // XXX check if we can delete this // ssytnik@

        Event event = createDefaultEventTemplate(uid, eventName, Option.x(startTs));
        event.setMainEventId(mainEventId);
        event.setSequence(0);
        event.setFields(eventOverrides);
        event.setFieldValueDefault(EventFields.RECURRENCE_ID, null);
        event.setFieldValueDefault(EventFields.REPETITION_ID, null);
        isClosed.ifPresent(closed -> event.setPermAll(closed ? EventActionClass.NONE : EventActionClass.VIEW));
        // XXX fill ActionInfo data
        event.setId(genericBeanDao.insertBeanGetGeneratedKey(event));
        return event;
    }

    public Event createDefaultEventWithEventLayerAndEventUser(PassportUid uid, String eventName) {
        return createDefaultEventWithEventLayerAndEventUser(uid, eventName, new Event());
    }

    public Event createDefaultEventWithEventLayerAndEventUser(PassportUid uid, String eventName, Instant startTs, Instant endTs) {
        return createDefaultEventWithEventLayerAndEventUser(uid, eventName, startTs, endTs, Optional.empty());
    }

    public Event createDefaultEventWithEventLayerAndEventUser(PassportUid uid, String eventName, Instant startTs, Instant endTs,
                                                              Optional<Boolean> isClosed) {
        Event eventOverrides = new Event();
        eventOverrides.setStartTs(startTs);
        eventOverrides.setEndTs(endTs);
        return createDefaultEventWithEventLayerAndEventUser(uid, eventName, eventOverrides, isClosed);
    }

    public Event createDefaultEventWithEventLayerAndEventUser(PassportUid uid, String eventName, Event eventOverrides) {
        return createDefaultEventWithEventLayerAndEventUser(uid, eventName, eventOverrides, new EventUser());
    }

    public Event createDefaultEventWithEventLayerAndEventUser(PassportUid uid, String eventName, Event eventOverrides,
                                                              Optional<Boolean> isClosed) {
        return createDefaultEventWithEventLayerAndEventUser(uid, eventName, eventOverrides, new EventUser(), isClosed);
    }

    public Event createDefaultEventWithEventLayerAndEventUser(PassportUid uid, String eventName, Event eventOverrides,
                                                              EventUser eventUserOverrides) {
        return createDefaultEventWithEventLayerAndEventUser(uid, eventName, eventOverrides, eventUserOverrides, Optional.empty());
    }

    public Event createDefaultEventWithEventLayerAndEventUser(PassportUid uid, String eventName, Event eventOverrides,
                                                              EventUser eventUserOverrides, Optional<Boolean> isClosed) {
        Event event = createDefaultEvent(uid, eventName, eventOverrides, isClosed.orElse(false));

        final long eventUser = createEventUser(uid, event.getId(), Decision.YES, Option.empty(), eventUserOverrides);
        final EventLayer eventLayer = createEventLayer(layerRoutines.getOrCreateDefaultLayer(uid), event.getId(), true);
        log.debug("uid = " + uid + ", eventName = " + eventName + ", eventOverrides = " + eventOverrides +
                ", eventUserOverrides = " + eventUserOverrides + ", isClosed = " + isClosed +
                " -> eventUser = " + eventUser + ", eventLayer = " + eventLayer.getLayerId()
        );
        return event;
    }

    public Event createDefaultEventWithEventLayerAndEventUserInFuture(PassportUid uid, String eventName) {
        val now = TestDateTimes.moscow(NEXT_YEAR, 1, 1, 0, 0);
        return createDefaultEventWithEventLayerAndEventUser(uid, eventName, new Event(), new EventUser(), Optional.empty(), now);
    }

    public Event createDefaultEventWithEventLayerAndEventUser(PassportUid uid, String eventName, Event eventOverrides,
                                                              EventUser eventUserOverrides, Optional<Boolean> isClosed,
                                                              Instant now) {
        Event event = createDefaultEvent(uid, eventName, eventOverrides, isClosed.orElse(false),
                now, Optional.of(now));

        createEventUser(uid, event.getId(), Decision.YES, Option.empty(), eventUserOverrides);
        createEventLayer(layerRoutines.getOrCreateDefaultLayer(uid), event.getId(), true);

        return event;
    }

    private Event createDefaultMeeting(PassportUid uid, String eventName, EventType eventType, Optional<Boolean> isClosed) {
        Event event = new Event();
        event.setType(eventType);
        return createDefaultEvent(uid, eventName, event, isClosed.orElse(false));
    }

    public Event createDefaultMeetingInFuture(PassportUid uid, String eventName) {
        val now = TestDateTimes.moscow(NEXT_YEAR, 1, 1, 0, 0);
        Event event = new Event();
        event.setType(EventType.USER);
        return createDefaultEvent(uid, eventName, event, false, now, Optional.of(now));
    }

    public Tuple2<ListF<Event>, Repetition> createMeetingWithRepetitionDueAndRecurrenceIdO(
            PassportUid uid, String eventNameBase, DateTime eventStart,
            RegularRepetitionRule repetitionType, int count,
            ListF<Either<PassportUid, Resource>> attendees,
            Option<Tuple2<LocalDate, Duration>> recurrenceIdO) // note: recurrenceIdO is ignored in RepetitionInstanceInfo
    {
        Validate.V.isTrue(repetitionType == RegularRepetitionRule.DAILY || repetitionType == RegularRepetitionRule.WEEKLY);

        DateTime eventEnd = eventStart.plusMinutes(30);
        InstantInterval eventInterval = new InstantInterval(eventStart.toInstant(), eventEnd.toInstant());
        DateTimeZone tz = eventStart.getZone();

        long repetitionId = repetitionType == RegularRepetitionRule.DAILY ?
                createDailyRepetition() :
                createWeeklyRepetition(DayOfWeek.fromDay(eventStart.toLocalDate()));
        Repetition repetition = eventDao.findRepetitionById(repetitionId);
        RepetitionInstanceInfo rii = RepetitionInstanceInfo.create(eventInterval, tz, Option.of(repetition));
        Instant lastInstanceStart = RepetitionUtils.instanceStart(rii, count);
        eventDao.updateRepetitionDueTs(repetitionId, lastInstanceStart.toDateTime(tz).plusDays(1).toInstant());

        long mainEventId = eventRoutines.createMainEvent(tz, ActionInfo.webTest(TestDateTimes.moscow(2000, 1, 1, 0, 0)));

        ListF<Event> createdEvents = Cf.arrayList();
        for (int i = 0; i <= recurrenceIdO.size(); i++) { // 0 = master, 1 = recurrence
            Event eventData = new Event();
            if (i == 0) {
                eventData.setStartTs(eventInterval.getStart());
                eventData.setEndTs(eventInterval.getEnd());
                eventData.setRepetitionId(repetitionId);
            } else {
                LocalDate recurrenceIdDate = recurrenceIdO.get().get1();
                Duration diff = recurrenceIdO.get().get2();

                eventData.setStartTs(recurrenceIdDate.toDateTime(eventStart).plus(diff).toInstant());
                eventData.setEndTs(recurrenceIdDate.toDateTime(eventEnd).plus(diff).toInstant());
                eventData.setRecurrenceId(recurrenceIdDate.toDateTime(eventStart).toInstant());
                eventData.setName(eventNameBase + " - recurrenceId");
            }
            Event event = createDefaultEvent(uid, eventNameBase, eventData, mainEventId);
            createdEvents.add(event);

            addUserParticipantToEvent(event.getId(), uid, Decision.YES, true);
            for (Either<PassportUid, Resource> attendee : attendees) {
                if (attendee.isLeft()) {
                    addUserParticipantToEvent(event.getId(), attendee.getLeft(), Decision.YES, false);
                } else {
                    addResourceParticipantToEvent(event.getId(), attendee.getRight());
                }
            }
        }

        return Tuple2.tuple(createdEvents, repetition);
    }


    public TodoItem createDefaultTodoItem(PassportUid uid, String todoName, long todoListId) {
        return createDefaultTodoItem(uid, todoName, todoListId, new TodoItem());
    }

    public TodoItem createDefaultTodoItem(PassportUid uid, String todoName, long todoListId, TodoItem overrides) {
        TodoItem todoItem = new TodoItem();
        todoItem.setTitle(todoName);
        todoItem.setTodoListId(todoListId);
        todoItem.setCreatorUid(uid);
        todoItem.setCreationTs(Instant.now());
        todoItem.setLastUpdateTs(Instant.now());
        todoItem.setExternalId(CalendarUtils.generateExternalId());
        todoItem.setPos(1);
        todoItem.setFields(overrides);
        todoItem.setId(todoDao.saveTodoItem(todoItem, ActionInfo.webTest(Instant.now())));
        return todoItem;
    }

    public Event createDefaultMeeting(PassportUid uid, String eventName) {
        return createDefaultMeeting(uid, eventName, Optional.empty());
    }

    public Event createDefaultMeeting(PassportUid uid, String eventName, Optional<Boolean> isClosed) {
        return createDefaultMeeting(uid, eventName, EventType.USER, isClosed);
    }


    public long createDailyRepetitionAndLinkToEvent(long eventId) {
        return createDailyRepetitionInnerAndLinkToEvent(eventId, Option.empty());
    }

    public long createDailyRepetitionWithDueTsAndLinkToEvent(long eventId, Instant dueTs) {
        return createDailyRepetitionInnerAndLinkToEvent(eventId, Option.of(dueTs));
    }

    private long createDailyRepetitionInnerAndLinkToEvent(long eventId, Option<Instant> dueTsO) {
        Repetition r = createDailyRepetitionTemplate();
        if (dueTsO.isPresent()) {
            r.setDueTs(dueTsO.get());
        }
        long repetitionId = genericBeanDao.insertBeanGetGeneratedKey(r);

        linkRepetitionToEvent(eventId, repetitionId);
        return repetitionId;
    }

    public void linkRepetitionToEvent(long eventId, long repetitionId) {
        Event e = new Event();
        e.setId(eventId);
        e.setRepetitionId(repetitionId);
        genericBeanDao.updateBean(e);
    }

    public void addUserParticipantToEvent(
            long eventId, TestUserInfo user, Decision decision, boolean isOrganizer)
    {
        addUserParticipantToEvent(eventId, user.getUid(), decision, isOrganizer);
    }

    public void addUserParticipantToEvent(
            long eventId, PassportLogin login, Decision decision, boolean isOrganizer)
    {
        PassportUid uid = userManager.getUidByLoginForTest(login);
        addUserParticipantToEvent(eventId, uid, decision, isOrganizer);
    }

    public void addUserParticipantToEvent(long eventId, PassportUid uid,
            Decision decision, boolean isOrganizer)
    {
        long layerId = layerRoutines.getOrCreateDefaultLayer(uid);

        createEventUser(uid, eventId, decision, Option.of(isOrganizer));
        createEventLayer(layerId, eventId);
    }

    public void addSubscriberToEvent(long eventId, PassportUid uid) {
        long layerId = layerRoutines.getOrCreateDefaultLayer(uid);

        EventUser eventUser = new EventUser();
        eventUser.setIsSubscriber(true);
        createEventUser(uid, eventId, Decision.YES, Option.empty(), eventUser);
        createEventLayer(layerId, eventId);
    }

    public void saveEventNotifications(PassportUid uid, long eventId, Notification ... notifications) {
        notificationDbManager.saveEventNotifications(
                eventUserDao.findEventUserByEventIdAndUid(eventId, uid).getOrThrow("event user not found").getId(),
                Cf.list(notifications));
    }

    /**
     * @see #addUserParticipantToEvent(long, PassportUid, Decision, boolean)
     */
    public void batchAddUserParticipantsToEvents(ListF<BatchParticipationParameters> parameters) {
        ListF<EventUser> eventUsers = parameters.map(p -> {
            EventUser eventUser = new EventUser();
            eventUser.setEventId(p.getEventId());
            eventUser.setUid(p.getInvitee().getUid());
            eventUser.setDecision(p.getDecision());
            eventUser.setAvailability(Availability.MAYBE);
            eventUser.setIsOrganizer(p.isOrganizer());
            eventUser.setIsAttendee(true);
            return eventUser;
        });
        genericBeanDao.insertBeans(eventUsers, eventUsers.first().getSetFields());

        ListF<EventLayer> eventLayers = parameters.map(p -> {
            EventLayer eventLayer = new EventLayer();
            eventLayer.setEventId(p.getEventId());
            eventLayer.setLayerId(p.getInviteeLayerId());
            eventLayer.setLCreatorUid(p.getInvitee().getUid());

            eventLayer.setEventStartTs(new Instant(0));
            eventLayer.setEventEndTs(new Instant(0));
            eventLayer.setRepetitionDueTsNull();

            return eventLayer;
        });
        genericBeanDao.insertBeans(eventLayers, eventLayers.first().getSetFields());

        ListF<EventInvitation> invitations = parameters.map(p -> {
            EventInvitation invitation = new EventInvitation();
            invitation.setDecision(p.getDecision());
            invitation.setEventId(p.getEventId());
            invitation.setEmail(p.getInvitee().getEmail());
            invitation.setCreationTs(new LocalDate(2010, 9, 12).toDateTimeAtStartOfDay(DateTimeZone.UTC).toInstant());
            invitation.setCreatorUid(new PassportUid(12345L));
            invitation.setIsOrganizer(false);
            return invitation;
        });
        genericBeanDao.insertBeans(invitations, invitations.first().getSetFields());
    }

    public void addExternalUserParticipantToEvent(long eventId, Email email, Decision decision, boolean isOrganizer) {
        EventInvitation invitation = new EventInvitation();
        invitation.setDecision(decision);
        invitation.setEventId(eventId);
        invitation.setEmail(email.normalize());
        invitation.setCreationTs(new LocalDate(2010, 9, 12).toDateTimeAtStartOfDay(DateTimeZone.UTC).toInstant());
        invitation.setCreatorUid(eventDao.findEventById(eventId).getCreatorUid());
        invitation.setIsOrganizer(isOrganizer);
        invitation.setPrivateToken(Random2.R.nextAlnum(8));
        eventInvitationDao.saveEventInvitation(invitation, ActionInfo.webTest(Instant.now()));
    }

    public void addResourceParticipantToEvent(long eventId, Resource resource) {
        resourceScheduleDao.updateResourceSchedulesSetNotValidByResourceIds(Cf.list(resource.getId()));
        createEventResource(resource.getId(), eventId);
    }

    public Resource cleanAndCreateThreeLittlePigs() {
        return cleanAndCreateResource(testExchangeThreeLittlePigsEmail.getLocalPart(), "Три поросенка");
    }

    public Resource cleanAndCreateSmolny() {
        return cleanAndCreateResource(testExchangeSmolnyEmail.getLocalPart(), "Смольный");
    }

    public Resource cleanAndCreateConfRr21() {
        return cleanAndCreateResource(testExchangeConfRr21.getLocalPart(), "2_1");
    }

    public Resource cleanAndCreateResource(String exchangeName, String resourceName) {
        cleanResourceByName(exchangeName);
        return createResource(exchangeName, resourceName);
    }

    public Resource cleanAndCreateResourceWithNoSyncWithExchange(String exchangeName, String resourceName) {
        return cleanAndCreateResourceWithNoExchSync(exchangeName, resourceName, ResourceType.ROOM);
    }

    public Resource cleanAndCreateResourceWithNoExchSync(String exchangeName, String resourceName, ResourceType type) {
        Resource r = cleanAndCreateResource(exchangeName, resourceName);

        r.setType(type);
        r.setSyncWithExchange(false);
        resourceDao.updateResource(r);

        return r;
    }

    public Resource createResource(String exchangeName, String resourceName) {
        return createResource(exchangeName, resourceName, createDefaultOffice());
    }

    public Resource createResource(String exchangeName, String name, Office office) {
        return createResource(exchangeName, name, PassportAuthDomain.YANDEX_TEAM_RU.getDomain(), office);
    }

    public Resource cleanAndCreateResource(String exchangeName, String name, Office office) {
        return cleanAndCreateResource(exchangeName, name, PassportAuthDomain.YANDEX_TEAM_RU.getDomain(), office);
    }

    public Resource cleanAndCreateResource(String exchangeName, String name, InternetDomainName domain, Office office) {
        cleanResourceByDomainAndName(domain, exchangeName);
        return createResource(exchangeName, name, domain, office);
    }

    public Resource createResource(String exchangeName, String name, InternetDomainName domain, Office office) {
        Resource r = new Resource();

        r.setExchangeName(exchangeName);
        r.setDomain(domain.getDomain());

        r.setDesk(false);
        r.setLcdPanel(0);
        r.setMarkerBoard(false);
        r.setProjector(0);
        r.setVoiceConferencing(false);
        r.setIsActive(true);
        r.setSyncWithExchange(true);
        r.setOfficeId(office.getId());
        r.setName(name);
        r.setNameEn(TranslitUtils.translit(name));

        r.setId(resourceRoutines.createResource(r));
        return r;
    }

    public void updateNoSyncWithExchange(ListF<Resource> resources) {
        genericBeanDao.updateBeans(resources.map(r -> {
            Resource data = new Resource();

            data.setId(r.getId());
            data.setSyncWithExchange(false);

            return data;
        }));
    }

    private void cleanResourceByName(String exchangeName) {
        cleanResourceByDomainAndName(PassportAuthDomain.YANDEX_TEAM_RU.getDomain(), exchangeName);
    }

    private void cleanResourceByDomainAndName(InternetDomainName domain, String exchangeName) {
        ListF<Resource> rs = resourceDao.findResourcesByDomainAndExchangeNames(domain, Cf.list(exchangeName)).get2();
        rs.forEach(this::cleanResource);
    }

    private void cleanResource(Resource resource) {
        eventResourceDao.deleteEventResourceByResourceId(Cf.list(resource.getId()));
        resourceScheduleDao.deleteResourceSchedulesByResourceIds(Cf.list(resource.getId()));
        genericBeanDao.deleteBeanById(ResourceHelper.INSTANCE, resource.getId());
        eventInvitationManager.removeParticipantIdFromCacheByEmail(resourceRoutines.getExchangeEmail(resource));
        resourceReservationDao.deleteByResourceIds(Cf.list(resource.getId()));
    }

    public Office createDefaultOffice(InternetDomainName domain) {
        return createOffice(domain, "Test office");
    }

    public Office createOffice(InternetDomainName domain, String officeName) {
        Office office = new Office();
        office.setName(officeName);
        office.setNameEn(officeName);
        office.setIsActive(true);
        office.setStaffId(Random2.R.nextLong());
        office.setDomain(domain.getDomain());
        return resourceDao.saveOffice(office);
    }

    public Office createDefaultOffice() {
        return createDefaultOffice(PassportDomain.YANDEX_TEAM_RU.getDomain());
    }

    public ParticipantsData createParticipantsData(Email organizerEmail, Email attendeeEmail) {
        return ParticipantsData.merge(
                new ParticipantData(organizerEmail, organizerEmail.getEmail(), Decision.YES, true, true, false),
                Cf.list(new ParticipantData(attendeeEmail, attendeeEmail.getEmail(), Decision.YES, true, false, false)));
    }

    public static YandexUser createRandomYaTeamUser(long offset) {
        return createRandomYaTeamUserWithCustomLogin(offset, Option.empty());
    }

    public static YandexUser createRandomYaTeamUserWithCustomLogin(long offset, Option<String> loginO) {
        final long FIRST_YANDEX_TEAM = 1120000000000000L;
        PassportUid uid = PassportUid.cons(FIRST_YANDEX_TEAM + offset);
        PassportLogin login = new PassportLogin(loginO.getOrElse("fake-user-" + uid));
        return new YandexUser(
                uid,
                login,
                Option.of(login.getNormalizedValue()),
                Option.of(new Email(login.getNormalizedValue() + "@yandex-team.ru")),
                Option.of(PassportDomain.YANDEX_TEAM_RU.toString()), Option.empty(), Option.empty());
    }

    public static YandexUser createSsytnik() {
        return new YandexUser(
                TestUsers.SSYTNIK,
                new PassportLogin("ssytnik"),
                Option.of("Sergey Sytnik"),
                Option.of(new Email("ssytnik@yandex-team.ru")),
                Option.of(PassportDomain.YANDEX_TEAM_RU.toString()), Option.empty(), Option.empty());
    }

    public static YandexUser createAkirakozov() {
        return new YandexUser(
                TestUsers.AKIRAKOZOV,
                new PassportLogin("akirakozov"),
                Option.of("Alexander Kirakozov"),
                Option.of(new Email("akirakozov@yandex-team.ru")),
                Option.of(PassportDomain.YANDEX_TEAM_RU.toString()), Option.empty(), Option.empty());
    }

    public static YandexUser createDbrylev() {
        return new YandexUser(
                TestUsers.DBRYLEV,
                new PassportLogin("dbrylev"),
                Option.of("Danil Brylev"),
                Option.of(new Email("dbrylev@yandex-team.ru")),
                Option.of(PassportDomain.YANDEX_TEAM_RU.toString()), Option.empty(), Option.empty());
    }

    public static YandexUser createAkirakozovWithLdEmail() {
        return new YandexUser(
                TestUsers.AKIRAKOZOV,
                new PassportLogin("akirakozov"),
                Option.of("Alexander Kirakozov"),
                Option.of(new Email("akirakozov@ld.yandex.ru")),
                Option.of(PassportDomain.YANDEX_TEAM_RU.toString()), Option.empty(), Option.empty());
    }

    public static YandexUser createHelga() {
        return new YandexUser(
                TestUsers.HELGA,
                new PassportLogin("helga"),
                Option.of("Olga Kuritsina"),
                Option.of(new Email("helga@yandex-team.ru")),
                Option.of(PassportDomain.YANDEX_TEAM_RU.toString()), Option.empty(), Option.empty());
    }

    public static YandexUser createYashunsky() {
        return new YandexUser(
                TestUsers.YASHUNSKY,
                new PassportLogin("yashunsky"),
                Option.of("Vladimir Yashunskiy"),
                Option.of(new Email("yashunsky@yandex-team.ru")),
                Option.of(PassportDomain.YANDEX_TEAM_RU.toString()), Option.empty(), Option.empty());
    }

    public static YandexUser createTestUser(int id) {
        return new YandexUser(
                new PassportUid(1130000010000000L + id),
                new PassportLogin("test_user" + id),
                Option.of("Test User " + id),
                Option.of(new Email("test_user" + id + "@yandex.ru")),
                Option.of(PassportDomain.YANDEX_RU.toString()), Option.empty(), Option.empty());
    }


    public static YandexUser createResourceMaster() {
        return new YandexUser(
            ROBOT_RES_MASTER_UID,
            new PassportLogin(ROBOT_RES_MASTER_EMAIL.getLocalPart()),
            Option.of("Робот переговорник"),
            Option.of(ROBOT_RES_MASTER_EMAIL),
            Option.of(PassportDomain.YANDEX_TEAM_RU.toString()), Option.empty(), Option.empty());
    }

    public ListF<Notification> sms25MinutesBefore() {
        return Cf.list(Notification.sms(Duration.standardMinutes(-25)));
    }

    public long createDailyRepetition() {
        return eventDao.saveRepetition(createDailyRepetitionTemplate());
    }

    public static Repetition createDailyRepetitionTemplate() {
        Repetition repetition = new Repetition();
        repetition.setREach(1);
        repetition.setType(RegularRepetitionRule.DAILY);
        repetition.setDueTsNull();
        repetition.setRWeeklyDaysNull();
        repetition.setRMonthlyLastweekNull();
        return repetition;
    }

    public static Repetition createWeeklyRepetition(String day) {
        Repetition repetition = new Repetition();
        repetition.setREach(1);
        repetition.setType(RegularRepetitionRule.WEEKLY);
        repetition.setDueTsNull();
        repetition.setRWeeklyDays(day);
        repetition.setRMonthlyLastweekNull();
        return repetition;
    }

    private void createEventUserWithDefaultEmailNotification(PassportUid uid, long eventId) {
        EventUser eventUser = new EventUser();
        eventUser.setEventId(eventId);
        eventUser.setUid(uid);
        // XXX fill ActionInfo data
        long eventUserId = genericBeanDao.insertBeanGetGeneratedKey(eventUser);
        notificationDbManager.saveEventNotifications(eventUserId, notificationRoutines.getDefaultNotifications());
    }

    public void updateEventTimezone(long eventId, DateTimeZone tz) {
        MainEvent mainEvent = mainEventDao.findMainEventByEventId(eventId);

        MainEvent update = new MainEvent();
        update.setId(mainEvent.getId());
        update.setTimezoneId(tz.getID());

        genericBeanDao.updateBean(update);
    }

    public void updateIsEwser(TestUserInfo... users) {
        genericBeanDao.updateBeans(Cf.x(users).map(u -> {
            SettingsYt data = new SettingsYt();
            data.setUid(u.getUid());
            data.setIsEwser(true);

            settingsRoutines.invalidateCacheForUid(u.getUid());

            return data;
        }));
    }

    public boolean isEwser(PassportUid uid) {
        return settingsRoutines.getIsEwser(uid);
    }

    public void openEventAndLayer(long eventId, long layerId) {
        Event event = new Event();
        event.setId(eventId);
        event.setPermAll(EventActionClass.VIEW);
        eventDao.updateEvent(event);
    }

    public static Event createRecurrence(Event event, Duration offset, String name) {
        Instant recurrenceStart = event.getStartTs().plus(offset);
        Event tomorrowRecurrence = event.copy();
        tomorrowRecurrence.unsetField(EventFields.ID);
        tomorrowRecurrence.unsetField(EventFields.REPETITION_ID);
        tomorrowRecurrence.setRecurrenceId(recurrenceStart);
        tomorrowRecurrence.setStartTs(recurrenceStart);
        tomorrowRecurrence.setEndTs(recurrenceStart.plus(Duration.standardHours(1)));
        tomorrowRecurrence.setName(name);
        return tomorrowRecurrence;
    }

    public long createDefaultLayerForUser(PassportUid uid, Instant now) {
        Layer layer = new Layer();
        layer.setName("layer");
        layer.setCreatorUid(uid);
        layer.setType(LayerType.USER);
        layer.setCreationTs(now);
        layer.setLastUpdateTs(now);
        long layerId = layerDao.saveLayer(layer);

        LayerUser layerUser = new LayerUser();
        layerUser.setUid(uid);
        layerUser.setLayerId(layerId);
        layerUser.setFieldDefaults(layerRoutines.createDefaultLayerUserOverrides(uid.getDomain()));
        layerUser.setPerm(LayerActionClass.ADMIN);
        layerUserDao.saveLayerUser(layerUser);

        settingsRoutines.updateDefaultLayer(uid, layerId);

        return layerId;
    }

    public Tuple2List<Layer, LayerUser> createDefaultLayersForUsers(ListF<PassportUid> uids, Instant now) {
        return createLayers(uids, now, LayerType.USER);
    }

    public void updateEventTimeIndents(Event... events) {
        updateEventTimeIndents(Cf.x(events).mapToLongArray(Event::getId));
    }

    public void updateEventTimeIndents(long... eventIds) {
        Function<String, String> qF = table -> "UPDATE " + table + " SET"
                + " event_start_ts = e.start_ts,"
                + " event_end_ts = e.end_ts,"
                + " repetition_due_ts = CASE WHEN r.id IS NOT NULL THEN COALESCE(r.due_ts, '2100-01-01') END,"
                + " rdates_min_ts = ("
                + "    SELECT MIN(rd.start_ts) FROM rdate rd WHERE is_rdate AND e.id = rd.event_id),"
                + " rdates_max_ts = ("
                + "    SELECT MAX(COALESCE(rd.end_ts, rd.start_ts + (e.end_ts - e.start_ts)))"
                + "    FROM rdate rd WHERE is_rdate AND e.id = rd.event_id)"
                + " FROM event e LEFT JOIN repetition r ON r.id = e.repetition_id"
                + " WHERE event_id = e.id AND e.id " + SqlQueryUtils.inSet(Cf.longList(eventIds));

        genericBeanDao.getJdbcTemplate().update(qF.apply("event_layer"));
        genericBeanDao.getJdbcTemplate().update(qF.apply("event_resource"));
    }

    public Instant getRecurrenceIdInFuture(Event event, Instant now) {
        RepetitionInstanceInfo repetitionInstanceInfo = repetitionRoutines.getRepetitionInstanceInfoByEvent(event);
        return RepetitionUtils.getInstanceIntervalStartingAfter(repetitionInstanceInfo, now).get().getStart();
    }

    public void runWithFileResourceJetty(Function1V<Integer> function) {
        SingleWarJetty jetty = startFileResourceJetty();
        try {
            function.apply(jetty.getActualHttpPort());
        } finally {
            jetty.stop();
        }
    }

    public SingleWarJetty startFileResourceJetty() {
        SingleWarJetty jetty = new SingleWarJetty();

        jetty.addServletMapping("/*", new ResourceServletSupport() {
            protected ResourceInfo getResource(HttpServletRequestX req) {
                String pathInfo = StringUtils.substringAfter(req.getPathInfoO().getOrElse(""), "/");
                return new ResourceInfo(new File2(pathInfo), pathInfo);
            }
        });
        jetty.setLookupServletsInContext(false);
        jetty.start();

        return jetty;
    }

    public <T> void setValue(DynamicProperty<T> property, T value) {
        dynamicPropertyManager.setValue(property, value);

        MethodX.getSingleMethod(DynamicProperty.class, "set")
                .<MethodX>setAccessibleTrueReturnThis().invoke(property, value);
    }
}
