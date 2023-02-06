package ru.yandex.market.tsum.tms.service;

import org.hamcrest.core.StringStartsWith;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import ru.yandex.market.tsum.clients.calendar.CalendarEvent;
import ru.yandex.market.tsum.clients.staff.StaffGroup;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.core.duty.Duty;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ResolveDutyLoginStateTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void expectExceptionWhenUnableToCreateErrorMessage() {
        expectedException.expect(IllegalStateException.class);
        expectedException
            .expectMessage(StringStartsWith.startsWith("Unable to create error message from switch duty state:"));

        getFullState().getErrorMessage();
    }

    @Test
    public void getErrorMessage_expectEmptyCalendarMessage() {
        ResolveDutyLoginState state = getFullState();
        state.setCalendarEvents(Collections.emptyList());
        assertEquals("Календарь пуст", state.getErrorMessage());
    }

    @Test
    public void getErrorMessage_expectDutyNotFoundInStaffMessage() {
        ResolveDutyLoginState state = getFullState();
        state.setStaffPerson(null);
        assertEquals("Дежурный не найден в Стаффе", state.getErrorMessage());
    }

    @Test
    public void getErrorMessage_expectStaffPeopleNotFoundMessage() {
        ResolveDutyLoginState state = getFullState();
        state.setStaffPeople(Collections.emptyList());
        assertEquals("Группа сервиса в Стаффе пустая", state.getErrorMessage());
    }

    @Test
    public void getErrorMessage_expectNextDutyLoginNotFound() {
        ResolveDutyLoginState state = getFullState();
        state.setNextDutyPersonLogin("");
        assertEquals("Логин дежурного в календаре заполнен некорректно", state.getErrorMessage());
    }

    private ResolveDutyLoginState getFullState() {
        Duty duty = Mockito.mock(Duty.class);
        StaffGroup staffGroup = Mockito.mock(StaffGroup.class);
        CalendarEvent calendarEvent = Mockito.mock(CalendarEvent.class);
        StaffPerson staffPerson = Mockito.mock(StaffPerson.class);

        ResolveDutyLoginState fullState = new ResolveDutyLoginState(
            duty,
            staffGroup,
            1,
            "someService"
        );
        fullState.setCalendarEvents(Collections.singletonList(calendarEvent));
        fullState.setStaffPerson(staffPerson);
        fullState.setStaffPeople(Collections.singletonList(staffPerson));
        fullState.setNextDutyPersonLogin("someLogin");
        return fullState;
    }
}