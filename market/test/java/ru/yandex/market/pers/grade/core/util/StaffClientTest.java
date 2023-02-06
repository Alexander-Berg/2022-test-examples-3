package ru.yandex.market.pers.grade.core.util;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import ru.yandex.market.pers.grade.core.MockedTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.pers.grade.client.mock.HttpClientMockHelpers.mockResponseWithFile;
import static ru.yandex.market.pers.grade.client.mock.HttpClientMockHelpers.withQuery;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 09.07.2020
 */
public class StaffClientTest extends MockedTest {
    private static final String RESPONSE_FILE = "/data/staff.api.users.json";
    public static final String OAUTH_TOKEN = "test-token";

    private final HttpClient httpClient = mock(HttpClient.class);
    private final StaffClient staffClient = new StaffClient("http://localhost", OAUTH_TOKEN,
        CommonUtils.jsonRestTemplate(httpClient));

    @Test
    public void testParse() throws Exception {
        mockResponseWithFile(httpClient, HttpStatus.SC_OK, RESPONSE_FILE);
        StaffClient.StaffPersonResponse response = staffClient.getPersonsPage(1, 2);
        assertEquals(118, StaffClient.extractExternalLogins(response).size());
        assertEquals(541, response.pages);
    }

    @Test
    public void testGetLoginsFromEmail() throws Exception {
        assertEquals("kukabara", StaffClient.getLoginFromEmail("kukabara@ya.kz"));
        assertEquals("kukabara", StaffClient.getLoginFromEmail("kukabara@yandex.ru"));
        assertEquals("kukabara@yandex-team.ru", StaffClient.getLoginFromEmail("kukabara@yandex-team.ru"));
        assertEquals("kukabara@gmail.com", StaffClient.getLoginFromEmail("kukabara@gmail.com"));
        assertNull(StaffClient.getLoginFromEmail("invalid e-mail@ya"));
    }

    @Test
    public void testInternalLoginsFromEmail() throws Exception {
        assertFalse(staffClient.isInternalLogin("somelogin"));
        assertTrue(staffClient.isInternalLogin("yndx-somelogin"));
        assertTrue(staffClient.isInternalLogin("yndx.somelogin"));
        assertTrue(staffClient.isInternalLogin("YnDx.somelogin"));
        assertTrue(staffClient.isInternalLogin("yndx-YnDx.somelogin"));
        assertTrue(staffClient.isInternalLogin("yndx-YdDx.somelogin"));
    }

    @Test
    public void testPersonRequestFast() throws Exception {
        // check yndx-* login calls staff client

        mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/staff.single.json",
            withQuery("/v3/persons", ".*?_query=yandex.login==\"yndx-somelogin\".*?")
        );

        assertEquals("testlogin", staffClient.getPersonByExternalLoginFast("yndx-somelogin").orElse(null));
        verify(httpClient, times(1)).execute(any(HttpUriRequest.class), any(HttpContext.class));

        mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/staff.single.json",
            withQuery("/v3/persons", ".*?_query=yandex.login==\"test-login\".*?")
        );

        mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/staff.empty.json",
            withQuery("/v3/persons", ".*?_query=yandex.login==\"other-login\".*?")
        );

        assertEquals("testlogin", staffClient.getPersonByExternalLoginFast("test-login").orElse(null));
        assertNull(staffClient.getPersonByExternalLoginFast("other-login").orElse(null));

        verify(httpClient, times(3)).execute(any(HttpUriRequest.class), any(HttpContext.class));
    }

    @Test
    public void testPersonRequestAccurate() throws Exception {
        // check yndx-* login calls staff client (regular + internal)
        mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/staff.empty.json",
            withQuery("/v3/persons", ".*?_query=yandex.login==\"yndx-somelogin\".*?")
        );
        mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/staff.single.json",
            withQuery("/v3/persons", ".*?login=somelogin.*?")
        );

        assertEquals("testlogin", staffClient.getPersonByExternalLoginAccurate("yndx-somelogin").orElse(null));
        verify(httpClient, times(2)).execute(any(HttpUriRequest.class), any(HttpContext.class));

        // check extract with accurate search
        mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/staff.single.json",
            withQuery("/v3/persons", ".*?_query=yandex.login==\"test-login\".*?")
        );

        assertEquals("testlogin", staffClient.getPersonByExternalLoginAccurate("test-login").orElse(null));
        verify(httpClient, times(3)).execute(any(HttpUriRequest.class), any(HttpContext.class));

        // check extract by login regex
        mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/staff.empty.json",
            withQuery("/v3/persons", ".*?_query=yandex.login==\"test-login\".*?")
        );

        mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/staff.single.json",
            withQuery("/v3/persons", ".*?_query=yandex.login==regex\\(\"\\^test\\[-\\\\\\.\\]login\\$\",\"i\"\\).*?")
        );

        assertEquals("testlogin", staffClient.getPersonByExternalLoginAccurate("test-login").orElse(null));
        verify(httpClient, times(5)).execute(any(HttpUriRequest.class), any(HttpContext.class));

        // check email search
        mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/staff.empty.json",
            withQuery("/v3/persons", ".*?_query=yandex.login==\"test-login\".*?")
        );

        mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/staff.empty.json",
            withQuery("/v3/persons", ".*?_query=yandex.login==regex\\(\"\\^test\\[-\\\\\\.\\]login\\$\",\"i\"\\).*?")
        );

        mockResponseWithFile(httpClient, HttpStatus.SC_OK,
            "/data/staff.single.json",
            withQuery("/v3/persons", ".*?_query=emails.address==regex\\(\"\\^test\\[-\\\\\\.\\]login" +
                "@ya\\(ndex\\)\\?\\\\\\.\\(ru\\|ua\\|by\\|com\\|kz\\)\\$\",\"i\"\\).*?")
        );

        assertEquals("testlogin", staffClient.getPersonByExternalLoginAccurate("test-login").orElse(null));
        verify(httpClient, times(8)).execute(any(HttpUriRequest.class), any(HttpContext.class));
    }
}
