package ru.yandex.direct.api.v5.security.token;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.DisabledException;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.logging.ApiLogRecord;
import ru.yandex.direct.api.v5.security.DirectApiCredentials;
import ru.yandex.direct.api.v5.security.exception.AuthenticationServiceException;
import ru.yandex.direct.api.v5.security.exception.BadCredentialsException;
import ru.yandex.direct.api.v5.security.exception.NoRegistrationException;
import ru.yandex.direct.api.v5.security.exception.TokenAbsentOrHasInvalidFormatException;
import ru.yandex.direct.api.v5.security.internal.DirectApiInternalAuthRequest;
import ru.yandex.direct.core.entity.application.service.ApiAppAccessService;
import ru.yandex.direct.web.auth.blackbox.BlackboxOauthAuth;
import ru.yandex.direct.web.auth.blackbox.BlackboxOauthAuthProvider;
import ru.yandex.direct.web.auth.blackbox.exception.BadBlackboxCredentialsException;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.api.v5.security.token.DirectApiTokenAuthProvider.DIRECT_API_SCOPE;

public class DirectApiTokenAuthProviderTest {

    private static final String APPLICATION_ID = "aaaa";
    private static final String LOGIN = "login-login";
    private static final Long UID = 23423L;
    private static final String VALID_OAUTH_TOKEN = "valid"; // Валидный с точки зрения perl-ого Direct-а токен
    private static final String NOT_VALID_OAUTH_TOKEN = "$valid$"; // Невалидный с точки зрения perl-ого Direct-а токен

    @Mock
    private BlackboxOauthAuthProvider blackboxOauthAuthProvider;
    @Mock
    private PersistentTokenAuthProvider persistentTokenAuthProvider;
    @Mock
    private ApiAppAccessService apiAppAccessService;
    @Mock
    private DirectApiTokenAuthRequest tokenAuthRequest;
    @Mock
    private DirectApiCredentials requestInfo;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BlackboxOauthAuth blackboxOauthAuth;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PersistentTokenAuth persistentTokenAuth;

    @Mock
    private ApiContextHolder apiContextHolder;

    @InjectMocks
    private DirectApiTokenAuthProvider authProviderUnderTest;

    @Mock
    private ApiContext apiContext;

    @Mock
    private ApiLogRecord apiLogRecord;

    @Before
    public void setUp() {
        initMocks(this);

        when(blackboxOauthAuth.getOauthResponse().getOAuthInfoOptional())
                .thenReturn(Optional.of(new BlackboxOAuthInfo(APPLICATION_ID, "", Cf.list(DIRECT_API_SCOPE))));
        when(blackboxOauthAuth.getPrincipal()).thenReturn(LOGIN);
        when(blackboxOauthAuth.getUid()).thenReturn(UID);
        when(blackboxOauthAuthProvider.authenticate(any())).thenReturn(blackboxOauthAuth);

        when(persistentTokenAuth.getUid()).thenReturn(UID);
        when(persistentTokenAuth.getPrincipal()).thenReturn(LOGIN);
        when(persistentTokenAuth.getApplicationId()).thenReturn(APPLICATION_ID);
        when(persistentTokenAuthProvider.authenticate(any())).thenReturn(persistentTokenAuth);

        when(apiAppAccessService.checkApplicationAccess(APPLICATION_ID)).thenReturn(true);

        authProviderUnderTest = new DirectApiTokenAuthProvider(
                blackboxOauthAuthProvider, persistentTokenAuthProvider, apiAppAccessService, apiContextHolder
        );

        when(requestInfo.getOauthToken()).thenReturn(VALID_OAUTH_TOKEN);
        when(requestInfo.isTokenPersistent()).thenReturn(false);
        when(tokenAuthRequest.getCredentials()).thenReturn(requestInfo);

        apiContext = mock(ApiContext.class);
        apiLogRecord = mock(ApiLogRecord.class);
        when(apiContextHolder.get()).thenReturn(apiContext);
        when(apiContext.getApiLogRecord()).thenReturn(apiLogRecord);
    }

