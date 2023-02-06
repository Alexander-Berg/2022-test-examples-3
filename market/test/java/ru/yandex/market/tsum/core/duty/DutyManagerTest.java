package ru.yandex.market.tsum.core.duty;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.Futures;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.abc.AbcApiClient;
import ru.yandex.market.tsum.clients.abc.models.AbcPerson;
import ru.yandex.market.tsum.clients.abc.models.AbcShift;
import ru.yandex.market.tsum.clients.calendar.CalendarClient;
import ru.yandex.market.tsum.clients.calendar.CalendarEvent;
import ru.yandex.market.tsum.clients.calendar.CalendarId;
import ru.yandex.market.tsum.clients.juggler.JugglerApiClient;
import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.market.tsum.clients.staff.StaffGroup;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.clients.telegraph.TelegraphApiClient;
import ru.yandex.market.tsum.core.duty.exceptions.DepartmentNotFoundException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 10/08/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class DutyManagerTest {

    private static final int CALENDAR_ID = 10;
    private static final String JUGGLER_NOTIFICATION_ID = "5d0b1b69ef1625006dc208d4";
    private static final String STAFF_GROUP = "src_noname_dutywork";
    private static final String DEPARTMENT = "logistics-delivery";
    private static final String INCIDENT_DUTY = "incident";

    private static CalendarEvent calendarEvent(String name) {
        final CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setName(name);
        return calendarEvent;
    }

    private static StaffPerson staffPerson(String login) {
        return new StaffPerson(login, 0, null, null, null, null);
    }

    @Test(expected = DepartmentNotFoundException.class)
    public void testSwitchDuty_departmentNotFound_DepartmentNotFoundExceptionThrown() throws Exception {
        String departmentName = "test_department";
        String dutyType = "some_duty";
        String otherDepartmentName = "this_is_other_department";
        String switchTo = "dutyName";

        Table<String, String, Duty> dutyStore = HashBasedTable.create();

        Duty mockDuty = mock(Duty.class, Mockito.RETURNS_DEEP_STUBS);

        dutyStore.put(departmentName, dutyType, mockDuty);
        DutyManager dutyManager = new DutyManager(null, null, null, null, null);

        Field field = dutyManager.getClass().getDeclaredField("dutyStore");
        field.setAccessible(true);
        field.set(dutyManager, dutyStore);

        dutyManager.switchDuty(otherDepartmentName, dutyType, switchTo);

        fail("Exception DepartmentNotFoundException not thrown");
    }

    @Test(expected = DepartmentNotFoundException.class)
    public void testSwitchDutyFromCalendar_departmentNotFound_DepartmentNotFoundExceptionThrown() throws Exception {
        String departmentName = "test_department";
        String dutyType = "some_duty";
        String otherDepartmentName = "this_is_other_department";
        String switchTo = "dutyName";

        Table<String, String, Duty> dutyStore = HashBasedTable.create();

        Duty mockDuty = mock(Duty.class, Mockito.RETURNS_DEEP_STUBS);

        dutyStore.put(departmentName, dutyType, mockDuty);
        DutyManager dutyManager = new DutyManager(null, null, null, null, null);

        Field field = dutyManager.getClass().getDeclaredField("dutyStore");
        field.setAccessible(true);
        field.set(dutyManager, dutyStore);

        dutyManager.setDuty(otherDepartmentName, dutyType, switchTo);

        fail("Exception DepartmentNotFoundException not thrown");
    }

    /**
     * Проверяет, что из нескольких календарей вытаскивается нужный логин и склеивается с логинами из группы дежурства.
     * Первый в списке логин должен быть взят из календаря.
     */
    @Test
    public void testCorrectlyUpdateJugglerNotificationForLogistics() {
        // Замокать клиенты
        StaffApiClient staffApiClient = mock(StaffApiClient.class);
        AbcApiClient abcApiClient = mock(AbcApiClient.class);
        when(staffApiClient.getPerson("avetokhin")).thenReturn(Optional.of(staffPerson("avetokhin")));
        when(staffApiClient.getGroup(STAFF_GROUP))
            .thenReturn(Optional.of(new StaffGroup()));
        when(staffApiClient.getPersons(any(StaffGroup.class)))
            .thenReturn(Arrays.asList(staffPerson("vbauer"), staffPerson("sogreshilin")));

        CalendarClient calendarClient = mock(CalendarClient.class);
        when(calendarClient.getEvents(any(Date.class), any(Date.class), any(CalendarId.class)))
            .thenReturn(Futures.immediateFuture(Arrays.asList(
                calendarEvent("[NSK][SUPPORT]i-milyaev"),
                calendarEvent("[NSK][RELEASE][FUCKUP]avetokhin"))
            ));

        TelegraphApiClient telegraphApiClient = mock(TelegraphApiClient.class);
        JugglerApiClient jugglerApiClient = mock(JugglerApiClient.class);

        // Создать менеджер дежурств
        DutyManager dutyManager = new DutyManager(staffApiClient, calendarClient, telegraphApiClient,
            jugglerApiClient, abcApiClient);

        dutyManager.addDepartmentDuty(DEPARTMENT, INCIDENT_DUTY, Duty.newBuilder()
            .withCalendarLayerId(CALENDAR_ID)
            .withJugglerNotificationRuleId(JUGGLER_NOTIFICATION_ID)
            .withStaffGroupName(STAFF_GROUP)
            .withLoginExtractor(DutyLoginExtractors.logistics())
            .build()
        );

        dutyManager.setDuty(DEPARTMENT, INCIDENT_DUTY, null);

        // Проверить данные
        ArgumentCaptor<List<String>> loginsCaptor = ArgumentCaptor.forClass(List.class);
        verify(jugglerApiClient)
            .updateNotificationRule(eq(JUGGLER_NOTIFICATION_ID), loginsCaptor.capture());
        assertThat(loginsCaptor.getValue().size(), equalTo(3));
        assertThat(loginsCaptor.getValue().get(0), equalTo("avetokhin"));
        assertThat(loginsCaptor.getValue(), containsInAnyOrder("avetokhin", "vbauer", "sogreshilin"));

    }

    @Test
    public void testLogisticLoginExtractor() {
        Assert.assertEquals(
            "ivanov-af",
            DutyLoginExtractors.logistics().extractLogin("[RELEASE][FUCKUP][DEV]ivanov-af")
        );

        Assert.assertEquals(
            "ivanov-af",
            DutyLoginExtractors.logistics().extractLogin("[RELEASE][FUCKUP]ivanov-af")
        );
        Assert.assertEquals(
            "ivanov-af",
            DutyLoginExtractors.logistics().extractLogin("[FUCKUP][DEV]ivanov-af")
        );

        Assert.assertEquals(
            "ivanov-af",
            DutyLoginExtractors.logistics().extractLogin("[FUCKUP]ivanov-af")
        );
    }

    @Test
    public void testIncorrectAbcDutyScheduleValidation() {
        // Создать менеджер дежурств
        DutyManager dutyManager = new DutyManager(null, null, null, null, null);
        Duty duty = Duty.newBuilder()
            .withJugglerNotificationRuleId(JUGGLER_NOTIFICATION_ID)
            .withStaffGroupName(STAFF_GROUP)
            .withAbcDutySchedule("svc_1_1")
            .build();

        try {
            dutyManager.getNextDutyLoginFromAbc(duty);
            fail("No error for invalid abcDutySchedule");
        } catch (RuntimeException e) {
            Assert.assertThat(e.getMessage(), containsString("Incorrect abcDutySchedule"));
        }
    }

    @Test
    public void testAbcDutySchedule() {

        AbcApiClient abcApiClient = mock(AbcApiClient.class);
        AbcShift shift = new AbcShift();
        AbcPerson person = new AbcPerson();
        person.setLogin("asivolapov");
        shift.setPerson(person);

        when(abcApiClient.getAbcOnDuty("inc_manager", "inc_schedule"))
            .thenReturn(Collections.singletonList(shift));

        // Создать менеджер дежурств
        DutyManager dutyManager = new DutyManager(null, null, null, null, abcApiClient);

        Duty duty = Duty.newBuilder()
            .withJugglerNotificationRuleId(JUGGLER_NOTIFICATION_ID)
            .withStaffGroupName(STAFF_GROUP)
            .withAbcDutySchedule("@svc_inc_manager:inc_schedule")
            .build();
        Assert.assertEquals(person.getLogin(), dutyManager.getNextDutyLoginFromAbc(duty));
    }
}
