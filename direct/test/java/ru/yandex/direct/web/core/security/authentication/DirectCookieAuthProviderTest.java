package ru.yandex.direct.web.core.security.authentication;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.web.auth.blackbox.BlackboxCookieAuth;
import ru.yandex.direct.web.core.security.authentication.exception.BlackboxCookieAuthenticationException;
import ru.yandex.direct.web.core.security.authentication.exception.DirectAuthenticationServiceException;
import ru.yandex.inside.passport.blackbox.protocol.BlackboxClientException;

public class DirectCookieAuthProviderTest {
    private static final String NETWORKS_CONFIG_URL =
            "classpath:///ru/yandex/direct/web/core/security/authentication/network-config.unittest.json";
    private static final String MANAGER_LOGIN = "manager-login";
    private static final String CLIENT_LOGIN = "client-login";
    private static final long UID_MANAGER = 12345678L;
    private static final ClientId CLIENT_ID_MANAGER = ClientId.fromLong(12345678L);
    private static final long UID_CLIENT = 123456789L;
    private static final ClientId CLIENT_ID_CLIENT = ClientId.fromLong(123456789L);
    private static final RbacRole ROLE = RbacRole.MANAGER;

    private static final ru.yandex.direct.core.entity.user.model.User MANAGER_USER =
            new ru.yandex.direct.core.entity.user.model.User()
                    .withUid(UID_MANAGER)
                    .withLogin(MANAGER_LOGIN)
                    .withClientId(CLIENT_ID_MANAGER)
                    .withAllowedIps("")
                    .withStatusBlocked(false)
                    .withStatusEasy(false)
                    .withPassportKarma(0L)
                    .withRole(ROLE)
                    .withCaptchaFreq(0L)
                    .withAutobanned(false);

    private static final ru.yandex.direct.core.entity.user.model.User CLIENT_USER =
            new ru.yandex.direct.core.entity.user.model.User()
                    .withUid(UID_CLIENT)
                    .withLogin(CLIENT_LOGIN)
                    .withClientId(CLIENT_ID_CLIENT)
                    .withAllowedIps("")
                    .withStatusBlocked(false)
                    .withStatusEasy(false)
                    .withPassportKarma(0L)
                    .withRole(RbacRole.CLIENT)
                    .withCaptchaFreq(0L)
                    .withAutobanned(false);

    private DirectWebAuthRequest directWebAuthRequest;


    private DirectCookieAuthProvider testingProvider;

    private RbacService rbacService;
    private UserService userService;
    private ClientService clientService;
    private BlackboxCookieAuth authentication;

    @Before
    public void prepare() throws BlackboxClientException, UnknownHostException {
        directWebAuthRequest = new DirectWebAuthRequest(InetAddress.getByName("localhost"), CLIENT_LOGIN, null);
        authentication = Mockito.mock(BlackboxCookieAuth.class);
        Mockito.when(authentication.getUid()).thenReturn(UID_MANAGER);
        Mockito.when(authentication.getPrincipal()).thenReturn(MANAGER_LOGIN);
        Mockito.when(authentication.getName()).thenReturn(MANAGER_LOGIN);
        Mockito.when(authentication.isHostedPdd()).thenReturn(false);
        userService = Mockito.mock(UserService.class);
        Mockito.when(userService.getUser(UID_MANAGER)).thenReturn(MANAGER_USER);
        Mockito.when(userService.getUser(UID_CLIENT)).thenReturn(CLIENT_USER);
        Mockito.when(userService.getUidByLogin(CLIENT_LOGIN)).thenReturn(UID_CLIENT);
        clientService = Mockito.mock(ClientService.class);
        rbacService = Mockito.mock(RbacService.class);
        testingProvider = new DirectCookieAuthProvider(userService, rbacService,
                new NetAcl(LiveResourceFactory.get(NETWORKS_CONFIG_URL).getContent()), clientService) {
            @Override
            protected DirectWebAuthRequest getWebAuthRequest(boolean isInternalRole) {
                return directWebAuthRequest;
            }
        };
    }

    @Test
    public void returnsValidAuthentication() throws UnknownHostException {
        DirectAuthentication expectedAuth = new DirectAuthentication(MANAGER_USER, CLIENT_USER, null, null);
        Authentication actualAuth = testingProvider.authenticate(authentication);
        Assert.assertThat("invalid authentication object returned", actualAuth,
                BeanDifferMatcher.beanDiffer(expectedAuth));
    }

    @Test
    public void callsUserService() {
        testingProvider.authenticate(authentication);
        Mockito.verify(userService).getUser(UID_MANAGER);
    }

    @Test(expected = DirectAuthenticationServiceException.class)
    public void throwsServiceExceptionWhenRbacFails() {
        Mockito.when(userService.getUser(UID_MANAGER)).thenThrow(new IllegalArgumentException());
        testingProvider.authenticate(authentication);
    }

    @Test
    public void yandexAgencyClient_hasPermissionsForInternalRole() {
        Mockito.when(clientService.isYaAgencyClient(CLIENT_ID_CLIENT)).thenReturn(true);
        testingProvider.authenticate(authentication);
    }

    @Test(expected = BlackboxCookieAuthenticationException.class)
    public void yandexAgencyClient_hasNoPermissions() {
        Mockito.when(clientService.isYaAgencyClient(CLIENT_ID_CLIENT)).thenReturn(true);
        Mockito.when(userService.getUser(UID_MANAGER)).thenReturn(CLIENT_USER);
        testingProvider.authenticate(authentication);
    }
}
