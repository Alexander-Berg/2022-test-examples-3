package ru.yandex.market.mbo.cms.auth;


import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class BlackBoxAuthorizationImplTest {

    public static final String DUMMY_TVM_TICKET = "dummy_ticket";
    BlackBoxAuthorizationImpl blackBoxAuthorization;
    HttpClient httpClient;
    ArgumentCaptor<HttpGet> httpClientReqCaptor;
    TvmTicketManager tvmTicketManager;

    private static final long UID = 44296711;

    private static final String MOCK_ANSWER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<doc>\n" +
        "<age>81268</age>\n" +
        "<error>OK</error>\n" +
        "<status id=\"0\">VALID</status>\n" +
        "<uid hosted=\"0\">44296711</uid>\n" +
        "<login>Commince</login>\n" +
        "<regname>Commince</regname>\n" +
        "<display_name>\n" +
        "<name>Vasya.Pupkin</name>\n" +
        "<avatar>\n" +
        "<default>57243/enc-17f14d543f8b784dcaa453d0e62fad3ce50db31f62efe661ead6e6ffc36b2f27</default>\n" +
        "<empty>0</empty>\n" +
        "</avatar>\n" +
        "</display_name>\n" +
        "<auth>\n" +
        "<secure>1</secure>\n" +
        "</auth>\n" +
        "</doc>\n";

    @Before
    public void init() throws IOException {
        tvmTicketManager = Mockito.mock(TvmTicketManager.class);
        Mockito.when(tvmTicketManager.getTvmTicket()).thenReturn(DUMMY_TVM_TICKET);
        blackBoxAuthorization = new BlackBoxAuthorizationImpl(tvmTicketManager, "dummy");
        httpClient = Mockito.mock(HttpClient.class);
        blackBoxAuthorization.httpClient = httpClient;

        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.getEntity()).thenReturn(new StringEntity(MOCK_ANSWER));
        httpClientReqCaptor = ArgumentCaptor.forClass(HttpGet.class);
        Mockito.when(httpClient.execute(httpClientReqCaptor.capture())).thenReturn(response);
    }

    @Test
    public void testTvmTicketExistence() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(request.getCookies()).then((Answer<Cookie[]>) invocation -> {
            Cookie[] cookies = new Cookie[2];
            cookies[0] = new Cookie(BlackBoxAuthorization.SESSION_ID_COOKIE, "dummy");
            cookies[1] = new Cookie(BlackBoxAuthorization.SESSION_ID_2_COOKIE, "dummy");

            return cookies;
        });

        blackBoxAuthorization.authorize(request, response);
        Assert.assertEquals(DUMMY_TVM_TICKET,
            httpClientReqCaptor.getValue().getFirstHeader(BlackBoxAuthorization.X_YA_SERVICE_TICKET_HEADER).getValue());
    }

    @Test
    public void testFindUser() {
        AuthenticatedUser user = blackBoxAuthorization.findUser(UID);
        Assert.assertNotNull(user);
    }
}
