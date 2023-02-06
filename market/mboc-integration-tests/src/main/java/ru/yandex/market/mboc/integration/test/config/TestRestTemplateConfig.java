package ru.yandex.market.mboc.integration.test.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.application.properties.utils.Environments;

/**
 * @author s-ermakov
 */
@Configuration
public class TestRestTemplateConfig {

    @Value("${environment}")
    String environment;

    @Value("${mboc.integration-test.root-uri}")
    String rootUri;

    @Value("${mboc.integration-test.user-agent}")
    String integrationTestUserAgent;

    @Primary
    @Bean
    TestRestTemplate testRestTemplate() {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder()
            .rootUri(rootUri)
            .interceptors(
                (request, body, execution) -> {
                    // для локали и дева устанавливаем любую куку, так как этого требует бекенд
                    if (Environments.isLocalOrDevelopment(environment)) {
                        request.getHeaders().add("Cookie", "foo=bar");
                    }
                    request.getHeaders().add(HttpHeaders.USER_AGENT, integrationTestUserAgent);
                    return execution.execute(request, body);
                });

        return new TestRestTemplate(restTemplateBuilder, null, null,
            TestRestTemplate.HttpClientOption.ENABLE_COOKIES,
            TestRestTemplate.HttpClientOption.SSL);
    }
}
