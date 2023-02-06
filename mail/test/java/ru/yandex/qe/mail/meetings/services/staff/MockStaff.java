package ru.yandex.qe.mail.meetings.services.staff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;

import ru.yandex.qe.mail.meetings.services.staff.dto.Email;
import ru.yandex.qe.mail.meetings.services.staff.dto.Language;
import ru.yandex.qe.mail.meetings.services.staff.dto.Languages;
import ru.yandex.qe.mail.meetings.services.staff.dto.LocalizedString;
import ru.yandex.qe.mail.meetings.services.staff.dto.Location;
import ru.yandex.qe.mail.meetings.services.staff.dto.Office;
import ru.yandex.qe.mail.meetings.services.staff.dto.Official;
import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.services.staff.dto.PersonContainer;
import ru.yandex.qe.mail.meetings.services.staff.dto.Response;
import ru.yandex.qe.mail.meetings.services.staff.dto.StaffGroup;

/**
 * @author Sergey Galyamichev
 */
public class MockStaff implements StaffApiV3 {
    private static final Languages TRUE_RUSSIAN = new Languages("", Language.RUSSIAN, false, Language.RUSSIAN);
    private static final Set<String> ACTIVE = Set.of("man", "kurgin", "rubtsovdmv", "alexandrsm");
    public static final Set<String> DISMISSED = Set.of("a-morozova", "mycroft", "stassiak", "shushunder", "kladnitskaya");
    private static final AtomicInteger ids = new AtomicInteger(7);
    public static final String EXTERNAL = "mikhaila";
    private static final Location MSK = new Location(new Office(0, "1"));
    private final Set<String> dismissed;

    public MockStaff(Set<String> dismissed) {
        this.dismissed = new HashSet<>(dismissed);
    }

    @Override
    public Response<Person> persons(String fields, int limit, int page) {
        return toResponse(limit, page, all());
    }

    @Override
    public Response<Person> persons(String fields, int limit, int page, boolean dismissed) {
        return toResponse(limit, page, all().stream()
                .filter(p -> p.getOfficial().isDismissed() == dismissed)
                .collect(Collectors.toList()));
    }

    @Override
    public Response<Person> persons(String fields, int limit, int page, boolean dismissed, String logins) {
        throw new NotImplementedException();
    }

    @Override
    public Response<Person> person(String fields, String uid, String login) {
        if (login == null && uid == null || EXTERNAL.equals(login)) {
            return toResponse(50, 1, Collections.emptyList());
        }
        List<Person> collect = all().stream()
                .filter(p -> p.getUid().equals(uid) || p.getLogin().equals(login))
                .collect(Collectors.toList());
        if (collect.isEmpty()) {
            collect = Collections.singletonList(person(login, "" + ids.getAndIncrement(), dismissed.contains(login)));
        }
        return toResponse(1, 1, collect);
    }

    @Override
    public Response<StaffGroup> groups(String fields, int limit, int page) {
        throw new NotImplementedException();
    }

    @Override
    public Response<PersonContainer> groupMembership(String fields, int limit, int page, int groupId) {
        throw new NotImplementedException();
    }

    private Response<Person> toResponse(int limit, int page, List<Person> list) {
        return new Response<>(page, limit, list.size(), list.isEmpty() ? 0 : 1, list);
    }

    private List<Person> all() {
        Person d1 = person("d1", "1", true);
        Person d2 = person("d3", "3", true);
        Person d3 = person("d5", "5", true);
        Person l1 = person("l2", "2", false);
        Person l2 = person("l4", "4", false);
        Person l3 = person("l6", "6", false);
        List<Person> all = new ArrayList<>(Arrays.asList(l1, l2, l3, d1, d2, d3));

        ACTIVE.forEach(active -> all.add(person(active, "" + ids.getAndIncrement(), false)));
        dismissed.forEach(dismissed -> all.add(person(dismissed, "" + ids.getAndIncrement(), true)));
        return all;
    }

    private Person person(String login, String uid, boolean dismissed) {
        Email email = email(login + "@yandex-team.ru");
        List<Email> emails = Collections.singletonList(email);
        String name = "n m s" + uid;
        Official o = new Official(null, null,null,false, dismissed, false, false, false, null,null,null, false);
        return new Person(uid, login, name(name), null, emails, email.getAddress(), o, TRUE_RUSSIAN, MSK);
    }

    private Person.Name name(String name) {
        String[] names = name.split(" ");
        return new Person.Name(string(names[0]), names[1], string(names[2]), false);
    }

    private LocalizedString string(String string) {
        return new LocalizedString(Collections.singletonMap(Language.RUSSIAN, string));
    }

    private Email email(String email) {
        return new Email(1, Email.SourceType.STAFF, email);
    }
}
