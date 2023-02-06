package ru.yandex.qe.mail.meetings.ws.booking.mocks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;

import ru.yandex.qe.mail.meetings.services.staff.StaffApiV3;
import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.services.staff.dto.PersonContainer;
import ru.yandex.qe.mail.meetings.services.staff.dto.Response;
import ru.yandex.qe.mail.meetings.services.staff.dto.StaffGroup;
import ru.yandex.qe.mail.meetings.ws.booking.util.BookingTestHelper;


public class StaffApiV3Mock implements StaffApiV3 {
    private final List<BookingTestHelper.ObjectWithTimeTable<Person>> persons;

    public StaffApiV3Mock(List<BookingTestHelper.ObjectWithTimeTable<Person>> persons) {
        this.persons = persons;
    }

    @Override
    public Response<Person> persons(String fields, int limit, int page, boolean dismissed, String loginsStr) {
        var logins = new HashSet<>(Arrays.asList(loginsStr.split(",")));
        var result = persons
                .stream()
                .map(p -> p.obj)
                .filter(p -> logins.contains(p.getLogin()))
                .collect(Collectors.toUnmodifiableList());
        return new Response<>(1, logins.size(), result.size(), 1, result);
    }

    @Override
    public Response<Person> person(String fields, String uid, String login) {
        return persons("", 0, 0, false, login);
    }

    @Override
    public Response<StaffGroup> groups(String fields, int limit, int page) {
        throw new NotImplementedException();
    }

    @Override
    public Response<PersonContainer> groupMembership(String fields, int limit, int page, int groupId) {
        throw new NotImplementedException();
    }

    @Override
    public Response<Person> persons(String fields, int limit, int page) {
        throw new NotImplementedException();
    }

    @Override
    public Response<Person> persons(String fields, int limit, int page, boolean dismissed) {
        throw new NotImplementedException();
    }
}
