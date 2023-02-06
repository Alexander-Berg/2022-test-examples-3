package ru.yandex.mail.micronaut.micrometer;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.micronaut.micrometer.unistat.UnistatSlaConfiguration;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;

import static java.time.Duration.ofDays;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static net.javacrumbs.jsonunit.jsonpath.JsonPathAdapter.inPath;
import static org.assertj.core.api.Assertions.assertThat;

@Controller("/test")
class TestController {
    @Get("/dummy")
    public String foo() {
        return "foo";
    }

    @Post("/some/path")
    public int bar() {
        return 1;
    }
}

@Client("/")
interface TestClient {
    @Get("/test/dummy")
    String foo();

    @Post("/test/some/path")
    int bar();

    @Get("/unknown")
    String unknown();

    @Get("/unistat")
    @Produces(MediaType.TEXT_JSON)
    String unistat();
}

@MicronautTest
class MetricsTest {
    @Inject
    TestClient client;

    @Inject
    List<UnistatSlaConfiguration> slaConfigs;

    private static void assertIsSingleValueHistogram(Object element, String signal) {
        assertThatJson(inPath(element, "$[0]"))
            .isEqualTo(signal);
        assertThatJson(inPath(element, "$[1]"))
            .isArray()
            .anySatisfy(nested -> {
                assertThatJson(inPath(nested, "$[1]"))
                    .isEqualTo(1);
            });
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that micronaut-micrometer collects controller metrics")
    void httpMetricsTest() {
        assertThat(client.foo())
            .isEqualTo("foo");
        assertThat(client.bar())
            .isEqualTo(1);
        assertThat(client.unknown())
            .isNull();

        assertThatJson(client.unistat()).and(
            a -> a.node("")
        );

        assertThatJson(client.unistat())
            .isArray()
            .anySatisfy(elem -> assertIsSingleValueHistogram(elem, "http_server_requests_get_2xx_test_dummy_time_hgram"))
            .anySatisfy(elem -> assertIsSingleValueHistogram(elem, "http_server_requests_post_2xx_test_some_path_time_hgram"))
            .anySatisfy(elem -> assertIsSingleValueHistogram(elem, "http_client_requests_get_2xx_test_dummy_time_hgram"))
            .anySatisfy(elem -> assertIsSingleValueHistogram(elem, "http_client_requests_post_2xx_test_some_path_time_hgram"))
            .contains(
                json("[\"http_server_requests_4xx_not_found_count_summ\", 1]"),
                json("[\"http_client_requests_get_4xx_not_found_count_summ\", 1]"),

                json("[\"http_server_requests_get_2xx_test_dummy_count_summ\", 1]"),
                json("[\"http_server_requests_post_2xx_test_some_path_count_summ\", 1]"),

                json("[\"http_client_requests_get_2xx_test_dummy_count_summ\", 1]"),
                json("[\"http_client_requests_post_2xx_test_some_path_count_summ\", 1]")
            );
    }

    @Test
    @DisplayName("Verify that sla config parsing is correct")
    void testUnistatSlaConfig() {
        val expectedFixedConfig = new UnistatSlaConfiguration();
        expectedFixedConfig.setMetricName(Optional.of("^fixed"));
        expectedFixedConfig.setFixed(Optional.of(List.of(
            ofMillis(1), ofMillis(2), ofSeconds(1), ofDays(1)
        )));

        val generator = new UnistatSlaConfiguration.Generator();
        generator.setLeft(ofMillis(1));
        generator.setPivot(ofMillis(50));
        generator.setRight(ofMillis(500));
        generator.setTimeout(ofSeconds(1));
        val expectedGeneratedConfig = new UnistatSlaConfiguration();
        expectedGeneratedConfig.setTagKey(Optional.of("^key"));
        expectedGeneratedConfig.setTagValue(Optional.of("^value"));
        expectedGeneratedConfig.setGenerator(generator);

        assertThat(slaConfigs)
            .containsExactlyInAnyOrder(
                expectedFixedConfig,
                expectedGeneratedConfig
            );
    }
}
