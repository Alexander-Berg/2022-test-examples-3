import java.nio.charset.StandardCharsets;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.ApiClient;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.RestTemplateResponseErrorHandler;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.exception.TaxiVoiceGatewayBadGatewayException;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.exception.TaxiVoiceGatewayBadRequestException;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.exception.TaxiVoiceGatewayCommonException;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.BadGatewayErrorCode;
import ru.yandex.market.tpl.common.taxi.vgw.telephony.client.model.BadRequestErrorCode;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ContextConfiguration(classes = {
        DefaultApplicationArguments.class, ApiClient.class, RestTemplate.class, RestTemplateResponseErrorHandler.class
})
@ExtendWith(SpringExtension.class)
@RestClientTest
public class RestTemplateResponseErrorHandlerTest {
    @Autowired
    private RestTemplateBuilder builder;

    private MockRestServiceServer server;
    private RestTemplate restTemplate;

    private final RestTemplateResponseErrorHandler restTemplateResponseErrorHandler =
            new RestTemplateResponseErrorHandler();

    @BeforeEach
    public void init() {
        restTemplate = this.builder
                .errorHandler(restTemplateResponseErrorHandler)
                .build();
        server = MockRestServiceServer.createServer(restTemplate);
        Assertions.assertThat(this.server).isNotNull();
        Assertions.assertThat(this.builder).isNotNull();
    }

    @Test
    public void testCommonExceptionHandler() {
        String errorBody = "Testing random error response message";
        this.server
                .expect(ExpectedCount.once(), requestTo("/v1/forwardings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(BAD_REQUEST).body(errorBody));

        TaxiVoiceGatewayCommonException exception = assertThrows(
                TaxiVoiceGatewayCommonException.class,
                () -> restTemplate
                        .postForObject("/v1/forwardings", null, String.class)
        );

        Assertions.assertThat(exception.getMessage()).isEqualTo(BAD_REQUEST + " " + errorBody);
    }

    @Test
    public void testBadRequestExceptionHandler() {
        String errorBody = getFileContent("response/error/idempotencyConflictErrorResponse.json");
        this.server
                .expect(ExpectedCount.once(), requestTo("/v1/forwardings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(BAD_REQUEST).body(errorBody));

        TaxiVoiceGatewayBadRequestException exception = assertThrows(
                TaxiVoiceGatewayBadRequestException.class,
                () -> restTemplate
                        .postForObject("/v1/forwardings", null, String.class)
        );

        Assertions.assertThat(exception.getError()).isNotNull();
        Assertions.assertThat(exception.getError().getCode()).isEqualTo(BadRequestErrorCode.IDEMPOTENCYCONFLICT);
        Assertions.assertThat(exception.getError().getMessage()).isNotNull();
    }

    @Test
    public void testBadRequestRegionInNotSupportedExceptionHandler() {
        String errorBody = getFileContent("response/error/regionIsNotSupportedException.json");
        this.server
                .expect(ExpectedCount.once(), requestTo("/v1/forwardings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(BAD_REQUEST).body(errorBody));

        TaxiVoiceGatewayBadRequestException exception = assertThrows(
                TaxiVoiceGatewayBadRequestException.class,
                () -> restTemplate
                        .postForObject("/v1/forwardings", null, String.class)
        );

        Assertions.assertThat(exception.getError()).isNotNull();
        Assertions.assertThat(exception.getError().getCode()).isEqualTo(BadRequestErrorCode.REGIONISNOTSUPPORTED);
        Assertions.assertThat(exception.getError().getMessage()).isNotNull();
    }

    @Test
    public void testBadRequestInvalidJsonExceptionHandler() {
        String errorBody = getFileContent("response/error/idempotencyConflictErrorInvalidResponse.json");
        this.server
                .expect(ExpectedCount.once(), requestTo("/v1/forwardings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(BAD_REQUEST).body(errorBody));

        TaxiVoiceGatewayBadRequestException exception = assertThrows(
                TaxiVoiceGatewayBadRequestException.class,
                () -> restTemplate
                        .postForObject("/v1/forwardings", null, String.class)
        );

        Assertions.assertThat(exception.getCode()).isEqualTo(BadRequestErrorCode.IDEMPOTENCYCONFLICT.toString());
    }

    @Test
    public void testBadGatewayExceptionHandler() {
        String errorBody = getFileContent("response/error/partnerUnableToHandleErrorResponse.json");
        this.server
                .expect(ExpectedCount.once(), requestTo("/v1/forwardings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(BAD_REQUEST).body(errorBody));

        TaxiVoiceGatewayBadGatewayException exception = assertThrows(
                TaxiVoiceGatewayBadGatewayException.class,
                () -> restTemplate
                        .postForObject("/v1/forwardings", null, String.class)
        );

        Assertions.assertThat(exception.getError()).isNotNull();
        Assertions.assertThat(exception.getError().getCode()).isEqualTo(BadGatewayErrorCode.PARTNERUNABLETOHANDLE);
        Assertions.assertThat(exception.getError().getMessage()).isNotNull();
    }

    @Test
    public void testBadGatewayInvalidJsonExceptionHandler() {
        String errorBody = getFileContent("response/error/partnerUnableToHandleErrorInvalidResponse.json");
        this.server
                .expect(ExpectedCount.once(), requestTo("/v1/forwardings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(BAD_REQUEST).body(errorBody));

        TaxiVoiceGatewayBadGatewayException exception = assertThrows(
                TaxiVoiceGatewayBadGatewayException.class,
                () -> restTemplate
                        .postForObject("/v1/forwardings", null, String.class)
        );

        Assertions.assertThat(exception.getCode()).isEqualTo(BadGatewayErrorCode.PARTNERUNABLETOHANDLE.toString());
    }

    @SneakyThrows
    private String getFileContent(String filename) {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(filename)), StandardCharsets.UTF_8);
    }
}
