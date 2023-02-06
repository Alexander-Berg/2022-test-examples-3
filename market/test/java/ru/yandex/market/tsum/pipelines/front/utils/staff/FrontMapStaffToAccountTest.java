package ru.yandex.market.tsum.pipelines.front.utils.staff;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.pipelines.common.resources.StaffLoginToAccountMap;

@RunWith(MockitoJUnitRunner.class)
public class FrontMapStaffToAccountTest {
    @Mock
    private StaffApiClient staffApiClient;

    @Test
    public void shouldReturnEmptyMapWhenGivenEmptyList() {
        StaffLoginToAccountMap map = FrontMapStaffToAccount.mapLoginsToAccount(staffApiClient, Collections.emptyList(),
            StaffPerson.AccountType.TELEGRAM);
        Assert.assertTrue(map.getStaffToAccountMap().isEmpty());
    }

    @Test
    public void shouldHaveMapEntryWhenUserHasAccount() {
        List<String> users = List.of("user1");
        List<StaffPerson> personList = List.of(Mockito.mock(StaffPerson.class));
        Mockito.when(staffApiClient.getPersons(users)).thenReturn(personList);
        Mockito.when(personList.get(0).getLogin()).thenReturn("user1");
        Mockito.when(personList.get(0).getAccounts()).thenReturn(
            List.of(new StaffPerson.PersonAccount(StaffPerson.AccountType.TELEGRAM, "tgUser1")));
        StaffLoginToAccountMap map = FrontMapStaffToAccount.mapLoginsToAccount(staffApiClient, users,
            StaffPerson.AccountType.TELEGRAM);
        Assert.assertEquals("tgUser1", map.getStaffToAccountMap().get("user1"));
    }

    @Test
    public void shouldHaveMapEntryWithFirstAccountWhenUserHasMultipleAccounts() {
        List<String> users = List.of("user1");
        List<StaffPerson> personList = List.of(Mockito.mock(StaffPerson.class));
        Mockito.when(staffApiClient.getPersons(users)).thenReturn(personList);
        Mockito.when(personList.get(0).getLogin()).thenReturn("user1");
        Mockito.when(personList.get(0).getAccounts()).thenReturn(
            List.of(
                new StaffPerson.PersonAccount(StaffPerson.AccountType.TELEGRAM, "tgUser1"),
                new StaffPerson.PersonAccount(StaffPerson.AccountType.TELEGRAM, "tgUser2")
            ));
        StaffLoginToAccountMap map = FrontMapStaffToAccount.mapLoginsToAccount(staffApiClient, users,
            StaffPerson.AccountType.TELEGRAM);
        Assert.assertEquals("tgUser1", map.getStaffToAccountMap().get("user1"));
    }

    @Test
    public void shouldNotHaveMapEntryWhenUserDoesNotHaveAccount() {
        List<String> users = List.of("user1");
        List<StaffPerson> personList = List.of(Mockito.mock(StaffPerson.class));
        Mockito.when(staffApiClient.getPersons(users)).thenReturn(personList);
        Mockito.when(personList.get(0).getLogin()).thenReturn("user1");
        Mockito.when(personList.get(0).getAccounts()).thenReturn(
            List.of(new StaffPerson.PersonAccount(StaffPerson.AccountType.GITHUB, "user1")));
        StaffLoginToAccountMap map = FrontMapStaffToAccount.mapLoginsToAccount(staffApiClient, users,
            StaffPerson.AccountType.TELEGRAM);
        Assert.assertNull(map.getStaffToAccountMap().get("user1"));
    }

    @Test
    public void shouldNotHaveMapEntryWhenStaffCanNotFindUser() {
        List<String> users = List.of("user1");
        List<StaffPerson> personList = List.of(Mockito.mock(StaffPerson.class));
        Mockito.when(staffApiClient.getPersons(users)).thenReturn(Collections.emptyList());
        StaffLoginToAccountMap map = FrontMapStaffToAccount.mapLoginsToAccount(staffApiClient, users,
            StaffPerson.AccountType.TELEGRAM);
        Assert.assertNull(map.getStaffToAccountMap().get("user1"));
    }

}
