package ru.yandex.calendar.logic.event.avail.absence;

import lombok.val;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.frontend.ews.exp.EwsExportRoutines;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventFields;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.MainEvent;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.EventChangesInfo;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.sharing.perm.Authorizer;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.logic.user.Staff;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;

public class AbsenceUpdaterTest extends AbstractConfTest {
    @Autowired
    private AbsenceUpdater absenceUpdater;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private Authorizer authorizer;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private Staff staff;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private MainEventDao mainEventDao;

    private static final Absence ABSENCE_1 = new Absence(13, new PassportLogin("akirakozov"),
            new LocalDate(2010, 11, 8), new LocalDate(2010, 11, 21), AbsenceType.ABSENCE, "Всячески доступен", true);

    private static final Absence ABSENCE_2 = new Absence(17, new PassportLogin("akirakozov"),
            new LocalDate(2011, 11, 11), new LocalDate(2011, 11, 27), AbsenceType.CONFERENCE_TRIP, "Всячески доступен", false);

    private static final Absence ABSENCE_3 = new Absence(21, new PassportLogin("akirakozov"),
            new LocalDate(2021, 8, 13), new LocalDate(2021, 8, 13), AbsenceType.WORK_FROM_HOME, "Работаю из дома", false);

    @Test
    public void createAndCheckPerms() {
        val akirakozov = testManager.prepareYandexUser(TestManager.createAkirakozov());
        val ak = akirakozov.getUserInfo();

        absenceUpdater.updateFromTime(Cf.list(ABSENCE_1), MoscowTime.now().toInstant());

        val event = eventDao.findAbsenceEventsByUserId(akirakozov.getUid()).single();

        val eventWithRelations = eventDbManager.getEventWithRelationsById(event.getId());
        val eventAuthInfo = authorizer.loadEventInfoForPermsCheck(ak, eventWithRelations);

        assertThat(authorizer.canEditEvent(ak, eventAuthInfo, ActionSource.WEB)).isFalse();
        assertThat(authorizer.canDeleteEvent(ak, eventAuthInfo, ActionSource.WEB)).isFalse();
        assertThat(authorizer.canEditEventPermissions(ak, eventAuthInfo, ActionSource.WEB)).isFalse();
        assertThat(authorizer.canInviteToEvent(ak, eventAuthInfo, ActionSource.WEB)).isFalse();
        assertThat(authorizer.canSplitEvent(ak, eventAuthInfo, ActionSource.WEB)).isFalse();

        assertThat(authorizer.canViewEvent(ak, eventAuthInfo, ActionSource.WEB)).isTrue();
    }

    @Test
    public void createFromXml() {
        val akirakozov = testManager.prepareYandexUser(TestManager.createAkirakozov());

        absenceUpdater.updateFromTime(Cf.list(ABSENCE_1), Instant.now());

        val event = eventDao.findAbsenceEventsByUserId(akirakozov.getUid()).single();

        assertThat(event.getStartTs()).isEqualTo(ABSENCE_1.getStart(MoscowTime.TZ));
        assertThat(event.getEndTs()).isEqualTo(ABSENCE_1.getEnd(MoscowTime.TZ));
        assertThat(event.getName()).isEqualTo("Отсутствие");
        assertThat(event.getDescription()).isEqualTo("Всячески доступен");
    }

    @Test
    public void createAndCheckUserIsBusy() {
        val akirakozov = testManager.prepareYandexUser(TestManager.createAkirakozov());

        absenceUpdater.updateFromTime(Cf.list(ABSENCE_1, ABSENCE_2, ABSENCE_3), Instant.now());

        val events = eventDao.findAbsenceEventsByUserId(akirakozov.getUid());
        val eventUsers = eventUserDao.findEventUsersByEventIds(events.map(Event.getIdF()));

        assertThat(eventUsers)
            .hasSameSizeAs(events);
        assertThat(eventUsers.map(EventUser::getAvailability))
            .containsExactlyInAnyOrder(Availability.BUSY, Availability.AVAILABLE, Availability.AVAILABLE);
    }

