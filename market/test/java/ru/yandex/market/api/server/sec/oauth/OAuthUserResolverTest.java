package ru.yandex.market.api.server.sec.oauth;

import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.blackbox.BlackBoxService;
import ru.yandex.market.api.internal.blackbox.BlackBoxResolver;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.market.vendor.VendorApiService;
import ru.yandex.market.api.internal.market.vendor.domain.VendorUserPermission;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.OAuthToken;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.server.sec.exceptions.AuthInfoNotFoundException;
import ru.yandex.market.api.server.sec.exceptions.OAuthTokenWithException;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ClientTestUtil;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.concurrent.Pipelines;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author dimkarp93
 */
@WithContext
@WithMocks
public class OAuthUserResolverTest extends UnitTestBase {
    private OAuthUserResolver oAuthUserResolver;

    @Mock
    private BlackBoxService blackBoxService;

    @Mock
    private VendorApiService vendorApiService;

    @Mock
    private ClientHelper clientHelper;

    private MockClientHelper mockClientHelper;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        BlackBoxResolver resolver = new BlackBoxResolver(blackBoxService, blackBoxService);

        mockClientHelper = new MockClientHelper(clientHelper);
        oAuthUserResolver = new OAuthUserResolver(resolver, vendorApiService, false);
        ContextHolder.update(ctx -> ctx.setUserIp("0.0.0.0"));
        ContextHolder.update(ctx -> ctx.setRequest(MockRequestBuilder.start()
                .header("X-Forwarded-For-Y", "1.1.1.1")
                .build()));
    }


    @Test
    public void shouldNotAuthorizeUserWithoutVendorScope() {
        mockClientHelper.is(ClientHelper.Type.WHITE_APP_NEW, false);

        ClientTestUtil.clientOfType(Client.Type.VENDOR);
        clientOfVendorId(1L);
        when(vendorApiService.getVendorPermissions(any(OauthUser.class), anyLong()))
            .thenReturn(Pipelines.startWithValue(null));

        when(blackBoxService.getUser(any(), any(), anyBoolean())).thenReturn(
            Futures.newSucceededFuture(new OauthUser(1L)));

        OauthUser user = Futures.waitAndGet(
            oAuthUserResolver.resolve(
                new OAuthToken("123"),
                ContextHolder.get()
            )
        );
        OauthUser expectedUser = new OauthUser(1L);

        assertEquals(expectedUser.getUid(), user.getUid());
        assertArrayEquals(new String[]{}, user.getScopes());
    }

    @Test
    public void shouldAuthorizeVendorUser() {
        mockClientHelper.is(ClientHelper.Type.WHITE_APP_NEW, false);

        ClientTestUtil.clientOfType(Client.Type.VENDOR);
        clientOfVendorId(1L);

        when(blackBoxService.getUser(any(), any(), anyBoolean())).thenReturn(
            Futures.newSucceededFuture(new OauthUser(1L)));
        when(vendorApiService.getVendorPermissions(any(OauthUser.class), anyLong()))
            .thenReturn(Pipelines.startWithValue(EnumSet.of(VendorUserPermission.CHARACTERISTICS_READ)));


        OauthUser user = Futures.waitAndGet(
            oAuthUserResolver.resolve(
                new OAuthToken("123"),
                ContextHolder.get()
            )
        );
        OauthUser expectedUser = new OauthUser(1L);

        assertEquals(expectedUser.getUid(), user.getUid());
        assertArrayEquals(new String[]{"vnd:characteristics:read"}, user.getScopes());
    }

    @Test
    public void shouldNotResolveInvalidUser() {
        mockClientHelper.is(ClientHelper.Type.WHITE_APP_NEW, false);

        OauthUser oauthUser = new OauthUser(1L);
        oauthUser.setStatus(OauthUser.Status.INVALID);

        when(blackBoxService.getUser(any(), any(), anyBoolean())).thenReturn(
                Futures.newSucceededFuture(oauthUser));

        OauthUser result = Futures.waitAndGet(oAuthUserResolver.resolve(new OAuthToken("abc"), ContextHolder.get()));

        Assert.assertNull(result);
    }

    @Test
    public void shouldResolveValidUser() {
        mockClientHelper.is(ClientHelper.Type.WHITE_APP_NEW, false);

        OauthUser oauthUser = new OauthUser(1L);
        oauthUser.setStatus(OauthUser.Status.VALID);

        when(blackBoxService.getUser(any(), any(), anyBoolean())).thenReturn(
                Futures.newSucceededFuture(oauthUser));

        OauthUser result = Futures.waitAndGet(oAuthUserResolver.resolve(new OAuthToken("abc"), ContextHolder.get()));

        Assert.assertNotNull(result);
        Assert.assertEquals(1L, result.getUid());
        Assert.assertEquals(OauthUser.Status.VALID, result.getStatus());
    }

    @Test
    public void shouldResolveNeedResetUser() {
        mockClientHelper.is(ClientHelper.Type.WHITE_APP_NEW, false);

        OauthUser oauthUser = new OauthUser(1L);
        oauthUser.setStatus(OauthUser.Status.NEED_RESET);

        when(blackBoxService.getUser(any(), any(), anyBoolean())).thenReturn(
                Futures.newSucceededFuture(oauthUser));

        OauthUser result = Futures.waitAndGet(oAuthUserResolver.resolve(new OAuthToken("abc"), ContextHolder.get()));

        Assert.assertNotNull(result);
        Assert.assertEquals(1L, result.getUid());
        Assert.assertEquals(OauthUser.Status.NEED_RESET, result.getStatus());
    }

    @Test
    public void shouldFailInvalidUserMobileApp() {
        ClientTestUtil.clientOfType(Client.Type.MOBILE);

        OauthUser oauthUser = new OauthUser(1L);
        oauthUser.setStatus(OauthUser.Status.INVALID);

        when(blackBoxService.getUser(any(), any(), anyBoolean())).thenReturn(
                Futures.newSucceededFuture(oauthUser));

        exception.expect(OAuthTokenWithException.class);

        Futures.waitAndGet(oAuthUserResolver.resolve(new OAuthToken("abc"), ContextHolder.get()));
    }

    @Test
    public void shouldFailEmptyUserMobileApp() {
        ClientTestUtil.clientOfType(Client.Type.MOBILE);

        when(blackBoxService.getUser(any(), any(), anyBoolean())).thenReturn(
                Futures.newSucceededFuture(null));

        exception.expect(AuthInfoNotFoundException.class);

        Futures.waitAndGet(oAuthUserResolver.resolve(new OAuthToken("abc"), ContextHolder.get()));
    }

    @Test
    public void shouldNotFailValidUserMobileApp() {
        ClientTestUtil.clientOfType(Client.Type.MOBILE);

        OauthUser oauthUser = new OauthUser(1L);
        oauthUser.setStatus(OauthUser.Status.VALID);

        when(blackBoxService.getUser(any(), any(), anyBoolean())).thenReturn(
                Futures.newSucceededFuture(oauthUser));

        OauthUser result = Futures.waitAndGet(oAuthUserResolver.resolve(new OAuthToken("abc"), ContextHolder.get()));

        Assert.assertNotNull(result);
        Assert.assertEquals(1L, result.getUid());
        Assert.assertEquals(OauthUser.Status.VALID, result.getStatus());
    }

    @Test
    public void shouldNotFailNeedResetUserMobileApp() {
        ClientTestUtil.clientOfType(Client.Type.MOBILE);

        OauthUser oauthUser = new OauthUser(1L);
        oauthUser.setStatus(OauthUser.Status.NEED_RESET);

        when(blackBoxService.getUser(any(), any(), anyBoolean())).thenReturn(
                Futures.newSucceededFuture(oauthUser));

        OauthUser result = Futures.waitAndGet(oAuthUserResolver.resolve(new OAuthToken("abc"), ContextHolder.get()));

        Assert.assertNotNull(result);
        Assert.assertEquals(1L, result.getUid());
        Assert.assertEquals(OauthUser.Status.NEED_RESET, result.getStatus());
    }


    private static void clientOfVendorId(Long vendorId) {
        ContextHolder.update(ctx -> ctx.getClient().setVendorId(vendorId));
    }

}
