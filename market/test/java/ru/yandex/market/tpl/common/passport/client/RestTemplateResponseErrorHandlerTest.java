package ru.yandex.market.tpl.common.passport.client;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.common.passport.client.exception.PassportClientCommonException;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ContextConfiguration(classes = {
        DefaultApplicationArguments.class,
        ApiClient.class,
        RestTemplate.class,
        PassportApiRestTemplateErrorHandler.class
})
@ExtendWith(SpringExtension.class)
@RestClientTest
public class RestTemplateResponseErrorHandlerTest {
    @Autowired
    private RestTemplateBuilder builder;

    private MockRestServiceServer server;
    private RestTemplate restTemplate;

    private final PassportApiRestTemplateErrorHandler restTemplateResponseErrorHandler =
            new PassportApiRestTemplateErrorHandler();

    @BeforeEach
    public void init() {
        restTemplate = builder
                .errorHandler(restTemplateResponseErrorHandler)
                .build();
        server = MockRestServiceServer.createServer(restTemplate);
        assertThat(server).isNotNull();
        assertThat(builder).isNotNull();
    }

    @Test
    public void testCommonExceptionHandler_PlaintextErrorMessage() {
        // given
        String errorBody = "Testing random error response message";
        mockServer(withStatus(BAD_REQUEST).body(errorBody));

        // when
        PassportClientCommonException exception = doCall();

        // then
        assertThat(exception.getMessage()).contains(BAD_REQUEST.toString());
        assertThat(exception.getMessage()).contains(errorBody);
    }

    @Test
    public void testCommonExceptionHandler_SingleStringError() {
        // given
        mockServer(withStatus(BAD_REQUEST)
                .body(getFileContent("errors/singleErrorStringResponse.json"))
                .contentType(MediaType.APPLICATION_JSON));

        // when
        PassportClientCommonException exception = doCall();

        // then
        assertThat(exception.getResponseCode()).isEqualTo(BAD_REQUEST.toString());
        assertThat(exception.getMessage()).isNotNull()
                .contains(BAD_REQUEST.toString())
                .contains("display_language.empty");
    }

    @Test
    public void testCommonExceptionHandler_SingleObjectError() {
        // given
        mockServer(withStatus(SERVICE_UNAVAILABLE)
                .body(getFileContent("errors/singleErrorObjectResponse.json"))
                .contentType(MediaType.APPLICATION_JSON));

        // when
        PassportClientCommonException exception = doCall();

        // then
        assertThat(exception.getResponseCode()).isEqualTo(SERVICE_UNAVAILABLE.toString());
        assertThat(exception.getMessage()).isNotNull()
                .contains(SERVICE_UNAVAILABLE.toString())
                .contains("RedisRequest")
                .contains("Redis request failed");
    }

    @Test
    public void testCommonExceptionHandler_PermissionError() {
        // given
        mockServer(withStatus(FORBIDDEN)
                .body(getFileContent("errors/permissionError.json"))
                .contentType(MediaType.APPLICATION_JSON));

        // when
        PassportClientCommonException exception = doCall();

        // then
        assertThat(exception.getResponseCode()).isEqualTo(FORBIDDEN.toString());
        assertThat(exception.getMessage()).isNotNull()
                .contains(FORBIDDEN.toString())
                .contains("access.denied")
                .contains("Required grants: ['phone_bundle.base']'");
    }

    @Test
    public void testCommonExceptionHandler_MultipleErrors() {
        // given
        mockServer(withStatus(BAD_REQUEST)
                .body(getFileContent("errors/multipleErrorStringResponse.json"))
                .contentType(MediaType.APPLICATION_JSON));

        // when
        PassportClientCommonException exception = doCall();

        // then
        assertThat(exception.getResponseCode()).isEqualTo(BAD_REQUEST.toString());
        assertThat(exception.getMessage()).isNotNull()
                .contains(BAD_REQUEST.toString())
                .contains("country.invalid")
                .contains("phone_number.invalid");
    }

    private PassportClientCommonException doCall() {
        return assertThrows(
                PassportClientCommonException.class,
                () -> restTemplate.postForObject("http://endpoint", null, String.class)
        );
    }

    private void mockServer(ResponseCreator responseCreator) {
        server.expect(ExpectedCount.once(), requestTo("http://endpoint"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(responseCreator);
    }

    @SneakyThrows
    private String getFileContent(String filename) {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(filename)), StandardCharsets.UTF_8);
    }
}