    @Test
    public void authenticateOauth() {
        DirectApiInternalAuthRequest res = authProviderUnderTest.authenticate(tokenAuthRequest);
        verify(blackboxOauthAuthProvider).authenticate(tokenAuthRequest);
        verify(apiAppAccessService).checkApplicationAccess(APPLICATION_ID);

        DirectApiInternalAuthRequest expected =
                new DirectApiInternalAuthRequest(requestInfo, UID, LOGIN, APPLICATION_ID, null);
        assertThat(res)
                .usingRecursiveComparison()
                .ignoringFields("credentials")
                .isEqualTo(expected);
    }

    @Test(expected = TokenAbsentOrHasInvalidFormatException.class)
    public void authenticateOauthFailsWhenTokenAbsent() {
        when(requestInfo.getOauthToken()).thenReturn("");
        authProviderUnderTest.authenticate(tokenAuthRequest);
    }

    @Test(expected = TokenAbsentOrHasInvalidFormatException.class)
    public void authenticateOauthFailsWhenTokenHasInvalidFormat() {
        when(requestInfo.getOauthToken()).thenReturn(NOT_VALID_OAUTH_TOKEN);
        authProviderUnderTest.authenticate(tokenAuthRequest);
    }

    @Test(expected = BadCredentialsException.class)
    public void authenticateOauthFailsWhenBlackboxThrowsDisabledException() {
        when(blackboxOauthAuthProvider.authenticate(any())).thenThrow(new DisabledException("xxx"));
        authProviderUnderTest.authenticate(tokenAuthRequest);
    }

    @Test(expected = BadCredentialsException.class)
    public void authenticateOauthFailsWhenBlackboxThrowsBadCredentialsException() {
        when(blackboxOauthAuthProvider.authenticate(any())).thenThrow(new BadCredentialsException("xxx"));
        authProviderUnderTest.authenticate(tokenAuthRequest);
    }

    @Test(expected = TokenAbsentOrHasInvalidFormatException.class)
    public void authenticateOauthFailsWhenBlackboxThrowsBadBlackboxCredentialsException() {
        when(blackboxOauthAuthProvider.authenticate(any())).thenThrow(new BadBlackboxCredentialsException());
        authProviderUnderTest.authenticate(tokenAuthRequest);
    }

    @Test(expected = AuthenticationServiceException.class)
    public void authenticateOauthFailsWhenBlackboxThrowsIllegalArgumentException() {
        when(blackboxOauthAuthProvider.authenticate(any())).thenThrow(new IllegalArgumentException("xxx"));
        authProviderUnderTest.authenticate(tokenAuthRequest);
    }

    @Test(expected = AuthenticationServiceException.class)
    public void authenticateOauthFailsWhenBlackboxThrowsException() {
        when(blackboxOauthAuthProvider.authenticate(any())).thenThrow(new RuntimeException("xxx"));
        authProviderUnderTest.authenticate(tokenAuthRequest);
    }

    @Test
    public void authenticateOauthFailsWhenAppHasWrongScopeThrowsException() {
        when(blackboxOauthAuth.getOauthResponse().getOAuthInfoOptional())
                .thenReturn(Optional.of(new BlackboxOAuthInfo(APPLICATION_ID, "", Cf.list("login:info"))));
        assertThatThrownBy(() -> authProviderUnderTest.authenticate(tokenAuthRequest))
                .isInstanceOf(BadCredentialsException.class);
        verify(apiLogRecord).withApplicationId(APPLICATION_ID);
    }

    @Test
    public void authenticateOauthFailsWhenAppHasNoAccessThrowsException() {
        when(apiAppAccessService.checkApplicationAccess(APPLICATION_ID)).thenReturn(false);
        assertThatThrownBy(() -> authProviderUnderTest.authenticate(tokenAuthRequest))
                .isInstanceOf(NoRegistrationException.class);
        verify(apiLogRecord).withApplicationId(APPLICATION_ID);
    }

    @Test
    public void authenticatePersistent() {
        when(requestInfo.isTokenPersistent()).thenReturn(true);
        DirectApiInternalAuthRequest res = authProviderUnderTest.authenticate(tokenAuthRequest);
        verify(persistentTokenAuthProvider).authenticate(tokenAuthRequest);
        verify(apiAppAccessService, never()).checkApplicationAccess(APPLICATION_ID);

        DirectApiInternalAuthRequest expected =
                new DirectApiInternalAuthRequest(requestInfo, UID, LOGIN, APPLICATION_ID, null);
        assertThat(res)
                .usingRecursiveComparison()
                .ignoringFields("credentials")
                .isEqualTo(expected);
    }
}
