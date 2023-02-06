package ru.yandex.calendar.test.generic;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import javax.naming.ConfigurationException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.calendar.tvm.TvmClient;
import ru.yandex.calendar.tvm.TvmFirewall;
import ru.yandex.calendar.tvm.TvmHeaders;
import ru.yandex.calendar.tvm.TvmManager;
import ru.yandex.calendar.tvm.TvmServiceResponse;
import ru.yandex.calendar.tvm.TvmUserResponse;
import ru.yandex.mail.tvmlocal.BinarySandboxSource;
import ru.yandex.mail.tvmlocal.BinarySource;
import ru.yandex.mail.tvmlocal.TvmTool;
import ru.yandex.mail.tvmlocal.options.Mode;
import ru.yandex.mail.tvmlocal.options.ResourceConfigLocation;
import ru.yandex.mail.tvmlocal.options.TvmToolOptions;
import ru.yandex.passport.tvmauth.TicketStatus;

import static java.util.Collections.emptyMap;

@lombok.Value
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
class TicketResponse {
    long tvmId;
    String ticket;
}

@lombok.Value
@AllArgsConstructor(onConstructor_= @JsonCreator)
class CheckServiceTicketData {
    int src;
}

@lombok.Value
@AllArgsConstructor(onConstructor_= @JsonCreator)
class CheckUserTicketData {
    long[] uids;
}

class HttpTvmToolClient implements TvmClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final TypeReference<Map<String, TicketResponse>> TICKETS_RESPONSE_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String toolAuthToken;
    private final HttpClient client;

    private static boolean is2xx(HttpResponse response) {
        return (response.statusCode() / 100) == 2;
    }

    @SneakyThrows
    private static void checkIsSuccess(HttpResponse response) {
        if (!is2xx(response)) {
            throw new IOException("Tvmtool is inaccessible, status code = " + response.statusCode() +
                ", body = " + response.body().toString());
        }
    }

    @SneakyThrows
    private <T> T parseBody(HttpResponse<String> response, Class<T> bodyType) {
        return objectMapper.readValue(response.body(), bodyType);
    }

    @SneakyThrows
    private <T> T parseBody(HttpResponse<String> response, TypeReference<T> bodyType) {
        return objectMapper.readValue(response.body(), bodyType);
    }

    @SneakyThrows
    private HttpRequest request(String uri, Map<String, String> headers) {
        val builder = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(uri))
            .header("Authorization", toolAuthToken)
            .timeout(TIMEOUT);

        headers.forEach(builder::header);
        return builder.build();
    }

    private HttpRequest request(String uri) {
        return request(uri, emptyMap());
    }

    @SneakyThrows
    private HttpResponse<String> exec(HttpRequest request) {
        return client.send(request, BodyHandlers.ofString());
    }

    private <T> T exec(HttpRequest request, TypeReference<T> responseType) {
        val response = exec(request);
        checkIsSuccess(response);
        return parseBody(response, responseType);
    }

    public HttpTvmToolClient(String host, int port, String toolAuthToken) {
        this.objectMapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.baseUrl = "http://" + host + ':' + port;
        this.toolAuthToken = toolAuthToken;
        this.client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(TIMEOUT)
            .build();
    }

    @Override
    @SneakyThrows
    public String getServiceTicketFor(int clientId) {
        val response = exec(request(baseUrl + "/tvm/tickets?dsts=" + clientId), TICKETS_RESPONSE_TYPE);
        return response.values()
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid tvm client id " + clientId))
            .getTicket();
    }

    @Override
    public TvmServiceResponse checkServiceTicket(String ticketBody) {
        val response = exec(request(baseUrl + "/tvm/checksrv", Map.of(TvmHeaders.SERVICE_TICKET, ticketBody)));
        if (!is2xx(response)) {
            return new TvmServiceResponse(TicketStatus.EXPIRED, Optional.empty());
        } else {
            val data = parseBody(response, CheckServiceTicketData.class);
            return new TvmServiceResponse(TicketStatus.OK, Optional.of(data.getSrc()));
        }
    }

    @Override
    public TvmUserResponse checkUserTicket(String ticketBody) {
        val response = exec(request(baseUrl + "/tvm/checkusr", Map.of(TvmHeaders.USER_TICKET, ticketBody)));
        if (!is2xx(response)) {
            return new TvmUserResponse(TicketStatus.EXPIRED, new long[0]);
        } else {
            val data = parseBody(response, CheckUserTicketData.class);
            return new TvmUserResponse(TicketStatus.OK, data.getUids());
        }
    }
}

@Configuration
public class TvmClientTestConfiguration {
    private static final BinarySource TVMTOOL_SOURCE = new BinarySandboxSource();
    private static final String CONFIG = "tvmtool.conf";
    private static final String CALENDAR_SECRET_ENV = "CALENDAR_TVMTOOL_TOKEN";
    private static final String CALENDAR_SECRET_PROPERTY = "calendar.tvmtool.token";
    private static final String AUTH_TOKEN = TvmToolOptions.generateAuthToken();

    @SneakyThrows
    private static String readTokenFromFile(String path) {
        return Files.readString(Path.of(path));
    }

    @SneakyThrows
    @Bean(destroyMethod = "stop")
    public TvmTool getTvmTool(@Value("${tvmtool.token.path:-}") String tvmSecretTokenPathFromEnv,
                              @Value("${" + CALENDAR_SECRET_PROPERTY + ":-}") String tvmSecretTokenFromConfig) {
        if (tvmSecretTokenPathFromEnv.isBlank() && tvmSecretTokenFromConfig.isBlank()) {
            throw new ConfigurationException("Tvm secret token not found. Set  CALENDAR_TVMTOOL_TOKEN_PATH environment property, "
                + "or set " + CALENDAR_SECRET_PROPERTY + " configuration property");
        }

        val token = (tvmSecretTokenPathFromEnv.isBlank()) ? tvmSecretTokenFromConfig : readTokenFromFile(tvmSecretTokenPathFromEnv);
        val env = Map.of(
            CALENDAR_SECRET_ENV, token
        );
        val options = new TvmToolOptions(OptionalInt.empty(), new ResourceConfigLocation(CONFIG), Mode.REAL, env, AUTH_TOKEN);
        return TvmTool.start(TVMTOOL_SOURCE, options);
    }

    @Bean
    public TvmClient getTvmClient(TvmTool tvmtool) {
        // NOTE: test parameter java.net.preferIPv6Addresses=true breaks 'localhost' resolve, so lets use raw ip address
        return new HttpTvmToolClient("127.0.0.1", tvmtool.getPort(), AUTH_TOKEN);
    }

    @Bean
    public TvmFirewall tvmFirewall() {
        return new TvmFirewall.AllowAny();
    }

    @Bean
    public TvmManager tvmManager() {
        return new TvmManager();
    }
}
