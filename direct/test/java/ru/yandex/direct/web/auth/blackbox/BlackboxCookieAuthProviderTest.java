package ru.yandex.direct.web.auth.blackbox;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.web.auth.blackbox.exception.BadBlackboxCredentialsException;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox.protocol.BlackboxClientException;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthStatus;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxSessionIdException;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxSessionIdException.BlackboxSessionIdStatus;
import ru.yandex.misc.ip.IpAddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BlackboxCookieAuthProviderTest {

    private static final String SERVICE_HOST = "direct.yandex.ru";
    private static final String USER_IP = "195.230.1.10";
    private static final String SESSION_ID = "01234";
    private static final String SSL_SESSION_ID = "0123456789";
    private static final String LOGIN = "login";
    private static final long UID = 911;
    private static final String TVM_TICKET = "tvm_ticket";

    private BlackboxCookieAuthProvider testingProvider;

    private BlackboxCookieCredentials credentials;
    private BlackboxCookieAuthRequest authRequest;

    private BlackboxClient blackboxClient;

    private BlackboxResponseBuilder responseBuilder;

    @Before
    public void prepare() throws BlackboxClientException {
        credentials = new BlackboxCookieCredentials(SERVICE_HOST, USER_IP, SESSION_ID, SSL_SESSION_ID);

        authRequest = mock(BlackboxCookieAuthRequest.class);
        when(authRequest.getTvmTicket()).thenReturn(TVM_TICKET);
        when(authRequest.getCredentials()).thenReturn(credentials);

        responseBuilder = new BlackboxResponseBuilder(BlackboxMethod.SESSION_ID)
                .setStatus(BlackboxOAuthStatus.VALID.value())
                .setLogin(Optional.of(LOGIN))
                .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(UID), PassportDomain.YANDEX_RU)));

        blackboxClient = mock(BlackboxClient.class);

        when(blackboxClient.sessionId(
                any(IpAddress.class),
                any(),
                any(),
                eq(null),
                eq(false),
                ArgumentMatchers.any(),
                anyBoolean(),
                any())).thenAnswer(i -> responseBuilder.build().getOrThrow());


        testingProvider = new BlackboxCookieAuthProvider(blackboxClient);
    }

    @Test
    public void callsBlackboxQueryableOauth() {
        testingProvider.authenticate(authRequest);
        verify(blackboxClient)
                .sessionId(IpAddress.parse(USER_IP),
                        SESSION_ID,
                        SERVICE_HOST,
                        null,
                        false,
                        Optional.of(SSL_SESSION_ID),
                        true,
                        TVM_TICKET);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWhenNullAuthenticationRequestProvided() {
        testingProvider.authenticate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWhenNullCredentialsProvided() {
        when(authRequest.getCredentials()).thenReturn(null);
        testingProvider.authenticate(authRequest);
    }

    @Test(expected = BadBlackboxCredentialsException.class)
    public void failsWhenSessionIdIsNull() {
        credentials = new BlackboxCookieCredentials(SERVICE_HOST, USER_IP, null, SSL_SESSION_ID);
        doReturn(credentials).when(authRequest).getCredentials();
        testingProvider.authenticate(authRequest);
    }

    @Test(expected = BadBlackboxCredentialsException.class)
    public void failsWhenIncompleteCredentialsProvided() {
        credentials = new BlackboxCookieCredentials(null, null, null, null);
        when(authRequest.getCredentials()).thenReturn(credentials);
        testingProvider.authenticate(authRequest);
    }

    @Test(expected = RuntimeException.class)
    public void failsWhenNullOauthResponse() throws BlackboxClientException {
        when(blackboxClient.sessionId(
                any(IpAddress.class),
                any(),
                any(),
                eq(null),
                eq(false),
                ArgumentMatchers.any(),
                anyBoolean(),
                any())).thenReturn(null);
        testingProvider.authenticate(authRequest);
    }

    @Test(expected = CredentialsExpiredException.class)
    public void throwsBadCredentialsExceptionWhenBlackboxStatusInvalid() {
        when(blackboxClient.sessionId(
                any(IpAddress.class),
                any(),
                any(),
                eq(null),
                eq(false),
                ArgumentMatchers.any(),
                anyBoolean(),
                any())).thenAnswer(i -> {
            throw new BlackboxSessionIdException(BlackboxSessionIdStatus.INVALID, "", "");
        });
        testingProvider.authenticate(authRequest);
    }

    @Test(expected = DisabledException.class)
    public void throwsDisabledExceptionExceptionWhenBlackboxStatusDisabled() {
        when(blackboxClient.sessionId(
                any(IpAddress.class),
                any(),
                any(),
                eq(null),
                eq(false),
                ArgumentMatchers.any(),
                anyBoolean(),
                any())).thenAnswer(i -> {
            throw new BlackboxSessionIdException(BlackboxSessionIdStatus.DISABLED, "", "");
        });
        testingProvider.authenticate(authRequest);
    }

}
