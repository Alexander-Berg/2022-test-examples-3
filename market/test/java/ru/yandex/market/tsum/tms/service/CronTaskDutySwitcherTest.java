package ru.yandex.market.tsum.tms.service;

import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.abc.models.AbcPerson;
import ru.yandex.market.tsum.clients.abc.models.AbcShift;
import ru.yandex.market.tsum.clients.calendar.CalendarEvent;
import ru.yandex.market.tsum.clients.notifications.email.EmailNotification;
import ru.yandex.market.tsum.clients.pollers.Poller;
import ru.yandex.market.tsum.clients.staff.StaffGroup;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.core.duty.Duty;
import ru.yandex.market.tsum.core.duty.DutyLoginExtractors;
import ru.yandex.market.tsum.core.duty.DutyManager;
import ru.yandex.market.tsum.core.duty.exceptions.UpdatingJugglerNotificationRuleException;
import ru.yandex.market.tsum.tms.service.email.SwitchDutyErrorEmailNotification;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class CronTaskDutySwitcherTest {


    private static final String TEST_DEPARTMENT = "department";
    private static final String TEST_DUTY_TYPE = "incident";
    private static final String TEST_STAFF_GROUP = "staffGroup";
    private static final int TEST_CALENDAR_LAYER_ID = 1;
    private static final int TEST_DUTY_GROUP_PHONE = 42;
    private static final String TEST_JUGGLER_NOTIFICATION_RULE = "someJugglerRule";
    private static final String LOGIN_FROM_CALENDAR = "LOGIN_FROM_CALENDAR";
    private static final String LOGIN_FROM_ABC = "ABC_LOGIN";
    private static final String TEST_DUTY_CALENDAR_LINK = "http://testCalendarLayerLink";
    private final Duty expectedCurrentDuty = createDuty();
    private final Duty currentDutyWithEmptyStaffGroup = createDutyWithEmptyStaffGroup();
    private final StaffGroup expectedStaffGroup = createStaffGroup();
    private final List<CalendarEvent> calendarEvents = createCalendarEvents(LOGIN_FROM_CALENDAR);
    private final StaffPerson staffPerson = createStaffPerson();
    private final AbcShift abcShift = createAbcShift();
    private final Date expectedDate = new Date();
    private final DutyManager dutyManager = Mockito.mock(DutyManager.class);
    private final Poller.Sleeper sleeper = Mockito.mock(Poller.Sleeper.class);
    private final CronTaskDutySwitcher cronTaskDutySwitcher = new CronTaskDutySwitcher(dutyManager, sleeper);
    private String notificationErrorMessage;

    @Test
    public void verifyResolveDutyLoginStateTest() {
        setupMock(
            expectedCurrentDuty,
            expectedStaffGroup,
            calendarEvents,
            staffPerson,
            LOGIN_FROM_CALENDAR
        );

        ResolveDutyLoginState resolveDutyLoginState = getResolveDutyLoginState();
        verifySwitchDutyProcessState(expectedCurrentDuty, expectedStaffGroup, resolveDutyLoginState);
    }

    @Test
    public void verifyResolveDutyLoginStateTest_EmptyStaffGroup() {
        setupMock(
            currentDutyWithEmptyStaffGroup,
            expectedStaffGroup,
            calendarEvents,
            staffPerson,
            LOGIN_FROM_CALENDAR
        );

        ResolveDutyLoginState resolveDutyLoginState = getResolveDutyLoginState();
        assertEquals(currentDutyWithEmptyStaffGroup, resolveDutyLoginState.getCurrentDuty());
        assertNotNull(resolveDutyLoginState.getLinkToCalendar());
        assertFalse(resolveDutyLoginState.getCalendarEvents().isEmpty());
        assertTrue(resolveDutyLoginState.getNextDutyPersonLoginToSwitch().isPresent());
    }

    @Test
    public void checkResolveDutyLogin_SuccessfulIfNextDutyLoginFound() {
        setupMock(
            expectedCurrentDuty,
            expectedStaffGroup,
            calendarEvents,
            staffPerson,
            LOGIN_FROM_CALENDAR
        );

        ResolveDutyLoginResult result = getResolveDutyLogin();
        assertTrue(result.isSuccessful());
        assertEquals(LOGIN_FROM_CALENDAR, result.getNextDutyLogin());
    }

    @Test
    public void checkResolveDutyLogin_SuccessfulAbc() {
        setupMockAbc(
            createDutyAbc(),
            abcShift,
            expectedStaffGroup,
            createStaffPerson(LOGIN_FROM_ABC),
            LOGIN_FROM_ABC
        );

        ResolveDutyLoginResult result = getResolveDutyLogin();
        assertTrue(result.isSuccessful());
        assertEquals("ABC_LOGIN", result.getNextDutyLogin());
    }

    @Test
    public void checkResolveDutyLogin_failedIfNextDutyNotFound() {
        setupMock(
            expectedCurrentDuty,
            expectedStaffGroup,
            calendarEvents,
            staffPerson,
            null
        );
        ResolveDutyLoginResult result = getResolveDutyLogin();
        assertFalse(result.isSuccessful());
        assertNull(result.getNextDutyLogin());
    }

    @Test
    public void checkSwitchDutyProcessState() {
        setupMock(
            expectedCurrentDuty,
            expectedStaffGroup,
            calendarEvents,
            staffPerson,
            LOGIN_FROM_CALENDAR
        );
        ResolveDutyLoginResult resolveDutyLogin = getResolveDutyLogin();
        MonitoringSystemUpdateResult switchDutyResult = cronTaskDutySwitcher.getSwitchDutyResult(resolveDutyLogin);
        assertTrue(switchDutyResult.isSuccessful());
        verifyContext(expectedCurrentDuty, expectedStaffGroup, switchDutyResult.getStaffGroupNotificationContext());
    }

    @Test
    public void verifyErrorNotification_whenCalendarLoginIsInvalid() {
        notificationErrorMessage = "Календарь пуст";
        setupMock(
            expectedCurrentDuty,
            expectedStaffGroup,
            Collections.emptyList(),
            staffPerson,
            null
        );
        verifyNotificationOnFail(getSwitchDutyResult());
    }

    @Test
    public void verifyErrorNotification_WhenDutyNotFoundInStaff() {
        notificationErrorMessage = "Дежурный не найден в Стаффе";
        setupMock(
            expectedCurrentDuty,
            expectedStaffGroup,
            calendarEvents,
            null,
            null
        );
        verifyNotificationOnFail(getSwitchDutyResult());
    }

    @Test
    public void verifyErrorNotification_WhenLoginIsInvalid() {
        notificationErrorMessage = "Логин дежурного в календаре заполнен некорректно";
        setupMock(
            expectedCurrentDuty,
            expectedStaffGroup,
            calendarEvents,
            staffPerson,
            null
        );
        verifyNotificationOnFail(getSwitchDutyResult());
    }

    @Test
    public void verifyErrorNotification_WhenErrorInJugglerOccurred() {
        String jugglerError = "Juggler error";
        Mockito.doThrow(new UpdatingJugglerNotificationRuleException(jugglerError, null)).when(dutyManager)
            .updateLoginInJugglerNotificationRule(expectedCurrentDuty, LOGIN_FROM_CALENDAR);

        notificationErrorMessage = "Ошибка при переключении дежурного в Juggler: " + jugglerError;
        setupMock(
            expectedCurrentDuty,
            expectedStaffGroup,
            calendarEvents,
            staffPerson,
            LOGIN_FROM_CALENDAR
        );

        verifyNotificationOnFail(getSwitchDutyResult());
    }

    @Test
    public void verifyErrorNotification_TelegraphErrorOccurred() {
        String telegraphError = "Telegraph error";
        Mockito.doThrow(new IllegalArgumentException(telegraphError, null)).when(dutyManager)
            .updatePhoneInTelegraph(expectedCurrentDuty, LOGIN_FROM_CALENDAR);

        notificationErrorMessage = "Ошибка при переключении дежурного в Telegraph: " + telegraphError;
        setupMock(
            expectedCurrentDuty,
            expectedStaffGroup,
            calendarEvents,
            staffPerson,
            LOGIN_FROM_CALENDAR
        );

        SwitchDutyResult switchDutyResult = cronTaskDutySwitcher.switchDuty(TEST_DEPARTMENT, TEST_DUTY_TYPE);
        verifyNotificationOnFail(switchDutyResult);
    }


    void verifyNotificationOnFail(SwitchDutyResult result) {
        assertFalse(result.isSuccessful());
        StaffGroupNotificationContext errorNotificationContext = result.getStaffGroupNotificationContext();
        SwitchDutyErrorEmailNotification emailNotification =
            SwitchDutyErrorEmailNotification.fromContext(errorNotificationContext);
        verifyNotification(emailNotification);
    }

    ResolveDutyLoginResult getResolveDutyLogin() {
        return cronTaskDutySwitcher.getResolveDutyLoginResult(TEST_DEPARTMENT, TEST_DUTY_TYPE);
    }

    SwitchDutyResult getSwitchDutyResult() {
        return cronTaskDutySwitcher.switchDuty(TEST_DEPARTMENT, TEST_DUTY_TYPE);
    }

    ResolveDutyLoginState getResolveDutyLoginState() {
        return cronTaskDutySwitcher.getResolveDutyLoginState(
            TEST_DEPARTMENT,
            TEST_DUTY_TYPE
        );
    }

    void setupMock(Duty expectedCurrentDuty,
                   StaffGroup expectedStaffGroup,
                   List<CalendarEvent> calendarEvents,
                   StaffPerson staffPerson,
                   String nextDutyLogin) {
        Mockito.when(dutyManager.getCurrentDuty(TEST_DEPARTMENT, TEST_DUTY_TYPE))
            .thenReturn(expectedCurrentDuty);
        Mockito.when(dutyManager.getStaffGroup(TEST_STAFF_GROUP))
            .thenReturn(expectedStaffGroup);
        Mockito.when(dutyManager.getDutyCalendarLink(expectedCurrentDuty))
            .thenReturn(TEST_DUTY_CALENDAR_LINK);
        Mockito.when(dutyManager.getCurrentCalendarEvents(expectedCurrentDuty))
            .thenReturn(calendarEvents);
        Mockito.when(dutyManager.getNextDutyLoginFromCalendarEvents(expectedCurrentDuty, calendarEvents))
            .thenReturn(nextDutyLogin);
        Mockito.when(dutyManager.getDutyPersonFromStaff(nextDutyLogin))
            .thenReturn(Optional.ofNullable(staffPerson));
        Mockito.when(dutyManager.getStaffPeople(expectedStaffGroup))
            .thenReturn(Collections.singletonList(staffPerson));
    }
    void setupMockAbc(Duty expectedCurrentDuty,
                   AbcShift abcShift,
                   StaffGroup expectedStaffGroup,
                   StaffPerson staffPerson,
                   String nextDutyLogin) {
        Mockito.reset(dutyManager);
        Mockito.when(dutyManager.getCurrentDuty(TEST_DEPARTMENT, TEST_DUTY_TYPE))
            .thenReturn(expectedCurrentDuty);
        Mockito.when(dutyManager.getStaffGroup(TEST_STAFF_GROUP))
            .thenReturn(expectedStaffGroup);
        Mockito.when(dutyManager.getNextAbcShift(expectedCurrentDuty))
            .thenReturn(abcShift);
        Mockito.when(dutyManager.getDutyPersonFromStaff(nextDutyLogin))
            .thenReturn(Optional.ofNullable(staffPerson));
        Mockito.when(dutyManager.getStaffPeople(expectedStaffGroup))
            .thenReturn(Collections.singletonList(staffPerson));
    }


    private void verifySwitchDutyProcessState(Duty expectedCurrentDuty,
                                              StaffGroup expectedStaffGroup,
                                              ResolveDutyLoginState resolveDutyLoginState) {
        verifyContext(expectedCurrentDuty, expectedStaffGroup, resolveDutyLoginState);
        assertFalse(resolveDutyLoginState.getCalendarEvents().isEmpty());
        assertTrue(resolveDutyLoginState.getNextDutyPersonLoginToSwitch().isPresent());
    }

    private void verifyContext(Duty expectedCurrentDuty,
                               StaffGroup expectedStaffGroup,
                               StaffGroupNotificationContext context) {
        assertNotNull(context);
        assertEquals(expectedCurrentDuty, context.getCurrentDuty());
        assertEquals(expectedStaffGroup.getName(), context.getStaffGroupName());
        assertNotNull(context.getLinkToCalendar());
        assertNotNull(context.getStaffPerson());
        assertFalse(context.getStaffPeople().isEmpty());
    }

    private StaffGroup createStaffGroup() {
        return new StaffGroup(1, TEST_STAFF_GROUP, TEST_STAFF_GROUP);
    }

    private Duty createDuty() {
        return Duty.newBuilder()
            .withCalendarLayerId(TEST_CALENDAR_LAYER_ID)
            .withDutyGroupPhone(TEST_DUTY_GROUP_PHONE)
            .withJugglerNotificationRuleId(TEST_JUGGLER_NOTIFICATION_RULE)
            .withStaffGroupName(TEST_STAFF_GROUP)
            .withLoginExtractor(DutyLoginExtractors.defaultExtractor())
            .build();
    }

    private Duty createDutyWithEmptyStaffGroup() {
        return Duty.newBuilder()
            .withCalendarLayerId(TEST_CALENDAR_LAYER_ID)
            .withDutyGroupPhone(TEST_DUTY_GROUP_PHONE)
            .withJugglerNotificationRuleId(TEST_JUGGLER_NOTIFICATION_RULE)
            .withStaffGroupName("")
            .withLoginExtractor(DutyLoginExtractors.defaultExtractor())
            .build();
    }

    private Duty createDutyAbc() {
        return Duty.newBuilder()
            .withDutyGroupPhone(TEST_DUTY_GROUP_PHONE)
            .withJugglerNotificationRuleId(TEST_JUGGLER_NOTIFICATION_RULE)
            .withStaffGroupName(TEST_STAFF_GROUP)
            .withAbcDutySchedule("@svc_service:schedule")
            .withLoginExtractor(DutyLoginExtractors.defaultExtractor())
            .build();
    }

    private StaffPerson createStaffPerson(String login) {
        return new StaffPerson(
            login,
            0,
            null,
            null,
            null,
            null
        );
    }

    private StaffPerson createStaffPerson() {
        return createStaffPerson(LOGIN_FROM_CALENDAR);
    }

    private AbcShift createAbcShift() {
        AbcShift shift = new AbcShift();
        AbcPerson person = new AbcPerson();
        person.setLogin(LOGIN_FROM_ABC);
        shift.setPerson(person);
        return shift;
    }

    private void verifyNotification(EmailNotification emailNotification) {
        String expectedMessage = String.format(
            "Добрый день. Вы получили это письмо, потому что входите в состав Cтафф группы '%s' сервиса '%s'.\n" +
                "Во время выполнения задачи переключения дежурного произошла ошибка:\n" +
                "'%s'\n" +
                "Полное описание дежурства: '%s'\n" +
                "Дата: '%s'\n" +
                "Ссылка на календарь: %s\n",
            TEST_STAFF_GROUP,
            TEST_DEPARTMENT,
            notificationErrorMessage,
            expectedCurrentDuty,
            ISO_LOCAL_DATE.withZone(ZoneId.systemDefault()).format(expectedDate.toInstant()),
            TEST_DUTY_CALENDAR_LINK);
        assertEquals(expectedMessage, emailNotification.getEmailMessage());
        assertEquals(String.format("Ошибка во время переключения дежурного в сервисе %s", TEST_DEPARTMENT),
            emailNotification.getEmailSubject());
    }

    @SuppressWarnings("SameParameterValue")
    private List<CalendarEvent> createCalendarEvents(String nameFromCalendar) {
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setName(nameFromCalendar);
        return Collections.singletonList(calendarEvent);
    }

}