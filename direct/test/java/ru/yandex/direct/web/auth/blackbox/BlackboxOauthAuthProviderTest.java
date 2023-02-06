package ru.yandex.direct.web.auth.blackbox;

import java.util.Optional;

import com.google.common.net.InetAddresses;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.web.auth.blackbox.exception.BadBlackboxCredentialsException;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox.protocol.BlackboxClientException;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthException;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthStatus;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;
import ru.yandex.misc.ip.IpAddress;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BlackboxOauthAuthProviderTest {

    private static final String SERVICE_HOST = "direct.yandex.ru";
    private static final String USER_IP = "195.230.1.10";
    private static final String OAUTH_TOKEN = "0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f";
    private static final String LOGIN = "login";
    private static final long UID = 911;
    private static final String TVM_TICKET = "tvm_ticket";

    private BlackboxOauthAuthProvider testingProvider;

    private BlackboxOauthCredentials credentials;
    private BlackboxOauthAuthRequest authRequest;

    private BlackboxClient blackboxClient;

    private BlackboxResponseBuilder responseBuilder;

    @Before
    public void prepare() throws BlackboxClientException {
        credentials = new BlackboxOauthCredentials(SERVICE_HOST, InetAddresses.forString(USER_IP), OAUTH_TOKEN);

        authRequest = mock(BlackboxOauthAuthRequest.class);
        when(authRequest.getTvmTicket()).thenReturn(TVM_TICKET);
        when(authRequest.getCredentials()).thenReturn(credentials);

        responseBuilder = new BlackboxResponseBuilder(BlackboxMethod.OAUTH)
                .setStatus(BlackboxOAuthStatus.VALID.value())
                .setLogin(Optional.of(LOGIN))
                .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(UID), PassportDomain.YANDEX_RU)));

        blackboxClient = mock(BlackboxClient.class);

        when(blackboxClient.oAuth(
                /* userIp = */ any(IpAddress.class),
                /* token = */ any(),
                /* dbFields = */ anyList(),
                /* attributes = */ anyList(),
                /* emails = */ eq(Optional.empty()),
                /* aliases = */ eq(Optional.empty()),
                /* getUserTicket = */ anyBoolean(),
                /* tvmTicket = */ any())).thenAnswer(i -> responseBuilder.build().getOrThrow());

        testingProvider = new BlackboxOauthAuthProvider(blackboxClient);
    }

    @Test
    public void callsBlackboxQueryableOauth() {
        testingProvider.authenticate(authRequest);
        verify(blackboxClient)
                .oAuth(
                        /* userIp = */ IpAddress.parse(USER_IP),
                        /* token = */ OAUTH_TOKEN,
                        /* dbFields = */ emptyList(),
                        /* attributes = */ emptyList(),
                        /* emails = */ Optional.empty(),
                        /* aliases = */ Optional.empty(),
                        /* getUserTicket = */ true,
                        /* tvmTicket = */ TVM_TICKET);
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
    public void failsWhenOauthTokenIsNull() {
        credentials = new BlackboxOauthCredentials(SERVICE_HOST, InetAddresses.forString(USER_IP), null);
        BlackboxAuthRequest blackboxAuthRequest = doReturn(credentials).when(authRequest);
        blackboxAuthRequest.getCredentials();
        testingProvider.authenticate(authRequest);
    }

    @Test(expected = BadBlackboxCredentialsException.class)
    public void failsWhenIncompleteCredentialsProvided() {
        credentials = new BlackboxOauthCredentials(null, null, null);
        when(authRequest.getCredentials()).thenReturn(credentials);
        testingProvider.authenticate(authRequest);
    }

    @Test(expected = RuntimeException.class)
    public void failsWhenNullOauthResponse() throws BlackboxClientException {
        when(blackboxClient.oAuth(
                /* userIp = */ any(IpAddress.class),
                /* token = */ any(),
                /* dbFields = */ anyList(),
                /* attributes = */ anyList(),
                /* emails = */ eq(Optional.empty()),
                /* aliases = */ eq(Optional.empty()),
                /* getUserTicket = */ anyBoolean(),
                /* tvmTicket = */ any())).thenReturn(null);
        testingProvider.authenticate(authRequest);
    }

    @Test(expected = CredentialsExpiredException.class)
    public void throwsBadCredentialsExceptionWhenBlackboxStatusInvalid() {
        when(blackboxClient.oAuth(
                /* userIp = */ any(IpAddress.class),
                /* token = */ any(),
                /* dbFields = */ anyList(),
                /* attributes = */ anyList(),
                /* emails = */ eq(Optional.empty()),
                /* aliases = */ eq(Optional.empty()),
                /* getUserTicket = */ anyBoolean(),
                /* tvmTicket = */ any())).thenAnswer(i -> {
            throw new BlackboxOAuthException(BlackboxOAuthStatus.INVALID, "", "");
        });
        testingProvider.authenticate(authRequest);
    }

    @Test(expected = DisabledException.class)
    public void throwsDisabledExceptionExceptionWhenBlackboxStatusDisabled() {
        when(blackboxClient.oAuth(
                /* userIp = */ any(IpAddress.class),
                /* token = */ any(),
                /* dbFields = */ anyList(),
                /* attributes = */ anyList(),
                /* emails = */ eq(Optional.empty()),
                /* aliases = */ eq(Optional.empty()),
                /* getUserTicket = */ anyBoolean(),
                /* tvmTicket = */ any())).thenAnswer(i -> {
            throw new BlackboxOAuthException(BlackboxOAuthStatus.DISABLED, "", "");
        });
        testingProvider.authenticate(authRequest);
    }

}


