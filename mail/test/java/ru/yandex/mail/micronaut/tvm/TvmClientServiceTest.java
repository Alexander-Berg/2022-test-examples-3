package ru.yandex.mail.micronaut.tvm;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.micronaut.tvm.auth.TvmSecured;
import ru.yandex.mail.micronaut.tvm.auth.TvmUid;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.mail.micronaut.tvm.Constants.TVM_USER_TICKET_HEADER;
import static ru.yandex.mail.micronaut.tvm.TvmClientServiceTest.TEST_ENV;
import static ru.yandex.mail.micronaut.tvm.TvmToolClientTest.USER_MAIN_UID;
import static ru.yandex.mail.micronaut.tvm.TvmToolClientTest.USER_TICKET;

@TvmSecured
@Client(id = SelfClient.ID)
@Requires(env = TEST_ENV)
interface SelfClient {
    String ID = "self";

    @Get("/test/ping")
    String ping();

    @Get("/test/user")
    String user(@Header(TVM_USER_TICKET_HEADER) String userTicket);
}

@Client(id = UnauthorizedSelfClient.ID)
interface UnauthorizedSelfClient {
    String ID = "self-unauthorized";

    @Get("/test/ping")
    String ping();

    @Get("/test/user")
    String user(@QueryValue long uid);
}

@Controller("/test")
@Requires(env = TEST_ENV)
class SelfController {
    @TvmSecured
    @Get("/ping")
    public String ping() {
        return "pong";
    }

    @TvmSecured
    @Get("/user")
    public String user(TvmUid uid) {
        return "Hi, " + uid;
    }
}

@MicronautTest(environments = {Environment.TEST, TEST_ENV})
class TvmClientServiceTest {
    static final String TEST_ENV = "client-service-test";

    @Inject
    SelfClient selfClient;

    @Inject
    UnauthorizedSelfClient unauthorizedSelfClient;

    private static boolean isUnauthorized(Throwable e) {
        if (!(e instanceof HttpClientResponseException)) {
            return false;
        }

        val ex = (HttpClientResponseException) e;
        return ex.getStatus() == HttpStatus.UNAUTHORIZED;
    }

    @Test
    @DisplayName("Verify that request with service tvm ticket returns expected value")
    void testRequestWithServiceTicket() {
        assertThat(selfClient.ping()).isEqualTo("pong");
    }

    @Test
    @DisplayName("Verify that request without service tvm ticket returns error")
    void testRequestWithoutServiceTicket() {
        assertThatThrownBy(() -> unauthorizedSelfClient.ping())
            .isInstanceOf(HttpClientResponseException.class)
            .matches(TvmClientServiceTest::isUnauthorized);
    }

    @Test
    @DisplayName("Verify that request with required user tvm ticket returns expected value")
    void testRequestWithUserTicket() {
        assertThat(selfClient.user(USER_TICKET))
            .isEqualTo("Hi, " + USER_MAIN_UID);
    }

    @Test
    @DisplayName("Verify that request without required user tvm ticket returns error")
    void testRequestWithoutUserTicket() {
        assertThatThrownBy(() -> unauthorizedSelfClient.user(USER_MAIN_UID))
            .isInstanceOf(HttpClientResponseException.class)
            .matches(TvmClientServiceTest::isUnauthorized);

    }
}
