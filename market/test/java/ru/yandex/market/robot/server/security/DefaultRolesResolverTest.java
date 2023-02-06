package ru.yandex.market.robot.server.security;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.gwt.shared.User;
import ru.yandex.market.robot.shared.models.Role;
import ru.yandex.market.security.SecManager;

/**
 * @author inenakhov
 */
public class DefaultRolesResolverTest extends Assert {
    private User someUser = new User("login", 1, "role", System.currentTimeMillis());

    private class MainSecManagerMock implements SecManager {
        private Operation allowedOperation;

        MainSecManagerMock(Operation allowedOperation) {
            this.allowedOperation = allowedOperation;
        }

        @Override
        public boolean canDo(String operationName, Object data) {
            return allowedOperation.toString().equals(operationName);
        }

        @Override
        public boolean hasAuthority(String authority, String param, Object data) {
            return false;
        }
    }

    @Test
    public void resolveRoleAdmin() throws Exception {
        MainSecManagerMock adminOperationIsAllowed = new MainSecManagerMock(Operation.ROBOT_ADMIN);
        DefaultRolesResolver resolver = new DefaultRolesResolver(adminOperationIsAllowed);
        assertEquals(Role.ADMIN, resolver.resolveRole(someUser));
    }

    @Test
    public void resolveRoleUser() throws Exception {
        MainSecManagerMock userOperationIsAllowed = new MainSecManagerMock(Operation.ROBOT_USER);
        DefaultRolesResolver resolver = new DefaultRolesResolver(userOperationIsAllowed);
        assertEquals(Role.USER, resolver.resolveRole(someUser));
    }

    @Test
    public void resolveRoleOperator() throws Exception {
        MainSecManagerMock operatorRoleIsAllowed = new MainSecManagerMock(Operation.ROBOT_OPERATOR);
        DefaultRolesResolver resolver = new DefaultRolesResolver(operatorRoleIsAllowed);
        assertEquals(Role.OPERATOR, resolver.resolveRole(someUser));
    }

    @Test
    public void resolveRolePartnerContentAdmin() throws Exception {
        MainSecManagerMock operatorRoleIsAllowed = new MainSecManagerMock(Operation.PARTNER_CONTENT_ADMIN);
        DefaultRolesResolver resolver = new DefaultRolesResolver(operatorRoleIsAllowed);
        assertEquals(Role.PARTNER_CONTENT_ADMIN, resolver.resolveRole(someUser));
    }
}