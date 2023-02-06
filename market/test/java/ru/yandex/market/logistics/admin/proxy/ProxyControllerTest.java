package ru.yandex.market.logistics.admin.proxy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.request.trace.Module;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Unit-test ProxyController")
class ProxyControllerTest {

    private static final String SCHEME = "http";
    private static final String HOST = "localhost";
    private static final int PORT = 80;
    private static final String URI = String.format("%s://%s:%d", SCHEME, HOST, PORT);

    private RestTemplate restTemplate = mock(RestTemplate.class);
    private TvmTicketProvider tvmTicketProvider = mock(TvmTicketProvider.class);

    private final ProxyController proxyController = new TestableProxyController();
    private final ProxyController proxyControllerWOTvm = new TestableProxyControllerWOTvm();
    private final ProxyController proxyControllerWithRoleHeaders = new TestableProxyRolesHeaderController();

    ProxyControllerTest() {
        when(tvmTicketProvider.provideServiceTicket()).thenReturn("tvmTicketProvider-ServiceTicket");
        when(tvmTicketProvider.provideUserTicket()).thenReturn("tvmTicketProvider-provideUserTicket");
    }

    @Test
    @DisplayName("Проксирование получения сущности")
    void proxyForEntity() throws URISyntaxException {
        RequestEntity<?> request = defaultRequestEntity();
        proxyController.proxyForEntity(request, "/admin/lms/test/entity", null, String.class);

        verify(restTemplate).exchange(
            new URI(SCHEME, null, HOST, PORT, "/admin/lms/test/entity", "page=0&size=20", null),
            HttpMethod.GET,
            new HttpEntity<>(request.getBody(), enrichHeaders(request.getHeaders(), true, null)),
            String.class
        );
    }

    @Test
    @DisplayName("Проксирование получения сущности с указанием дополнительных параметров запроса")
    void proxyForEntityWithAdditionalQueryParams() throws URISyntaxException {
        RequestEntity<?> request = defaultRequestEntity();
        List<NameValuePair> additionalQueryParams = Arrays.asList(
            new BasicNameValuePair("param1", "value1"),
            new BasicNameValuePair("param2", "value2"),
            new BasicNameValuePair("param3", null)
        );

        proxyController.proxyForEntity(request, "/admin/lms/test/entity", additionalQueryParams, String.class);

        verify(restTemplate).exchange(
            new URI(
                SCHEME,
                null,
                HOST,
                PORT,
                "/admin/lms/test/entity",
                "page=0&size=20&param1=value1&param2=value2&param3",
                null
            ),
            HttpMethod.GET,
            new HttpEntity<>(request.getBody(), enrichHeaders(request.getHeaders(), true, null)),
            String.class
        );
    }

    @Test
    @DisplayName("Проксирование получения сущности (без TVM)")
    void proxyForEntityWOTvm() throws URISyntaxException {
        RequestEntity<?> request = defaultRequestEntity();
        proxyControllerWOTvm.proxyForEntity(request, "/admin/lms/test/entity", null, String.class);

        verify(restTemplate).exchange(
            new URI(SCHEME, null, HOST, PORT, "/admin/lms/test/entity", "page=0&size=20", null),
            HttpMethod.GET,
            new HttpEntity<>(request.getBody(), enrichHeaders(request.getHeaders(), false, null)),
            String.class
        );
    }

    @Test
    @DisplayName("Прокисирование ролей")
    void proxyRoleHeader() throws URISyntaxException {
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("SOME_ROLE"));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(null, null, authorities));
        RequestEntity<?> request = defaultRequestEntity();
        proxyControllerWithRoleHeaders.proxyForEntity(request, "/admin/lms/test/entity", null, String.class);

        verify(restTemplate).exchange(
                new URI(SCHEME, null, HOST, PORT, "/admin/lms/test/entity", "page=0&size=20", null),
                HttpMethod.GET,
                new HttpEntity<>(request.getBody(), enrichHeaders(request.getHeaders(), false, authorities)),
                String.class
        );
    }

    @Nonnull
    private RequestEntity<Object> defaultRequestEntity() throws URISyntaxException {
        return new RequestEntity<>(
            HttpMethod.GET,
            new URI(SCHEME, null, HOST, PORT, "/admin", "page=0&size=20", null)
        );
    }

    private HttpHeaders enrichHeaders(HttpHeaders headers, boolean withTvm, List<GrantedAuthority> roles) {
        HttpHeaders enrichedHeaders = new HttpHeaders();
        enrichedHeaders.putAll(headers);
        HashMap<String, String> map = new HashMap<>();
        map.put("Host", String.format("%s:%d", HOST, PORT));
        if (withTvm) {
            map.put("X-Ya-User-Ticket", "tvmTicketProvider-provideUserTicket");
            map.put("X-Ya-Service-Ticket", "tvmTicketProvider-ServiceTicket");
        }
        if (CollectionUtils.isNotEmpty(roles)) {
            String rolesString = roles.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
            map.put("X-Admin-Roles", rolesString);
        }
        enrichedHeaders.setAll(map);
        return enrichedHeaders;
    }

    private class TestableProxyController extends ProxyController {

        TestableProxyController() {
            super(URI, Module.LOGISTICS_MANAGEMENT_SERVICE, tvmTicketProvider);
        }

        protected RestTemplate buildRestTemplate(Module module) {
            return restTemplate;
        }
    }

    private class TestableProxyControllerWOTvm extends ProxyController {

        TestableProxyControllerWOTvm() {
            super(URI, Module.LOGISTICS_MANAGEMENT_SERVICE, null);
        }

        protected RestTemplate buildRestTemplate(Module module) {
            return restTemplate;
        }
    }

    private class TestableProxyRolesHeaderController extends ProxyController {
        TestableProxyRolesHeaderController() {
            super(URI, Module.LOGISTICS_MANAGEMENT_SERVICE, true, null);
        }

        @Override
        protected RestTemplate buildRestTemplate(Module module) {
            return restTemplate;
        }
    }
}
