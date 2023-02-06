package ru.yandex.direct.api.v5.security;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import ru.yandex.direct.api.v5.security.exception.AccessToApiDeniedException;
import ru.yandex.direct.api.v5.security.exception.NotClientInClientLoginException;
import ru.yandex.direct.api.v5.security.exception.UnknownLoginInClientLoginOrFakeLoginException;
import ru.yandex.direct.api.v5.security.utils.ApiUserMockBuilder;
import ru.yandex.direct.api.v5.ws.annotation.ServiceType;
import ru.yandex.direct.core.entity.user.model.ApiEnabled;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.security.AccessDeniedException;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.rbac.UserPerminfo;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author egorovmv
 */
public final class DirectApiAuthorizationHelperTest {
    private static final ApiUser NOT_BLOCKED_CLIENT = new ApiUserMockBuilder("client", 1, 1, RbacRole.CLIENT)
            .withStatusBlocked(false)
            .build();
    private static final ApiUser BLOCKED_CLIENT = new ApiUserMockBuilder("client", 1, 1, RbacRole.CLIENT)
            .withStatusBlocked(true)
            .build();
    private static final String APPLICATION_ID = "123abc";

    private static final String API_GET_OPERATION = "get";
    private static final String API_NOT_GET_OPERATION = "update";

    private static final ApiUser SIMPLE_CLIENT = new ApiUserMockBuilder("simple-client", 2, 2, RbacRole.CLIENT).build();
    private static final ApiUser AGENCY = new ApiUserMockBuilder("agency", 3, 3, RbacRole.AGENCY).build();
    private static final ApiUser AGENCY_CHIEF = new ApiUserMockBuilder("agency-chief", 4, 3, RbacRole.AGENCY).build();
    private static final ApiUser SUBCLIENT = new ApiUserMockBuilder("subclient", 5, 4, RbacRole.CLIENT).build();
    private static final ApiUser SUBCLIENT_CHIEF =
            new ApiUserMockBuilder("subclient-chief", 6, 4, RbacRole.CLIENT).build();

    private static final ApiUser DISABLED_SUBCLIENT = new ApiUserMockBuilder("subclient", 5, 4, RbacRole.CLIENT)
            .withApiEnabled(ApiEnabled.NO).build();
    private static final ApiUser DISABLED_SUBCLIENT_CHIEF =
            new ApiUserMockBuilder("subclient-chief", 6, 4, RbacRole.CLIENT)
                    .withApiEnabled(ApiEnabled.NO).build();
    private static final ApiUser SUPER = new ApiUserMockBuilder("super", 7, 8, RbacRole.SUPER).build();
    private static final long FREELANCER_UID = 9;
    private static final ApiUser FREELANCER =
            new ApiUserMockBuilder("freelancer", FREELANCER_UID, 10, RbacRole.CLIENT).build();


    private static DirectApiAuthentication newClientAuthentication(ApiUser client) {
        return new DirectApiAuthentication(client, client, client, client, true, null, "", null);
    }

    private static DirectApiAuthentication newAgencyWithSubclient(ApiUser agency, ApiUser subclient) {
        return new DirectApiAuthentication(agency, agency, subclient, subclient, null);
    }

    private static DirectApiAuthentication newAuth(ApiUser operator, ApiUser chiefOperator,
                                                   ApiUser subclient, ApiUser chiefSubclient, boolean clientLoginIsEmpty) {
        return new DirectApiAuthentication(operator, chiefOperator, subclient, chiefSubclient, clientLoginIsEmpty,
                null, "", null);
    }

    private DirectApiAuthorizationHelper authorizationHelper;
    private RbacService rbacService;

    @Before
    public void before() {
        rbacService = mock(RbacService.class);
        when(rbacService.isOwner(anyLong(), anyLong())).thenReturn(true);

        UserPerminfo simpleUserPerminfo = mock(UserPerminfo.class);
        when(simpleUserPerminfo.canHaveRelationship()).thenReturn(false);
        when(rbacService.getUserPermInfo(anyLong())).thenReturn(simpleUserPerminfo);

        UserPerminfo freelancerUserPerminfo = mock(UserPerminfo.class);
        when(freelancerUserPerminfo.canHaveRelationship()).thenReturn(true);
        when(rbacService.getUserPermInfo(ArgumentMatchers.eq(FREELANCER_UID))).thenReturn(freelancerUserPerminfo);

        authorizationHelper = new DirectApiAuthorizationHelper(rbacService);
    }

    @Test
    public void testSimpleClient() {
        DirectApiAuthentication authentication = newClientAuthentication(NOT_BLOCKED_CLIENT);
        authorizationHelper.authorize(authentication, ServiceType.CLIENT, API_NOT_GET_OPERATION);
    }

    @Test
    public void testAgencyWithNotBlockedSubclientAndGetMethod() {
        DirectApiAuthentication authentication = newAgencyWithSubclient(AGENCY, NOT_BLOCKED_CLIENT);
        authorizationHelper.authorize(authentication, ServiceType.CLIENT, API_GET_OPERATION);
    }

    @Test
    public void testAgencyWithNotBlockedSubclientAndNotGetMethod() {
        DirectApiAuthentication authentication = newAgencyWithSubclient(AGENCY, NOT_BLOCKED_CLIENT);
        authorizationHelper.authorize(authentication, ServiceType.CLIENT, API_NOT_GET_OPERATION);
    }

