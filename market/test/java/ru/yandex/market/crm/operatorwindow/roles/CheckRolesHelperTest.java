package ru.yandex.market.crm.operatorwindow.roles;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import ru.yandex.market.crm.operatorwindow.http.security.roles.ExternalOperatorRole;
import ru.yandex.market.crm.operatorwindow.http.security.roles.FirstLineOperatorRole;
import ru.yandex.market.crm.operatorwindow.http.security.roles.OperatorRole;
import ru.yandex.market.crm.operatorwindow.http.security.roles.SeniorOperatorRole;
import ru.yandex.market.crm.operatorwindow.http.security.roles.SupervisorRole;
import ru.yandex.market.crm.operatorwindow.roles.testdata.TestControllerGenericOperator;
import ru.yandex.market.crm.operatorwindow.roles.testdata.TestControllerOnlyClassRoles;
import ru.yandex.market.crm.operatorwindow.roles.testdata.TestControllerOnlyMethodRoles;
import ru.yandex.market.crm.operatorwindow.roles.testdata.TestControllerWithClassAndMethodRoles;
import ru.yandex.market.crm.operatorwindow.roles.testdata.TestErrorControllerWithoutRoles;
import ru.yandex.market.crm.operatorwindow.util.CheckRolesHelper;
import ru.yandex.market.jmf.module.ou.security.impl.security.RolesUtils;

public class CheckRolesHelperTest {

    private static final List<String> NO_ROLES = Collections.emptyList();
    private static final List<String> ADMIN_ROLE = Collections.singletonList(RolesUtils.ADMIN_ROLE.asSpringRole());
    private static final List<String> OPERATOR_ROLE =
            Collections.singletonList(RolesUtils.OPERATOR_ROLE.asSpringRole());
    private static final List<String> SUPERVISOR_ROLE =
            Collections.singletonList(RolesUtils.SUPERVISOR_ROLE.asSpringRole());

    @Test
    public void getEmptyAnnotations() throws NoSuchMethodException {
        HandlerMethod handlerMethod = createHandlerMethod(new TestErrorControllerWithoutRoles());
        Assertions.assertEquals(
                Collections.emptySet(),
                CheckRolesHelper.getRoleAnnotations(handlerMethod)
        );
    }

    @Test
    public void getAnnotationsOnlyClass() throws NoSuchMethodException {
        HandlerMethod handlerMethod = createHandlerMethod(new TestControllerOnlyClassRoles());
        Assertions.assertEquals(
                Collections.singleton(SupervisorRole.class),
                CheckRolesHelper.getRoleAnnotations(handlerMethod)
        );
    }

    @Test
    public void getAnnotationsOnlyMethod() throws NoSuchMethodException {
        HandlerMethod handlerMethod = createHandlerMethod(new TestControllerOnlyMethodRoles());
        Assertions.assertEquals(
                Collections.singleton(OperatorRole.class),
                CheckRolesHelper.getRoleAnnotations(handlerMethod)
        );
    }

    @Test
    public void getAnnotationsClassAndMethod() throws NoSuchMethodException {
        HandlerMethod handlerMethod = createHandlerMethod(new TestControllerWithClassAndMethodRoles());
        Assertions.assertEquals(
                Sets.newHashSet(SupervisorRole.class, OperatorRole.class),
                CheckRolesHelper.getRoleAnnotations(handlerMethod)
        );
    }

    @Test
    public void errorControllerWithoutRoles() throws NoSuchMethodException {
        HandlerMethod handlerMethod = createHandlerMethod(new TestErrorControllerWithoutRoles());
        checkRolePresent(NO_ROLES, handlerMethod);
        checkRolePresent(ADMIN_ROLE, handlerMethod);
        checkRolePresent(OPERATOR_ROLE, handlerMethod);
        checkRolePresent(SUPERVISOR_ROLE, handlerMethod);
    }

    @Test
    public void controllerOnlyClassRoles() throws NoSuchMethodException {
        HandlerMethod handlerMethod = createHandlerMethod(new TestControllerOnlyClassRoles());
        checkRoleAbsent(NO_ROLES, handlerMethod);
        checkRolePresent(ADMIN_ROLE, handlerMethod);
        checkRoleAbsent(OPERATOR_ROLE, handlerMethod);
        checkRolePresent(SUPERVISOR_ROLE, handlerMethod);
    }

    @Test
    public void controllerOnlyMethodRoles() throws NoSuchMethodException {
        HandlerMethod handlerMethod = createHandlerMethod(new TestControllerOnlyMethodRoles());
        checkRoleAbsent(NO_ROLES, handlerMethod);
        checkRolePresent(ADMIN_ROLE, handlerMethod);
        checkRolePresent(OPERATOR_ROLE, handlerMethod);
        checkRoleAbsent(SUPERVISOR_ROLE, handlerMethod);
    }

    @Test
    public void controllerWithClassAndMethodRoles() throws NoSuchMethodException {
        HandlerMethod handlerMethod = createHandlerMethod(new TestControllerWithClassAndMethodRoles());
        checkRoleAbsent(NO_ROLES, handlerMethod);
        checkRolePresent(ADMIN_ROLE, handlerMethod);
        checkRolePresent(OPERATOR_ROLE, handlerMethod);
        checkRolePresent(SUPERVISOR_ROLE, handlerMethod);
    }

    @Test
    public void genericOperatorRole() throws NoSuchMethodException {
        HandlerMethod handlerMethod = createHandlerMethod(new TestControllerGenericOperator());
        Assertions.assertEquals(
                Sets.newHashSet(
                        ExternalOperatorRole.class,
                        OperatorRole.class,
                        FirstLineOperatorRole.class,
                        SeniorOperatorRole.class),
                CheckRolesHelper.getRoleAnnotations(handlerMethod)
        );
    }

    private void checkRolePresent(List<String> userRoles, HandlerMethod handlerMethod) {
        Assertions.assertTrue(
                CheckRolesHelper.hasRole(userRoles, CheckRolesHelper.getRoleAnnotations(handlerMethod))
        );
    }

    private void checkRoleAbsent(List<String> userRoles, HandlerMethod handlerMethod) {
        Assertions.assertFalse(
                CheckRolesHelper.hasRole(userRoles, CheckRolesHelper.getRoleAnnotations(handlerMethod))
        );
    }

    private HandlerMethod createHandlerMethod(Object bean) throws NoSuchMethodException {
        return new HandlerMethod(bean, bean.getClass().getMethod("method"));
    }
}
