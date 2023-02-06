package ru.yandex.direct.web.auth;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import ru.yandex.direct.web.auth.blackbox.PassportUrls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BlackboxUtilsTest {

    @Test
    public void getAuthUrl_operateWithUnexpectedServerNameInRequest() {
        String wrongServerName = "localhost";
        HttpServletRequest request = mockRequest(wrongServerName);
        String actual = PassportUrls.AUTH.build(request);
        assertThat(actual).startsWith("https://passport.yandex.com/auth");
    }

    private HttpServletRequest mockRequest(String serverName) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn(serverName);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://" + serverName));
        return request;
    }

}
