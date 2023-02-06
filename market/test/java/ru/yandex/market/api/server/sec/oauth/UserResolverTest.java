package ru.yandex.market.api.server.sec.oauth;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.controller.Parameters;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.LinkedAccount;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.sberlog.SberlogClient;
import ru.yandex.market.api.internal.sberlog.SberlogStatus;
import ru.yandex.market.api.server.context.ContextHelper;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.AuthorizationTokenExtractor;
import ru.yandex.market.api.server.sec.AuthorizationType;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.exceptions.AuthInfoNotFoundException;
import ru.yandex.market.api.server.sec.exceptions.SberlogException;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.concurrent.Pipelines;
import ru.yandex.market.api.util.httpclient.adapter.FuturePipeImpl;
import ru.yandex.market.sdk.userinfo.domain.AggregateUserInfo;
import ru.yandex.market.sdk.userinfo.domain.Error;
import ru.yandex.market.sdk.userinfo.domain.Options;
import ru.yandex.market.sdk.userinfo.domain.PassportError;
import ru.yandex.market.sdk.userinfo.domain.PassportInfo;
import ru.yandex.market.sdk.userinfo.domain.PassportResponse;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.domain.UidType;
import ru.yandex.market.sdk.userinfo.service.UserInfoService;

import static org.mockito.Matchers.any;

/**
 * @authror dimkarp93
 */
@WithMocks
@WithContext
public class UserResolverTest extends BaseTest {
    private UserResolver resolver;

    private HttpServletRequest request;

    @Mock
    private OAuthUserResolver oAuthUserResolver;
    @Mock
    private UserInfoService userInfoService;
    @Mock
    private Parameters.MarketUidParser muidParser;
    @Mock
    private Parameters.YandexuidParser yandexuidParser;
    @Mock
    private SberlogClient sberlogClient;
    @Mock
    private OAuthSecurityConfig oAuthSecurityConfig;

    @Override
    public void setUp() throws Exception {
        request = MockRequestBuilder.start()
                .header("X-User-Authorization", "SberLog: 123")
                .build();

        resolver = new UserResolver(
                oAuthUserResolver,
                userInfoService,
                sberlogClient,
                muidParser,
                yandexuidParser,
                new AuthorizationTokenExtractor(oAuthSecurityConfig)
        );

        Mockito.when(yandexuidParser.get(any()))
                .thenReturn(Result.newResult(null));

        Mockito.when(oAuthSecurityConfig.isAvailableType(Mockito.any(AuthorizationType.class)))
                .thenAnswer((invoke) -> {
                            AuthorizationType type = invoke.getArgumentAt(0, AuthorizationType.class);
                            if (ContextHelper.isMobile()) {
                                return AuthorizationType.OAuth == type || AuthorizationType.SberLog == type;
                            } else {
                                return AuthorizationType.OAuth == type;
                            }
                        }
                );

    }


    @Test
    public void sberlogSuccess() {
        ContextHolder.update(ctx -> {
            Client client = new Client();
            client.setType(Client.Type.MOBILE);
            ctx.setClient(client);
        });

        OauthUser user1 = new OauthUser(11L);

        Mockito.when(sberlogClient.getUser(any()))
                .thenReturn(Pipelines.startWithValue(Result.newResult(Collections.singletonList(user1))));

        User user = Futures.waitAndGet(resolver.getUser(request, null));
        Assert.assertThat(user.getOAuthUser().getUid(), Matchers.is(11L));
    }

    @Test
    public void sberlogFailed() {
        SberlogStatus sberlogStatus = new SberlogStatus();
        sberlogStatus.setText("blalba");

        Mockito.when(sberlogClient.getUser(any()))
                .thenReturn(Pipelines.startWithValue(Result.newError(sberlogStatus)));

        User user = Futures.waitAndGet(resolver.getUser(request, null));
        Assert.assertThat(user, Matchers.nullValue(User.class));
    }