    @Test
    public void remove() {
        val akirakozov = testManager.prepareYandexUser(TestManager.createAkirakozov());
        val updateTime = MoscowTime.dateTime(2010, 1, 1, 0, 0).toInstant();

        val absence1 = new Absence(
                35, akirakozov.getLogin(),
                new LocalDate(2015, 11, 10), new LocalDate(2015, 11, 17),
                AbsenceType.ILLNESS, "Лечусь дома", false);

        absenceUpdater.updateFromTime(Cf.list(absence1), updateTime);
        assertThat(eventDao.findAbsenceEventsByUserId(akirakozov.getUid())).hasSize(1);

        absenceUpdater.updateFromTime(Cf.list(), updateTime);
        assertThat(eventDao.findAbsenceEventsByUserId(akirakozov.getUid())).isEmpty();
    }

    @Test
    public void update() {
        val user = testManager.prepareYandexUser(TestManager.createSsytnik());
        val updateTime = MoscowTime.dateTime(2011, 11, 11, 0, 0).toInstant();

        Absence absence = new Absence(
                127, user.getLogin(),
                new LocalDate(2011, 11, 11), new LocalDate(2011, 11, 12),
                AbsenceType.ABSENCE, "Меня не будет завтра", false);

        absenceUpdater.updateFromTime(Cf.list(absence), updateTime);
        val eventId = eventDao.findAbsenceEventsByUserId(user.getUid()).single().getId();

        absence = new Absence(
                127, user.getLogin(),
                new LocalDate(2011, 11, 12), new LocalDate(2011, 11, 13),
                AbsenceType.ABSENCE, "Передумал, меня не будет послезавтра", false);

        absenceUpdater.updateFromTime(Cf.list(absence), updateTime);

        val events = eventDao.findAbsenceEventsByUserId(user.getUid());
        assertThat(events).hasSize(1);
        assertThat(events.single().getId()).isEqualTo(eventId);
        assertThat(events.single().getDescription()).isEqualTo(absence.getComment());
        assertThat(events.single().getEndTs()).isEqualTo(absence.getEnd(MoscowTime.TZ));
        assertThat(events.single().getStartTs()).isEqualTo(absence.getStart(MoscowTime.TZ));
    }

    @Test
    public void updateAndRemove() {
        val akirakozov = testManager.prepareYandexUser(TestManager.createAkirakozov());

        val updateTime = MoscowTime.dateTime(2010, 1, 1, 0, 0).toInstant();

        val absence1 = new Absence(
                11, akirakozov.getLogin(),
                new LocalDate(2015, 11, 10), new LocalDate(2015, 11, 17),
                AbsenceType.ILLNESS, "Лечусь дома", false);
        val absence2 = new Absence(
                22, akirakozov.getLogin(),
                new LocalDate(2015, 10, 10), new LocalDate(2015, 12, 17),
                AbsenceType.ABSENCE, "Командировка", true);
        val absence3 = new Absence(
                33, akirakozov.getLogin(),
                new LocalDate(2015, 12, 1), new LocalDate(2015, 12, 8),
                AbsenceType.VACATION, "Таити", false);

        absenceUpdater.updateFromTime(Cf.list(absence1, absence2, absence3), updateTime);
        assertThat(eventDao.findAbsenceEventsByUserId(akirakozov.getUid())).hasSize(3);

        val absence2Updated = new Absence(
                22, akirakozov.getLogin(),
                new LocalDate(2015, 10, 10), new LocalDate(2015, 12, 17),
                AbsenceType.ABSENCE, "Командировка в Киев", true);

        absenceUpdater.updateFromTime(Cf.list(absence2Updated, absence3), updateTime);

        val events = eventDao.findAbsenceEventsByUserId(akirakozov.getUid());
        assertThat(events).hasSize(2);
        for (Event event : events) {
            if (event.getName().equals(absence2Updated.getEventName(Language.RUSSIAN))) {
                assertThat(event.getDescription()).isEqualTo(absence2Updated.getComment());
            }
        }
    }

