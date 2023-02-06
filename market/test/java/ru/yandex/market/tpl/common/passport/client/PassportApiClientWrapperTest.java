package ru.yandex.market.tpl.common.passport.client;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.common.passport.client.exception.ErrorEntry;
import ru.yandex.market.tpl.common.passport.client.exception.PassportClientCommonException;
import ru.yandex.market.tpl.common.passport.client.exception.PassportClientErrorException;
import ru.yandex.market.tpl.common.passport.client.model.CreateTrackRequest;
import ru.yandex.market.tpl.common.passport.client.model.CreateTrackResponse;
import ru.yandex.market.tpl.common.passport.client.model.DisplayLanguage;
import ru.yandex.market.tpl.common.passport.client.model.FormattedPhoneNumber;
import ru.yandex.market.tpl.common.passport.client.model.PhoneConfirmCommitRequest;
import ru.yandex.market.tpl.common.passport.client.model.PhoneConfirmCommitResponse;
import ru.yandex.market.tpl.common.passport.client.model.PhoneConfirmMethod;
import ru.yandex.market.tpl.common.passport.client.model.PhoneConfirmSubmitRequest;
import ru.yandex.market.tpl.common.passport.client.model.PhoneConfirmSubmitResponse;
import ru.yandex.market.tpl.common.passport.client.model.Status;
import ru.yandex.market.tpl.common.passport.client.model.TrackType;
import ru.yandex.market.tpl.common.passport.client.model.ValidatePhoneNumberRequest;
import ru.yandex.market.tpl.common.passport.client.model.ValidatePhoneNumberResponse;
import ru.yandex.passport.tvmauth.TvmClient;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "external.passport.url=http://api",
        "external.passport.tvmServiceId=123",
        "external.passport.connectTimeoutMillis=100",
        "external.passport.readTimeoutMillis=100",
        "external.passport.maxConnTotal=1"
})
@AutoConfigureMockRestServiceServer
@RestClientTest
@ContextConfiguration(classes = PassportClientConfiguration.class)
public class PassportApiClientWrapperTest {

    @Autowired
    private RestTemplate passportRestTemplate;

    @Autowired
    private PassportApiClient passportClient;

    @MockBean
    private TvmClient tvmClient;

    private MockRestServiceServer mockServer;

    private final String defaultConsumer = "consumer";
    private final ClientInfo defaultClientInfo = new ClientInfo("ip", "scheme", "agent");
    private final FormattedPhoneNumber defaultFormattedPhoneNumber = new FormattedPhoneNumber()
            .international("+7 916 123-45-67")
            .e164("+79161234567")
            .original("+79161234567")
            .maskedInternational("+7 916 123-**-**")
            .maskedE164("+7916123****")
            .maskedOriginal("+7916123****");

    @BeforeEach
    void init() {
        mockServer = MockRestServiceServer.bindTo(passportRestTemplate).build();
    }

