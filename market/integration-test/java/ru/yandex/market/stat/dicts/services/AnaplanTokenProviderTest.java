package ru.yandex.market.stat.dicts.services;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.stat.dicts.config.DictionariesITestConfig;
import ru.yandex.market.stat.dicts.integration.help.SpringDataProviderRunner;
import ru.yandex.market.stat.dicts.loaders.anaplan.AnaplanApiV2Test;
import ru.yandex.market.stat.dicts.loaders.anaplan.AnaplanTokenProvider;
import ru.yandex.market.stat.dicts.loaders.anaplan.CertificateInfo;
import ru.yandex.market.stat.dicts.loaders.anaplan.ThrowingCallableForToken;
import ru.yandex.market.stats.test.config.LocalPostgresInitializer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ActiveProfiles("integration-tests")
@RunWith(SpringDataProviderRunner.class)
@Slf4j
@ContextConfiguration(classes = DictionariesITestConfig.class, initializers = LocalPostgresInitializer.class)
public class AnaplanTokenProviderTest {

    private static final String MYTOKEN = "mytoken-long-token1";
    private static final String MYTOKEN2 = "mytoke-long-token2";
    private static final String USERNAME1 = "user1";
    private static final String USERNAME2 = "user2";
    private static final String USERNAME3 = "user3";
    private static final String USERNAME4 = "user4";
    private static final String PASSWORD = "v";
    private static final String KEY_STORE_FILE = "/test_anaplan_keystore.pkcs12";
    private static final String ANAPLAN_CERT_ALIAS = "testanaplancerts";
    private static final String ANAPLAN_KEY_PASS = "test_password";
    private static final String KEY_STORE_PASSWORD = "test_password";

    @Autowired
    private MetadataService metadataService;
    @Autowired
    private CuratorFramework curatorFramework;

    private AnaplanTokenProvider anaplanTokenProvider;

    @Mock
    private CloseableHttpClient anaplanHttpClient;
    @Mock
    private CloseableHttpResponse response;
    private AnaplanTokenProvider anotherAnaplanTokenProvider;

    @Before
    public void setup() {
        anaplanTokenProvider = getAnaplanTokenProvider(10);
        anotherAnaplanTokenProvider = getAnaplanTokenProvider(5);
    }

    @Test
    public void testGoToAuthOnceByPAss() throws IOException {
        givenTokenResponseFromAuth(MYTOKEN);
        String token = anaplanTokenProvider.getToken(USERNAME1, PASSWORD);
        assertThat(token, is(MYTOKEN));
        verify(anaplanHttpClient).execute(any(HttpPost.class));

        token = anaplanTokenProvider.getToken(USERNAME1, PASSWORD);
        assertThat(token, is(MYTOKEN));
        verifyNoMoreInteractions(anaplanHttpClient);
    }

    @Test
    public void testGoToAuthOnceByCert() throws IOException {
        givenTokenResponseFromAuth(MYTOKEN);

        String token = anaplanTokenProvider.getToken(getTestCertInfo());
        assertThat(token, is(MYTOKEN));
        verify(anaplanHttpClient).execute(any(HttpPost.class));

        token = anaplanTokenProvider.getToken(getTestCertInfo());
        assertThat(token, is(MYTOKEN));
        verifyNoMoreInteractions(anaplanHttpClient);

        anaplanTokenProvider.freeToken(getTestCertInfo().getAlias());
        anaplanTokenProvider.freeToken(getTestCertInfo().getAlias());
        assertFalse("Token should be  freed by now!", metadataService.hasUsedToken(getTestCertInfo().getAlias(), 10));
    }

    @Test
    public void testChangeTokenMultyThreading() throws IOException, InterruptedException {
        givenTokenResponseFromAuth(MYTOKEN);
        thenTokenIs(USERNAME2, MYTOKEN);
        thenTokenIsMarkedAsUsed(USERNAME2);

        givenTokenResponseFromAuth(MYTOKEN2);
        Thread grumpyThread =
                whenAnotherThreadTriesToResetToken(USERNAME2);
        thenNoNewRequestToAuthIsSent();

        whenTokenIsFreedTwice(USERNAME2);
        thenNewRequestToAuthIsSentFinalyEEEEE(grumpyThread);
        thenTokenIs(USERNAME2, MYTOKEN2);

        verifyNoMoreInteractions(anaplanHttpClient);
    }


    @Test
    public void testTooMuchNotUsed() throws IOException {
        givenTokenResponseFromAuth(MYTOKEN);
        String token = anaplanTokenProvider.getToken(USERNAME4, PASSWORD);
        assertThat(token, is(MYTOKEN));

        thenTokenIsMarkedAsUsed(USERNAME4);

        whenTokenIsFreedTwice(USERNAME4);
        whenTokenIsFreedTwice(USERNAME4);
        thenTokenIsMarkedAsNotUsed(USERNAME4);
    }