    @Test
    public void createAbsencesWithSameTimeForTwoDifferentUsers() {
        val user1 = testManager.prepareRandomYaTeamUser(7);
        val user2 = testManager.prepareRandomYaTeamUser(8);

        val updateTime = MoscowTime.dateTime(2010, 1, 1, 0, 0).toInstant();

        val absence1 = new Absence(
                11, user1.getLogin(),
                new LocalDate(2010, 11, 10), new LocalDate(2010, 11, 17),
                AbsenceType.ABSENCE, "Комадировка", true);

        val absence2 = new Absence(
                22, user2.getLogin(),
                new LocalDate(2010, 11, 10), new LocalDate(2010, 11, 17),
                AbsenceType.ABSENCE, "Комадировка", true);

        absenceUpdater.updateFromTime(Cf.list(absence1, absence2), updateTime);

        Event event1 = eventDao.findAbsenceEventsByUserId(user1.getUid()).single();
        Event event2 = eventDao.findAbsenceEventsByUserId(user2.getUid()).single();
        assertThat(event2.getId()).isNotEqualTo(event1.getId());
    }

    @Test
    public void thatAbsenceHistoryIsSaved() {
        val user1 = testManager.prepareRandomYaTeamUser(9);

        val updateTime1 = MoscowTime.dateTime(2010, 1, 1, 0, 0).toInstant();

        val absenceOld = new Absence(
                -35, user1.getLogin(),
                new LocalDate(2010, 3, 12), new LocalDate(2010, 3, 12),
                AbsenceType.ABSENCE, "Не будет меня", false);

        absenceUpdater.updateFromTime(Cf.list(absenceOld), updateTime1);

        val updateTime2 = MoscowTime.dateTime(2015, 1, 1, 0, 0).toInstant();

        val absence = new Absence(
                47, user1.getLogin(),
                new LocalDate(2015, 1, 10), new LocalDate(2015, 1, 17),
                AbsenceType.ABSENCE, "По личным обстоятельствам", false);

        absenceUpdater.updateFromTime(Cf.list(absence), updateTime2);

        assertThat(eventDao.findAbsenceEventsByUserId(user1.getUid())).hasSize(2);
    }

    @Test
    public void removeOnTimeBound() {
        val user1 = testManager.prepareRandomYaTeamUser(10);

        val updateTime = MoscowTime.dateTime(2010, 11, 12, 0, 0).toInstant();

        val absence = new Absence(
                13, user1.getLogin(),
                new LocalDate(2010, 10, 12), new LocalDate(2010, 11, 12),
                AbsenceType.ABSENCE, "Не будет меня", false);

        absenceUpdater.updateFromTime(Cf.list(absence), updateTime);
        assertThat(eventDao.findAbsenceEventsByUserId(user1.getUid())).hasSize(1);

        absenceUpdater.updateFromTime(Cf.list(), updateTime);
        assertThat(eventDao.findAbsenceEventsByUserId(user1.getUid())).isEmpty();
    }

    @Test
    public void updateOnTimeBound() {
        val user1 = testManager.prepareRandomYaTeamUser(10);

        val updateTime = MoscowTime.dateTime(2010, 11, 12, 0, 0).toInstant();

        val absence = new Absence(
                13, user1.getLogin(),
                new LocalDate(2010, 10, 12), new LocalDate(2010, 11, 12),
                AbsenceType.ABSENCE, "Не будет меня", false);

        absenceUpdater.updateFromTime(Cf.list(absence), updateTime);
        assertThat(eventDao.findAbsenceEventsByUserId(user1.getUid())).hasSize(1);

        val absence2 = new Absence(
                13, user1.getLogin(),
                new LocalDate(2010, 10, 12), new LocalDate(2010, 11, 12),
                AbsenceType.ABSENCE, "Не будет меня до 16-00", false);

        absenceUpdater.updateFromTime(Cf.list(absence2), updateTime);
        val event = eventDao.findAbsenceEventsByUserId(user1.getUid()).single();
        assertThat(event.getDescription()).isEqualTo(absence2.getComment());
    }

