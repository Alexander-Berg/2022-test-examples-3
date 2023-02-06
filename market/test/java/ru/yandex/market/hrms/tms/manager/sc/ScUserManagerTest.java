package ru.yandex.market.hrms.tms.manager.sc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.connect.client.YandexConnectClientFacade;
import ru.yandex.connect.client.model.Employee;
import ru.yandex.connect.client.model.Links;
import ru.yandex.connect.client.model.Name;
import ru.yandex.connect.client.model.PagingResult;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeRepo;
import ru.yandex.market.hrms.core.domain.outstaff.OutstaffEntity;
import ru.yandex.market.hrms.core.domain.outstaff.OutstaffV2Entity;
import ru.yandex.market.hrms.core.domain.position.OutstaffPositionEntity;
import ru.yandex.market.hrms.core.domain.ws.sc.client.ScUserCreateRequest;
import ru.yandex.market.hrms.core.service.outstaff.account.Gender;
import ru.yandex.market.hrms.core.service.sc.ScApiClient;
import ru.yandex.market.hrms.core.service.sc.ScApiConfigurer;
import ru.yandex.market.hrms.core.service.sc.ScUserRequest;
import ru.yandex.market.hrms.core.service.sc.ScUserStateManager;
import ru.yandex.market.hrms.core.service.wms.PasswordGenerator;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "ScUserManagerTest.before.csv")
public class ScUserManagerTest extends AbstractTmsTest {

    @Autowired
    private ScUserStateManager scUserStateManager;

    @Autowired
    private ScApiConfigurer scApiConfigurer;

    @SpyBean
    private YandexConnectClientFacade yandexConnectClientFacade;

    @SpyBean
    private ScApiClient scApiClient;

    @SpyBean
    private PasswordGenerator passwordGenerator;

    @Autowired
    private EmployeeRepo employeeRepo;

    @BeforeEach
    public void init() {
        scApiConfigurer.mockCreateOrUpdate("""
                {
                   "id": 123
                }
                """);
        scApiConfigurer.mockDelete(123L);
    }

    @AfterEach
    public void release() {
        Mockito.reset(scApiClient, yandexConnectClientFacade, passwordGenerator);
    }

    @Test
    @DbUnitDataSet(before = "ScUserManagerTest.create.before.csv", after = "ScUserManagerTest.create.after.csv")
    public void shouldFindAllUsersWithoutSc() {
        scUserStateManager.moveToInProgress();
    }

    @Test
    @DbUnitDataSet(before = "ScUserManagerTest.create.outstaff.before.csv",
            after = "ScUserManagerTest.create.outstaff.after.csv")
    public void shouldFindAllUsersWithoutScOutstaff() {
        scUserStateManager.moveToInProgressOutstaff();
    }

    @Test
    @DbUnitDataSet(before = "ScUserManagerTest.toready.outstaff.before.csv",
            after = "ScUserManagerTest.toready.outstaff.before.csv")
    public void shouldReuseExistingLoginsOutstaff() {
        scUserStateManager.moveToInProgressOutstaff();
    }

    @Test
    @DbUnitDataSet(before = "ScUserManagerTest.toready.before.csv",
            after = "ScUserManagerTest.toready.before.csv")
    public void shouldReuseExistingLogins() {
        scUserStateManager.moveToInProgress();
    }

    @Test
    @DbUnitDataSet(before = "ScUserManagerTest.create.outstaff.before.csv",
            after = "ScUserManagerTest.create.outstaff.duplicate.after.csv")
    public void shouldFindAllUsersWithoutScOutstaffDuplicates() {
        var existingUsers = List.of(new Employee(
                123L,
                "",
                List.of(),
                LocalDate.now(),
                List.of(),
                null,
                1L,
                null,
                false,
                false,
                new Name("Петр", "Петров", "Петрович"),
                "petrpep",
                "кладовщик",
                null,
                null,
                null,
                "",
                false,
                "user",
                "",
                "",
                "petrpep@hrms-sc.ru",
                List.of()
        ));

        when(yandexConnectClientFacade.searchUsersByNickname("sof-petrpep", true, 0, 2))
                .thenReturn(new PagingResult<>(1, 1, 2, 1,
                        Links.Companion.getEMPTY(), false, existingUsers));
        scUserStateManager.moveToInProgressOutstaff();
    }

