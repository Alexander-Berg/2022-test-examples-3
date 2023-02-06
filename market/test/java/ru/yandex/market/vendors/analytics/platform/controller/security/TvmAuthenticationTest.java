package ru.yandex.market.vendors.analytics.platform.controller.security;

import java.util.Collections;
import java.util.Optional;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.RestTemplateFactory;
import ru.yandex.market.vendors.analytics.core.security.TvmAuthenticationManager;
import ru.yandex.market.vendors.analytics.core.security.TvmProperties;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.passport.tvmauth.TvmClient;

@ContextConfiguration(classes = {TvmAuthenticationTest.StaticTestingConfig.class})
@DbUnitDataSet(before = "TvmAuthenticationTest.before.csv")
class TvmAuthenticationTest extends FunctionalTest {

    private static final String HEADER_SERVICE_TICKET = "X-Ya-Service-Ticket";
    private static final String HEADER_USER_TICKET = "X-Ya-User-Ticket";
    private static final Long USER_ID = 1L;
    private static final Long CLIENT_ID = 100L;
    private static final Long MANAGER_UID = 1000L;
    private static final String[] UNSECURED_METHODS = new String[]{"/shops/applications/**"};
    private static final String[] ALLOWED_CLIENT_IDS = new String[]{"100"};

    private static final RestTemplate REST_TEMPLATE = RestTemplateFactory.createRestTemplate();

    @Test
    @DisplayName("Аутентификация не проверяется, если для пути настроено исключение (unsecured_methods)")
    void authenticationIgnoresUnsecuredPaths() {
        String actual = getApplications(USER_ID, Optional.empty(), Optional.empty());
        var expected = loadFromFile("TvmAuthenticationTest.getApplications.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Аутентификация провеяет тикеты пользователя и сервиса")
    void authenticationPassedWithTickets() {
        String actualR = getDomains(USER_ID, Optional.of(CLIENT_ID), Optional.of(MANAGER_UID));
        var expectedR = loadFromFile("TvmAuthenticationTest.getDomains.response.json");
        JsonAssert.assertJsonEquals(expectedR, actualR);
    }

    @Test
    @DisplayName("Аутентификация проверят client_id по списку разрешенных клиентов")
    void authenticationValidatesClientId() {
        final Long clientId = 200L;

        HttpClientErrorException.BadRequest exception = Assertions.assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> getDomains(USER_ID, Optional.of(clientId), Optional.of(MANAGER_UID))
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        var expected = "400 Bad Request: [\"ClientId not allowed: " + clientId + "\"\r\n]";
        Assertions.assertEquals(expected, exception.getMessage());
    }

    @Test
    @DisplayName("Аутентификация проверяет тикет сервиса")
    void authenticationValidatesServiceTicket() {
        HttpClientErrorException.BadRequest exception = Assertions.assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> getDomains(USER_ID, Optional.empty(), Optional.empty())
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        var expected = "400 Bad Request: [\"Service ticket is not present\"\r\n]";
        Assertions.assertEquals(expected, exception.getMessage());
    }

    @Test
    @DisplayName("Аутентификация проверяет тикет пользователя")
    void authenticationValidatesUserTicket() {
        HttpClientErrorException.BadRequest exception = Assertions.assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> getDomains(USER_ID, Optional.of(CLIENT_ID), Optional.empty())
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        var expected = "400 Bad Request: [\"User ticket is not present\"\r\n]";
        Assertions.assertEquals(expected, exception.getMessage());
    }


    private String getApplications(long uid, Optional<Long> clientId, Optional<Long> managerUid) {
        String url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("shops", "applications")
                .queryParam("uid", uid)
                .toUriString();

        return get(url, clientId, managerUid);
    }

    private String getDomains(long uid, Optional<Long> clientId, Optional<Long> managerUid) {
        String url = UriComponentsBuilder.fromUriString(baseUrl())
                .pathSegment("shops", "domains")
                .queryParam("uid", uid)
                .toUriString();

        return get(url, clientId, managerUid);
    }

    private String get(String url, Optional<Long> clientId, Optional<Long> managerUid) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        clientId.ifPresent(aLong -> headers.set(HEADER_SERVICE_TICKET, aLong.toString()));
        managerUid.ifPresent(aLong -> headers.set(HEADER_USER_TICKET, aLong.toString()));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return REST_TEMPLATE.exchange(url, HttpMethod.GET, entity, String.class).getBody();
    }

    @TestConfiguration
    static public class StaticTestingConfig {
        @Autowired
        private TvmClient tvmClient;

        @Bean
        @Primary
        public AuthenticationManager authenticationManager() {
            return new TvmAuthenticationManager(tvmClient, tvmProperties());
        }

        @Bean
        @Primary
        public TvmProperties tvmProperties() {
            return new TvmProperties(true, ALLOWED_CLIENT_IDS, UNSECURED_METHODS);
        }
    }
}
