package ru.yandex.direct.api.v5.security;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.net.InetAddresses;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockSettings;
import org.mockito.Mockito;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.direct.api.v5.security.exception.AccessToApiDeniedException;
import ru.yandex.direct.api.v5.security.exception.UnknownUserException;
import ru.yandex.direct.api.v5.security.internal.DirectApiInternalAuthProvider;
import ru.yandex.direct.api.v5.security.internal.DirectApiInternalAuthRequest;
import ru.yandex.direct.api.v5.security.utils.ApiUserMockBuilder;
import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.service.ApiUserService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.tvm.TvmIntegration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.api.v5.units.UseOperatorUnitsMode.FALSE;

public class DirectApiInternalAuthProviderAuthenticateTest {
    private static final String NETWORKS_CONFIG_URL =
            "classpath:///ru/yandex/direct/api/v5/security/network-config.apiv5.unittest.json";
    private static final MockSettings MOCK_SETTINGS = withSettings().serializable();

    private static final String APPLICATION_ID = "123abc";


    private static final String USER_API_ALLOWED_IPS = "14.14.14.14-14.14.14.20";
    //с этого адреса будем ходить в API - и должно получиться (входит в USER_API_ALLOWED_IPS)
    private static final String USER_WITH_API_ALLOWED_IPS_REMOTE_IP = "14.14.14.19";
    //с этого тоже будем ходить в APi - но должны получить отказ (не входит в USER_API_ALLOWED_IPS)
    private static final String API_ALLOWED_IPS_NONTRUSTED_IP = "15.15.15.15";

    private static final ApiUser SUPER = userMockBuilder("yndx-user-super", 1, 1, RbacRole.SUPER).build();
    private static final ApiUser SIMPLE_CLIENT = userMockBuilder("simple-client", 2, 2, RbacRole.CLIENT).build();
    private static final ApiUser AGENCY = userMockBuilder("agency", 3, 3, RbacRole.AGENCY).build();
    private static final ApiUser AGENCY_CHIEF = userMockBuilder("agency-chief", 4, 3, RbacRole.AGENCY).build();
    private static final ApiUser SUBCLIENT = userMockBuilder("subclient", 5, 4, RbacRole.CLIENT).build();
    private static final ApiUser SUBCLIENT_CHIEF = userMockBuilder("subclient-chief", 6, 4, RbacRole.CLIENT).build();

    private static final ApiUser SUPER2_BLOCKED = userMockBuilder("yndx-user2-super", 7, 5, RbacRole.SUPER)
            .withStatusBlocked(true).build();

    private static final ApiUser SUBCLIENT_WITH_API_ALLOWED_IPS =
            userMockBuilder("yndx-user-allowed-ips", 8, 6, RbacRole.CLIENT).
                    withApiAllowedIps(USER_API_ALLOWED_IPS).build();

    private static final ApiUser BRAND_CHIEF = userMockBuilder("brand-chief", 9, 7, RbacRole.CLIENT).build();
    private static final InetAddress USER_IP = InetAddresses.forString("12.12.12.12");

    private final List<ApiUser> users = Arrays.asList(SUPER,
            SIMPLE_CLIENT,
            AGENCY, AGENCY_CHIEF,
            SUBCLIENT, SUBCLIENT_CHIEF,
            SUPER2_BLOCKED,
            SUBCLIENT_WITH_API_ALLOWED_IPS,
            BRAND_CHIEF
    );
    private RbacService rbacServiceMock;
    private ApiUserService apiUserServiceMock;
    private UserService userServiceMock;
    private DirectApiInternalAuthProvider authProvider;
    private ClientService clientService;
    private NetAcl netAcl;
    private BlackboxClient blackboxClient;
    private TvmIntegration tvmIntegration;

    private static ApiUserMockBuilder userMockBuilder(String login, long uid, long clientId, RbacRole role) {
        return new ApiUserMockBuilder(login, uid, clientId, role, MOCK_SETTINGS);
    }