    @Test
    public void testSimultaneousRequestForToken() throws IOException, InterruptedException {
        givenTokenResponseFromAuth(MYTOKEN);
        whenTwoThreadsAskForToken(USERNAME3);
        thenAuthApiWasCalledOnce();
        thenTokenIsMarkedAsUsed(USERNAME3);

        thenTokenIs(USERNAME3, MYTOKEN);
    }

    @Test
    public void testExecuteWithToken() throws IOException {
        givenTokenResponseFromAuth(MYTOKEN);
        List<String> tokenholder = new ArrayList<>();
        anaplanTokenProvider.executeWithToken("test1", PASSWORD, (ThrowingCallableForToken<Object>) tokenholder::add);
        assertThat(tokenholder.size(), is(1));
        assertThat(tokenholder.get(0), is(MYTOKEN));
        assertFalse("Token should be  freed by now!", metadataService.hasUsedToken("test1", 10));
    }

    @Test
    public void testExecuteWithTokenForCert() throws IOException {
        givenTokenResponseFromAuth(MYTOKEN);
        List<String> tokenholder = new ArrayList<>();
        anaplanTokenProvider.executeWithToken(getTestCertInfo(), (ThrowingCallableForToken<Object>) tokenholder::add);
        assertThat(tokenholder.size(), is(1));
        assertThat(tokenholder.get(0), is(MYTOKEN));
        assertFalse("Token should be  freed by now!", metadataService.hasUsedToken(getTestCertInfo().getAlias(), 10));
    }

    private void givenTokenResponseFromAuth(String mytoken) throws IOException {
        when(response.getStatusLine()).thenReturn(statusLineWithCode());
        BasicHttpEntity value = new BasicHttpEntity();
        value.setContent(IOUtils.toInputStream("{\"tokenInfo\" : {\"tokenValue\" : \"" + mytoken + "\"}}",
                Charset.forName(
                        "UTF-8")));
        when(response.getEntity()).thenReturn(value);

        when(anaplanHttpClient.execute(any(HttpPost.class))).thenReturn(response);
    }

    private void whenTwoThreadsAskForToken(String username) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            anaplanTokenProvider.getToken(username, PASSWORD);
        });

        Thread t2 = new Thread(() -> {
            anaplanTokenProvider.getToken(username, PASSWORD);
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }


    private void whenTokenIsFreedTwice(String username) {
        anotherAnaplanTokenProvider.freeToken(username);
        anotherAnaplanTokenProvider.freeToken(username);
    }

    private Thread whenAnotherThreadTriesToResetToken(String username) throws InterruptedException {
        //этот считает, что токен истекает,тк проверяет ttl-5 =0
        Thread t3 = new Thread(() -> {
            anotherAnaplanTokenProvider.getToken(username, PASSWORD);
        });
        t3.start();
        TimeUnit.SECONDS.sleep(10);
        return t3;
    }

    private void thenNoNewRequestToAuthIsSent() throws IOException {
        verify(anaplanHttpClient, times(1)).execute(any(HttpPost.class));
    }


    private void thenTokenIsMarkedAsUsed(String username) {
        assertTrue(metadataService.hasUsedToken(username, 10));
    }

    private void thenTokenIsMarkedAsNotUsed(String username) {
        assertFalse(metadataService.hasUsedToken(username, 10));
    }

    private void thenAuthApiWasCalledOnce() throws IOException {
        verify(anaplanHttpClient, times(1)).execute(any(HttpPost.class));
        verifyNoMoreInteractions(anaplanHttpClient);
    }


    private void thenNewRequestToAuthIsSentFinalyEEEEE(Thread grumpyThread) throws IOException, InterruptedException {
        grumpyThread.join();
        verify(anaplanHttpClient, times(2)).execute(any(HttpPost.class));

    }

    private void thenTokenIs(String username, String mytoken) {
        String token = anaplanTokenProvider.getToken(username, PASSWORD);
        assertThat(token, is(mytoken));
    }

    @SneakyThrows
    private CertificateInfo getTestCertInfo() {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(AnaplanApiV2Test.class.getResourceAsStream(KEY_STORE_FILE), KEY_STORE_PASSWORD.toCharArray());
        return new CertificateInfo(keyStore, ANAPLAN_CERT_ALIAS, ANAPLAN_KEY_PASS);
    }


    private StatusLine statusLineWithCode() {
        return new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return null;
            }

            @Override
            public int getStatusCode() {
                return 200;
            }

            @Override
            public String getReasonPhrase() {
                return null;
            }
        };
    }

    private AnaplanTokenProvider getAnaplanTokenProvider(int ttl_minutes) {
        return new AnaplanTokenProvider(metadataService, anaplanHttpClient, curatorFramework, ttl_minutes,
                "/market/dictionaries-yt/dev/anaplan_token");
    }


}