    @Test
    @DbUnitDataSet(before = "ScUserManagerTest.toready.before.csv", after = "ScUserManagerTest.toready.after.csv")
    public void shouldCreateLogins() {
        scUserStateManager.moveFromInProgressToReady();
    }

    @Test
    @DbUnitDataSet(
            before = "ScUserManagerTest.toready.outstaff.before.csv",
            after = "ScUserManagerTest.toready.outstaff.after.csv")
    public void shouldCreateLoginsOutstaff() {
        scUserStateManager.moveFromInProgressToReady();
    }

    @Test
    @DbUnitDataSet(
            before = "ScUserManagerTest.toready.outstaff.before.csv",
            after = "ScUserManagerTest.createScLoginsForOutstaff.outstaff.after.csv")
    public void createScLoginsForOutstaff() {
        scUserStateManager.createScUserForOutstaffV2(
                2L,
                1L,
                OutstaffV2Entity.builder()
                        .id(1L)
                        .birthday(LocalDate.MIN)
                        .city("Москва")
                        .companyId(7L)
                        .firstName("Жора")
                        .lastName("Жожа")
                        .midName("Жорочкин")
                        .companyLoginId(2L)
                        .email("qwe")
                        .gender(Gender.MALE)
                        .hasRussianCitizenship(false)
                        .phone("123")
                        .oldOutstaffs(List.of(OutstaffEntity.builder()
                                .id(2L)
                                .build()))
                        .positions(List.of(OutstaffPositionEntity.builder()
                                        .title("кладовщик")
                                        .domainId(1L)
                                .build()))
                        .build());


    }

    @Test
    @DbUnitDataSet(
            before = "ScUserManagerTest.toready.before.csv",
            after = "ScUserManagerTest.toready.exceptionsc.after.csv")
    public void shouldCreateLoginsWithExceptionSc() {
        var user = new ScUserRequest(172L, "valera", "sof-ivanov@hrms-sc.ru", "STOCKMAN", "user1");
        doThrow(new RuntimeException())
                .when(scApiClient).createOrUpdateUser(user);
        scUserStateManager.moveFromInProgressToReady();
    }

    @Test
    @DbUnitDataSet(
            before = "ScUserManagerTest.toready.before.csv",
            after = "ScUserManagerTest.toready.exceptionconnect.after.csv")
    public void shouldCreateLoginsWithExceptionConnect() {
        var employee = employeeRepo.findById(1L).orElseThrow();
        when(passwordGenerator.generate()).thenReturn("12345");
        var lastName = Optional.ofNullable(employee.getLastName()).orElse("-");
        var middleName = Optional.ofNullable(employee.getMiddleName()).orElse("-");

        var name = new Name(employee.getFirstName(), lastName, middleName);
        var request = new ScUserCreateRequest(name, 1L, "sof-ivanov", "кладовщик","12345");
        doThrow(new RuntimeException()).when(yandexConnectClientFacade).createUser(request);
        scUserStateManager.moveFromInProgressToReady();
    }

    @Test
    @DbUnitDataSet(before = "ScUserManagerTest.toinactive.before.csv", after = "ScUserManagerTest.toinactive.after.csv")
    public void shouldDisable() {
        mockClock(Instant.parse("2022-01-01T00:00:00Z"));
        scUserStateManager.moveFromReadyToInactive();
    }

    @Test
    @DbUnitDataSet(before = "ScUserManagerTest.toinactive.noflag.before.csv",
            after = "ScUserManagerTest.toinactive.noflag.after.csv")
    public void shouldDisableWhenFlagIsFalse() {
        scUserStateManager.moveFromReadyToInactive();
    }

    @Test
    @DbUnitDataSet(before = "ScUserManagerTest.toinactive.before.csv",
            after = "ScUserManagerTest.toinactive.noflag.after.csv")
    public void shouldDisableWithException() {
        doThrow(new RuntimeException()).when(scApiClient).deleteUser(123L);
        assertThrows(TplIllegalStateException.class, () -> scUserStateManager.moveFromReadyToInactive());
    }
}
