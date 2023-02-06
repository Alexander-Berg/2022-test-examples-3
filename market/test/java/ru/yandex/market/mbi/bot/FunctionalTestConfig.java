package ru.yandex.market.mbi.bot;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.notification.telegram.bot.client.PartnerBotRestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.createOkResponse;

@Configuration
@Profile("functionalTest")
public class FunctionalTestConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer tgApiMock() {
        var tgApiMock = newWireMockServer();
        // Стабы описаны тут потому, что запросы выполняются при поднятии контекста
        tgApiMock.stubFor(any(urlPathEqualTo("/bottoken/deleteWebhook"))
                .willReturn(ok(createOkResponse())));
        tgApiMock.stubFor(any(urlPathMatching("/(.*)/setMyCommands"))
                .willReturn(ok(createOkResponse())));

        return tgApiMock;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer tgCallbackMock() {
        return newWireMockServer();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer mbiPartnerMock() {
        return newWireMockServer();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer mbiApiMock() {
        return newWireMockServer();
    }

    @Bean
    public RestTemplate partnerBotRestTemplate() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(List.of(
                new StringHttpMessageConverter(StandardCharsets.UTF_8),
                new MappingJackson2HttpMessageConverter(objectMapper)
        ));
        return restTemplate;
    }

    @Lazy
    @Bean
    public PartnerBotRestClient partnerBotRestClient(RestTemplate partnerBotRestTemplate,
                                                     @Value("${local.server.port}") int localServerPort) {
        return new PartnerBotRestClient(partnerBotRestTemplate, "http://localhost:" + localServerPort + "/");
    }

    private static WireMockServer newWireMockServer() {
        return new WireMockServer(new WireMockConfiguration().dynamicPort().notifier(new ConsoleNotifier(true)));
    }
}