    @Test
    public void historySaveOnTimeBound() {
        val user1 = testManager.prepareRandomYaTeamUser(10);

        val updateTime = MoscowTime.dateTime(2010, 10, 12, 0, 0).toInstant();

        val absence = new Absence(
                13, user1.getLogin(),
                new LocalDate(2010, 10, 11), new LocalDate(2010, 10, 12),
                AbsenceType.ABSENCE, "Не будет меня", false);

        absenceUpdater.updateFromTime(Cf.list(absence), updateTime);
        assertThat(eventDao.findAbsenceEventsByUserId(user1.getUid())).hasSize(1);

        val updateTime2 = MoscowTime.dateTime(2010, 10, 13, 0, 0).toInstant();

        absenceUpdater.updateFromTime(Cf.list(), updateTime2);
        assertThat(eventDao.findAbsenceEventsByUserId(user1.getUid())).hasSize(1);
    }

    // smoke test, just check that staff returns valid xml
    @Test
    public void parseXmlFromStaff() {
        staff.getAbsences(Instant.now(), Instant.now().plus(Duration.standardDays(1)));
    }

    @Test
    public void updateEwsAbsence() {
        val user = testManager.prepareYandexUser(TestManager.createSsytnik());
        val updateTime = MoscowTime.dateTime(2011, 11, 11, 0, 0).toInstant();

        Absence absence = new Absence(
                127, user.getLogin(),
                new LocalDate(2011, 11, 11), new LocalDate(2011, 11, 12),
                AbsenceType.ABSENCE, "Меня не будет завтра", false);

        absenceUpdater.updateFromTime(Cf.list(absence), updateTime);
        val eventId = eventDao.findAbsenceEventsByUserId(user.getUid()).single().getId();

        MainEvent patchedMainEvent = mainEventDao.findMainEventByEventId(eventId).copy();
        patchedMainEvent.setIsExportedWithEws(true);
        mainEventDao.updateMainEvent(patchedMainEvent);

        absence = new Absence(
                127, user.getLogin(),
                new LocalDate(2011, 11, 12), new LocalDate(2011, 11, 13),
                AbsenceType.ABSENCE, "Передумал, меня не будет послезавтра", false);

        val mockEwsExportRoutines = Mockito.mock(EwsExportRoutines.class);

        val changesCaptor = ArgumentCaptor.forClass(EventChangesInfo.class);

        Mockito.doNothing().when(mockEwsExportRoutines)
                .exportToExchangeIfNeededOnUpdate(
                        Mockito.anyLong(), changesCaptor.capture(), Mockito.any(), Mockito.any());

        try {
            absenceUpdater.setEwsExportRoutinesForTest(mockEwsExportRoutines);
            absenceUpdater.updateFromTime(Cf.list(absence), updateTime);
        } finally {
            absenceUpdater.setEwsExportRoutinesForTest(ewsExportRoutines);
        }

        val changes = changesCaptor.getValue().getEventChanges();
        assertThat(changes.getSetFields().unique()).isEqualTo(Cf.set(EventFields.DESCRIPTION, EventFields.START_TS, EventFields.END_TS));

        assertThat(changes.getDescription()).isEqualTo(absence.getComment());
        assertThat(changes.getEndTs()).isEqualTo(absence.getEnd(MoscowTime.TZ));
        assertThat(changes.getStartTs()).isEqualTo(absence.getStart(MoscowTime.TZ));
    }
}
