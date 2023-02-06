package ru.yandex.market.admin.service.remote;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.client.UIPassport;
import ru.yandex.market.admin.ui.model.users.UIUsersSearchFieldType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.passport.BlackBoxPassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.staff.EmployeeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RemoteUsersUIServiceTest extends FunctionalTest {
    @Autowired
    EmployeeService employeeService;

    RemoteUsersUIService remoteUsersUIService;

    @BeforeEach
    void setUp() {
        var blackBoxPassportService = mock(BlackBoxPassportService.class);
        when(blackBoxPassportService.getUsersByLogin(eq("vasyapupkin"), anyLong(), anyLong()))
                .thenReturn(List.of(VASILY));
        when(blackBoxPassportService.getUsersByEmail(eq("vasyapupkin@yandex.ru"), anyLong(), anyLong()))
                .thenReturn(List.of(VASILY));
        when(blackBoxPassportService.getUsersByEmail(eq("notexist"), anyLong(), anyLong()))
                .thenReturn(Collections.emptyList());
        when(blackBoxPassportService.getUsersByLogin(eq("notexist"), anyLong(), anyLong()))
                .thenReturn(Collections.emptyList());

        remoteUsersUIService = new RemoteUsersUIService();
        remoteUsersUIService.setContactService(mock(ContactService.class));
        remoteUsersUIService.setEmployeeService(employeeService);
        remoteUsersUIService.setPassportService(blackBoxPassportService);
    }

    private static final UserInfo NADYA = new UserInfo(1, "nadya habibulina", "nadya@yandex-team.ru", "yndx-nadya");
    private static final UserInfo VASILY = new UserInfo(10, "vasya pupkin", "vasyapupkin@yandex.ru", "vasyapupkin");

    @Test
    void testGetListUsersFromPassport() {
        isRequiredUser(requestByLogin("vasyapupkin"), VASILY);
        isRequiredUser(requestByEmail("vasyapupkin@yandex.ru"), VASILY);
    }

    @Test
    @DbUnitDataSet(before = "getListOfUsersForUI.before.csv")
    void testGetListUsersFromEmployeeService() {
        isRequiredUser(requestByLogin("yndx-nadya"), NADYA);
        isRequiredUser(requestByEmail("nadya@yandex-team.ru"), NADYA);
    }

    @Test
    void testGetEmptyUserList() {
        assertThat(requestByLogin("notexist")).isEmpty();
        assertThat(requestByEmail("notexist")).isEmpty();
    }

    private static void isRequiredUser(List<UIPassport> response, UserInfo needed) {
        assertThat(response)
                .singleElement()
                .satisfies(passport -> {
                    assertThat(passport.getStringField(UIPassport.ID)).isEqualTo(needed.getId().toString());
                    assertThat(passport.getStringField(UIPassport.LOGIN)).isEqualTo(needed.getLogin());
                    assertThat(passport.getStringField(UIPassport.NAME)).isEqualTo(needed.getName());
                    assertThat(passport.getStringField(UIPassport.EMAIL)).isEqualTo(needed.getEmail());
                });
    }

    private List<UIPassport> requestByLogin(String searchText) {
        return remoteUsersUIService.listUsers(UIUsersSearchFieldType.LOGIN, searchText, 0, 10);
    }

    private List<UIPassport> requestByEmail(String searchText) {
        return remoteUsersUIService.listUsers(UIUsersSearchFieldType.EMAIL, searchText, 0, 10);
    }
}