    @Test
    void testPostCreateTrackOk() {
        // given
        mockOkResponse(makeConsumerUrl("http://api/1/track/"), getFileContent("createTrackOkResponse.json"));
        var request = new CreateTrackRequest()
                .trackType(TrackType.REGISTER);
        var expected = new CreateTrackResponse()
                .status(Status.OK)
                .id("some-track-id");

        // when
        var actual = passportClient.createTrack(defaultConsumer, defaultClientInfo, request);

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testPostValidatePhoneNumberOk() {
        // given
        mockOkResponse(
                makeConsumerUrl("http://api/1/bundle/validate/phone_number/"),
                getFileContent("validatePhoneNumberOkResponse.json")
        );
        var request = new ValidatePhoneNumberRequest()
                .trackId("id")
                .phoneNumber("+79161234567")
                .validateForCall(true);
        var expected = new ValidatePhoneNumberResponse()
                .status(Status.OK)
                .phoneNumber(defaultFormattedPhoneNumber)
                .validForCall(true)
                .validForFlashCall(false);

        // when
        var actual = passportClient.validatePhoneNumber(defaultConsumer, defaultClientInfo, request);

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testPostPhoneConfirmSubmitOk() {
        // given
        mockOkResponse(
                makeConsumerUrl("http://api/1/bundle/phone/confirm/submit/"),
                getFileContent("phoneConfirmSubmitOkResponse.json")
        );
        var request = new PhoneConfirmSubmitRequest()
                .displayLanguage(DisplayLanguage.RU)
                .confirmMethod(PhoneConfirmMethod.FLASH_CALL)
                .number("+79161234567");
        var expected = new PhoneConfirmSubmitResponse()
                .status(Status.OK)
                .trackId("some-track-id")
                .number(defaultFormattedPhoneNumber)
                .denyResendUntil(1234L);

        // when
        var actual = passportClient.phoneConfirmSubmit(defaultConsumer, defaultClientInfo, request);

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testPostPhoneConfirmCommitOk() {
        // given
        mockOkResponse(
                makeConsumerUrl("http://api/1/bundle/phone/confirm/commit/"),
                getFileContent("phoneConfirmCommitOkResponse.json")
        );
        var request = new PhoneConfirmCommitRequest()
                .trackId("some-track-id")
                .code("123456");
        var expected = new PhoneConfirmCommitResponse()
                .status(Status.OK)
                .number(defaultFormattedPhoneNumber);

        // when
        var actual = passportClient.phoneConfirmCommit(defaultConsumer, defaultClientInfo, request);

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testErrorResponseWithOkStatus() {
        // given
        mockErrorResponse(
                getFileContent("errors/singleErrorStringResponse.json")
        );
        var error = ErrorEntry.create("display_language.empty");

        // when
        List<Runnable> calls = List.of(
                () -> passportClient.phoneConfirmCommit(defaultConsumer, defaultClientInfo,
                        new PhoneConfirmCommitRequest().code("").trackId("")),
                () -> passportClient.phoneConfirmSubmit(defaultConsumer, defaultClientInfo,
                        new PhoneConfirmSubmitRequest().displayLanguage(DisplayLanguage.RU).number("")),
                () -> passportClient.validatePhoneNumber(defaultConsumer, defaultClientInfo,
                        new ValidatePhoneNumberRequest().phoneNumber(""))
        );
        for (var call : calls) {
            var exception = assertThrows(PassportClientErrorException.class, call::run);

            // then
            assertThat(exception.getError()).isEqualTo(error);
            assertThat(exception.getErrors()).asList().containsExactly(error);
            assertThat(exception.getResponseCode()).isEqualTo(HttpStatus.OK.toString());
            assertThat(exception.getMessage()).isNotNull()
                    .contains(HttpStatus.OK.toString())
                    .contains("display_language.empty");
        }
    }

    @Test
    void testMultipleErrorsResponseWithOkStatus() {
        // given
        mockErrorResponse(
                getFileContent("errors/multipleErrorStringResponse.json")
        );
        var request = new ValidatePhoneNumberRequest()
                .phoneNumber("some-track-id");
        var expectedError = List.of(
                ErrorEntry.create("country.invalid"),
                ErrorEntry.create("phone_number.invalid")
        );

        // when
        var exception = assertThrows(PassportClientCommonException.class,
                () -> passportClient.validatePhoneNumber(defaultConsumer, defaultClientInfo, request));

        // then
        assertThat(exception.getResponseCode()).isEqualTo(HttpStatus.OK.toString());
        assertThat(exception.getErrors()).asList().containsExactlyInAnyOrderElementsOf(expectedError);
        assertThat(exception.getMessage()).isNotNull()
                .contains(HttpStatus.OK.toString())
                .contains("country.invalid")
                .contains("phone_number.invalid");
    }

    private void mockOkResponse(String url, String body) {
        mockServer
                .expect(ExpectedCount.once(), requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Ya-Consumer-Client-Ip", "ip"))
                .andExpect(header("Ya-Consumer-Client-Scheme", "scheme"))
                .andExpect(header("Ya-Client-User-Agent", "agent"))
                .andRespond(withStatus(HttpStatus.OK)
                        .body(body)
                        .contentType(MediaType.parseMediaType("application/json"))
                );
    }

    private void mockErrorResponse(String body) {
        mockServer
                .expect(ExpectedCount.manyTimes(), anything())
                .andRespond(withStatus(HttpStatus.OK)
                        .body(body)
                        .contentType(MediaType.parseMediaType("application/json"))
                );
    }

    private String makeConsumerUrl(String url) {
        return url + "?consumer=" + defaultConsumer;
    }

    @SneakyThrows
    private String getFileContent(String filename) {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(filename)), StandardCharsets.UTF_8);
    }
}
