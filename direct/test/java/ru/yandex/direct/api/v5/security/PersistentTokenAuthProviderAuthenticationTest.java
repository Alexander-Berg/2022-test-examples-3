package ru.yandex.direct.api.v5.security;

import java.net.InetAddress;

import com.google.common.net.InetAddresses;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.direct.api.v5.security.exception.BadCredentialsException;
import ru.yandex.direct.api.v5.security.token.DirectApiTokenAuthRequest;
import ru.yandex.direct.api.v5.security.token.PersistentTokenAuth;
import ru.yandex.direct.api.v5.security.token.PersistentTokenAuthProvider;
import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.api.v5.units.UseOperatorUnitsMode.FALSE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class PersistentTokenAuthProviderAuthenticationTest {

    private static final String APPLICATION_ID = "test-application-id";
    private static final String LOGIN = "test-login";
    private static final Long UID = 10L;
    private static final String PERSISTENT_TOKEN = "877ccc670f9b9b7ef54e56ec287689c8b0ca0f77fb08e0b37216bc799e600296";
    private static final InetAddress INET_ADDRESS = InetAddresses.forString("12.12.12.12");
    private static final String TVM_TICKET = "tvm_ticket";

    @Mock(serializable = true)
    private NetAcl netAcl;

    @Mock(serializable = true)
    private ShardHelper shardHelper;

    private PersistentTokenAuthProvider authProvider;

    @Before
    public void setUp() {
        initMocks(this);

        when(shardHelper.getUidByLogin(anyString()))
                .then(x -> UID);
        when(netAcl.isIpInNetworks(any(InetAddress.class), anyCollection()))
                .then(x -> true);

        authProvider = new PersistentTokenAuthProvider(shardHelper, netAcl,
                singletonList("classpath:///test-persistent-token.conf"));
    }

    @Test
    public void testPersistentTokenAuth() {
        PersistentTokenAuth persistentTokenAuth = tryAuth(PERSISTENT_TOKEN, LOGIN);
        assertThat(persistentTokenAuth).is(matchedBy(
                authMatcher(INET_ADDRESS, PERSISTENT_TOKEN, LOGIN, UID, APPLICATION_ID)));
    }

    private PersistentTokenAuth tryAuth(String token, String login) {
        return authProvider.authenticate(createInternalAuthRequest(INET_ADDRESS, token, login));
    }

    private DirectApiTokenAuthRequest createInternalAuthRequest(InetAddress fromIP, String token, String login) {
        DirectApiCredentials directApiCredentials =
                new DirectApiCredentials("api.direct.yandex.ru", fromIP,
                        token, login, null,
                        FALSE, true);

        return new DirectApiTokenAuthRequest(directApiCredentials, TVM_TICKET);
    }

    private BeanDifferMatcher<PersistentTokenAuth> authMatcher(InetAddress inetAddress, String token, String login, Long uid, String appId) {
        PersistentTokenAuth expectedAuth =
                new PersistentTokenAuth(new DirectApiCredentials("api.direct.yandex.ru", inetAddress, token, login, null, FALSE, true), uid, login, appId);

        return beanDiffer(expectedAuth).useCompareStrategy(
                allFields()
                        .forFields(newPath("applicationId")).useMatcher(equalTo(expectedAuth.getApplicationId()))
                        .forFields(newPath("login")).useMatcher(equalTo(expectedAuth.getCredentials().getClientLogin()))
        );
    }

    @Test(expected = BadCredentialsException.class)
    public void shouldThrowBadCredentialsExceptionIfTokenNotFound() {
        DirectApiTokenAuthRequest nonExistentToken = mock(DirectApiTokenAuthRequest.class, Answers.RETURNS_DEEP_STUBS);

        when(nonExistentToken.getCredentials().getOauthToken())
                .thenReturn(randomToken());

        authProvider.authenticate(nonExistentToken);
    }

    @Test(expected = BadCredentialsException.class)
    public void shouldThrowBadCredentialsExceptionIfTokenIsFromAnotherNetwork() {
        DirectApiTokenAuthRequest tokenOutOfNetwork = mock(DirectApiTokenAuthRequest.class, Answers.RETURNS_DEEP_STUBS);

        when(tokenOutOfNetwork.getCredentials().getOauthToken())
                .thenReturn(randomToken());
        when(netAcl.isIpInNetworks(any(InetAddress.class), anyCollection()))
                .then(x -> false);

        authProvider.authenticate(tokenOutOfNetwork);
    }

    private static String randomToken() {
        return RandomStringUtils.randomAlphanumeric(64);
    }

}
