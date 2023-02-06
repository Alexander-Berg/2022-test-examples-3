package ru.yandex.market.tsum.clients.huawei.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.Consts;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.huawei.http.FakeResponseBuilder;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthTokenProviderTest {

    private static final String CLIENT_ID = "clientId";
    private static final String DOMAIN = "domain";
    private static final String CLIENT_SECRET = "clientSecret";

    @Captor
    private ArgumentCaptor<HttpPost> captor;
    @Mock
    private HttpClient httpClient;
    @InjectMocks
    private AuthTokenProvider authTokenProvider;

    @Test
    public void ok() throws AuthenticationException, IOException {
        when(httpClient.execute(any())).thenReturn(FakeResponseBuilder.create().status(200)
            .content("{\"access_token\":\"returned_token\",\"expires_in\":172800}")
            .build());

        String token = authTokenProvider.obtainToken(DOMAIN, CLIENT_ID, CLIENT_SECRET);
        assertEquals("returned_token", token);

        verify(httpClient).execute(captor.capture());
        BufferedReader br = new BufferedReader(new InputStreamReader(captor.getValue().getEntity().getContent(),
            Consts.UTF_8));
        JsonObject jsonObject = new Gson().fromJson(br, JsonObject.class);
        assertEquals(CLIENT_ID, jsonObject.get("client_id").getAsString());
        assertEquals(CLIENT_SECRET, jsonObject.get("client_secret").getAsString());
        assertEquals("client_credentials", jsonObject.get("grant_type").getAsString());
    }

    @Test(expected = AuthenticationException.class)
    public void fail() throws AuthenticationException, IOException {
        when(httpClient.execute(any())).thenReturn(FakeResponseBuilder.create().status(403).emptyJsonContent().build());
        authTokenProvider.obtainToken(DOMAIN, CLIENT_ID, CLIENT_SECRET);
    }
}