    @Test
    public void testFreelancerWithNotBlockedSubclientAndNotGetMethod() {
        DirectApiAuthentication authentication = newAgencyWithSubclient(FREELANCER, NOT_BLOCKED_CLIENT);
        authorizationHelper.authorize(authentication, ServiceType.CLIENT, API_NOT_GET_OPERATION);
    }

    @Test
    public void testAgencyWithBlockedSubclientAndGetMethod() {
        DirectApiAuthentication authentication = newAgencyWithSubclient(AGENCY, BLOCKED_CLIENT);
        authorizationHelper.authorize(authentication, ServiceType.CLIENT, API_GET_OPERATION);
    }

    @Test(expected = AccessToApiDeniedException.class)
    public void testAgencyWithBlockedSubclientAndNotGetMethod() {
        DirectApiAuthentication authentication = newAgencyWithSubclient(AGENCY, BLOCKED_CLIENT);
        authorizationHelper.authorize(authentication, ServiceType.CLIENT, API_NOT_GET_OPERATION);
    }

    @Test(expected = AccessToApiDeniedException.class)
    public void subclientCanNotAccessWhenChiefSubclientHasApiDisabled() throws Exception {

        doCheckClientService(DISABLED_SUBCLIENT, DISABLED_SUBCLIENT_CHIEF, DISABLED_SUBCLIENT, DISABLED_SUBCLIENT_CHIEF,
                false);
    }

    @Test
    public void agencyCanAccessWhenChiefSubclientHasApiDisabled() throws Exception {

        doCheckClientService(AGENCY, AGENCY_CHIEF, DISABLED_SUBCLIENT, DISABLED_SUBCLIENT_CHIEF, false);
    }

    @Test
    public void internalUsersCanAccessToWhenChiefSubclientHasApiDisabled() throws Exception {
        doCheckClientService(SUPER, SUPER, DISABLED_SUBCLIENT, DISABLED_SUBCLIENT_CHIEF, false);
    }

    @Test
    public void clientCanUseOwnedClientLogin() throws Exception {
        doCheckClientService(SUBCLIENT, SUBCLIENT_CHIEF, SUBCLIENT, SUBCLIENT_CHIEF, false);
    }

    @Test
    public void clientCanOmitClientLogin() throws Exception {
        doCheckClientService(SUBCLIENT, SUBCLIENT_CHIEF, SUBCLIENT, SUBCLIENT_CHIEF, true);
    }

    @Test
    public void clientCanUseForeignClientLogin() throws Exception {
        assertThatCode(
                () -> doCheckClientService(SUBCLIENT,
                        SUBCLIENT_CHIEF,
                        SIMPLE_CLIENT,
                        SIMPLE_CLIENT, false))
                .doesNotThrowAnyException();
    }

    @Test(expected = UnknownLoginInClientLoginOrFakeLoginException.class)
    public void agencyCannotUseForeignClientLogin() throws Exception {
        when(rbacService.isOwner(anyLong(), anyLong())).thenReturn(false);
        doCheckClientService(AGENCY, AGENCY_CHIEF, SIMPLE_CLIENT, SIMPLE_CLIENT, false);
    }

    @Test(expected = NotClientInClientLoginException.class)
    public void agencyCanNotAuthWithoutClientLoginOnClientService() throws Exception {
        doCheckClientService(AGENCY, AGENCY_CHIEF, AGENCY, AGENCY_CHIEF, true);
    }

    @Test(expected = NotClientInClientLoginException.class)
    public void subclientMustHasClientRoleOnClientService() throws Exception {
        doCheckClientService(AGENCY, AGENCY_CHIEF, AGENCY, AGENCY_CHIEF, false);
    }

    @Test
    public void clientDoesNotHaveAccessToAgencyService() {
        assertThatThrownBy(() -> doCheckAgencyService(SUBCLIENT, SUBCLIENT_CHIEF, SUBCLIENT, SUBCLIENT_CHIEF, true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void freelancerCanOmitClientLogin() {
        assertThatCode(() -> doCheckAgencyService(FREELANCER, FREELANCER, FREELANCER, FREELANCER, false))
                .doesNotThrowAnyException();
    }

    @Test
    public void agencyCanOmitClientLogin() throws Exception {
        doCheckAgencyService(AGENCY, AGENCY_CHIEF, AGENCY, AGENCY_CHIEF, false);
    }

    private void doCheckClientService(ApiUser operator, ApiUser chiefOperator, ApiUser subclient,
                                      ApiUser chiefSubclient, boolean clientLoginIsEmpty) throws Exception {
        newAuth(AGENCY, AGENCY_CHIEF, DISABLED_SUBCLIENT, DISABLED_SUBCLIENT_CHIEF, false);

        DirectApiAuthentication auth = new DirectApiAuthentication(
                operator, chiefOperator, subclient, chiefSubclient, clientLoginIsEmpty, null, "", null);
        authorizationHelper.authorize(auth, ServiceType.CLIENT, API_NOT_GET_OPERATION);
    }


    private void doCheckAgencyService(ApiUser operator, ApiUser chiefOperator, ApiUser subclient,
                                      ApiUser chiefSubclient, boolean clientLoginIsEmpty) throws Exception {
        DirectApiAuthentication auth = new DirectApiAuthentication(
                operator, chiefOperator, subclient, chiefSubclient, clientLoginIsEmpty, null, "", null);
        authorizationHelper.authorize(auth, ServiceType.AGENCY, API_NOT_GET_OPERATION);
    }


}
