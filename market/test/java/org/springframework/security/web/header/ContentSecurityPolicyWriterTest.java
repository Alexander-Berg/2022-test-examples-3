package org.springframework.security.web.header;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 24.05.2019
 */
@RunWith(MockitoJUnitRunner.class)
public class ContentSecurityPolicyWriterTest {

    private static final String DIRECTIVES = "object-src 'none'";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private SecurityContext securityContext;

    @Before
    public void setUp() {
        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void writeHeader() {
        ContentSecurityPolicyWriter writer = new ContentSecurityPolicyWriter("http://host",
            "test-unit-service", DIRECTIVES);

        writer.writeHeaders(request, response);

        verify(response).setHeader("Content-Security-Policy",
            "report-uri http://host?from=test-unit-service&yandex_login=not-authenticated; object-src 'none'");
    }

    @Test
    public void writeHeaderLoggedIn() {
        ContentSecurityPolicyWriter writer = new ContentSecurityPolicyWriter("http://host",
            "test-unit-service", DIRECTIVES);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("robot-mrk-clab-itest");
        securityContext.setAuthentication(authentication);

        writer.writeHeaders(request, response);

        verify(response).setHeader("Content-Security-Policy",
            "report-uri http://host?from=test-unit-service&yandex_login=robot-mrk-clab-itest; object-src 'none'");
    }


    @Test
    public void writeHeaderReportOnly() {
        ContentSecurityPolicyWriter writer = new ContentSecurityPolicyWriter("http://host",
            "test-unit-service", DIRECTIVES);
        writer.setReportOnly(true);

        writer.writeHeaders(request, response);

        verify(response).setHeader("Content-Security-Policy-Report-Only",
            "report-uri http://host?from=test-unit-service&yandex_login=not-authenticated; object-src 'none'");
    }

    @Test
    public void dontWriteHeaderOnEmptyHost() {
        ContentSecurityPolicyWriter writer = new ContentSecurityPolicyWriter("",
            "test-unit-service", DIRECTIVES);
        writer.setReportOnly(true);

        writer.writeHeaders(request, response);

        verify(response, never()).setHeader(any(), any());
    }
}
