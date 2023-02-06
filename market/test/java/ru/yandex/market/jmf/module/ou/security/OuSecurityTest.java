package ru.yandex.market.jmf.module.ou.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.UniqueAttributeValidationException;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.security.AuthRunnerService;
import ru.yandex.market.jmf.security.SecurityDataService;
import ru.yandex.market.jmf.security.test.impl.MockAuthRunnerService;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InternalModuleOuSecurityTestConfiguration.class)
public class OuSecurityTest {

    @Inject
    BcpService bcpService;
    @Inject
    SecurityDataService securityDataService;
    @Inject
    AuthRunnerService authRunnerService;
    @Inject
    OuTestUtils ouTestUtils;

    @BeforeEach
    public void setUp() {
        setCurrentUserProfiles(Collections.singletonList(Profiles.THE_CREATOR_PROFILE_ID));
        setInitialEmployee(null);
    }

    @Transactional
    @Test
    public void cantCreateUserWithUnavailableRole() {
        // Создаем роль, которую текущий пользователь не может выдавать
        var newEmployeeRole = createRole(
                "cantCreateUserWithUnavailableRole_role",
                "cantCreateUserWithUnavailableRole_role",
                new ArrayList<>());

        // Создадим текущего пользователя
        Employee currentEmployee = createEmployee(
                "canCreateUserWithAvailableRole_current_employee",
                List.of());
        setInitialEmployee(currentEmployee);

        // и пытаемся создать с ней пользователя
        Assertions.assertThrows(ValidationException.class, () -> createEmployee(
                "cantCreateUserWithUnavailableRole_employee",
                List.of(newEmployeeRole)));
    }

    @Transactional
    @Test
    public void cantEditUserWithUnavailableRole() {
        // Создаем роль, которую текущий пользователь не может выдавать
        var newEmployeeRole = createRole(
                "cantCreateUserWithUnavailableRole_role",
                "cantCreateUserWithUnavailableRole_role",
                new ArrayList<>());

        // Создадим текущего пользователя
        Employee currentEmployee = createEmployee(
                "canCreateUserWithAvailableRole_current_employee",
                List.of());
        setInitialEmployee(currentEmployee);

        // и пытаемся создать с ней пользователя
        var newEmployee = createEmployee(
                "cantCreateUserWithUnavailableRole_employee",
                List.of());

        Assertions.assertThrows(ValidationException.class, () -> bcpService.edit(newEmployee, Map.of(
                Employee.ROLES, List.of(newEmployeeRole)
        )));
    }

    @Transactional
    @Test
    public void canCreateUserWithAvailableRole() {
        // Создадим роль, которую потом захотим дать сотруднику
        var otherEmployeeRole = createRole(
                "canCreateUserWithAvailableRole_role",
                "canCreateUserWithAvailableRole_role",
                new ArrayList<>());

        // Создадим роль для текущего пользователя, который создает нового пользователя,
        // Даем этой роли право давать роль otherEmployeeRole
        EmployeeRole currentUserRole = createRole(
                "canCreateUserWithAvailableRole_role_for_current_user",
                "canCreateUserWithAvailableRole_role_for_current_user",
                List.of(otherEmployeeRole));

        // Создадим текущего пользователя
        Employee currentEmployee = createEmployee(
                "canCreateUserWithAvailableRole_current_employee",
                List.of(currentUserRole));

        // Установим текущего пользователя, чтобы отработал скрипт
        setInitialEmployee(currentEmployee);

        // пытаемся создать сотрудника, дав ему роль, которая доступна к выдаче текущим пользователем
        var newEmployee = createEmployee(
                "canCreateUserWithAvailableRole_employee",
                List.of(otherEmployeeRole));

        Assertions.assertEquals(1, newEmployee.getRoles().size());
        Assertions.assertEquals(
                otherEmployeeRole.getCode(),
                newEmployee.getRoles().iterator().next().getCode());
    }

    @Transactional
    @Test
    public void allEmployeeRoleCodesInLowerCase() {
        createRole("Тестовая роль 1", "testRole", List.of());
        // Из-за приведения кодов ролей к нижнему регистру роль с кодом 'tEsTRolE' создать нельзя,
        // роль с таким кодом уже существует
        Assertions.assertThrows(UniqueAttributeValidationException.class,
                () -> createRole("Тестовая роль 2", "tEsTRolE", List.of()));
    }

    private Employee createEmployee(String title, List<EmployeeRole> roles) {
        ImmutableMap<String, Object> properties = ImmutableMap.of(
                Employee.TITLE, title,
                Employee.ROLES, roles,
                Employee.OU, ouTestUtils.createOu()
        );
        return bcpService.create(Employee.FQN_DEFAULT, properties);
    }

    private EmployeeRole createRole(String title, String code, List<EmployeeRole> rolesForCreate) {
        ImmutableMap<String, Object> properties = ImmutableMap.of(
                EmployeeRole.TITLE, title,
                EmployeeRole.CODE, code,
                EmployeeRole.ROLES_FOR_CREATE, rolesForCreate
        );
        return bcpService.create(EmployeeRole.FQN, properties);
    }

    private void setCurrentUserProfiles(List<String> employeeRoles) {
        ((MockSecurityDataService) securityDataService).setCurrentUserProfiles(employeeRoles);
    }

    private void setInitialEmployee(Employee employee) {
        ((MockSecurityDataService) securityDataService).setInitialEmployee(employee);
    }

    private void setCurrentUserSuperUser(boolean isCurrentUserSuperUser) {
        ((MockAuthRunnerService) authRunnerService).setCurrentUserSuperUser(isCurrentUserSuperUser);
    }

    interface Profiles {
        String THE_CREATOR_PROFILE_ID = "@theCreator";
    }
}
