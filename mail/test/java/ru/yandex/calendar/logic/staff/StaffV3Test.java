package ru.yandex.calendar.logic.staff;

import java.io.IOException;
import java.util.Set;

import com.amazonaws.util.json.JSONException;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffUser;

import static org.junit.Assert.assertEquals;

public class StaffV3Test {

    StaffV3 staffV3 = new StaffV3("https://staff-api.yandex-team.ru", new TestStaffHttpProvider());


    @Test
    public void testPersons() throws IOException, JSONException {
        var persons = staffV3.persons(1000, 1, "", false);
        assertEquals(1, persons.size());
        assertEquals(1, staffV3.totalEntries("persons"));
        var user0 = persons.get(0).getUser();
        assertEquals(1120000000022901L, user0.getUid().getValue());
        assertEquals("amosov-f", user0.getLogin());
        assertEquals("amosov-f@yandex-team.ru", user0.getInfo().getWorkEmail());
        assertEquals(StaffUser.Gender.MALE, user0.getInfo().getGender());
        assertEquals(81, persons.get(0).getDepartmentIds().size());
    }

    @Test
    public void testGroups() throws IOException, JSONException {
        var groups = staffV3.groups(1000, 1, "", false);
        assertEquals(2, groups.size());
        assertEquals(2, staffV3.totalEntries("groups"));
        var group0 = groups.get(0);

        assertEquals(188238L, group0.getId().getValue());
        assertEquals("Строганов коворкинг", group0.getName());
        assertEquals(true, group0.isActive());
        assertEquals(Set.of(), group0.getInfo().getChiefUids());
        assertEquals("svc_stroganovcoworking", group0.getInfo().getUrl());

        var group1 = groups.get(1);

        assertEquals(115248, group1.getId().getValue());
        assertEquals("Mail analytics group", group1.getName());
        assertEquals(true, group1.isActive());
        assertEquals(Set.of(1120000000000907L), group1.getInfo().getChiefUids());
        assertEquals("yandex_rkub_mobdev_pers_dep47221", group1.getInfo().getUrl());
    }

    @Test
    public void testOffices() throws IOException, JSONException {
        var offices = staffV3.offices(1000, 1, "", false);
        assertEquals(2, offices.size());
        assertEquals(113, staffV3.totalEntries("offices"));
        var office0 = offices.get(0);
        assertEquals(1L, office0.getId().getValue());
        assertEquals("Moscow, BC Morozov", office0.getName());
        assertEquals("Москва", office0.getInfo().getCityName().getRu());
    }


    @Test
    public void testRooms() throws IOException, JSONException {
        var rooms = staffV3.rooms(1000, 1, "", false);
        assertEquals(1, rooms.size());
        assertEquals(1, staffV3.totalEntries("rooms"));
        var room0 = rooms.get(0);
        assertEquals(4713L, room0.getId().getValue());
        assertEquals("conf_aur_5с14", room0.getName());
        assertEquals(5, room0.getInfo().getFloorNumber().getAsInt());
    }

    @Test
    public void testTelegramLogin() throws IOException {
        Option<String> telegramLogin = staffV3.getTelegramLogin("login");
        assertEquals("Nikita_Andreev1", telegramLogin.get());
    }

}
