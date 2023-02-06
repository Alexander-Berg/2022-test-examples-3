package ru.yandex.market.api.server.sec;

import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.server.LifecycleStatus;
import ru.yandex.market.api.server.LifecycleStatusService;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.AuthorizationType;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.exceptions.AccessDeniedException;
import ru.yandex.market.api.server.sec.exceptions.AuthInfoNotFoundException;
import ru.yandex.market.api.server.sec.exceptions.IllegalUserStatusException;
import ru.yandex.market.api.server.sec.oauth.annotation.AuthSecured;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.user.order.MarketUid;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithContext
@WithMocks
public class AuthorizationServiceBeanTest extends ContainerTestBase {

    private static final String VALID_UUID = "12345678901234567890123456789012";

    @Inject
    private AuthorizationServiceBean service;

    @AuthSecured
    public void allOptionalTestMethod() {
    }

    @AuthSecured(anyOf = {AuthType.OAUTH, AuthType.MUID})
    public void alternativeTestMethod() {
    }

    @AuthSecured(allOf = AuthType.OAUTH)
    public void oauthRequiredTestMethod() {
    }

    @Override
    public void setUp() {

        ContextHolder.get().setUserIp("0.0.0.1");

        Client client = createTestClient("test-client-id");
        ContextHolder.get().setClient(client);
        LifecycleStatusService.INSTANCE.setStatus(LifecycleStatus.RUNNING);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        LifecycleStatusService.INSTANCE.setStatus(LifecycleStatus.STARTING);
    }

    @Test
    public void shouldAllowAlternatives() throws Exception {
        setUser(null, new MarketUid(1), null, null);

        invokeMethod("alternativeTestMethod");
    }

    @Test
    public void shouldAllowNoUserOnAllOptional() throws Exception {
        setUser(null, null, null, null);
        invokeMethod("allOptionalTestMethod");
    }

    @Test
    public void shouldNotAllowEmptyOauthUser() throws Exception {
        setUser(null, new MarketUid(123), new Uuid(VALID_UUID), null);


        exception.expect(AuthInfoNotFoundException.class);
        exception.expectMessage(
            Matchers.allOf(
                Matchers.containsString("HAS_NO_AUTH_INFO"),
                Matchers.containsString("client ip: 0.0.0.1"),
                Matchers.containsString("OAUTH")
            )
        );

        invokeMethod("oauthRequiredTestMethod");
    }

    @Test
    public void shouldNotAllowEmptyUserOnAlternatives() throws Exception {
        setUser(null, null, null, null);

        exception.expect(AuthInfoNotFoundException.class);
        exception.expectMessage(
            Matchers.allOf(
                Matchers.containsString("HAS_NO_AUTH_INFO"),
                Matchers.containsString("client ip: 0.0.0.1"),
                Matchers.containsString("OAUTH|MUID")
            )
        );

        invokeMethod("alternativeTestMethod");
    }

    @Test
    public void shouldNotAllowOauthUserWithoutScopes() throws Exception {
        OauthUser oauthUser = new OauthUser(1);
        oauthUser.setScopes(new String[0]);
        setUser(oauthUser, new MarketUid(1), new Uuid(VALID_UUID), null);

        exception.expect(AccessDeniedException.class);
        exception.expectMessage("Access denied; ");

        invokeMethod("oauthRequiredTestMethod");
    }

    @Test
    public void shouldThrowIfInvalidUserStatusOauthRequired() throws Exception {
        OauthUser oauthUser = new OauthUser(1);
        oauthUser.setStatus(OauthUser.Status.DISABLED);
        setUser(oauthUser, new MarketUid(1), new Uuid(VALID_UUID), null);


        exception.expect(IllegalUserStatusException.class);
        exception.expectMessage(Matchers.containsString("Illegal user status 'DISABLED'"));

        invokeMethod("oauthRequiredTestMethod");
    }

    @Test
    public void shouldNotThrowIfInvalidUserStatusOauthNotRequired() throws Exception {
        OauthUser oauthUser = new OauthUser(1);
        oauthUser.setStatus(OauthUser.Status.DISABLED);
        setUser(oauthUser, new MarketUid(1), new Uuid(VALID_UUID), null);

        invokeMethod("allOptionalTestMethod");
    }

    @Test
    public void shouldThrowIfNoScopesForUserOauthRequired() throws Exception {
        OauthUser oauthUser = new OauthUser(1);
        oauthUser.setScopes(new String[0]);
        setUser(oauthUser, null, new Uuid(VALID_UUID), null);

        exception.expect(AccessDeniedException.class);
        exception.expectMessage("Access denied; ");

        invokeMethod("oauthRequiredTestMethod");
    }

    @Test
    public void shouldNotThrowIfNoScopesForUserOauthNotRequired() throws Exception {
        OauthUser oauthUser = new OauthUser(1);
        oauthUser.setScopes(new String[0]);
        setUser(oauthUser, null, new Uuid(VALID_UUID), null);

        invokeMethod("allOptionalTestMethod");
    }

    private Client createTestClient(String clientId) {
        Client client = new Client();
        client.setStatus(Client.Status.ENABLED);
        client.setId(clientId);
        client.setAuthorizationType(AuthorizationType.CONTENT_API);
        client.setType(Client.Type.INTERNAL);
        client.setRawAllowedIps("");
        return client;
    }

    private void invokeMethod(String methodName) throws NoSuchMethodException {
        HttpServletRequest request = MockRequestBuilder.start().build();
        Method method = this.getClass().getMethod(methodName);

        service.apply(request, method);
    }

    private void setUser(OauthUser oauthUser, MarketUid marketUid, Uuid uuid, YandexUid yandexUid) {
        ContextHolder.get().setUser(new User(oauthUser, marketUid, uuid, yandexUid));
    }

}