    @Before
    public void setUp() {
        rbacServiceMock = mock(RbacService.class, MOCK_SETTINGS);
        clientService = mock(ClientService.class);
        tvmIntegration = mock(TvmIntegration.class);
        when(rbacServiceMock.getChief(anyLong()))
                .then(x -> {
                    ApiUser user = getUser((Long) x.getArguments()[0]);

                    if (user == AGENCY) {
                        return AGENCY_CHIEF.getUid();
                    } else if (user == SUBCLIENT) {
                        return SUBCLIENT_CHIEF.getUid();
                    } else {
                        return user.getUid();
                    }
                });
        when(rbacServiceMock.isOwner(anyLong(), anyLong()))
                .then(x -> {
                    ApiUser operator = getUser((Long) x.getArguments()[0]);
                    ApiUser client = getUser((Long) x.getArguments()[1]);

                    ApiUser operatorChief = apiUserServiceMock.getChiefRepFor(operator);
                    ApiUser clientChief = apiUserServiceMock.getChiefRepFor(client);

                    return operator == client
                            || operator.getRole() == RbacRole.SUPER
                            || operatorChief == clientChief
                            || operatorChief == AGENCY_CHIEF && clientChief == SUBCLIENT_CHIEF;
                });
        when(rbacServiceMock.isUnderAgency(anyLong()))
                .then(x -> {
                    ApiUser user = getUser((Long) x.getArguments()[0]);
                    ApiUser clientChief = apiUserServiceMock.getChiefRepFor(user);
                    return clientChief == SUBCLIENT_CHIEF;
                });

        userServiceMock = mock(UserService.class, MOCK_SETTINGS);
        when(userServiceMock.getUidByLogin(anyString()))
                .then(x -> {
                    String login = (String) x.getArguments()[0];
                    try {
                        return getUser(login).getUid();
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                });

        apiUserServiceMock = mock(ApiUserService.class, MOCK_SETTINGS);
        when(apiUserServiceMock.getUser(anyLong()))
                .then(x -> getUser((Long) x.getArguments()[0]));
        when(apiUserServiceMock.getChiefRepFor(any(ApiUser.class)))
                .then(x -> {
                    ApiUser u = (ApiUser) x.getArguments()[0];
                    return getUser(rbacServiceMock.getChief(u.getUid()));
                });
        when(apiUserServiceMock.getBrandChiefRepFor(any(ApiUser.class)))
                .thenReturn(null);
        netAcl = new NetAcl(LiveResourceFactory.get(NETWORKS_CONFIG_URL).getContent());
        blackboxClient = mock(BlackboxClient.class);
        authProvider = new DirectApiInternalAuthProvider(
                apiUserServiceMock,
                userServiceMock,
                netAcl,
                clientService,
                EnvironmentType.PRODUCTION,
                blackboxClient,
                tvmIntegration);
    }

    @Test(expected = UnknownUserException.class)
    public void operatorIsUnknownUser() {
        Mockito.reset(apiUserServiceMock);
        when(apiUserServiceMock.getUser(anyLong())).thenReturn(null);
        tryAuth(SUBCLIENT, SUBCLIENT.getLogin(), null);
    }

    @Test
    public void superWorksWell() {
        assertThat(tryAuth(SUPER, SUBCLIENT.getLogin(), null),
                authMatcher(SUPER, SUPER, SUBCLIENT, SUBCLIENT_CHIEF));
    }

    @Test(expected = AccessToApiDeniedException.class)
    public void nonTrustedIpFailsForInternalRole() {
        tryAuthFromExternalIp(SUPER, SUBCLIENT.getLogin(), null, InetAddresses.forString("13.13.13.13"));
    }

    @Test(expected = AccessToApiDeniedException.class)
    public void allowedIpsFailureTest() {
        tryAuthFromExternalIp(SUBCLIENT_WITH_API_ALLOWED_IPS, SUBCLIENT_WITH_API_ALLOWED_IPS.getLogin(), null,
                InetAddresses.forString(API_ALLOWED_IPS_NONTRUSTED_IP));
    }

    @Test
    public void allowedIpsSuccessTest() {
        tryAuthFromExternalIp(SUBCLIENT_WITH_API_ALLOWED_IPS, SUBCLIENT_WITH_API_ALLOWED_IPS.getLogin(), null,
                InetAddresses.forString(USER_WITH_API_ALLOWED_IPS_REMOTE_IP));
    }

    @Test(expected = AccessToApiDeniedException.class)
    public void disabledSuperFails() {
        tryAuth(SUPER2_BLOCKED, SUBCLIENT.getLogin(), null);
    }

    @Test
    public void agencyWorksWellWithSubclient() {
        assertThat(tryAuth(AGENCY, SUBCLIENT.getLogin(), null),
                authMatcher(AGENCY, AGENCY_CHIEF, SUBCLIENT, SUBCLIENT_CHIEF));
    }

    @Test
    public void superCanUseFakeLoginForSubclientAtTesting() {
        authProvider = new DirectApiInternalAuthProvider(apiUserServiceMock,
                userServiceMock, netAcl, clientService, EnvironmentType.TESTING, blackboxClient, tvmIntegration);
        assertThat(tryAuth(SUPER, SUBCLIENT.getLogin(), SUBCLIENT.getLogin()),
                authMatcher(SUBCLIENT, SUBCLIENT_CHIEF, SUBCLIENT, SUBCLIENT_CHIEF));
    }

    @Test
    public void superCanUseFakeLoginForAgencyAtTesting() {
        authProvider = new DirectApiInternalAuthProvider(apiUserServiceMock,
                userServiceMock, netAcl, clientService, EnvironmentType.TESTING, blackboxClient, tvmIntegration);
        assertThat(tryAuth(SUPER, SUBCLIENT.getLogin(), AGENCY.getLogin()),
                authMatcher(AGENCY, AGENCY_CHIEF, SUBCLIENT, SUBCLIENT_CHIEF));
    }

    @Test(expected = AccessToApiDeniedException.class)
    public void yandexAgencyClient_hasNoPermissions() {
        when(clientService.isYaAgencyClient(SUBCLIENT.getClientId())).thenReturn(true);
        authProvider = new DirectApiInternalAuthProvider(apiUserServiceMock,
                userServiceMock, netAcl, clientService, EnvironmentType.TESTING, blackboxClient, tvmIntegration);
        tryAuth(SUPER, SUBCLIENT.getLogin(), AGENCY.getLogin());
    }

    @Test
    public void agencyCannotUseFakeLoginAtTesting() {
        authProvider = new DirectApiInternalAuthProvider(apiUserServiceMock,
                userServiceMock, netAcl, clientService, EnvironmentType.TESTING, blackboxClient, tvmIntegration);
        assertThat(tryAuth(AGENCY, SUBCLIENT.getLogin(), SUBCLIENT.getLogin()),
                authMatcher(AGENCY, AGENCY_CHIEF, SUBCLIENT, SUBCLIENT_CHIEF));
    }

    @Test
    public void superCanNotUseFakeLoginForSubclientAtProduction() {
        assertThat(tryAuth(SUPER, SUBCLIENT.getLogin(), SUBCLIENT.getLogin()),
                authMatcher(SUPER, SUPER, SUBCLIENT, SUBCLIENT_CHIEF));
    }

    @Test
    public void superCanNotUseFakeLoginForAgencyAtProduction() {
        assertThat(tryAuth(SUPER, SUBCLIENT.getLogin(), AGENCY.getLogin()),
                authMatcher(SUPER, SUPER, SUBCLIENT, SUBCLIENT_CHIEF));
    }

    @Test
    public void agencyCanNotUseFakeLoginAtProduction() {
        assertThat(tryAuth(AGENCY, SUBCLIENT.getLogin(), SUBCLIENT.getLogin()),
                authMatcher(AGENCY, AGENCY_CHIEF, SUBCLIENT, SUBCLIENT_CHIEF));
    }

    private DirectApiAuthentication tryAuth(ApiUser operator, String clientLogin, String fakeLogin) {
        return authProvider.checkClientLoginAccess(
                authProvider.checkApiAccessAllowed(createInternalAuthRequest(operator, clientLogin, fakeLogin)))
                .toDirectApiAuthentication();
    }

    private DirectApiAuthentication tryAuthFromExternalIp(ApiUser operator, String clientLogin, String fakeLogin,
                                                          InetAddress address) {
        return authProvider.checkClientLoginAccess(
                authProvider.checkApiAccessAllowed(createInternalAuthRequest(operator, clientLogin, fakeLogin,
                        address)))
                .toDirectApiAuthentication();
    }

    private BeanDifferMatcher<DirectApiAuthentication> authMatcher(ApiUser operator, ApiUser operatorChief,
                                                                   ApiUser client, ApiUser clientChief) {
        DirectApiAuthentication expectedAuth =
                new DirectApiAuthentication(operator, operatorChief, client, clientChief, false, null, APPLICATION_ID,
                        USER_IP);

        return beanDiffer(expectedAuth).useCompareStrategy(
                allFields()
                        .forFields(newPath("operator")).useMatcher(sameInstance(expectedAuth.getOperator()))
                        .forFields(newPath("chiefOperator")).useMatcher(sameInstance(expectedAuth.getChiefOperator()))
                        .forFields(newPath("subjectUser")).useMatcher(sameInstance(expectedAuth.getSubjectUser()))
                        .forFields(newPath("chiefSubclient")).useMatcher(sameInstance(expectedAuth.getChiefSubclient()))
        );
    }

    private DirectApiInternalAuthRequest createInternalAuthRequest(ApiUser operator, String clientLogin,
                                                                   String fakeLogin) {
        return createInternalAuthRequest(operator, clientLogin, fakeLogin, USER_IP);
    }

    private DirectApiInternalAuthRequest createInternalAuthRequest(ApiUser operator, String clientLogin,
                                                                   String fakeLogin, InetAddress clientIp) {
        DirectApiCredentials directApiCredentials =
                new DirectApiCredentials("api.direct.yandex.ru", clientIp,
                        "TOKEN", clientLogin, fakeLogin,
                        FALSE, false);

        return new DirectApiInternalAuthRequest(directApiCredentials, operator.getUid(),
                operator.getLogin(), APPLICATION_ID, null);
    }

    private ApiUser getUser(String login) {
        return getUser(u -> u.getLogin().equals(login));
    }

    private ApiUser getUser(long uid) {
        return getUser(u -> u.getUid().equals(uid));
    }

    private ApiUser getUser(Predicate<ApiUser> predicate) {
        return users.stream()
                .filter(predicate)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such user " + predicate));
    }
}