    @Test(expected = SberlogException.class)
    public void sberlogIncorrectUser() {
        ContextHolder.update(ctx -> {
            Client client = new Client();
            client.setType(Client.Type.MOBILE);
            ctx.setClient(client);
        });

        OauthUser oauth = new OauthUser(1L);
        oauth.setErrorCode("1");
        oauth.setErrorMessage("sberlog_error");

        List<OauthUser> oauths = Collections.singletonList(oauth);

        Mockito.when(sberlogClient.getUser(any()))
                .thenReturn(Pipelines.startWithValue(Result.newResult(oauths)));

        User user = Futures.waitAndGet(resolver.getUser(request, null));
        Assert.assertThat(user, Matchers.nullValue(User.class));
    }


    @Test
    public void sberlogLinkedUser() {
        ContextHolder.update(ctx -> {
            Client client = new Client();
            client.setType(Client.Type.MOBILE);
            ctx.setClient(client);
        });

        OauthUser sberOAuth = new OauthUser(1L);
        sberOAuth.setFirstName("abc");
        sberOAuth.setLastName("def");
        sberOAuth.setLinkedAccount(LinkedAccount.of(1L, "123", true));

        List<OauthUser> oauths = Collections.singletonList(sberOAuth);

        PassportInfo passInfo = Mockito.mock(PassportInfo.class);
        Mockito.when(passInfo.getUid()).thenReturn(new Uid(45, UidType.PUID));
        Mockito.when(passInfo.getFirstName()).thenReturn(Optional.of("xyz"));
        Mockito.when(passInfo.getLastName()).thenReturn(Optional.of("wvt"));
        Mockito.when(passInfo.getDisplayName()).thenReturn(Optional.of("qwe"));
        Mockito.when(passInfo.getAvatar(any())).thenReturn(Optional.of("some-avatar"));

        List<AggregateUserInfo> infos = Collections.singletonList(new AggregateUserInfo(passInfo));

        Mockito.when(sberlogClient.getUser(any()))
                .thenReturn(Pipelines.startWithValue(Result.newResult(oauths)));

        ru.yandex.market.sdk.userinfo.util.Result<List<AggregateUserInfo>, Error> result =
                ru.yandex.market.sdk.userinfo.util.Result.ofValue(infos);

        Mockito.when(
            userInfoService.getUserInfoRawAsync(
                Mockito.eq(Collections.singleton(123L)),
                any(Options.class))
        ).thenReturn(new FuturePipeImpl<>(Pipelines.startWithValue(result)));

        OauthUser user = Futures.waitAndGet(resolver.getUser(request, null)).getOAuthUser();

        Assert.assertThat(user.getUid(), Matchers.is(45L));
        Assert.assertThat(user.getFirstName(), Matchers.is("xyz"));
        Assert.assertThat(user.getLastName(), Matchers.is("wvt"));
        Assert.assertThat(user.getName(), Matchers.is("qwe"));
        Assert.assertThat(user.getStatus(), Matchers.is(OauthUser.Status.VALID));
    }

    @Test(expected = AuthInfoNotFoundException.class)
    public void sberlogLinkedUserFailed() {
        ContextHolder.update(ctx -> {
            Client client = new Client();
            client.setType(Client.Type.MOBILE);
            ctx.setClient(client);
        });

        OauthUser sberOAuth = new OauthUser(1L);
        sberOAuth.setFirstName("abc");
        sberOAuth.setLastName("def");
        sberOAuth.setLinkedAccount(LinkedAccount.of(1L, "123", true));

        List<OauthUser> oauths = Collections.singletonList(sberOAuth);

        Mockito.when(sberlogClient.getUser(any()))
                .thenReturn(Pipelines.startWithValue(Result.newResult(oauths)));

        PassportResponse.Exception exception = Mockito.mock(PassportResponse.Exception.class);
        Mockito.when(exception.getId()).thenReturn(-1L);
        Mockito.when(exception.getValue()).thenReturn("error");

        ru.yandex.market.sdk.userinfo.util.Result<List<AggregateUserInfo>, Error> result =
                ru.yandex.market.sdk.userinfo.util.Result.ofError(new PassportError(exception, "error"));

        Mockito.when(
                        userInfoService.getUserInfoRawAsync(
                                Mockito.eq(Collections.singleton(123L)),
                                any(Options.class))
                )
                .thenReturn(new FuturePipeImpl<>(Pipelines.startWithValue(result)));


        User user = Futures.waitAndGet(resolver.getUser(request, null));
    }

}
